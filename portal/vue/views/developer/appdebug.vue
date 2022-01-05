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
		
        <form class="form">
        
        <div v-if="saved.length">
        <label>Saved Requests:</label>
       <div class="list-group">
        <a v-for="ext in saved" :key="ext.url" class="list-group-item clearfix" href="javascript:" @click="doload(ext);">[{{ ext.type }}] <b>{{ ext.url }}</b> (<span class="small text-info">{{ ext.body }}</span>)<div class="float-right"><button type="button" class="btn btn-sm btn-default" @click="dodelete(ext);">delete</button></div></a>
       </div>
       
       </div>
        
       <div class="row extraspace" v-if="app.type=='mobile'">
         <div class="col-2">
           <label v-t="'appdebug.oauth_login'">OAuth-Login:</label>
           <div>
           <a class="btn btn-default" @click="doOauthLogin();" href="javascript:" v-t="'appdebug.login'"></a>
           </div>
         </div>
         <div class="col-2">
           <label v-t="'appdebug.device'">Device:</label>
           <input type="text" class="form-control" v-validate v-model="device">
         </div>
         
         <div class="col-8">
           <label v-t="'appdebug.refresh_token'">Refresh-Token:</label>
           <input type="text" class="form-control" v-validate v-model="refreshToken">
         </div>
       </div>
       
       <div class="row extraspace" v-if="app.type=='visualization'">
      
         <div class="col-3">
           <label v-t="'appdebug.portal_username'">Username:</label>
           <input type="text" class="form-control" v-validate v-model="portal.username">
         </div>
         
         <div class="col-3">
           <label v-t="'appdebug.portal_password'">Password:</label>
           <password class="form-control" v-model="portal.password"></password>
         </div>
         <div class="col-3">
           <label v-t="'appdebug.portal_role'">Role:</label>
           <select v-validate v-model="portal.role" class="form-control">
               <option v-for="role in roles" :key="role.value" :value="role.value">{{ $t(role.name) }}</option>
            </select>
         </div>
         <div class="col-3">
           <label><span v-t="'appdebug.portal_login'"></span> {{ app.name }}</label>
           <div>
           <a class="btn btn-default" @click="portalLogin()" v-t="'appdebug.portal_login_btn'"></a>
           </div>
         </div>
        </div>
        <div class="row extraspace" v-if="error">
         <div class="col-12" >
           <error-box :error="error"></error-box>
         </div>
       </div>
       
       <div class="row extraspace" v-if="app.defaultSubscriptions && app.defaultSubscriptions.length">
         <div class="col-2">
          <label v-t="'appdebug.backend_services'">Backend Services:</label>
        
		  <button :disabled="action!=null" type="button" class="btn btn-default" @click="stopDebug()" v-if="app.debugHandle" v-t="'appsubscriptions.debug.stop_btn'"></button>
      <button :disabled="action!=null" type="button" class="btn btn-default" @click="startDebug()" v-else v-t="'appsubscriptions.debug.start_btn'"></button>
		 </div>
		  <div class="col-8">
		  <label v-if="app.debugHandle" v-t="'appsubscriptions.debug.cmd_to_run'"></label>
		  <pre v-if="app.debugHandle">npx midata-tester {{ server }} {{ app.debugHandle }}</pre>
		  </div>
        </div>
        

        
        <div class="row extraspace">
         <div class="col-12"><label>Request to backend: {{ server }}</label></div>
         <div class="col-2">           
           <select class="form-control" v-validate v-model="fhirRelease">
               <option v-for="rel in releases" :key="rel.name" :value="rel.header">{{ rel.name }}</option>
            </select>
         </div>
         <div class="col-2">           
           <select class="form-control" v-validate v-model="type"><option>GET</option><option>POST</option><option>PUT</option><option>DELETE</option></select>
         </div>
         <div class="col-8">
         <input type="text" class="form-control" v-validate v-model="url">
         </div>
        </div>

        <div class="row">          
          <div class="col-12 extraspace">
          <label>Authorization:</label>
          <input class="form-control" v-validate v-model="authheader" >
          </div>
        </div>

        <div class="row" v-if="type != 'GET'">
          <div class="col-12 extraspace">
          <label>Body:</label>
          <textarea class="form-control" v-validate v-model="body" rows="10"></textarea>
          </div>
        </div>
        
        <div class="row">
        <div class="col-12 extraspace">
        <router-link :to="{ path : './manageapp', query : {appId:appId}}" class="btn btn-default mr-1" v-t="'common.back_btn'"></router-link>
        <button class="btn btn-primary mr-1" :disabled="action!=null" type="button" @click="dosubmit();">submit</button> 
        <button class="btn btn-default mr-1" :disabled="action!=null" type="button" @click="dosave();">save</button>
        </div>
        </div>
       
      </form>
      
      <div class="row">
      <div class="col-12">
      <label>Request Results:</label>
      <textarea class="form-control" v-validate v-model="results" rows="40"></textarea>
      </div>
      </div>
      
	</panel>
</template>
<script>

import ChangeLog from "components/tiles/ChangeLog.vue"
import Panel from "components/Panel.vue"
import session from "services/session.js"
import server from "services/server.js"
import apps from "services/apps.js"
import { rl, status, ErrorBox, FormGroup, Password } from 'basic-vue3-components'
import ENV from "config";
import Axios from "axios";
import _ from "lodash";

export default {

    data: () => ({	        
        server : ENV.apiurl,
        app : null,
        url : "/fhir/Patient",
        type : "GET",
        authheader : "",
        results : "empty",
        count : 1,
        saved : [],
        device : sessionStorage.oldDevice || "debug",
        portal : { username : "", password : "", role:"MEMBER" },
        releases : [
    	   { name : "FHIR STU3", header : "fhirVersion=3.0" },
    	   { name : "FHIR R4", header : "fhirVersion=4.0" }
        ],
        fhirRelease : sessionStorage.oldFhirRelease || "fhirVersion=4.0",
        roles : [
            { value : "MEMBER", name : "enum.userrole.MEMBER" },
            { value : "PROVIDER" , name : "enum.userrole.PROVIDER"},
            { value : "RESEARCH" , name : "enum.userrole.RESEARCH"},   		
            { value : "DEVELOPER" , name : "enum.userrole.DEVELOPER"}
        ],
        body : "",
        refreshToken : ""
    }),

    components: {  Panel, ErrorBox, FormGroup, ChangeLog, Password },

    mixins : [ status, rl ],

    methods : {
        getTitle() {
            const { $data, $t } = this, me = this;
            if (!$data.app) return $t('appdebug.title');
            return $t('appdebug.title')+": "+$data.app.name+" "+$data.app.filename;
        },
        dosubmit() {
          const { $data, $route } = this, me = this;
            if (!$data.url.startsWith("/")) {
              $data.results = "Invalid URL: No '/' at beginning of path.";
              return;
            }  
          
          var url = $data.server + $data.url;
                          
          var body = "";
          
          if ($data.type !== "GET") {
          try {
            body = JSON.parse($data.body);
          } catch (e) {          
            $data.results = "Error processing body: \n"+e;
            return;
          }
          }
          
          var call = { method: $data.type, url: url, data : body, headers:{} }; 
          if ($data.authheader) call.headers = { "Authorization" : $data.authheader, "Prefer" : "return=representation" };
          call.headers.Accept = "application/fhir+json; "+$data.fhirRelease;
          
          $data.results = "Processing request...";
                          
            Axios(call)
            .then(function(data1) {
              var data  = data1.data;
              if ($data.url === "/v1/auth" && data.authToken) {
                $data.authheader = "Bearer " + data.authToken;
              } 
              $data.results = JSON.stringify(data, null, 2); 
              }, function(x) { $data.results = x.response.status + ":" + JSON.stringify(x.response.data, null, 2); });
          
        },

        dosave() {
          const { $data, $route } = this, me = this;
          $data.saved.push({ type : $data.type, url : $data.url, body : ($data.type != "GET" ? $data.body : "") } );
          window.localStorage.urls = JSON.stringify($data.saved);
        },

        doload(e) {
          const { $data, $route } = this, me = this;
          $data.url = e.url;
          $data.body = e.body;
          $data.type = e.type;
        },

        dodelete(e) {
          const { $data, $route } = this, me = this;
          $data.saved.splice($data.saved.indexOf(e), 1);
          window.localStorage.urls = JSON.stringify($data.saved);
        },

        init(userId, appId) {
          const { $data, $route } = this, me = this;
          $data.userId = userId;
          $data.appId = appId;
                
          me.doBusy(apps.getApps({ "_id" : appId }, ["filename", "name", "targetUserRole", "secret", "redirectUri", "debugHandle", "type", "defaultSubscriptions"])
          .then(function(data) { 
              $data.app = data.data[0];
              //sessionStorage.returnTo = document.location.href;
              //$data.body = "{\n    \"appname\":\""+$data.app.filename+"\",\n    \"device\" : \"debug\",\n    \"secret\" : \""+$data.app.secret+"\",\n    \"username\" : \"FILLOUT\",\n    \"password\" : \"FILLOUT\",\n    \"role\" : \""+$data.app.targetUserRole+"\"\n}";
              
              if ($route.query.code) {
                me.requestAccessToken($route.query.code);
              }
          }));
	    },
	
	    reload() {
        const { $data, $route } = this, me = this;
		    me.init($data.userId, $data.appId);
	    },
	
	    getOAuthLogin() {
        const { $data, $route } = this, me = this;
        if (!$data.app) return "";
        var back = document.location.href;				
        if (back.indexOf("?") > 0) back = back.substr(0, back.indexOf("?"));
        return "/oauth.html#/portal/oauth2?response_type=code&client_id="+encodeURIComponent($data.app.filename)+"&redirect_uri="+encodeURIComponent(back)+"&device_id="+encodeURIComponent($data.device);
	    },
	
	    doOauthLogin() {
        const { $data, $route } = this, me = this;
        sessionStorage.oldToken = sessionStorage.token;
        sessionStorage.oldDevice = $data.device;
        sessionStorage.oldFhirRelease = $data.fhirRelease;
        sessionStorage.oldApp = $data.app._id;
        window.document.location.href = me.getOAuthLogin();
	    },
	
	    startDebug() {
        const { $data, $route } = this, me = this;
	    	var data = { plugin : $data.app._id, action : "start" };
	        me.doAction("debug", server.post(jsRoutes.controllers.Market.setSubscriptionDebug().url, data))
	        .then(function(result) {
	     	   $data.app.debugHandle = result.data.debugHandle;
	        });
	    },
	    
	    stopDebug() {
        const { $data, $route } = this, me = this;
	       var data = { plugin : $data.app._id, action : "stop" };
	       me.doAction("debug", server.post(jsRoutes.controllers.Market.setSubscriptionDebug().url, data))
	       .then(function(result) {
	    	   $data.app.debugHandle = result.data.debugHandle;
	       });
	    },
	 
	    portalLogin() {
        const { $data, $route } = this, me = this;
        $data.error = null;
        var data = {"email": $data.portal.username, "password": crypto.getHash($data.portal.password), "role":$data.portal.role };
        var func = function(data) {
            return server.post(jsRoutes.controllers.Application.authenticate().url, data);
        };
          
        session.performLogin(func, data, $data.portal.password)
        .then(function(result) {
          if (result.data.sessionToken) {
            var properties = {"visualization": $data.app._id};
                var fields = ["name", "visualization", "type", "owner", "order"];
                var data = {"properties": properties, "fields": fields};
                return Axios.post(ENV.apiurl + jsRoutes.controllers.Spaces.get().url, data, { headers : { "X-Session-Token" : result.data.sessionToken, "Prefer" : "return=representation" } })
                  .then(function(r2) {
                    if (r2.data.length>0) {
                      var spaceId = r2.data[0]._id;
                      return Axios.get(ENV.apiurl + jsRoutes.controllers.Spaces.getUrl(spaceId, r2.data[0].owner).url, { headers : { "X-Session-Token" : result.data.sessionToken, "Prefer" : "return=representation" } })
                      .then(function(r3) {
                        $data.authheader = "Bearer "+r3.data.token;
                        //console.log(r3.data);
                      });
                    } else { $data.error = "error.missing.plugin"; } 
                  });			       
                
          }
        }).catch(function(err) { $data.error = err.response.data; });
	    },
        
      requestAccessToken(code) {
        const { $data, $route } = this, me = this;   
            //console.log("CODE: "+code);
            var body = "grant_type=authorization_code&redirect_uri=x&client_id="+encodeURIComponent($data.app.filename)+"&code="+encodeURIComponent(code);	    
            Axios.post(ENV.apiurl+"/v1/token", body, { headers : { 'Content-Type': 'application/x-www-form-urlencoded' } }).then(function(result) {
            //console.log(result.data);
            
            $data.authheader = "Bearer " + result.data.access_token;
            $data.refreshToken = result.data.refresh_token;
            
            }); 
	    },
	
      onAuthorized(url) {
        const { $data, $route } = this, me = this;
            var message = null;
            var error = null;

            var arguments1 = url.split("&");
            var keys = _.map(arguments1, function(argument) { return argument.split("=")[0]; });
            var values = _.map(arguments1, function(argument) { return argument.split("=")[1]; });
            var params = _.zipObject(keys, values);
            
            if (_.has(params, "error")) {
                error = "The following error occurred: " + params.error + ". Please try again.";
            } else if (_.has(params, "code")) {
                message = "User authorization granted. Requesting access token...";
                me.requestAccessToken(params.code);
            } else if (_.has(params, "oauth_verifier")) {
                message = "User authorization granted. Requesting access token...";
                me.requestAccessToken(params.oauth_verifier, params);
            } else {
                error = "An unknown error occured while requesting authorization. Please try again.";
            }
    
            
            
            $data.message = message;
            $data.error = error;
            if (error) {
                $data.authorizing = false;
            }
        
            //authWindow.close();
      }	
		
    },

    created() {
      const { $data, $route } = this, me = this;
        if (window.localStorage.urls) {
          $data.saved = JSON.parse(window.localStorage.urls);
          if (!$data.saved) $data.saved = [];         
          //console.log($data.saved);
        }
        session.currentUser.then(function(userId) { me.init(userId, $route.query.appId || sessionStorage.oldApp); });	
    }
}
</script>