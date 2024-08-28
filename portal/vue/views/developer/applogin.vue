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
    <panel :title="getTitle()" :busy="isBusy">		  	

		<p v-t="'applogin.description'"></p>
		<form name="myform" ref="myform" novalidate role="form" class="form-horizontal" :class="{ 'mark-danger' : app._id }" @submit.prevent="updateApp()">
		    <error-box :error="error"></error-box>
	      
    
		    <form-group label="appicon.name">
		        <p class="form-control-plaintext">{{ app.name }} / {{ app.filename }} (<router-link v-if="app.termsOfUse" :to="{ path : './terms', query :  { which:app.termsOfUse }}" v-t="'applogin.show_terms'"></router-link><span v-else v-t="'applogin.no_terms'"></span>)</p>
		    </form-group>	

            <form-group label="applogin.links">
		        <div class="form-control-plaintext"> 
                    <div v-if="links.length==0" v-t="'applogin.no_links'"></div>
                    <div v-for="link in links" :key="link._id">{{ (link.study || {}).code }} {{ (link.study || {}).name }} {{ (link.provider || {}).name }} {{ (link.serviceApp || {}).name }} (<router-link v-if="link.termsOfUse" :to="{ path : './terms', query :  { which:link.termsOfUse }}" v-t="'applogin.show_terms'"></router-link><span v-else v-t="'applogin.no_terms'"></span>)</div>
                </div>
		    </form-group>		   
		    		              
		    <form-group name="requirements" label="Requirements" class="danger-change" v-if="app.type!='analyzer' && app.type!='endpoint'" :path="errors.requirements">
		        <check-box v-for="req in requirements" :key="req" :checked="app.requirements.indexOf(req)>=0" :name="'chk_'+req" @click="toggle(app.requirements, req);requireLogout();">
                    <span>{{ $t('enum.userfeature.'+req) }}</span>
		        </check-box>		        
		    </form-group>		  		  
		    
            <form-group name="loginTemplate" label="manageapp.loginTemplate" class="danger-change" v-if="app.type=='mobile'" :path="errors.loginTemplate">
		        <select id="loginTemplate" name="loginTemplate" class="form-control" @change="requireLogout();clearApproval();" v-validate v-model="app.loginTemplate" required>
                    <option v-for="template in loginTemplates" :key="template" :value="template">{{ $t('enum.logintemplate.'+template) }}</option>
                </select>
                <p class="form-text text-success" v-if="app.loginTemplateApprovedDate">{{ $t('applogin.approvedby') }} {{ app.loginTemplateApprovedByEmail }}, {{ $t('applogin.approvedat') }} {{ $filters.dateTime(app.loginTemplateApprovedDate) }}</p>	        
                <p class="form-text text-danger" v-if="!app.loginTemplateApprovedDate && app.loginTemplate!='GENERATED' && app.loginTemplate!= 'TERMS_OF_USE_AND_GENERATED'">{{ $t('applogin.not_approved') }}</p>
		    </form-group>	

            <form-group name="loginButtonsTemplate" label="manageapp.loginButtonsTemplate" class="danger-change" v-if="app.type=='mobile'" :path="errors.loginButtonsTemplate">
		        <select id="loginButtonsTemplate" :disabled="app.loginTemplate=='REDUCED'" name="loginButtonsTemplate" class="form-control" @change="requireLogout();" v-validate v-model="app.loginButtonsTemplate" required>
                    <option v-for="template in loginButtonsTemplates" :key="template" :value="template">{{ $t('enum.loginbuttonstemplate.'+template) }}</option>
                </select>		        
		    </form-group>	

            <form-group label="applogin.urlParams">
		        <input type="text" class="form-control" v-model="urlParams" @keypress="refresh()">
                <p class="form-text">{{ $t('applogin.urlParams2') }}</p>
		    </form-group>	
		  
		   <form-group name="withLogout" label="manageapp.logout" v-if="app._id">
		     <check-box name="withLogout" v-model="app.withLogout" :path="errors.withLogout" :required="logoutRequired">		      
		        <span v-t="'manageapp.pleaseLogout1'"></span>
		        <span v-if="app.targetUserRole=='RESEARCH'"> / </span>
		        <span v-if="app.targetUserRole=='RESEARCH'" v-t="'manageapp.pleaseLogout2'"></span>
		        <span class="text-danger" v-if="logoutRequired" v-t="'manageapp.logout_required'"></span>
		        <span class="text-success" v-if="!logoutRequired" v-t="'manageapp.logout_optional'"></span>
             </check-box>
		    <div v-if="app._id && app.withLogout" class="alert alert-warning">
		      <strong v-t="'manageapp.important'"></strong>
		      <p v-if="app.targetUserRole!='RESEARCH'" v-t="'manageapp.logoutwarning'"></p>
		      <p v-else v-t="'manageapp.researchwarning'"></p>		    
		    </div>  		  
		  </form-group>
		  		  
		  <form-group name="x" label="common.empty">
		    <router-link :to="{ path : './manageapp' ,query :  {appId:app._id}}" class="btn btn-default space" v-t="'common.back_btn'"></router-link>		    
		    <button type="submit" :disabled="action!=null || (logoutRequired && !app.withLogout)" class="btn btn-primary space">Submit</button>		    		   		    
		  </form-group>		  
	     </form>	  
                  
            <form-group name="x" label="applogin.preview">
          <select id="previewType" name="previewType" class="form-control" v-model="previewType" >
                <option v-for="template in previewTypes" :key="template" :value="template">{{ $t('applogin.previewtype.'+template) }}</option>
            </select>
            </form-group>

            <div class="loginpreview"><div class="inner">

            <div class="outerpreview">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.login'">Test</strong>
                    <div class="preview-req" v-t="'applogin.required'">Required</div>
                </div>
                
                <div class="previewtile">                
                    <OAuth2 :preview="app"></OAuth2>
                </div>
            </div>
            
            <div class="outerpreview" v-if="previewType=='NEW'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.registration'">Test</strong>
                    <div class="preview-req" v-t="'applogin.required'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <Registration :preview="app" :previewlinks="links" :query="convertParams(urlParams)"></Registration>
                </div>
            </div>

         

            <div class="outerpreview" v-if="hasRequirement('PASSWORD_SET') && previewType != 'RELOGIN'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.password_set'">Test</strong>
                    <div class="preview-req" v-t="'applogin.required'">Required</div>
                </div>
                <div class="previewtile">
                    <postregister :preview="{ requirement : 'PASSWORD_SET' }"></postregister>
                </div>
            </div>

            <div class="outerpreview" v-if="hasRequirement('EMAIL_VERIFIED') && previewType != 'RELOGIN'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.email_verified'">Test</strong>
                    <div class="preview-req" v-if="previewType=='EXISTING'" v-t="'applogin.optional'">Required</div>
                    <div class="preview-req" v-if="previewType=='NEW'" v-t="'applogin.required'">Required</div>
                </div>
                <div class="previewtile">
                    <postregister :preview="{ requirement : 'EMAIL_VERIFIED' }"></postregister>
                </div>
            </div>
            
             <div class="outerpreview" v-if="hasUnlockCode() && previewType == 'EXISTING'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.unlock_code'">Test</strong>                    
                    <div class="preview-req" v-t="'applogin.required'">Required</div>
                </div>
                <div class="previewtile">
                    <postregister :preview="{ requirement : 'APP_UNLOCK_CODE' }"></postregister>
                </div>
            </div>

            <div class="outerpreview" v-if="hasRequirement('BIRTHDAY_SET') && previewType == 'EXISTING'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.birthday_set'">Test</strong>
                    <div class="preview-req" v-t="'applogin.optional'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <postregister :preview="{ requirement : 'BIRTHDAY_SET' }"></postregister>
                </div>
            </div>
            
             <div class="outerpreview" v-if="hasRequirement('GENDER_SET') && previewType == 'EXISTING'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.gender_set'">Test</strong>
                    <div class="preview-req" v-t="'applogin.optional'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <postregister :preview="{ requirement : 'GENDER_SET' }"></postregister>
                </div>
            </div>

            <div class="outerpreview" v-if="hasRequirement('ADDRESS_ENTERED') && hasRequirement('PHONE_ENTERED') && previewType == 'EXISTING'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.address_phone_entered'">Test</strong>
                    <div class="preview-req" v-t="'applogin.optional'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <postregister :preview="{ requirements : ['ADDRESS_ENTERED','PHONE_ENTERED'] }"></postregister>
                </div>
            </div>

            <div class="outerpreview" v-if="hasRequirement('ADDRESS_ENTERED') && !hasRequirement('PHONE_ENTERED') && previewType == 'EXISTING'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.address_entered'">Test</strong>
                    <div class="preview-req" v-t="'applogin.optional'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <postregister :preview="{ requirement : 'ADDRESS_ENTERED' }"></postregister>
                </div>
            </div>


            <div class="outerpreview" v-if="hasRequirement('PHONE_ENTERED') && !hasRequirement('ADDRESS_ENTERED') && previewType == 'EXISTING'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.phone_entered'">Test</strong>
                    <div class="preview-req" v-t="'applogin.optional'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <postregister :preview="{ requirement : 'PHONE_ENTERED' }"></postregister>
                </div>
            </div>

            <div class="outerpreview" v-if="hasRequirement('ADMIN_VERIFIED') && previewType != 'RELOGIN'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.admin_verified'">Test</strong>
                    <div class="preview-req" v-if="previewType=='EXISTING'" v-t="'applogin.optional'">Required</div>
                    <div class="preview-req" v-if="previewType=='NEW'" v-t="'applogin.required'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <postregister :preview="{ requirement : 'ADMIN_VERIFIED' }"></postregister>
                </div>
            </div>

            <div class="outerpreview" v-if="hasRequirement('ADDRESS_VERIFIED') && previewType != 'RELOGIN'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.address_verified'">Test</strong>
                    <div class="preview-req" v-t="'applogin.optional'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <postregister :preview="{ requirement : 'ADDRESS_VERIFIED' }"></postregister>
                </div>
            </div>
        
         
            
            <div class="outerpreview" v-if="hasRequirement('NEWEST_TERMS_AGREED')">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.newest_terms_agreed'">Test</strong>
                    <div class="preview-req" v-t="'applogin.optional'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <postregister :preview="{ requirement : 'NEWEST_TERMS_AGREED' }"></postregister>
                </div>
            </div>    

            <div class="outerpreview" v-if="hasRequirement('PHONE_VERIFIED') && previewType != 'RELOGIN'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.phone_verified'">Test</strong>
                    <div class="preview-req" v-if="previewType=='EXISTING'" v-t="'applogin.optional'">Required</div>
                    <div class="preview-req" v-if="previewType=='NEW'" v-t="'applogin.required'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <postregister :preview="{ requirement : 'PHONE_VERIFIED' }"></postregister>
                </div>
            </div>                                  

            <div class="outerpreview" v-if="hasRequirement('AUTH2FACTOR') && previewType != 'RELOGIN'">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.auth2factorsetup'">Test</strong>
                    <div class="preview-req" v-if="previewType=='EXISTING'" v-t="'applogin.optional'">Required</div>
                    <div class="preview-req" v-if="previewType=='NEW'" v-t="'applogin.required'">Required</div>
                </div>
                      
                <div class="previewtile" >
                    <postregister :preview="{ requirement : 'AUTH2FACTORSETUP' }"></postregister>
                </div>
            </div>

            <div class="outerpreview" v-if="hasRequirement('AUTH2FACTOR')">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.auth2factor'">Test</strong>
                    <div class="preview-req" v-t="'applogin.optional'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <postregister :preview="{ requirement : 'AUTH2FACTOR' }"></postregister>
                </div>
            </div>
        
            <div class="outerpreview" v-if="previewType != 'RELOGIN' && app.loginButtonsTemplate != 'ONE_CONFIRM_PER_PAGE' && (app.loginTemplate != 'REDUCED' || previewType=='EXISTING')">
                <div class="alert alert-info m-2">
                    <strong v-t="'applogin.step.confirmation'">Test</strong>
                    <div class="preview-req" v-t="'applogin.required'">Required</div>
                </div>
            
                <div class="previewtile" >
                    <confirm :preview="app" :previewpage="0"></confirm>
                </div>
            </div>
           
            <div style="display:inline-block" v-if="app.loginButtonsTemplate == 'ONE_CONFIRM_PER_PAGE' && previewType != 'RELOGIN'">
                <div class="outerpreview" v-if="previewType != 'RELOGIN' && (app.loginTemplate != 'REDUCED' || previewType=='EXISTING')">
                    <div class="alert alert-info m-2">
                        <strong v-t="'applogin.step.confirmation'">Test</strong>
                        <div class="preview-req" v-t="'applogin.required'">Required</div>
                    </div>
            
                    <div class="previewtile" >
                        <confirm :preview="app" :previewpage="0"></confirm>
                    </div>
                </div>

                <div class="outerpreview" v-for="(link,idx) in links" :key="idx">
                    <div class="alert alert-info m-2">
                        <strong v-t="'applogin.step.confirmation2'">Test</strong>
                        <div class="preview-req" v-t="'applogin.required'">Required</div>
                    </div>
                                         
                    <div class="previewtile">
                        <confirm :preview="app" :previewpage="(1+idx)"></confirm>
                    </div>
                </div>
            </div>

             </div>
            </div>		    	 
    </panel>  
</template>
<script>

import Panel from "components/Panel.vue"
import AccessQuery from "components/tiles/AccessQuery.vue"
import Confirm from "views/shared/public/confirm.vue"
import OAuth2 from "views/shared/public/oauth2.vue"
import Registration from "views/member/public/registration.vue"
import Postregister from "views/shared/public/postregister.vue"
import server from "services/server.js"
import terms from "services/terms.js"
import formats from "services/formats.js"
import session from "services/session.js"
import languages from "services/languages.js"
import apps from "services/apps.js"
import { status, ErrorBox, CheckBox, FormGroup, Typeahead } from 'basic-vue3-components'
import ENV from 'config';

export default {

    data: () => ({	        
        loginTemplates : [ "GENERATED", "TERMS_OF_USE_AND_GENERATED", "TERMS_OF_USE", "REDUCED" ],
        loginButtonsTemplates : [ "ONE_CONFIRM_AND_OPTIONAL_CHECKBOXES", "ONE_CONFIRM_PER_PAGE", "CHECKBOXES_WITH_LINKED_TERMS" ],
        previewTypes : [ "NEW", "EXISTING", "RELOGIN" ],
        previewType : "NEW",
		app : {},
		requirements : apps.userfeatures,
		query : {},		
		terms : [],
        links : [],
        logoutRequired : false,
        urlParams : ""
    }),

    components: {  OAuth2, Registration, Confirm, Postregister, Panel, ErrorBox, FormGroup, CheckBox, Typeahead, AccessQuery },

    mixins : [ status ],

    methods : {
             
         getTitle() {
            const { $route, $t, $data } = this;
            let p = this.$data.app ? this.$data.app.name+" - " : "";
            return p+$t("manageapp.applogin_btn");                       
        },

        loadApp(appId) {
			const { $data, $route, $router } = this, me = this;
		    me.doBusy(apps.getApps({ "_id" : appId }, ["creator", "creatorLogin", "developerTeam", "developerTeamLogins", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "refreshTkExchangeParams", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "pluginVersion", "requirements", "termsOfUse", "orgName", "publisher", "unlockCode", "codeChallenge", "writes", "icons", "apiUrl", "noUpdateHistory", "pseudonymize", "predefinedMessages", "defaultSubscriptions", "sendReports", "consentObserving", "loginTemplate", "loginButtonsTemplate", "loginTemplateApprovedDate", "loginTemplateApprovedById", "loginTemplateApprovedByEmail", "usePreconfirmed", "accountEmailsValidated", "allowedIPs", "decentral", "organizationKeys", "acceptTestAccounts", "acceptTestAccountsFromApp", "acceptTestAccountsFromAppNames", "testAccountsCurrent", "testAccountsMax"])
		    .then(function(data) { 
                let app = data.data[0];					
                if (!app.requirements) { app.requirements = []; }				                                
                app.withLogout = false;
                $data.app = app;        
            }));

            me.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("app-use", appId).url)
            .then(function(links) {               
                $data.links = links.data;
            }));
			
            me.doBusy(server.get(jsRoutes.controllers.Market.getReviews(appId).url)
            .then(function(reviews) {
                $data.reviews = {};
                for (var i=0;i<reviews.data.length;i++) {
                    var status = reviews.data[i].status;
                    $data.reviews[reviews.data[i].check] = (status == "OBSOLETE" ? null : status);
                }			
            }));
	    },

        hasRequirement(req) {
            return this.$data.app.requirements.indexOf(req)>=0;
        },
        
        hasUnlockCode() {
           return this.$data.app.unlockCode;
        },
        
	    go(where) {
			const { $data, $route, $router } = this, me = this;
		    this.$router.push({ path : where, query : { appId : this.$route.query.appId }});
	    },
	
	    hasIcon() {
			const { $data, $route, $router } = this, me = this;
            if (!$data.app || !$data.app.icons) return false;
            return $data.app.icons.indexOf("APPICON") >= 0;
	    },
	
	    getIconUrl() {
			const { $data, $route, $router } = this, me = this;
            if (!$data.app) return null;
            return ENV.apiurl + "/api/shared/icon/APPICON/" + $data.app.filename;
	    },
	
	    requireLogout() {
			const { $data, $route, $router } = this, me = this;
		    $data.logoutRequired = true;
	    },

        clearApproval() {            
            this.$data.app.loginTemplateApprovedDate = null;
            if (this.$data.app.loginTemplate == 'REDUCED') this.$data.app.loginButtonsTemplate = "CHECKBOXES_WITH_LINKED_TERMS";
        },
	
	    getOAuthLogin() {
			const { $data, $route, $router } = this, me = this;
            if (!$data.app || !$data.app.redirectUri) return "";
            return "/oauth.html#/portal/oauth2?response_type=code&client_id="+encodeURIComponent($data.app.filename)+"&redirect_uri="+encodeURIComponent($data.app.redirectUri.split(" ")[0]);
	    },

        convertParams(inp) {
           if (!inp || inp == "") return navigator;
           let obj = Object.fromEntries(new URLSearchParams(inp));
           return obj;
        },
	
	    keyCount(obj) {
			const { $data, $route, $router } = this, me = this;
            if (!obj) return 0;
            return Object.keys(obj).length;
	    },
	
	    hasCount(obj) {
			const { $data, $route, $router } = this, me = this;
            if (!obj) return false;
            if (obj.content && obj.content.length==0) return false;
            return Object.keys(obj).length > 0;
	    },
	
	    hasSubRole(subRole) {	
			const { $data, $route, $router } = this, me = this;
		    return $data.user.subroles.indexOf(subRole) >= 0;
	    },

        toggle(array,itm) {
			const { $data, $route, $router } = this, me = this;            
            var pos = array.indexOf(itm);
            if (pos < 0) array.push(itm); else array.splice(pos, 1);
	    },
		    
		updateApp() {
		
			const { $data, $route, $router } = this, me = this;
											
			me.doAction('submit', apps.updatePlugin($data.app))
			.then(function() { $router.push({ path : "./manageapp", query : { appId : $route.query.appId } }); });			
		},
        
        refresh() {
           let p = this.$data.previewType;
           if (p) {
            this.$data.previewType = "";
            window.setTimeout(() => { this.$data.previewType = p },100);
           }
        }
    },

    created() {
        const { $data, $route } = this, me = this;		
		
      	me.doBusy(session.currentUser.then(function(userId) {			
			$data.user = session.user;	
	    	return me.doBusy(terms.search({}, ["name", "version", "language", "title"]));
		}).then(function(result) {
			$data.terms = result.data;
			me.loadApp($route.query.appId);			
		}));
	
    }
}
</script>
<style scoped>
   .loginpreview {
      overflow-x: auto;
      width: 100%;      
   }

   .inner {
       white-space: nowrap; 
       margin: 0px;
       vertical-align: top;
   }

   .outerpreview {
      display: inline-block; 
      margin: 0px;
      vertical-align: top;
   }

  .previewtile { 
      zoom: 0.5;
      transform-origin: 0 0;
      max-width: 320px; 
      display: inline-block;
      white-space: normal; 
      pointer-events: none;
      overflow: hidden;
   }
</style>