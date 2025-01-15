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
			<!-- Login -->
			<div class="col-sm-12">
			    <div class="d-none d-lg-block" style="padding-top:100px;"></div>
				<div class="panel-container" style="max-width:600px; padding-top:20px; margin:0 auto;">
					<div class="panel panel-primary">
		            	<div class="panel-heading">
		              		<h3 class="panel-title" v-t="'setpw.title'"></h3>
		            	</div>
		            	<div class="panel-body">
			            	<error-box :error="error" />

							<div class="alert alert-success" v-if="success">
								<p v-t="'setpw.success'"></p>
								<p v-if="homeUrl">
								  <a :href="homeUrl" v-t="'setpw.back_to_login'"></a>
								</p>
								<p v-else>
									<router-link :to="{ path : '/public/login' }" v-t="'setpw.back_to_login'"></router-link>
								</p>
							</div>
							<div v-if="!success">
								<p v-t="'setpw.enter_new'"></p>
								<p v-if="advancedPassword()" v-t="'registration.password_policy2'"></p>
								<p v-else v-t="'registration.password_policy'"></p>
								
								<form @submit.prevent="submit()" name="myform" ref="myform" role="form" novalidate>
									<password class="form-control" :placeholder="$t('setpw.new_password')" v-model="setpw.password" name="password" required style="margin-bottom:5px;" autofocus />								
									<password class="form-control" :placeholder="$t('setpw.new_password_repeat')" v-model="setpw.passwordRepeat" name="passwordRepeat" required style="margin-bottom:5px;" />
									<div class="d-grid gap-2 mt-3 mb-2">
									  <button type="submit" :disabled="action!=null" class="btn btn-primary" v-submit v-t="'setpw.set_new_btn'">Set New Password</button>
									</div>
								</form>
							</div>
		            	</div>
					</div>
				</div>
			</div>
			
		</div>
	</div>
</template>
<script>
import server from "services/server.js";
import { status, FormGroup, ErrorBox, Password } from 'basic-vue3-components';
import crypto from "services/crypto.js";

function getAppInfo(name, type) {
    var data = { "name": name };
    if (type) data.type = type;
    return server.post(jsRoutes.controllers.Plugins.getInfo().url, data);
};

export default {
  data: () => ({
     success : false,
     secure : false,
     role : "member",
	 homeUrl : null,
     setpw : {
        token : "",
		password : "",
        passwordRepeat : "",
		app : null
     }
     
  }),

  components : {
     FormGroup, ErrorBox, Password
  },

  mixins : [ status ],
 
  methods : {
  	 advancedPassword() {
	    return this.$data.role != "member";
	 },
	 
     submit() {
                  
        const { $data } = this;      
		var pwvalid = crypto.isValidPassword($data.setpw.password, $data.role != "member");         
        if (!pwvalid) {
        	$data.error = { code : (($data.role != "member") ? "error.tooshort.password2" : "error.tooshort.password") };
        	return;
        }
		
		if (!$data.setpw.passwordRepeat || $data.setpw.passwordRepeat != $data.setpw.password) {
			$data.error = { code : "error.invalid.password_repetition" };
			return;
		}
				
		this.doAction("submit", crypto.generateKeys($data.setpw.password).then(function(keys) {
			var data = { "token": $data.setpw.token };
			
			if ($data.secure) {
				data.password = keys.pw_hash;
				data.pub = keys.pub;
				data.priv_pw = keys.priv_pw;
				data.recovery = keys.recovery;
				data.recoveryKey = keys.recoveryKey;		
			} else {
				data.password = $data.setpw.password;
			}
			return server.post(jsRoutes.controllers.Application.setPasswordWithToken().url, data);
		}).then(function() { 
			
			if ($data.setpw.app) {
				getAppInfo($data.setpw.app)     	
				.then(function(results) {
				  if (results.data && results.data.homeUrl) {
					$data.homeUrl = results.data.homeUrl;
				  }
				  $data.success = true;
				});
			} else $data.success = true;
        }));	
     }
  },

  created() {    
     const { $data, $route } = this;
     $data.setpw.token = $route.query.token;
     $data.secure = $route.query.ns != 1;	
     $data.role = $route.query.role || "member";	
	 $data.setpw.app = $route.query.app;
     this.loadEnd();
  }
}
</script>