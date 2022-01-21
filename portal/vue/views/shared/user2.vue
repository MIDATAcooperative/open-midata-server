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
													
			<div class="row" v-if="true">
			  <div class="col-sm-12">			  
		   	    <router-link class="btn btn-block btn-default" :to="{ path : './changeaddress', query : { actions : actions } }" v-t="'user.change_address_btn'"></router-link>
			    <router-link class="btn btn-block btn-default" :to="{ path : './changepassword', query : { actions : actions } }" v-t="'user.change_password_btn'"></router-link>
                <router-link class="btn btn-block btn-default" :to="{ path : './auditlog', query : { actions : actions } }" v-t="'user.auditlog_btn'"></router-link>
			    <router-link class="btn btn-block btn-default" v-show="user.role!='ADMIN'" :to="{ path : './accountwipe', query : { actions : actions } }" v-t="'user.wipe_account_btn'"></router-link>                
                <button type="button" class="btn btn-block btn-default" @click="skip()" v-t="'common.cancel_btn'"></button>
              </div>
            </div>			
		</panel>				   				
	</div>
	
</template>
<script>
import Panel from "components/Panel.vue"
import session from "services/session.js"
import actions from "services/actions.js"
import users from "services/users.js"
import languages from "services/languages.js"
import ENV from "config"
import { setLocale } from 'services/lang.js';
import { status, ErrorBox, FormGroup, RadioBox, Success } from 'basic-vue3-components'

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
        stats : null,
        actions : null
	}),	

    components: { RadioBox, Panel, FormGroup, ErrorBox, Success },

    mixins : [ status ],
  
    methods : {
              
        skip() {
            const { $data, $route, $router } = this, me = this;
            actions.showAction($router, $route);                
	    },

        updateSettings() {
            const me = this, { $data } = this;
			//if ($scope.locked) $scope.user.searchable = false;
		    me.doAction("changesettings", users.updateSettings($data.user))
		    .then(function() {		        
		        setLocale($data.user.language);
		    });
        },
        
            
        init() {
            const { $data, $route } = this, me = this;
            $data.actions = $route.query.actions;
            me.doBusy(session.currentUser.then(function(myUserId) {                                
	            me.doBusy(users.getMembers({"_id": myUserId}, ["name", "email", "searchable", "language", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone", "emailStatus", "agbStatus", "contractStatus", "role", "subroles", "confirmedAt", "birthday", "midataID", "status", "gender", "authType", "notifications"]))
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
