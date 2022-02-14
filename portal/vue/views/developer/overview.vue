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
    <panel :title="$t('manageapp.overview')" :busy="isBusy">		  	
		<error-box :error="error"></error-box>
		<div v-if="app">
	      <div class="float-left" v-if="hasIcon()" style="margin-right:10px">
	        <img style="max-width:64px" :src="getIconUrl()">
	      </div>	
		  <p class="lead">{{ app.name }}</p>
		  <p>{{ app.description }}</p>

          <table>
            <tr>
		      <td style="padding-right:130px" v-t="'manageapp.filename'"></td>
		      <td><b>{{ app.filename }}</b></td>
		    </tr>
		    <tr>
		      <td v-t="'manageapp.type'"></td>
			  <td><b>{{ $t('enum.plugintype.' + app.type) }}</b>
			  <span v-if="app.type=='external'"> - 
			  <router-link :to="{ path : './servicekeys' }" v-t="'manageapp.manageyourkeys'"></router-link>
			  </span>
			  </td>
		    </tr><tr>
		      <td v-t="'manageapp.targetUserRole'"></td>
              <td><b>{{ $t('enum.userrole.'+app.targetUserRole) }}</b></td>
            </tr><tr>
              <td v-t="'manageapp.status.title'"></td>
              <td><b>{{ $t('manageapp.status.'+app.status) }}</b></td>
            </tr>           
            <tr>
              <td v-t="'manageapp.creator'"></td>
              <td>
                <span v-if="hasSubRole('USERADMIN')"><router-link :to="{ path : './address', query :  {userId : app.creator }}"><b>{{ app.creatorLogin }}</b></router-link></span>
                <span v-if="!hasSubRole('USERADMIN')"><b>{{ app.creatorLogin }}</b></span>
              </td>
            </tr>
            <tr v-if="app.developerTeamLogins.length">
              <td v-t="'manageapp.developerTeam'"></td>
              <td>
                <div v-for="(login,idx) in app.developerTeamLogins" :key="login">
                <span v-if="hasSubRole('USERADMIN')"><router-link :to="{ path : './address', query :  {userId : app.developerTeam[idx] }}"><b>{{ login }}</b></router-link></span>
                <span v-if="!hasSubRole('USERADMIN')"><b>{{ login }}</b></span>
                </div>
              </td>
            </tr>
            <tr>
              <td v-t="'manageapp.organization'"></td>
              <td><b>{{ app.orgName || "-" }}</b></td>
            </tr>
            <tr>
              <td v-t="'manageapp.publisher'"></td>
              <td><b>{{ app.publisher || "-" }}</b></td>
            </tr>
            <tr>
              <td v-t="'manageapp.reviews'"></td>
              <td><div class="container">
                <div class="row">
                    <div v-for="check in checks" :key="check" class="col-lg-4"><span :class="{ 'text-success' : reviews[check] == 'ACCEPTED', 'text-danger' : reviews[check] == 'NEEDS_FIXING', 'text-dark' : !reviews[check] }"><span v-if="reviews[check]=='ACCEPTED'" class="fas fa-check"></span><span v-if="reviews[check]=='NEEDS_FIXING'" class="fas fa-exclamation"></span><span v-if="!reviews[check]" class="fas fa-times"></span> <span>{{ $t('appreviews.'+check) }}</span></span></div></div></div></td>
            </tr>
          </table>
          <div class="extraspace"></div>
		  
		  <p><b v-t="'manageapp.choose'"></b></p>
		  <table class="table clickable">
			<tr>
			  <td @click="go('editapp')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/editapp.jpg"></div>														   
				<div><b v-t="'manageapp.edit_btn'"></b> <span class="badge" :class="{ 'badge-success' : reviews.DESCRIPTION=='ACCEPTED', 'badge-danger' : reviews.DESCRIPTION=='NEEDS_FIXING', 'badge-light' : !reviews.DESCRIPTION }" style="margin-left:10px"><span v-if="reviews.DESCRIPTION">{{ $t('manageapp.'+reviews.DESCRIPTION) }}</span><span v-if="!reviews.DESCRIPTION" v-t="'manageapp.not_reviewed'"></span></span></div>
				<div v-t="'manageapp.edit_help'"></div>																
			  </td>
			</tr>
			<tr>
			  <td @click="go('query')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/question.jpg"></div>														   
				<div><b v-t="'manageapp.query_btn'"></b><span class="badge" style="margin-left:10px" v-if="hasCount(app.defaultQuery)">1 <span v-t="'manageapp.defined'"></span></span><span class="badge badge-danger" style="margin-left:10px" v-if="!hasCount(app.defaultQuery)">0 <span v-t="'manageapp.defined'"></span></span> <span class="badge" :class="{ 'badge-success' : reviews.ACCESS_FILTER=='ACCEPTED', 'badge-danger' : reviews.ACCESS_FILTER=='NEEDS_FIXING', 'badge-light' : !reviews.ACCESS_FILTER }" style="margin-left:10px"><span v-if="reviews.ACCESS_FILTER">{{ $t('manageapp.'+reviews.ACCESS_FILTER) }}</span><span v-if="!reviews.ACCESS_FILTER" v-t="'manageapp.not_reviewed'"></span></span></div>
				<div v-t="'manageapp.query_help'"></div>																
			  </td>
			</tr>
			<tr v-if="app.type=='mobile'">
			  <td @click="go('applogin')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/editapp.jpg"></div>														   
				<div><b v-t="'manageapp.applogin_btn'"></b> <span class="badge" :class="{ 'badge-success' : reviews.TERMS_OF_USE_MATCH_QUERY=='ACCEPTED', 'badge-danger' : reviews.TERMS_OF_USE_MATCH_QUERY=='NEEDS_FIXING', 'badge-light' : !reviews.TERMS_OF_USE_MATCH_QUERY }" style="margin-left:10px"><span v-if="reviews.TERMS_OF_USE_MATCH_QUERY">{{ $t('manageapp.'+reviews.TERMS_OF_USE_MATCH_QUERY) }}</span><span v-if="!reviews.TERMS_OF_USE_MATCH_QUERY" v-t="'manageapp.not_reviewed'"></span></span></div>
				<div v-t="'manageapp.applogin_help'"></div>																
			  </td>
			</tr>
			<tr v-if="app.type!='endpoint'">
			  <td @click="go('appsubscriptions')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/trigger.jpg"></div>														   
				<div><b v-t="'manageapp.subscriptions_btn'"></b><span class="badge" style="margin-left:10px">{{ app.defaultSubscriptions.length || 0 }} <span v-t="'manageapp.defined'"></span></span> </div>
				<div v-t="'manageapp.subscriptions_help'"></div>																
			  </td>
			</tr>
			<tr v-if="app.type == 'visualization' || app.type == 'oauth1' || app.type == 'oauth2'">
			  <td @click="doInstall()">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/testfromlocal.jpg"></div>														   
				<div><b v-t="'manageapp.testfromlocal_btn'"></b></div>
				<div v-t="'manageapp.testfromlocal_help'"></div>																
			  </td>
			</tr>
			<tr v-if="(app.type == 'oauth1' || app.type == 'oauth2')">
			  <td @click="go('autoimport')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/serverimport.jpg"></div>														   
				<div><b v-t="'manageapp.serverimport_btn'"></b></div>
				<div v-t="'manageapp.serverimport_help'"></div>																
			  </td>
			</tr>
			<tr v-if="!(app.type=='analyzer' || app.type=='external' || app.type=='endpoint')">
			  <td @click="go('applink')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/link.jpg"></div>														   
				<div><b v-t="'manageapp.link_btn'"></b> <span class="badge" :class="{ 'badge-success' : reviews.PROJECTS=='ACCEPTED', 'badge-danger' : reviews.PROJECTS=='NEEDS_FIXING', 'badge-light' : !reviews.PROJECTS }" style="margin-left:10px"><span v-if="reviews.PROJECTS">{{ $t('manageapp.'+reviews.PROJECTS) }}</span><span v-if="!reviews.PROJECTS" v-t="'manageapp.not_reviewed'"></span></span></div>
				<div v-t="'manageapp.link_help'"></div>																
			  </td>
			</tr>
			<tr v-if="app.type!='endpoint'">
			  <td @click="go('repository')">				    
				<div class="float-left" ><img width="80" class="img-responsive" src="/images/repository.jpg"></div>														   
				<div><b v-t="'manageapp.repository_btn'"></b>
                    <span class="badge" :class="{ 'badge-success' : reviews.CODE_REVIEW=='ACCEPTED', 'badge-danger' : reviews.CODE_REVIEW=='NEEDS_FIXING', 'badge-light' : !reviews.CODE_REVIEW }" style="margin-left:10px">
                        <span v-if="reviews.CODE_REVIEW">{{ $t('manageapp.'+reviews.CODE_REVIEW) }}</span><span v-if="!reviews.CODE_REVIEW" v-t="'manageapp.not_reviewed'"></span></span></div>
				<div v-t="'manageapp.repository_help'"></div>																	
			  </td>
			</tr>
			<tr v-if="app.type!='endpoint'">
			  <td @click="go('appicon')">				    
				<div class="float-left" ><img width="80" class="img-responsive" src="/images/editicons.jpg"></div>														   
				<div><b v-t="'manageapp.icon_btn'"></b><span class="badge" style="margin-left:10px">{{ app.icons.length || 0 }} <span v-t="'manageapp.defined'"></span></span> <span class="badge" :class="{ 'badge-success' : reviews.ICONS=='ACCEPTED', 'badge-danger' : reviews.ICONS=='NEEDS_FIXING', 'badge-light' : !reviews.ICONS }" style="margin-left:10px"><span v-if="reviews.ICONS">{{ $t('manageapp.'+reviews.ICONS) }}</span><span v-if="!reviews.ICONS" v-t="'manageapp.not_reviewed'"></span></span></div>
				<div v-t="'manageapp.icon_help'"></div>	
																	
			  </td>
			</tr>
			<tr  v-if="!(app.type=='analyzer' || app.type=='external' || app.type=='endpoint')">
			  <td @click="go('appmessages')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/mail.jpg"></div>														   
				<div><b v-t="'manageapp.messages_btn'"></b><span class="badge" style="margin-left:10px">{{ keyCount(app.predefinedMessages) }} <span v-t="'manageapp.defined'"></span></span> <span class="badge" :class="{ 'badge-success' : reviews.MAILS=='ACCEPTED', 'badge-danger' : reviews.MAILS=='NEEDS_FIXING', 'badge-light' : !reviews.MAILS }" style="margin-left:10px"><span v-if="reviews.MAILS">{{ $t('manageapp.'+reviews.MAILS) }}</span><span v-if="!reviews.MAILS" v-t="'manageapp.not_reviewed'"></span></span></div>
				<div v-t="'manageapp.messages_help'"></div>																
			  </td>
			</tr>
			<tr>
			  <td @click="go('appstats')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/stats.jpg"></div>														   
				<div><b v-t="'manageapp.stats_btn'"></b> <span class="badge" :class="{ 'badge-success' : reviews.QUERIES=='ACCEPTED', 'badge-danger' : reviews.QUERIES=='NEEDS_FIXING', 'badge-light' : !reviews.QUERIES }" style="margin-left:10px"><span v-if="reviews.QUERIES">{{ $t('manageapp.'+reviews.QUERIES) }}</span><span v-if="!reviews.QUERIES" v-t="'manageapp.not_reviewed'"></span></span></div>
				<div v-t="'manageapp.stats_help'"></div>																
			  </td>
			</tr>
			<tr v-if="allowExport">
			  <td @click="go('usagestats')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/usagestats.jpg"></div>														   
				<div><b v-t="'manageapp.usagestats_btn'"></b></div>
				<div v-t="'manageapp.usagestats_help'"></div>																
			  </td>
			</tr>			
			<tr>
			  <td @click="go('appdebug')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/apitest.jpg"></div>														   
				<div><b v-t="'manageapp.debug_btn'"></b></div>
				<div v-t="'manageapp.debug_help'"></div>																
			  </td>
			</tr>
			
			<tr v-if="app.type!='endpoint'">
			  <td @click="go('applicence')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/licence.jpg"></div>														   
				<div><b v-t="'manageapp.applicence_btn'"></b></div>
				<div v-t="'manageapp.applicence_help'"></div>																
			  </td>
			</tr>
			
			
			<tr v-if="allowExport">
			  <td @click="exportPlugin()">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/export.jpg"></div>														   
				<div><b v-t="'manageapp.export_btn'"></b></div>
				<div v-t="'manageapp.export_help'"></div>																
			  </td>
			</tr>
			<tr>
			  <td @click="go('appreviews')">				    
				<div class="float-left"><img width="80" class="img-responsive" src="/images/checked.jpg"></div>														   
				<div><b v-t="'manageapp.appreviews_btn'"></b></div>
				<div v-t="'manageapp.appreviews_help'"></div>																
			  </td>
			</tr>
			
		 </table>
	  </div>								    	 
    </panel>
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import terms from "services/terms.js"
import formats from "services/formats.js"
import session from "services/session.js"
import languages from "services/languages.js"
import apps from "services/apps.js"
import { rl, status, ErrorBox } from 'basic-vue3-components'
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
		app : { version:0, tags:[], i18n : {}, sendReports:true, redirectUri : "http://localhost", targetUserRole : "ANY", requirements:[], defaultQuery:{ content:[] }, tokenExchangeParams : "client_id=<client_id>&grant_type=<grant_type>&code=<code>&redirect_uri=<redirect_uri>"  },
		allowDelete : false,
		allowExport : false,
		allowStudyConfig : false,
		languages : languages.array,
		requirements : apps.userfeatures,
		writemodes : apps.writemodes,
		query : {},
		codesystems : formats.codesystems,
		terms : []
    }),

    components: {  Panel, ErrorBox },

    mixins : [ status, rl ],

    methods : {

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
                if (!app.requirements) { app.requirements = []; }
				if (!app.defaultSubscriptions) app.defaultSubscriptions = [];
				if (!app.icons) app.icons = [];
                if (app.developerTeamLogins) app.developerTeamLoginsStr = app.developerTeamLogins.join(", "); 
				else app.developerTeamLogins = [];
                if (app.type === "oauth2" && ! (app.tokenExchangeParams) ) app.tokenExchangeParams = "client_id=<client_id>&grant_type=<grant_type>&code=<code>&redirect_uri=<redirect_uri>";
                app.defaultQueryStr = JSON.stringify(app.defaultQuery);
                //$data.updateQuery();
                $data.app = app;        
            }));
		
            me.doBusy(server.get(jsRoutes.controllers.Market.getReviews(appId).url)
            .then(function(reviews) {
                $data.reviews = {};
                for (var i=0;i<reviews.data.length;i++) {
                    var status = reviews.data[i].status;
                    $data.reviews[reviews.data[i].check] = (status == "OBSOLETE" ? null : status);
                }			
            }).catch(function() { $data.reviews = {}; }));
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
		
			for (let lang of $data.languages) {			
				if ($data.app.i18n[lang] && $data.app.i18n[lang].name == "") {
					delete $data.app.i18n[lang];
				} 
			}
			
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
				$data.app.defaultQueryStr = JSON.stringify($data.app.defaultQuery);
				//$scope.updateQuery();				
			}
		}));
	
    }
}
</script>