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
    <div class="midata-overlay borderless">
	    <div class="overlay-body">
            <panel :title="title" :busy="isBusy">
                <error-box :error="error"></error-box>

                <div v-if="!authorized">
                    <p>
				    The MIDATA plugin called <b>{{ appName }}</b> needs authorization for access to an external site to import data on your behalf.
				    <br><br>
				    After clicking on "Authorize Now" you will be redirected to an external login page.
				    </p>
				    <button type="button" class="btn btn-success" :disabled="authorizing" @click="authorize()" v-t="'importrecords.authorize_btn'"></button>
				    <div class="extraspace"></div>				
                </div>
				<p v-if="message" class="alert alert-info">{{message}}</p>
				<div v-if="authorized">			      	
				  <div id="iframe" style="min-height:200px;width:100%;" v-pluginframe="url"></div>								
				</div>                
            </panel>
        </div>
    </div>
</template>
<script>
import Panel from 'components/Panel.vue';
import { getLocale } from 'services/lang.js';
import spaces from 'services/spaces.js';
import server from 'services/server.js';
import session from 'services/session.js';
import { status, ErrorBox } from 'basic-vue3-components';
import _ from "lodash";

let app = {};

export default {

	data: () => ({
		title : "",
		userId : null,
		spaceId : null,
		url : null,
        params : null,
        appName : "",
        authorized : false,
        authorizing : false,
        message : null
	}),				

	components : { ErrorBox, Panel },

    mixins : [ status ],

    methods : {
        openAppLink(data) {
            const { $data, $route, $router } = this;
		    data = data || {};
		    data.user = $route.query.user;
		    spaces.openAppLink($router, $route, $data.userId, data);	 
		},
		
		    
        getAuthToken(space, again) {
            const { $data, $route } = this, me = this;
		    var func = again ? me.doBusy(spaces.regetUrl(space, $route.query.user)) : me.doBusy(spaces.getUrl(space, $route.query.user));
		    func.then(function(result) {
			    if (result.data && result.data.authorizationUrl) {
		        app = result.data;
                $data.appName = result.data.name;
                $data.title = result.data.name;
			    $data.authorized = false;
			    $data.message = null;	
			  
			    if (sessionStorage.authString) {	
				    $data.authorizing = true;
				    me.onAuthorized(sessionStorage.authString);
				    sessionStorage.removeItem("authString");
				    sessionStorage.removeItem("returnTo");
			    }
			  
			} else {
			  var url = spaces.mainUrl(result.data, getLocale());			  
			  $data.url = url;			  			  
			  $data.message = null;			  			  
			  $data.authorized = true;			 
			}
		});
	},
	
	
	authorize() {
        const { $data } = this, me = this;
		$data.authorizing = true;
		$data.message = "Authorization in progress...";
		var redirectUri = window.location.protocol + "//" + window.location.hostname +  "/authorized.html";
		if (app.type === "oauth2") {
			if (!app.scopeParameters) app.scopeParameters="";
			var parameters = "response_type=code" + "&client_id=" + app.consumerKey + "&scope=" + app.scopeParameters +
				"&redirect_uri=" + redirectUri;
			sessionStorage.returnTo=document.location.href;
			if (app.authorizationUrl.indexOf("?")>=0) parameters = "&"+parameters; else parameters = "?"+parameters;
			document.location.href = app.authorizationUrl + encodeURI(parameters);
		
		} else if (app.type === "oauth1") {
			me.doBusy(server.get(jsRoutes.controllers.Plugins.getRequestTokenOAuth1($data.spaceId).url))
			.then(function(results) {
					authorizationUrl = results.data;					
					me.authorizeOAuth1();
			});
		} else {
			$data.error = "App type not supported yet.";
			$data.authorizing = false;
		}
	},
	
	
	authorizeOAuth1() {
		sessionStorage.returnTo=document.location.href;
		document.location.href = authorizationUrl;				
	},
	
	onAuthorized(url) {
		
		var message = null;
		var error = null;

		var arguments1 = url.split("&");
		var keys = _.map(arguments1, function(argument) { return argument.split("=")[0]; });
		var values = _.map(arguments1, function(argument) { return argument.split("=")[1]; });
		var params = _.object(keys, values);
		
		if (_.has(params, "error")) {
			error = "The following error occurred: " + params.error + ". Please try again.";
		} else if (_.has(params, "code")) {
			message = "User authorization granted. Requesting access token...";
			requestAccessToken(params.code);
		} else if (_.has(params, "oauth_verifier")) {
			message = "User authorization granted. Requesting access token...";
			requestAccessToken(params.oauth_verifier, params);
		} else {
			error = "An unknown error occured while requesting authorization. Please try again.";
		}
 				
		$data.message = message;
		$data.error = error;
		if (error) {
			$data.authorizing = false;
		}
			
	},
	
	requestAccessToken(code, additional) {
        const { $data } = this, me = this;
		var data = {"code": code};
		if (additional) data.params = additional;
		var requestTokensUrl = null; 
		if (app.type === "oauth2") {
			requestTokensUrl = jsRoutes.controllers.Plugins.requestAccessTokenOAuth2($data.spaceId).url;
		} else if (app.type === "oauth1") {
			requestTokensUrl = jsRoutes.controllers.Plugins.requestAccessTokenOAuth1($data.spaceId).url;
		}
		server.post(requestTokensUrl, data).
        then(function() {
            $data.authorized = true;
            $data.authorizing = false;
            $data.message = "Loading app...";
            getAuthToken($data.spaceId, true);
        }, function(err) {
            $data.error = "Requesting access token failed: " + err.data;
            $data.authorizing = false;
        });
	},
	
		
	init() {
			const { $data, $route } = this, me = this;
			$data.spaceId = $route.query.spaceId;		
			$data.params = $route.query.params ? JSON.parse($route.query.params) : null;
			this.doBusy(session.currentUser
			.then(function(userId) {
				$data.userId = userId;			
				me.getAuthToken($data.spaceId/*, $state.params.user*/);
			}));
		}
    },

	watch : {		
		$route() { this.init(); }
	},

    created() {
		this.init();
    }
}
</script>