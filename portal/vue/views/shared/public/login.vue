<!--
 This file is part of the Open MIDATA Server.
 
 The Open MIDATA Server is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 any later version.
 
 The Open MIDATA Server is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
-->

<template>
	<div class="welcome">
		<div id="mainheader"><div class="container"><div class="row">
			<!-- Login -->
			<div class="col-sm-12 d-none d-md-block" style="margin-top:30px;margin-bottom:30px;font-family:Sans-serif;color:white">

                <div v-show="!notPublic" style="margin-bottom:40px"></div>
              
                <h1 style="color:white;">{{ $t('member_login.teaser') }}</h1>
                <div v-show="notPublic">
                    <div v-t="'member_login.info1'"></div>
                    <div v-t="'member_login.info2'"></div>
                    <div v-t="'member_login.info3'"></div>             
                </div>
			    <router-link :to="{ path : './registration', query : {actions:actions} }" style="margin-top:10px;margin-bottom:40px;" class="btn btn-primary" v-show="!(offline||notPublic)" v-t="'member_login.register_now'"></router-link> 
			</div>
		</div></div>
		</div><div class="container"><div class="row">
			<div class="col-sm-12">
                <panel :title="$t('member_login.sign_in')+getAppName()" style="max-width:330px; padding-top:30px; margin:0 auto;">                    
				    <div class="alert alert-info" v-if="serviceLogin && !app">
		                <strong class="alert-heading" v-t="'login.service_login'"></strong>                 
                        <div v-if="!app" v-t="'login.service_login2'"></div>                                                     
                    </div>
                            
                    <div v-if="app" class="extraspace">
                        <center><img v-if="hasIcon()" :src="getIconUrl()"></center>																        
                    </div>
		            	
			        <error-box :error="error">
			            <span v-if="error.code == 'error.invalid.credentials_hint'">
			            	    
			            	<router-link class="alert-link" to="public_research.login" v-t="'enum.userrole.RESEARCH'"></router-link>,
			                <router-link class="alert-link" to="public_provider.login" v-t="'enum.userrole.PROVIDER'"></router-link>
			                <span v-t="'common.or'"></span>
			                <router-link class="alert-link" to="public_developer.login" v-t="'enum.userrole.DEVELOPER'"></router-link>
			                ?		
			            </span>                           
                    </error-box>
	                        
	                <p v-if="offline" class="alert alert-danger" v-t="'error.offline'"></p>
					<form name="myform" ref="myform" class="form" @submit.prevent="dologin()" role="form" v-if="!offline">
						<div class="form-group">
							<input type="email" class="form-control" :placeholder="$t('login.email_address')" required v-validate v-model="login.email" style="margin-bottom:5px;" autofocus>
							<password class="form-control" :placeholder="$t('login.password')" required v-model="login.password" style="margin-bottom:5px;"></password>
							<select class="form-control" v-model="login.role" v-validate required>
                                <option v-for="role in roles" :key="role.value" :value="role.value">{{ $t(role.name) }}</option>
                            </select>
						</div>
								
						<button type="submit" v-submit :disabled="action!=null" class="btn btn-lg btn-primary btn-block" v-t="'login.sign_in_btn'"></button>
						<div class="margin-top">
						    <router-link :to="{ path : './lostpw' }" v-t="'login.forgot_your_password'"></router-link>
						</div>
						<div class="margin-top" v-if="serviceLogin">
							<hr>
							<router-link class="btn btn-primary btn-block" :to="{ path : './registration', query : { actions : actions } }"  v-t="'login.no_account'"></router-link>
						</div>
					</form>
                </panel>
		    </div>
		</div></div>
	</div>   
</template>
<script>

import { status, ErrorBox, Password } from 'basic-vue3-components';
import server from "services/server";
import session from "services/session";
import crypto from "services/crypto";
import actions from "services/actions";
import apps from "services/apps";
import Panel from "components/Panel.vue";
import ENV from "config";

export default {
    data: () => ({
        login : { role : "MEMBER" },
        actions : null,
        offline : false,
        notPublic : ENV.instanceType == "prod",
        serviceLogin : false,
        roles : [
            { value : "MEMBER", name : "enum.userrole.MEMBER" },
		    { value : "PROVIDER" , name : "enum.userrole.PROVIDER"},
		    { value : "RESEARCH" , name : "enum.userrole.RESEARCH"},
		    { value : "DEVELOPER" , name : "enum.userrole.DEVELOPER"},
        ],
        app : null			
    }),

    components : { ErrorBox, Panel, Password },

    mixins : [ status ],

    methods : {
        fatalError(err) {
            const { $data } = this;
		    $data.error = err;
		    $data.serviceLogin = false;
		    $data.actions=null;
	    },
    
        hasIcon() {
            const { $data } = this;
		    if (!$data.app || !$data.app.icons) return false;
		    return $data.app.icons.indexOf("LOGINPAGE") >= 0;
	    },
	
	    getIconUrl() {
            const { $data } = this;
		    if (!$data.app) return null;
		    return ENV.apiurl + "/api/shared/icon/LOGINPAGE/" + $data.app.filename;
        },
        
        dologin() {
            const { $data, $router, $route } = this, me = this;
		    // check user input
		    if (!$data.login.email || !$data.login.password) {
			    $data.error = { code : "error.missing.credentials" };
                return;
            }
				
		    // send the request
		    let data = {"email": $data.login.email, "password": crypto.getHash($data.login.password), "role" : $data.login.role  };
		    let func = function(data) {
			    return me.doAction("login", server.post(jsRoutes.controllers.Application.authenticate().url, data));
		    };
		
		    session.performLogin(func, data, $data.login.password)
		    .then(function(result) {
		        session.postLogin(result, $router, $route);
		    });				
        },
        
        getAppName() {
            const { $data } = this;
            if (!$data.app) return "";
		    if ($data.app && $data.app.i18n && $data.app.i18n[$data.lang]) return " - "+$data.app.i18n[$data.lang].name;
		    return " - "+$data.app.name;
        }
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.actions = $route.query.actions;
	    $data.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);	
        $data.serviceLogin = ($route.query.actions != null);	
        $data.login.role = $route.meta.role.toUpperCase();

        if ($route.query.login) {
		    $data.login.email = $route.query.login;
	    }
	    if ($route.query.role) {
		    $data.login.role = $route.query.role;
        }
        
        let appName = actions.getAppName($route);
        if (appName) {    	
    	    apps.getAppInfo(appName, "visualization")
		    .then(function(results) {
			    $data.app = results.data;
			    if (!$data.app) { me.fatalError("error.unknown.plugin"); }
            }, function() { me.fatalError("error.unknown.plugin"); });
        }
    }
}
</script>