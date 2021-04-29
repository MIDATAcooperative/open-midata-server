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
  <div class="container">

     <div class="row">

		<div class="col-sm-12">
			<panel style="max-width: 600px; padding-top: 20px; margin: 0 auto;" :busy="isBusy">			
	  				<div v-if="pleaseConfirm">			
						<p>{{ $t('oauth2.please_confirm', { consent : consent }) }}"></p>						    			
					</div>

					
					<div>			    			
						<div class="midatalogo">
						  <img src="/images/logo.png" style="height: 36px;" alt="">
						</div>
						<div v-if="hasIcon()" class="appicon" :style="getIconUrlBG()">					
					</div>
                	<div class="mi-or-login__instance"></div>
					<p>
						<strong>{{ appname() }}</strong> <span v-t="'oauth2.requests_permission'"></span>
					</p>
							
					<div v-if="!hideRegistration">				
						<p v-t="'oauth2.no_account'"></p>
							
						<button class="btn btn-primary btn-block" type="button" @click="showRegister(true);">
						<span v-t="'oauth2.sign_up_btn'"></span>					  
						</button>					
						<hr>
						<div style="margin-top:20px"></div>
						<div v-t="'oauth2.have_account'"></div>
						<div v-t="'oauth2.have_account2'"></div>
						<div class="extraspace"></div>
				    </div>
				
					<form @submit.prevent="dologin()" role="form" v-if="!offline" ref="myform" novalidate class="required">

						<FormGroup label="login.email_address" name="username" :path="errors.username">
							<input :disabled="studies" required name="username" v-validate v-model="login.email" class="form-control"
								type="email" /> 
						</FormGroup>

						<FormGroup label="login.password" :path="errors.password">
							<password required  name="password" :disabled="studies" v-model="login.password" class="form-control"
								/>
						</FormGroup>

						<FormGroup label="oauth2.role" v-if="app.targetUserRole=='ANY'">
							<select id="account" name="account" :disabled="studies" v-model="login.role" class="form-control" required>
								<option v-for="role in roles" :key="role.value" :value="role.value" v-t="role.name"></option>
							</select> 
						</FormGroup>

						<FormGroup label="login.study" v-if="studies">
							<select id="study" name="study" v-model="login.studyLink" class="form-control" required>
								<option v-for="study in studies" :key="study._id" :value="study._id">{{ study.name }}</option>							
							</select>
						</FormGroup>
                        <error-box :error="error" />

						<div class="alert alert-danger" v-if="offline">
						<span v-t="'error.offline'"></span>
						</div>

						<button class="btn btn-block btn-primary" v-submit type="submit" :disabled="action=='login' || doneLock">
							<span v-t="'oauth2.sign_in_btn'">login</span>
						</button>

					</form>
					<div class="extraspace"></div>
					<div>				
						<a v-t="'login.forgot_your_password'" href="javascript:" @click="lostpw();"></a>
					</div>
																
					<div>
						<small>{{ $t('oauth2.device', { 'device' : device }) }}</small>
					</div>
				<!-- <div class="mi-or-login__legal_mentions">
					<span class="mi-at-text mi-at-text--smallest mi-at-text--white">(c) 2018 ETH Zürich</span>
				</div>  -->
				</div>			
			</panel>
		</div>		
  </div>
  
</div>

</template>
<script>

import server from "services/server.js";
import oauth from "services/oauth.js";
import { status, FormGroup, ErrorBox, Password } from 'basic-vue3-components';
import session from "services/session.js";
import ENV from "config";
import { getLocale, setLocale } from "services/lang.js";
import Panel from 'components/Panel.vue';

function getAppInfo(name, type) {
    var data = { "name": name };
    if (type) data.type = type;
    return server.post(jsRoutes.controllers.Plugins.getInfo().url, data);
};


export default {
  data: () => ({
    app : { i18n : { en : { name : "" }}},	
    login : { role : "MEMBER", confirmStudy:[] },
    lang : 'en',
	pleaseConfirm : false,
	hideRegistration : false,
	offline : false,
	labels : [],
	roles : [
		{ value : "MEMBER", name : "enum.userrole.MEMBER" },
		{ value : "PROVIDER" , name : "enum.userrole.PROVIDER"},
		{ value : "RESEARCH" , name : "enum.userrole.RESEARCH"}
	],
	device : "",
	consent : "",
	studies : null,	
	doneLock : false
  }),

  components : {
     FormGroup, ErrorBox, Panel, Password
  },

  mixins : [ status ],
  
  methods: {
  
  	 hasIcon() {
		    if (!this.$data.app || !this.$data.app.icons) return false;
		    return this.$data.app.icons.indexOf("LOGINPAGE") >= 0;
	   },
	
	   getIconUrl() {
		    if (!this.$data.app) return null;
		    return ENV.apiurl + "/api/shared/icon/LOGINPAGE/" + this.$data.app.filename;
	   },
	
	   getIconUrlBG() {
		    if (!this.$data.app) return null;
		    return { "background-image" : "url('"+ENV.apiurl + "/api/shared/icon/LOGINPAGE/" + this.$data.app.filename+"')" };
	   },

	   showRegister() {
		  const { $route, $router } = this;
		  let params = JSON.parse(JSON.stringify($route.query));
		  params.login = params.email;
		  $router.push({ path : "./registration", query : params }); 		  
	   },
	
	   lostpw() {
		 const { $router } = this;
		 $router.push({ path : "./lostpw" });
	   },

	   appname() {
		 const { $data } = this;
		 if ($data.app && $data.app.i18n && $data.app.i18n[$data.lang]) return $data.app.i18n[$data.lang].name;
		 return $data.app.name;
	   },

	   dologin() {
		   let { $data, $route, $router } = this;
		  
		oauth.setUser($data.login.email, $data.login.password, $data.login.role, $data.login.studyLink);
		if ($route.query.joincode) oauth.setJoinCode($route.query.joincode);
		if ($route.query.project) oauth.setProject($route.query.project);
		
		this.doAction("login", oauth.login(false))
		.then(function(result) {
			
		  if (result === "CONFIRM" || result === "CONFIRM-STUDYOK") {
			  let params = JSON.parse(JSON.stringify($route.query)) || {};
			  if (result === "CONFIRM-STUDYOK") params.nostudies = true;
			  
			  $router.push({ path : "./oauthconfirm", query : params });
			  
		  } else if (result !== "ACTIVE") {
			  if (result.studies) {
				  $data.studies = result.studies;
			  } else if (result.istatus) { $data.pleaseConfirm = true; }
			  else {
				  session.postLogin({ data : result}, $router, $route);  
			  }
		  } else { $data.doneLock = true; }
		});
        
	   }
  },

  created: function () {
	  const { $route, $data, $methods } = this, me = this;		  
	  $data.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	
	  $data.lang = getLocale();

      if ($route.query.lang) {
		  $data.lang = $route.query.lang;
		  setLocale($route.query.lang);
	  }

	  if ($route.query.email) {
		$data.login.email = $route.query.email;
	  }
	
	  if ($route.query.login) {
		$data.login.email = $route.query.login;
	  }

      this.doBusy(getAppInfo($route.query.client_id)     	
		  .then(function(results) {
			$data.app = results.data;      
			if (!$data.app || !$data.app.targetUserRole) $data.error ="error.unknown.app";
				
			$data.login.role = $data.app.targetUserRole === 'ANY'? "MEMBER" : $data.app.targetUserRole;
			oauth.init(me, $route.query.client_id, $route.query.redirect_uri, $route.query.state, $route.query.code_challenge, $route.query.code_challenge_method, $route.query.device_id);
			$data.device = oauth.getDeviceShort();
			$data.consent = "App: "+$data.app.name+" (Device: "+$data.device+")";			
			oauth.app = $data.app;
				
			if ($route.query.isnew=="true") $methods.showRegister();
			if ($route.query.isnew=="never") $data.hideRegistration = true;
		
		}));  
  }
};
</script>
<style scoped>
 .has-value {
	font-size: 12px;
    top: -20px;
 }
 
 body.localhost .mi-or-login__instance::after {
    content : 'Instance: localhost';    
 }

  body.test .mi-or-login__instance::after {
    content : 'Instance: test.midata.coop';      
  }

  body.demo .mi-or-login__instance::after {
    content : 'Instance: demo.midata.coop';    
  }
  
  .appicon {
    width:90px;
    height:90px;
    background-size: 95px,95px,cover;    
    position: relative;
    border-radius: 18px;
    background-position: center;
    background-repeat: no-repeat;
    background-color: white;
    left: 50%;
    -webkit-transform: translateX(-50%);
    transform: translateX(-50%);
    margin-top: -10px;
    margin-bottom: 10px;
  }
</style>