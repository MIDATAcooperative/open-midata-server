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

		<p v-if="app._id" v-t="'manageapp.logout_explain'"></p>
		<form name="myform" ref="myform" novalidate role="form" class="form-horizontal" :class="{ 'mark-danger' : app._id }" @submit.prevent="updateApp()">
		    <error-box :error="error"></error-box>
	      
		    <form-group name="filename" label="manageapp.filename" class="danger-change" :path="errors.filename">
		        <input type="text" id="filename" name="filename" class="form-control" @change="requireLogout();" v-validate v-model="app.filename" autofocus required>
		        <p class="form-text text-muted" v-t="'manageapp.info.filename'"></p>
		    </form-group>
		    <form-group name="type" label="manageapp.type" class="danger-change" :path="errors.type">
		        <select id="type" name="type" class="form-control" v-validate v-model="app.type" :disabled="app._id" @change="requireLogout();" required>
                    <option v-for="type in types" :key="type.value" :value="type.value">{{ $t('enum.plugintype.'+type.value) }}</option>
                </select>
		        <p class="form-text text-muted" v-t="'manageapp.info.type'"></p>
		    </form-group>
		    <form-group name="targetUserRole" label="manageapp.targetUserRole" class="danger-change" v-if="app.type!='analyzer' && app.type!='endpoint'" :path="errors.targetUserRole">
		        <select id="targetUserRole" name="targetUserRole" class="form-control" @change="requireLogout();" v-validate v-model="app.targetUserRole" required>
                    <option v-for="role in targetUserRoles" :key="role.value" :value="role.value">{{ $t('enum.userrole.'+role.value) }}</option>
                </select>
		        <p class="form-text text-muted" v-t="'manageapp.info.targetUserRole'"></p>
		    </form-group>			  	  
		    <form-group name="requirements" label="Requirements" class="danger-change" v-if="app.type!='analyzer' && app.type!='endpoint'" :path="errors.requirements">
		        <check-box v-for="req in requirements" :key="req" :checked="app.requirements.indexOf(req)>=0" :name="'chk_'+req" @click="toggle(app.requirements, req);requireLogout();">
                    <span>{{ $t('enum.userfeature.'+req) }}</span>
		        </check-box>		        
		    </form-group>		  		  
		    <hr>
		    <form-group name="name" label="Name" :path="errors.name">
		        <input type="text" id="name" name="name" class="form-control" placeholder="Name" v-validate v-model="app.name" required>
		        <p class="form-text text-muted" v-t="'manageapp.info.name'"></p>
		    </form-group>
		    <form-group name="description" label="Description" :path="errors.description">
		        <textarea rows="5" id="description" name="description" class="form-control" placeholder="Description" v-validate v-model="app.description" required></textarea>
		        <p class="form-text text-muted" v-t="'manageapp.info.description'"></p>
		    </form-group>
		    <form-group name="defaultSpaceName" label="Tile Name" v-if="app.type == 'visualization' || app.type == 'oauth1' || app.type == 'oauth2'" :path="errors.defaultSpaceName">
		    <input type="text" id="defaultSpaceName" name="defaultSpaceName" class="form-control" placeholder="Tile Name" v-validate v-model="app.defaultSpaceName">
		    <p class="form-text text-muted" v-t="'manageapp.info.defaultSpaceName'"></p>
		  </form-group>
		  <form-group name="orgName" label="Organization Name" :path="errors.orgName">
		    <input type="text" id="orgName" name="orgName" class="form-control" v-validate v-model="app.orgName">		 
		  </form-group>
		  <form-group name="publisher" label="Publisher" :path="errors.publisher">
		    <input type="text" id="publisher" name="publisher" class="form-control" v-validate v-model="app.publisher">		 
		  </form-group>
		  <form-group name="developerTeamLogins" label="manageapp.developerTeam" :path="errors.developerTeamLogins">
		    <input type="text" id="developerTeamLogins" name="developerTeamLogins" class="form-control" v-validate v-model="app.developerTeamLoginsStr">
		    <formerror name="developerTeamLogins" type="unknown" message="error.unknown.user"></formerror>
		    <p class="form-text text-muted" v-t="'manageapp.info.developerTeam'"></p>
		  </form-group>
		 
		  <hr>
		  <div v-if="app.type!='endpoint' && app.type!='analyzer'">
		  <form-group name="x" label="Multi Language Support">
		    <div class="form-text text-muted">
		    <span v-for="l in languages" :key="l">
		      <span v-if="app.i18n[l] && app.i18n[l].name"><span class="fas fa-check" aria-hidden="true"></span>{{l}} </span>		      
		    </span>
		    </div>
		  </form-group>
		  <form-group name="lang" label="I18n Language Selection">
		    <select id="lang" name="lang" class="form-control" v-validate v-model="sel.lang">
                <option v-for="lang in languages" :key="lang" :value="lang">{{ lang }}</option>
            </select>		    
		  </form-group>
		  <form-group name="name_i18n" label="I18n Name" :path="errors.name_i18n">		    
		    <input type="text" id="name_i18n" name="name_i18n" class="form-control" placeholder="Name" v-validate v-model="app.i18n[sel.lang].name">
		  </form-group>
		  <form-group name="description_i18n" label="I18n Description" :path="errors.description_i18n">
		    <textarea rows="5" id="description_i18n" name="description_i18n" class="form-control" placeholder="Description" v-validate v-model="app.i18n[sel.lang].description"></textarea>
		  </form-group>
		  <form-group name="defaultSpaceName_i18n" label="I18n Tile Name" v-if="app.type == 'visualization' || app.type == 'oauth1' || app.type == 'oauth2'" :path="errors.defaultSpaceName_i18n">		    
		    <input type="text" id="defaultSpaceName_i18n" name="defaultSpaceName_i18n" class="form-control" placeholder="Tile Name" v-validate v-model="app.i18n[sel.lang].defaultSpaceName">
		  </form-group>
		  <hr>		  
		  </div>
		  <form-group name="tags" label="Tags" v-if="app.type!='analyzer' && app.type!='endpoint'">
		    <check-box v-for="tagdef in tags" :key="tagdef" :name="tagdef" :checked="app.tags.indexOf(tagdef)>=0" @click="toggle(app.tags, tagdef)">
		       {{ tagdef }}
		    </check-box>
		    <p class="form-text text-muted" v-t="'manageapp.info.tags'"></p>
		  </form-group>  		
		
		  <form-group name="url" label="URL" v-if="app.type == 'visualization' || app.type == 'oauth1' || app.type == 'oauth2'" :path="errors.url">
		    <input type="text" id="url" name="url" class="form-control" placeholder="URL (must include &quot;:authToken&quot;)" v-validate v-model="app.url">		    
		    <p class="form-text text-muted" v-t="'manageapp.info.url'"></p>
		  </form-group>
		  <form-group name="previewUrl" label="Dashboard Tile URL" v-if="app.type == 'visualization' || app.type == 'oauth1' || app.type == 'oauth2'" :path="errors.previewUrl">
		    <input type="text" id="previewUrl" name="previewUrl" class="form-control" placeholder="URL (must include &quot;:authToken&quot;)" v-validate v-model="app.previewUrl">
		    <p class="form-text text-muted" v-t="'manageapp.info.previewUrl'"></p>
		  </form-group>		 
		  <form-group name="defaultSpaceContext" label="Default Dashboard" v-if="app.type == 'visualization' || app.type == 'oauth1' || app.type == 'oauth2'" :path="errors.defaultSpaceContext">
		    <input type="text" id="defaultSpaceContext" name="defaultSpaceContext" class="form-control" placeholder="Dashboard Name" v-validate v-model="app.defaultSpaceContext">
		    <p class="form-text text-muted" v-t="'manageapp.info.defaultSpaceContext'"></p>
		  </form-group>		 
		  <form-group name="defaultQuery" label="manageapp.access_query_json" class="danger-change" v-if="app._id">
		    <access-query :query="app.defaultQuery" details="true" isapp="true"></access-query>		    
		    <a href="javascript:" @click="go('./query')" v-t="'manageapp.queryeditor_btn'"></a>
		  </form-group>
		  <form-group name="writes" label="manageapp.write_mode" class="danger-change" :path="errors.writes">
		    <select class="form-control" name="writes" v-validate v-model="app.writes" @change="requireLogout();">
                <option v-for="mode in writemodes" :key="mode" :value="mode">{{ $t('enum.writepermissiontype.'+mode) }}</option>
            </select>
		  </form-group>
		   <form-group name="noUpdateHistory" label="manageapp.no_update_history" v-if="app.targetUserRole=='RESEARCH'" >
            
                <check-box name="noUpdateHistory" v-model="app.noUpdateHistory" :path="errors.noUpdateHistory">		    
		            <span v-t="'manageapp.info.no_update_history'"></span>
               </check-box>		    
		  </form-group>
		   <form-group name="pseudonymize" label="manageapp.pseudonymize" v-if="app.type=='analyzer' || app.type=='endpoint'">
               <check-box name="pseudonymize" v-model="app.pseudonymize" :path="errors.pseudonymize">		    
		            <span v-t="'manageapp.info.pseudonymize'"></span>
               </check-box>		    
		  </form-group>		
		  <form-group name="resharesData" label="Resharing" class="danger-change" v-if="app.type!='endpoint'">
                <check-box name="resharesData" v-model="app.resharesData" :path="errors.resharesData" @change="requireLogout();">		    
		            <span v-t="'manageapp.info.resharesData'"></span>
                </check-box>		    		    
		  </form-group>
		   <form-group name="allowsUserSearch" label="User Search" v-if="!(app.type == 'visualization' || app.type == 'oauth1' || app.type == 'oauth2' || app.type=='analyzer' || app.type=='endpoint')" class="danger-change">
               <check-box name="allowsUserSearch" v-model="app.allowsUserSearch" :path="errors.allowsUserSearch" @change="requireLogout();">		    
		            <span v-t="'manageapp.info.allowsUserSearch'"></span>
               </check-box>		    
		  </form-group>
		   <form-group name="consentObserving" label="Consent Observing" v-if="app.type == 'external'">
               <check-box name="consentObserving" v-model="app.consentObserving" disabled>
                   <span v-t="'manageapp.info.consentObserving'"></span>
               </check-box>		    
		  </form-group>		
		  				  
		  <form-group name="apiUrl" label="API Base URL" v-if="app.type == 'oauth1' || app.type == 'oauth2'" :path="errors.apiUrl">
		    <input type="text" id="apiUrl" name="apiUrl" class="form-control" placeholder="API Base URL" v-validate v-model="app.apiUrl">		    
		  </form-group>	 
		  <form-group name="authorizationUrl" label="Authorization URL" v-if="app.type == 'oauth1' || app.type == 'oauth2'" :path="errors.authorizationUrl">
		    <input type="text" id="authorizationUrl" name="authorizationUrl" class="form-control" placeholder="Authorization URL" v-validate v-model="app.authorizationUrl">
		  </form-group>
		  <form-group name="accessTokenUrl" label="Access Token URL" v-if="app.type == 'oauth1' || app.type == 'oauth2'" :path="errors.accessTokenUrl">
		    <input type="text" id="accessTokenUrl" name="accessTokenUrl" class="form-control" placeholder="Access token URL" v-validate v-model="app.accessTokenUrl">
		  </form-group>
		  <form-group name="tokenExchangeParams" label="manageapp.tokenExchangeParams" v-if="app.type == 'oauth2'" :path="errors.tokenExchangeParams">
		    <input type="text" id="tokenExchangeParams" name="tokenExchangeParams" class="form-control" v-validate v-model="app.tokenExchangeParams">
		    <p class="form-text text-muted" v-t="'manageapp.info.tokenExchangeParams'"></p>
		  </form-group>
		  <form-group name="consumerKey" label="Consumer Key" v-if="app.type == 'oauth1' || app.type == 'oauth2'" :path="errors.consumerKey">
		    <input type="text" id="consumerKey" name="consumerKey" class="form-control" placeholder="Consumer key" v-validate v-model="app.consumerKey">
		  </form-group>
		  <form-group name="consumerSecret" label="Consumer Secret" v-if="app.type == 'oauth1' || app.type == 'oauth2'" :path="errors.consumerSecret">
		    <input type="text" id="consumerSecret" name="consumerSecret" class="form-control" placeholder="Consumer secret" v-validate v-model="app.consumerSecret">
		  </form-group>
		  <form-group name="requestTokenUrl" label="Request Token URL" v-if="app.type == 'oauth1'" :path="errors.requestTokenUrl">
		    <input type="text" id="requestTokenUrl" name="requestTokenUrl" class="form-control" placeholder="Request token URL" v-validate v-model="app.requestTokenUrl">
		  </form-group>
		  <form-group name="scopeParameters" label="Scope Parameters" v-if="app.type == 'oauth2'" :path="errors.scopeParameters">
		    <input type="text" id="scopeParameters" name="scopeParameters" class="form-control" placeholder="Scope parameters" v-validate v-model="app.scopeParameters">
		  </form-group>
		  <form-group name="secret" label="Application Secret" v-if="app.type == 'mobile' || app.type == 'service'" :path="errors.secret">
		    <input type="text" id="secret" name="secret" class="form-control" placeholder="Secret" v-validate v-model="app.secret">
		    <p class="form-text text-muted" v-t="'manageapp.info.secret'"></p>
		  </form-group>
		  <form-group name="redirectUri" label="Redirect URI (optional, required for OAuth2 login)" v-if="app.type == 'mobile'" :path="errors.redirectUri">
		    <input type="text" id="redirectUri" name="redirectUri" class="form-control" placeholder="Redirect URI" v-validate v-model="app.redirectUri">
		    <p class="form-text text-muted" v-t="'manageapp.info.redirectUri'"></p>
		  </form-group>
		   <form-group name="developmentServer" label="Development Server" v-if="!(app.type == 'mobile' || app.type == 'service' || !app.developmentServer)" >
		    <p class="form-control-plaintext">{{ app.developmentServer }}</p>
		  </form-group>
		   <form-group name="unlockCode" label="manageapp.unlock_code" v-if="app.type == 'mobile'" :path="errors.unlockCode">
		    <input type="text" id="unlockCode" name="unlockCode" class="form-control" v-validate v-model="app.unlockCode">
		    <p class="form-text text-muted" v-t="'manageapp.info.unlock_code'"></p>
		  </form-group>
		   <form-group name="codeChallenge" v-if="app.type == 'mobile'" label="manageapp.code_challenge" :path="errors.codeChallenge">
		      <div class="form-check">
		         <input type="checkbox" id="codeChallenge" name="codeChallenge" class="form-check-input" v-validate v-model="app.codeChallenge">
		         <label for="codeChallenge" class="form-check-label">{{ $t('manageapp.info.code_challenge') }}</label>
		      </div>		    
		  </form-group>
		   <form-group name="sendReports" label="manageapp.send_reports" :path="errors.sendReports">
		      <div class="form-check">
		         <input type="checkbox" id="sendReports" name="sendReports" class="form-check-input" v-validate v-model="app.sendReports">
		         <label for="sendReports" class="form-check-label">{{ $t('manageapp.info.send_reports', { user : app.creatorLogin}) }}</label>
		      </div>		    
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
		  
		  <div v-if="(app.type == 'mobile' || app.type == 'external') && app.targetUserRole != 'RESEARCH'">
		  <hr v-if="app.type == 'mobile'">
		  <p class="alert alert-info" v-if="app.type == 'mobile'" v-t="'manageapp.admin_only'"></p>
		  <form-group name="termsOfUse" label="manageapp.terms_of_use" class="danger-change" :path="errors.termsOfUse">
		    <typeahead id="termsOfUse" :disabled="!allowStudyConfig" name="termsOfUse" class="form-control" @selection="requireLogout();" v-model="app.termsOfUse" field="id" display="fullname" :suggestions="terms" />
		    
		     <p class="form-text text-muted" v-if="app.termsOfUse"><router-link :to="{ path : './terms', query :  { which:app.termsOfUse }}" v-t="'manageapp.show_terms'"></router-link></p> 
		  </form-group>		  
		  </div>
		
		  
		  <form-group name="x" label="common.empty">
		    <router-link v-if="app._id" :to="{ path : './manageapp' ,query :  {appId:app._id}}" class="btn btn-default space" v-t="'common.back_btn'"></router-link>
		    <router-link v-else :to="{ path : './yourapps' }" class="btn btn-default space" v-t="'common.back_btn'"></router-link>
		    <button type="submit" :disabled="action!=null || (logoutRequired && !app.withLogout)" class="btn btn-primary space">Submit</button>		    		   
		    <button type="button" class="btn btn-danger space" v-if="allowDelete" @click="doDelete()" :disabled="action!=null" v-t="'common.delete_btn'"></button>		    
		  </form-group>		  
	     </form>	  
		  						    	 
    </panel>
</template>
<script>

import Panel from "components/Panel.vue"
import AccessQuery from "components/tiles/AccessQuery.vue"
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
        checks : [ "CONCEPT", "DATA_MODEL", "CODE_REVIEW", "TEST_CONCEPT", "TEST_PROTOKOLL", "CONTRACT" ],
        sel : { lang : 'de' },
	    targetUserRoles : [
            { value : "ANY", label : "Any Role" },
            { value : "MEMBER", label : "Account Holders" },
            { value : "PROVIDER", label : "Healthcare Providers" },
            { value : "RESEARCH", label : "Researchers" },
            { value : "DEVELOPER", label : "Developers" }
        ],
	    types : [
            { value : "visualization", label : "Plugin" },
            { value : "service", label : "Service" },
            { value : "oauth1", label : "OAuth 1 Import" },
            { value : "oauth2", label : "OAuth 2 Import" },
            { value : "mobile", label : "Mobile App" },
            { value : "external", label : "External Service" },
            { value : "analyzer", label : "Project analyzer" },
            { value : "endpoint", label : "FHIR endpoint" }
	    ],
	    tags : [
	        "Analysis", "Import", "Planning", "Protocol", "Expert"
        ],
        loginTemplates : [ "GENERATED", "TERMS_OF_USE_AND_GENERATED", "TERMS_OF_USE", "REDUCED" ],
		app : { version:0, tags:[], i18n : {}, sendReports:true, withLogout:true, redirectUri : "http://localhost", targetUserRole : "ANY", requirements:[], defaultQuery:{ content:[] }, tokenExchangeParams : "client_id=<client_id>&grant_type=<grant_type>&code=<code>&redirect_uri=<redirect_uri>"  },
		allowDelete : false,
		allowExport : false,
		allowStudyConfig : false,
		languages : languages.array,
		requirements : apps.userfeatures,
		writemodes : apps.writemodes,
		query : {},
		codesystems : formats.codesystems,
		terms : [],
        logoutRequired : false
    }),

    components: {  Panel, ErrorBox, FormGroup, CheckBox, Typeahead, AccessQuery },

    mixins : [ status ],

    methods : {
        getTitle() {
            const { $route, $t, $data } = this;
            let p = this.$data.app ? this.$data.app.name+" - " : "";
            if ($route.query.appId) return p+$t("manageapp.edit_btn");
            else return $t("manageapp.title_new");            
        },
                
        loadApp(appId) {
			const { $data, $route, $router } = this, me = this;
		    me.doBusy(apps.getApps({ "_id" : appId }, ["creator", "creatorLogin", "developerTeam", "developerTeamLogins", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "pluginVersion", "requirements", "termsOfUse", "orgName", "publisher", "unlockCode", "codeChallenge", "writes", "icons", "apiUrl", "noUpdateHistory", "pseudonymize", "predefinedMessages", "defaultSubscriptions", "sendReports", "consentObserving", "loginTemplate", "loginButtonsTemplate"])
		    .then(function(data) { 
                let app = data.data[0];	
				
                if (app.status == "DEVELOPMENT" || app.status == "BETA") {
                    $data.allowDelete = true;
                } else {
                    $data.allowDelete = $route.meta.allowDelete;
                }
                if (!app.i18n) { app.i18n = {}; }
				for (let lang of $data.languages) {
					if (!app.i18n[lang]) app.i18n[lang] = { name:"", description:"", defaultSpaceName:null };
				}
                if (!app.requirements) { app.requirements = []; }
				if (!app.defaultSubscriptions) app.defaultSubscriptions = [];
				if (!app.icons) app.icons = [];
                if (app.developerTeamLogins) app.developerTeamLoginsStr = app.developerTeamLogins.join(", "); 
				else app.developerTeamLogins = [];
                if (app.type === "oauth2" && ! (app.tokenExchangeParams) ) app.tokenExchangeParams = "client_id=<client_id>&grant_type=<grant_type>&code=<code>&redirect_uri=<redirect_uri>";
                app.defaultQueryStr = JSON.stringify(app.defaultQuery);
                //$data.updateQuery();
                app.withLogout = false;
                $data.app = app;        
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

        exportPlugin() {
			const { $data, $route, $router } = this, me = this;
		    me.doAction("download", server.token())
		    .then(function(response) {
		        document.location.href = ENV.apiurl + jsRoutes.controllers.Market.exportPlugin($data.app._id).url + "?token=" + encodeURIComponent(response.data.token);
		    });
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
	
	    getOAuthLogin() {
			const { $data, $route, $router } = this, me = this;
            if (!$data.app || !$data.app.redirectUri) return "";
            return "/oauth.html#/portal/oauth2?response_type=code&client_id="+encodeURIComponent($data.app.filename)+"&redirect_uri="+encodeURIComponent($data.app.redirectUri.split(" ")[0]);
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
	
	    doInstall() {
			const { $data, $route, $router } = this, me = this;
		    this.$router.push({ path : './visualization', query : { visualizationId : $data.app._id, context : "sandbox" } });
	    },
	
	    doDelete() {
			const { $data, $route, $router } = this, me = this;
            if ($route.meta.allowDelete) {
                me.doAction('delete', apps.deletePlugin($data.app))
                .then(function(data) { $router.push({ path : './yourapps' }); });
            } else {
                me.doAction('delete', apps.deletePluginDeveloper($data.app))
                .then(function(data) { $router.push({ path : './yourapps' }); });
            }
	    },

		updateApp() {
		
			const { $data, $route, $router } = this, me = this;
		
			let i18n = $data.app.i18n;
			for (let lang of $data.languages) {			
				if (i18n[lang] && i18n[lang].name == "") {
					delete i18n[lang];
				} 
			}
			$data.app.i18n = i18n;
			
			// check whether url contains ":authToken"
			if ($data.app.type && $data.app.type !== "mobile" && $data.app.type !== "service" && $data.app.url && $data.app.url.indexOf(":authToken") < 0) {
				this.setError("authToken", "Url must contain ':authToken' to receive the authorization token required to create records.");			  
				return;
			} 
			
			if ($data.app.targetUserRole!="RESEARCH") $data.app.noUpdateHistory=false;
			if ($data.app.type!="analyzer" && $data.app.type!="endpoint") $data.app.pseudonymize=false;
			
			if ($data.app.developerTeamLoginsStr) {
				$data.app.developerTeamLogins = $data.app.developerTeamLoginsStr.split(/[ ,]+/);
			} else $data.app.developerTeamLogins = [];
			
			
			if ($data.app._id == null) {
				me.doAction('submit', apps.registerPlugin($data.app))
				.then(function(data) { $router.push({ path : './manageapp', query : { appId : data.data._id }}); });
			} else {			
				me.doAction('submit', apps.updatePlugin($data.app))
				.then(function() { $router.push({ path : "./manageapp", query : { appId : $route.query.appId } }); });
			}
		}				
    },

    created() {
        const { $data, $route } = this, me = this;		
		$data.allowDelete = $route.meta.allowDelete;
		$data.allowExport = $route.meta.allowExport;
		$data.allowStudyConfig = $route.meta.allowStudyConfig;
		
		me.doBusy(session.currentUser.then(function(userId) {			
			$data.user = session.user;	
	    	return me.doBusy(terms.search({}, ["name", "version", "language", "title"]));
		}).then(function(result) {
			$data.terms = result.data;
			if ($route.query.appId != null) { me.loadApp($route.query.appId); }
			else {
				let app = { version:0, tags:[], i18n : {}, defaultSubscriptions:[], icons:[], sendReports:true, withLogout:true, redirectUri : "http://localhost", targetUserRole : "ANY", requirements:[], defaultQuery:{ content:[] }, tokenExchangeParams : "client_id=<client_id>&grant_type=<grant_type>&code=<code>&redirect_uri=<redirect_uri>"  };
				for (let lang of $data.languages) {
					if (!app.i18n[lang]) app.i18n[lang] = { name:"", description:"", defaultSpaceName:null };
				}
				app.defaultQueryStr = JSON.stringify(app.defaultQuery);
				$data.app = app;
				
				
			}
		}));
	
    }
}
</script>