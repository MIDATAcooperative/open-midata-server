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
    <panel :title="getTitle()" :busy="isBusy">		  
	
        <error-box :error="error"></error-box>
		
		<div v-if="!selmsg">
	
            <table class="table table-striped" v-if="messages.length">
                <tr>
                    <th v-t="'appmessages.reason'"></th>
                    <th v-t="'appmessages.code'"></th>
                    <th v-t="'appmessages.languages'"></th>
                    <th>&nbsp;</th>
                </tr>
                <tr v-for="msg in messages" :key="JSON.stringify(msg)">
                    <td><a @click="showMessage(msg)" href="javascript:">{{ $t('appmessages.reasons.' + msg.reason) }}</a></td>
                    <td>{{ msg.code }}</td>
                    <td>
                        <span v-for="l in languages" :key="l" @click="showMessage(msg,l)">
                            <span v-if="msg.text[l]"><span class="fas fa-check" aria-hidden="true"></span>{{l}}&nbsp;</span>		      
                        </span>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-default" @click="showMessage(msg)" v-t="'common.view_btn'"></button>
                    </td>
                </tr>
            </table>
                            
            <p v-if="!messages.length" v-t="'appmessages.empty'"></p>
            
            <router-link class="btn btn-default mr-1" :to="{ path : './manageapp', query : { appId : app._id } }" v-t="'common.back_btn'"></router-link>		
            <button class="btn btn-default" @click="addMessage()" v-t="'common.add_btn'"></button>
            
		</div>
		<div v-else>  		
				  		
		    <form name="myform" ref="myform" novalidate role="form" class="form-horizontal" @submit.prevent="updateApp()">
                <error-box :error="error"></error-box>		  		  		  		  		  
                <form-group name="reason" label="appmessages.reason" :path="errors.reason">
                    <select id="reason" name="reason" class="form-control" v-validate v-model="selmsg.reason">
                        <option v-for="reason in reasons" :key="reason" :value="reason">{{ $t('appmessages.reasons.' + reason) }}</option>
                    </select>		    
                </form-group>
            
                <form-group name="code" label="appmessages.code" :path="errors.code">		    
                    <input type="text" id="code" name="code" class="form-control" placeholder="Code" v-validate v-model="selmsg.code">
                </form-group>
                
                <hr>	
                        
                <form-group label="Multi Language Support">
                    <div class="form-text text-muted">
                        <span v-for="l in languages" :key="l" @click="showMessage(selmsg,l)">
                            <span v-if="selmsg.text[l]"><span class="fas fa-check" aria-hidden="true"></span>{{l}} </span>		      
                        </span>
                    </div>
                </form-group>
                        
                                                                    
                <form-group name="lang" label="appmessages.lang" :path="errors.lang">
                    <select id="lang" name="lang" class="form-control" v-validate v-model="sel.lang">
                        <option v-for="lang in languages" :key="lang" :value="lang">{{ lang }}</option>
                    </select>		    
                </form-group>
            
            
                <form-group name="title" label="appmessages.msgtitle" :path="errors.title">
                    <input type="text" id="title" name="title" class="form-control" v-validate v-model="selmsg.title[sel.lang]">
                </form-group>
                <form-group name="text" label="appmessages.text" :path="errors.text">
                    <textarea rows="5" id="text" name="text" class="form-control" v-validate v-model="selmsg.text[sel.lang]"></textarea>
                    <div class="form-text text-muted">
                        <span v-t="'appmessages.available_tags'"></span>:
                        <code v-for="tag in tags[selmsg.reason]" :key="tag">&lt;{{ tag }}&gt; </code>
                    </div>
                </form-group>
                    
                <form-group label="common.empty">
                    <button type="submit" v-submit :disabled="action!=null" class="btn btn-primary mr-1">Submit</button>	
                    <button type="button" class="btn btn-danger" v-t="'common.delete_btn'" @click="deleteMessage(msg)"></button>	    
                </form-group>
	        </form>	  
		</div>
			
    </panel>
</div>		

</template>
<script>

import Panel from "components/Panel.vue"
import languages from "services/languages.js"
import apps from "services/apps.js"
import { status, ErrorBox, Success, FormGroup } from 'basic-vue3-components'

export default {
    data: () => ({	
       
        appId : null,
        app : null,

        languages : languages.array,
        reasons : ['REGISTRATION', 'REGISTRATION_BY_OTHER_PERSON', 'FIRSTUSE_ANYUSER', 'FIRSTUSE_EXISTINGUSER', 'LOGIN', 'SERVICE_WITHDRAW', 'ACCOUNT_UNLOCK', 'CONSENT_REQUEST_OWNER_INVITED', 'CONSENT_REQUEST_OWNER_EXISTING', 'CONSENT_REQUEST_AUTHORIZED_INVITED', 'CONSENT_REQUEST_AUTHORIZED_EXISTING', 'CONSENT_CONFIRM_OWNER', 'CONSENT_CONFIRM_AUTHORIZED', 'CONSENT_REJECT_OWNER', 'CONSENT_REJECT_AUTHORIZED', 'CONSENT_REJECT_ACTIVE_AUTHORIZED', 'CONSENT_VERIFIED_OWNER', 'CONSENT_VERIFIED_AUTHORIZED', 'EMAIL_CHANGED_OLDADDRESS', 'EMAIL_CHANGED_NEWADDRESS', 'PASSWORD_FORGOTTEN', 'USER_PRIVATE_KEY_RECOVERED', 'RESOURCE_CHANGE', 'PROCESS_MESSAGE', 'NON_PERFECT_ACCOUNT_MATCH', 'TRIED_USER_REREGISTRATION' ],
	    sel : { lang : 'en' },
        selmsg : null,
        messages : [],
        tags : {
            'REGISTRATION': ["site", "confirm-url", "reject-url", "token", "firstname", "lastname", "email", "plugin-name", "midata-portal-url", "organization-name", "parent-organization-name", "top-organization-name"],
            'REGISTRATION_BY_OTHER_PERSON': ["site", "confirm-url", "reject-url", "token", "firstname", "lastname", "email", "executor-firstname", "executor-lastname", "executor-email", "plugin-name", "midata-portal-url"],
            'FIRSTUSE_ANYUSER' : ["firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'SERVICE_WITHDRAW' : ["firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'LOGIN' : ["firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'FIRSTUSE_EXISTINGUSER' : ["firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'EMAIL_CHANGED_OLDADDRESS' : ["firstname", "lastname", "old-email", "new-email", "midata-portal-url", "reject-url"],
            'EMAIL_CHANGED_NEWADDRESS' : ["firstname", "lastname", "old-email", "new-email", "midata-portal-url", "confirm-url"],
            'ACCOUNT_UNLOCK' : ["firstname", "lastname", "email", "midata-portal-url"],
            'CONSENT_REQUEST_OWNER_EXISTING' : ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-name", "grantor-email", "grantee-firstname", "grantee-lastname", "grantee-name", "grantee-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url", "confirm-url"],
            'CONSENT_REQUEST_AUTHORIZED_EXISTING' : ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-name", "grantor-email", "grantee-firstname", "grantee-lastname", "grantee-name", "grantee-email", "consent-name","firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'CONSENT_CONFIRM_OWNER' : ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-name", "grantor-email", "grantee-firstname", "grantee-lastname", "grantee-name", "grantee-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'CONSENT_CONFIRM_AUTHORIZED' : ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-name", "grantor-email", "grantee-firstname", "grantee-lastname", "grantee-name", "grantee-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url", "reject-url"],
            'CONSENT_REJECT_OWNER' : ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-name", "grantor-email", "grantee-firstname", "grantee-lastname", "grantee-name", "grantee-email", "consent-name","firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'CONSENT_REJECT_AUTHORIZED' : ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-name", "grantor-email", "grantee-firstname", "grantee-lastname", "grantee-name", "grantee-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'CONSENT_REJECT_ACTIVE_AUTHORIZED' : ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-name", "grantor-email", "grantee-firstname", "grantee-lastname", "grantee-name", "grantee-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'CONSENT_REQUEST_OWNER_INVITED' : ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-name", "grantor-email", "grantee-firstname", "grantee-lastname", "grantee-name", "grantee-email", "consent-name", "email", "plugin-name", "midata-portal-url", "confirm-url"],
            'CONSENT_REQUEST_AUTHORIZED_INVITED' : ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-name", "grantor-email", "grantee-name", "grantee-email", "consent-name", "email", "plugin-name", "midata-portal-url", "reject-url"],
            'CONSENT_VERIFIED_OWNER': ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-name", "grantor-email", "grantee-firstname", "grantee-lastname", "grantee-name", "grantee-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'CONSENT_VERIFIED_AUTHORIZED': ["executor-firstname", "executor-lastname", "executor-name", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-name", "grantor-email", "grantee-firstname", "grantee-lastname", "grantee-name", "grantee-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'PASSWORD_FORGOTTEN' : [ "site", "password-link", "firstname", "lastname", "email" ],
            'RESOURCE_CHANGE' : [ "midata-portal-url", "plugin-name", "firstname", "lastname", "email" ],
            'PROCESS_MESSAGE' : [ "midata-portal-url", "plugin-name", "firstname", "lastname", "email" ],
            'USER_PRIVATE_KEY_RECOVERED' : [ "firstname", "lastname", "email", "site" ],
            'NON_PERFECT_ACCOUNT_MATCH' : [ "plugin-name", "firstname", "lastname", "email", "site", "organization-name", "parent-organization-name", "top-organization-name" ],
            'TRIED_USER_REREGISTRATION' : [ "plugin-name", "firstname", "lastname", "email", "site", "organization-name", "parent-organization-name", "top-organization-name" ]
        }
    }),

    components: {  Panel, ErrorBox, FormGroup, Success },

    mixins : [ status ],

    methods : {
          getTitle() {
            const { $route, $t, $data } = this;
            let p = this.$data.app ? this.$data.app.name+" - " : "";
            return p+$t("manageapp.messages_btn");                       
        },

        loadApp(appId) {
            const { $data } = this, me = this;
            me.doBusy(apps.getApps({ "_id" : appId }, ["creator", "developerTeam", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "predefinedMessages", "requirements", "termsOfUse", "orgName", "publisher", "unlockCode", "codeChallenge", "writes", "loginTemplate", "loginButtonsTemplate", "usePreconfirmed", "accountEmailsValidated", "allowedIPs", "decentral", "organizationKeys", "sendReports", "acceptTestAccounts", "acceptTestAccountsFromApp", "acceptTestAccountsFromAppNames", "testAccountsCurrent", "testAccountsMax"])
            .then(function(data) { 
                let app = data.data[0];
                if (!app.predefinedMessages) app.predefinedMessages = {};
                $data.app = app;
                $data.messages = [];                
                for (let msg in app.predefinedMessages) { $data.messages.push(app.predefinedMessages[msg]); }
            }));
	    },
	
	
	    updateApp() {		            
            const { $data, $route } = this, me = this;
            let predefinedMessages = {};
            for (let msg of $data.messages) {
            
                for (let k in msg.text) if (msg.text[k]==="") { delete msg.text[k]; }
                for (let k in msg.title) if (msg.title[k]==="") { delete msg.title[k]; }
                                
                predefinedMessages[msg.reason+(msg.code ? ("_"+msg.code) : "")] = msg;
            }
            $data.app.predefinedMessages = predefinedMessages;
                        
            $data.app.msgOnly = true;				
            me.doAction('submit', apps.updatePlugin($data.app)
            .then(function() { 
                $data.selmsg = null;
                me.loadApp($route.query.appId); 
            }));		
	    },
	
	    addMessage() {
            const { $data } = this, me = this;
            $data.messages.push($data.selmsg = { title : {}, text: {} });
	    },
	
	    showMessage(msg, lang) {
            const { $data } = this, me = this;
            $data.selmsg = msg;
            $data.sel.lang = lang || $data.sel.lang;
	    },
	
	    deleteMessage() {
            const { $data } = this, me = this;
            $data.messages.splice($data.messages.indexOf($data.selmsg), 1);
            me.updateApp();
            //$scope.selmsg = null;
	    }
    },

    created() {
        const { $route } = this, me = this;
        me.loadApp($route.query.appId);
        
    }
}
</script>