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
    <div>		
		<panel :title="$t('user.account')" :busy="isBusy">
							
			<address>     
     		    <strong>{{ user.firstname }} {{ user.lastname }}</strong><br/>
     		    {{ user.address1 }}<br/>
     		    {{ user.address2 }}<br/>
     		    {{ user.zip }} {{ user.city }}
     		</address>  
     					
			<div class="row">
				<p class="col-sm-2" v-t="'user.email'"></p>
				<p class="col-sm-10">{{user.email}}</p>
			</div> 
			<div class="row" v-if="user.birthday">
				<p class="col-sm-2" v-t="'user.birthday'"></p>
				<p class="col-sm-10">{{ $filters.date(user.birthday) }}</p>
			</div>
			<div class="row" v-if="user.midataID">
				<p class="col-sm-2" v-t="'user.midataID'"></p>
				<p class="col-sm-10">{{user.midataID }}</p>
			</div>
			
			<div class="row" v-if="stats">
			  <p class="col-2 text-muted">Consents Out</p><p class="col-10 text-muted">{{ stats.numConsentsOwner }}</p>
			  <p class="col-2 text-muted">Consents In</p><p class="col-10 text-muted">{{ stats.numConsentsAuth }}</p>
			  <p class="col-2 text-muted">Streams Self</p><p class="col-10 text-muted">{{ stats.numOwnStreams }}</p>
			  <p class="col-2 text-muted">Streams Extern</p><p class="col-10 text-muted">{{ stats.numOtherStreams }}</p>			  			  			  
			  <p class="col-2 text-muted">Groups</p><p class="col-10 text-muted">{{ stats.numUserGroups }}</p>
			</div>
						
			<div class="row" v-if="true">
			  <div class="col-sm-12">
			  
		   	    <router-link class="btn btn-default" :to="{ path : './changeaddress' }" v-t="'user.change_address_btn'"></router-link>&nbsp;
			    <router-link class="btn btn-default" :to="{ path : './changepassword' }" v-t="'user.change_password_btn'"></router-link>&nbsp;
			    
			    <div class="btn-group">
                    <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        <span v-t="'user.action'"></span> <span class="caret"></span>
                    </button>
                    <div class="dropdown-menu">
                        <router-link class="dropdown-item" v-show="user.status=='ACTIVE'" :to="{ path : './changepassphrase' }" v-t="'user.change_passphrase_btn'"></router-link>
                        <router-link class="dropdown-item" :to="{ path : './changeemail' }" v-t="'user.change_email_btn'"></router-link>
                        <router-link class="dropdown-item" :to="{ path : './auditlog' }" v-t="'user.auditlog_btn'"></router-link>
                        <router-link class="dropdown-item" :to="{ path : './market', query : { tag : 'Expert' }}" v-t="'user.expert_tools_btn'"></router-link>
	                    <a class="dropdown-item" href="javascript:" :disabled="msg!=null" @click="fixAccount();" v-t="'user.repair_account_btn'"></a>
	                    <a class="dropdown-item" href="javascript:" :disabled="msg!=null" @click="resetSpaces();" v-t="'user.reset_spaces_btn'"></a>
	                    <router-link class="dropdown-item" :to="{ path : './records-delete' }" v-t="'user.delete_records_btn'"></router-link>
	                    <a class="dropdown-item" href="javascript:" @click="exportAccount()" v-t="'user.export_btn'"></a>
	                    <a class="dropdown-item" href="javascript:" v-show="beta" @click="metrics()" v-t="'user.metrics_btn'"></a>
	                    <router-link class="dropdown-item" v-show="user.role!='MEMBER'" :to="{ path : './servicekeys' }" v-t="'user.servicekeys_btn'"></router-link>
	                    <router-link class="dropdown-item" v-show="user.role!='ADMIN'" :to="{ path : './accountwipe' }" v-t="'user.wipe_account_btn'"></router-link>
                    </div>
                </div>			     			     			     			     
			    
			  </div>			  
			</div>
									
			<div class="extraspace"></div>
			<div v-show="msg" class="alert alert-info">{{ $t(msg) }}
			<ul v-if="repair.length">
			    <li v-for="msg in repair" :key="msg">{{ msg }}</li>
			</ul>			
			</div>
		</panel>

        <panel :title="$t('user.settings')" :busy="isBusy">
									
		    <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="updateSettings();" role="form">
                <error-box :error="error"></error-box>
			
                <form-group name="language" label="registration.language" :path="errors.language">
                    <select v-model="user.language" name="language" class="form-control" v-validate>
                        <option value selected disabled hidden>{{ $t('common.fillout') }}</option>
                        <option v-for="lang in languages" :key="lang.value" :value="lang.value">{{ $t(lang.name) }}</option>
                    </select>
                </form-group>
			
			    <form-group name="searchable" label="user.searchable_short">
                    <radio-box v-model="user.searchable" :value="true" name="searchable">				
				        <span class="margin-left" v-t="'user.searchable_yes'"></span>
                    </radio-box>
                    <radio-box v-model="user.searchable" :value="false" name="searchable" :path="errors.searchable">				
				        <span class="margin-left" v-t="'user.searchable_no'"></span>
                    </radio-box>                				  
				    <span class="form-text text-muted" v-t="'user.searchable_info'"></span>
                </form-group>
                <div class="extraspace"></div>
                <form-group name="auth_type" label="user.auth_type">
                    <radio-box v-for="mode in authTypes" :key="mode" v-model="user.authType" :value="mode" :path="errors.authType" name="authType">							  
				        <span  class="margin-left">{{ $t('enum.secondaryauthtype.'+mode) }}</span>
                    </radio-box>
                </form-group>
                <div class="extraspace"></div>
                <form-group name="notifications" label="user.notifications">			
			        <div v-t="'user.notifications2'"></div>
                    <radio-box v-for="mode in notificationTypes" :key="mode" :value="mode" v-model="user.notifications" :path="errors.notifications">				  
				    <span class="margin-left">{{ $t('enum.accountnotifications.'+mode) }}</span>
                    </radio-box>
                </form-group>
			
			    <button class="btn btn-default" :disabled="action != null" type="submit" v-submit v-t="'user.change_settings_btn'"></button>
                <success :finished="finished" action="changesettings" msg="user.change_settings_success"></success>
			    <div class="extraspace"></div>			    
		    	
			</form>
        </panel>				   				
	</div>
	
</template>
<script>
import Panel from "components/Panel.vue"
import server from "services/server.js"
import session from "services/session.js"
import users from "services/users.js"
import languages from "services/languages.js"
import ENV from "config"
import { setLocale } from 'services/lang.js';
import { status, CheckBox, ErrorBox, FormGroup, RadioBox, Success } from 'basic-vue3-components'

export default {
  
    data: () => ({
        user : {},        
        msg : null,
        repair : [],
        beta : ENV.instanceType == "test" || ENV.instanceType == "local" || ENV.instanceType == "demo",
        languages : languages.all,
        authTypes : ["NONE", "SMS"],
        notificationTypes : ["NONE", "LOGIN"],
        isSelf : false,
        stats : null
	}),	

    components: { RadioBox, Panel, FormGroup, ErrorBox, Success },

    mixins : [ status ],
  
    methods : {
        exportAccount() {
            const me = this;
		    me.doAction("download", server.token())
		    .then(function(response) {
		        document.location.href = ENV.apiurl + jsRoutes.controllers.Records.downloadAccountData().url + "?token=" + encodeURIComponent(response.data.token);
		    });
        },
        
        updateSettings() {
            const me = this, { $data } = this;
			//if ($scope.locked) $scope.user.searchable = false;
		    me.doAction("changesettings", users.updateSettings($data.user))
		    .then(function() {		        
		        setLocale($data.user.language);
		    });
        },
        
        resetSpaces() {
            const { $data } = this, me = this;
		    $data.msg = "Please wait...";
		    me.doBusy(server.delete(jsRoutes.controllers.Spaces.reset().url))
		    .then(function() { 
                $data.msg = "user.spaces_resetted";
                document.location.reload(); 
            });
        },
        
        fixAccount() {
            const { $data } = this, me = this;
		    $data.msg = "Please wait...";
		    server.post(jsRoutes.controllers.Records.fixAccount().url)
		    .then(function(results) { 
                $data.msg = "user.account_repaired";
                $data.repair=results.data; 
            });
        },
        
        metrics() {
            const { $data } = this, me = this;
		    $data.stats = {};
		    me.doBusy(server.get("/api/shared/users/stats").then(function(results) {
			   $data.stats = results.data;
		    }));
        },
        
        init() {
            const { $data, $route } = this, me = this;
            let userId = $route.query.userId;	
            me.doBusy(session.currentUser.then(function(myUserId) {
                userId = userId || myUserId;
                $data.isSelf = myUserId == userId;
	            me.doBusy(users.getMembers({"_id": userId}, ["name", "email", "searchable", "language", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone", "emailStatus", "agbStatus", "contractStatus", "role", "subroles", "confirmedAt", "birthday", "midataID", "status", "gender", "authType", "notifications"]))
		        .then(function(results) {
                    let user = results.data[0];
                    user.authType = user.authType || "NONE";
                    user.notifications = user.notifications || "NONE";
			        $data.user = user;
                });		
            }));
        }
						
    },

    created() {
        this.init();
    }
}
</script>
