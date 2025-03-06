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
		<div class="col-sm-12" v-if="!terms.active">
            <panel style="max-width: 600px; padding-top: 20px; margin: 0 auto;" :title="$t('postregister.title')" :busy="isBusy">  
							
				<div v-if="progress.REJECT">
					<p v-t="'postregister.reject.intro1'"></p>
					<p v-t="'postregister.reject.intro2'"></p>
					<button class="btn btn-danger" v-t="'postregister.reject.reject_btn'" @click="confirm(false)"></button>
					<hr>
					<p v-t="'postregister.reject.intro3'"></p>
					<button class="btn btn-default" v-t="'postregister.reject.confirm_btn'" @click="confirm(true)"></button>
					<div class="extraspace"></div>
				</div>
				
                <div v-if="progress.RELOGIN">
					<error-box :error="error"></error-box>
					<div class="extraspace">&nbsp;</div>
					<p v-t="'error.relogin'"></p>						  
				</div>
							
				<div v-if="progress.KEYRECOVERY">
					<p v-t="'postregister.keyrecovery.intro'"></p>
					<ul>
						<li v-t="'postregister.keyrecovery.way1'"></li>
						<li v-t="'postregister.keyrecovery.way2'"></li>
						<li v-t="'postregister.keyrecovery.way3'"></li>
						<li v-t="'postregister.keyrecovery.way4'"></li>
					</ul>
				</div>
						
				<div v-if="progress.VALID_LICENCE">
					<p v-t="'postregister.licence.intro'"></p>
					<p>&nbsp;</p>
					<p v-t="'postregister.licence.text'"></p>						  
				</div>
				
				<div v-if="progress.APP_UNLOCK_CODE">
				  <form ref="myform" name="myform" @submit.prevent="setUnlockCode()" role="form" class="form form-horizontal" novalidate>
				    <p v-t="'postregister.unlock_code'"></p>
					<form-group name="unlockCode" label="registration.unlock_code" :path="errors.unlockCode">									
                    	  <input type="text" class="form-control" id="unlockCode"
						   name="unlockCode" :placeholder="$t('registration.unlock_code')" v-model="unlockCode" v-validate required>
					</form-group>		
					<div class="d-grid gap-2 mt-3 mb-2">							
			    	  <button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'common.submit_btn'"></button>
					</div>
			      </form>
				</div>
						
				<div v-if="progress.AUTH2FACTOR || progress.PHONE_VERIFIED">
					<p v-t="'postregister.auth2factor'"></p>					
					<form ref="myform" name="myform" @submit.prevent="setSecurityToken()" role="form" class="form form-horizontal" novalidate>
						<form-group name="securityToken" label="postregister.securityToken" :path="errors.securityToken">
							<input type="text" class="form-control" name="securityToken" v-model="setpw.securityToken" style="margin-bottom:5px;" required v-validate ref="tokenInput" autofocus>
						</form-group>							  
						<div class="extraspace"></div>
						<div class="d-grid gap-2 mt-3 mb-2">
						  <button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'postregister.securityToken_btn'"></button>
						</div>						  	
						<div class="extraspace"></div>
                        <error-box :error="error"></error-box>
							  
						<div v-if="progress.PHONE_VERIFIED">							   							  
							<p v-t="'postregister.no_token'"></p>
							<div class="d-grid gap-2 mt-3 mb-2">
							  <button @click="noSecurityToken()" type="button" :disabled="action!=null" class="btn btn-default" v-t="'postregister.no_token_btn'"></button>
							</div>  
						</div>
						<div class="extraspace"></div>
					</form>
				</div>
							
				<div v-if="progress.PASSWORD_SET">
					<p v-t="'setpw.enter_new'"></p>
					<form ref="myform" name="myform" @submit.prevent="pwsubmit()" role="form" class="form form-horizontal" novalidate>
						<form-group name="password" label="setpw.new_password" :path="errors.password">
							<password class="form-control" name="password" v-model="setpw.password" style="margin-bottom:5px;" autofocus required />
						</form-group>
						<form-group name="passwordnew" label="setpw.new_password_repeat" :path="errors.passwordnew">
							<password class="form-control" name="passwordnew" v-model="setpw.passwordRepeat" style="margin-bottom:5px;" required />
						</form-group>						
						<div class="extraspace"></div>
						<button type="submit" v-submit :disabled="action!=null" class="btn btn-primary btn-block" v-t="'setpw.set_new_btn'"></button>
						<div class="extraspace"></div>
                        <error-box :error="error"></error-box>
						<!-- <p ng-show="error && !tokenIncluded" class="alert alert-danger" translate="{{ error.code || error }}"></p> -->
					</form>
				</div>
						
				<div v-if="!progress.PASSWORD_SET">
						
					<div v-if="progress.NEWEST_TERMS_AGREED">						
                        <p v-t="'postregister.terms_agreed'"></p>
						  
						<p>
						  <a @click="showTerms({which : progress.termsOfUse});" href="javascript:" v-t="'registration.agb3'"></a>
						</p>
						<div class="extraspace"></div>
						<div class="d-grid gap-2 mt-3 mb-2">
						  <button class="btn btn-primary" :disabled="action!=null" @click="agreedToTerms(progress.termsOfUse);" v-t="'postregister.noted_btn'"></button>
						</div>
						<div class="extraspace"></div>
						<error-box :error="error"></error-box>
					</div>
						
					<div v-if="progress.NEWEST_PRIVACY_POLICY_AGREED && !progress.NEWEST_TERMS_AGREED">
						<p v-t="'postregister.privacy_policy_agreed'"></p>
						  
						<p>
						  <a @click="showTerms({which : progress.privacyPolicy});" href="javascript:" v-t="'registration.privacypolicy3'"></a>
						</p>
						<div class="extraspace"></div>
						<div class="d-grid gap-2 mt-3 mb-2">
						  <button class="btn btn-primary" :disabled="action!=null" @click="agreedToTerms(progress.privacyPolicy);" v-t="'postregister.noted_btn'"></button>
						</div>
						<div class="extraspace"></div>
						<error-box :error="error"></error-box>
					</div>
						
					<div v-if="!(progress.NEWEST_TERMS_AGREED || progress.NEWEST_PRIVACY_POLICY_AGREED)">
						
						<div v-if="progress.EMAIL_VERIFIED">
						    <p v-t="'postregister.email_instructions'"></p>
							<button class="btn btn-default" @click="resend()" v-t="'postregister.resend_btn'"></button>
							<div class="extraspace"></div>
							<p v-show="resentSuccess" class="alert alert-success" v-t="'postregister.resent_success'"></p>
							<button class="btn btn-default" @click="retry()" v-t="'postregister.retry_btn'"></button>
							<hr>
							<p class="extraspace" v-t="'postregister.email_code'"></p>
							<form class="form form-horizontal">
							    <form-group name="code" label="postregister.code">
								    <input class="form-control" v-model="passphrase.code" type="text">
						        </form-group>
						    </form>
							<div class="extraspace"></div>	
							<div class="d-grid gap-2 mt-3 mb-2">
							  <button class="btn btn-primary" @click="enterMailCode(passphrase.code)" v-t="'postregister.enter_now_btn'"></button>
							</div>
							<div class="extraspace"></div>
							<error-box :error="error"></error-box>
							<hr>
						</div>
											
                        <div v-if="progress.ADMIN_VERIFIED">
                          <p v-t="'postregister.admin_verified'"></p>
                          <button class="btn btn-default" @click="retry()" v-t="'postregister.retry_btn'"></button>
                        </div>

						<div v-if="progress.ADDRESS_VERIFIED && !progress.ADDRESS_ENTERED">
							<div class="extraspace"></div>
							<div class="alert alert-info" v-t="'postregister.pilot_phase'"></div>
							<div class="extraspace"></div>
							<div v-if="progress.agbStatus=='NEW'">
								<p v-t="'member_user.agb_not_requested'"></p>
								<button :disabled="action!=null" class="btn btn-primary" href="javascript:"
									@click="requestMembership()" v-t="'member_user.request_membership_btn'"></button>
							</div>
							<div v-if="!(progress.agbStatus=='NEW')">
								<p v-t="'postregister.contract_instructions'"></p>
								<div v-if="!progress.confirmationCode">
									<div class="extraspace"></div>
									<p v-t="'postregister.code_intructions'"></p>
									<form class="form form-horizontal">
									    <form-group name="code" label="postregister.code">
										    <input class="form-control" v-model="passphrase.passphrase" type="text">
									    </form-group>
									</form>
									<div class="d-grid gap-2 mt-3 mb-2">
									  <button class="btn btn-primary" @click="sendCode()" v-t="'postregister.enter_now_btn'"></button>
									</div>
									<div class="extraspace"></div>
                                    <error-box :error="error"></error-box>									
								</div>
								<div v-show="progress.confirmationCode">
								  <p v-t="'postregister.code_done'"></p>
								</div>
							</div>
							<hr>
						</div>

                       <div v-if="progress.AUTH2FACTORSETUP">
							<div v-t="'postregister.auth2factorsetup'"></div>
														
							<div class="extraspace"></div>
							<form ref="myform" name="myform" novalidate class="css-form form-horizontal" @submit.prevent="changeAuthType()" role="form">
							
								
								<div class="dynheight">
									<form-group name="authType" label="postregister.auth_type" class="midata-checkbox-row">
										<radio-box name="authType" value="NONE" v-model="registration.authType" :path="errors.authType">                                     
									       <span v-t="'postregister.auth_type_none'"></span>
										</radio-box>
									    <radio-box name="authType" value="SMS" v-model="registration.authType" :path="errors.authType">                                     
									       <span v-t="'postregister.auth_type_sms'"></span>
										</radio-box>
									</form-group>									
								</div>
								<form-group name="mobile" label="registration.mobile_phone" :path="errors.mobile">
									<input type="text" class="form-control" id="mobile" name="mobile" v-model="registration.mobile" v-validate :required="registration.authType=='SMS'">
								</form-group>
								<div class="dynheight">
									<form-group name="emailnotify" label="postregister.emailnotify" class="midata-checkbox-row">
										<check-box name="emailnotify" v-model="registration.emailnotify" :path="errors.emailnotify">
											{{ $t('postregister.emailnotify2', { domain : ENV.apiurl}) }}
										</check-box>
									</form-group>									  
								</div>
									
								<div>{{ $t('postregister.auth2factorsetup2', { domain : ENV.apiurl} ) }}</div>
                                <div class="extraspace"></div>
								<button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'changeaddress.update_btn'"></button>
							    <div class="extraspace"></div>
							    <error-box :error="error"></error-box>
							</form>

						</div>

	                    <div v-if="progress.BIRTHDAY_SET">
	                    	<div v-t="'postregister.birthday_entered'"></div>
	                    	<div style="margin-top:20px"></div>
	                      
	                    	<form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="changeBirthday()" role="form">
	         
	                      		<form-group name="birthday" label="registration.birthday" :path="errors.birthday">
									<input required v-validate id="birthday" name="birthday" class="form-control" pattern="[0-3]?[0-9]\.[0-1]?[0-9]\.[0-9][0-9][0-9][0-9]" type="text" v-model="registration.birthdayDate" />
								</form-group>				
									   
						   		<div style="margin-top:30px"></div>
						   		<button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'changeaddress.update_btn'"></button>
								<div class="extraspace"></div>
								<error-box :error="error"></error-box>						   
							</form>
	                    </div>
	                    
	                    <div v-if="progress.GENDER_SET">
	                    	<div v-t="'postregister.gender_entered'"></div>
	                    	<div style="margin-top:20px"></div>
	                      
	                    	<form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="changeGender()" role="form">
	         
	                      		 <form-group name="gender" label="registration.gender" :path="errors.gender">
			                        <select class="form-control" id="gender" name="gender" v-model="registration.gender" required v-validate>
			                            <option value selected disabled hidden>{{ $t('common.fillout') }}</option>
			                            <option value="FEMALE" v-t="'enum.gender.FEMALE'">female</option>
			                            <option value="MALE" v-t="'enum.gender.MALE'">male</option>
			                            <option value="OTHER" v-t="'enum.gender.OTHER'">other</option>
			                        </select>
                    			</form-group>				
									   
						   		<div style="margin-top:30px"></div>
						   		<button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'changeaddress.update_btn'"></button>
								<div class="extraspace"></div>
								<error-box :error="error"></error-box>						   
							</form>
	                    </div>

						<div v-if="progress.ADDRESS_ENTERED || progress.PHONE_ENTERED">
							<div v-show="progress.ADDRESS_ENTERED" v-t="'postregister.address_entered'"></div>
							<div v-show="progress.PHONE_ENTERED" v-t="'postregister.phone_entered'"></div>
							<div v-show="progress.MIDATA_COOPERATIVE_MEMBER" v-t="'postregister.midata_cooperative_member'"></div>
							<div class="extraspace"></div>
							<form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="changeAddress()" role="form">
								<div v-if="addressNeeded()">
									<div class="required">
										<form-group mname="address1" label="registration.address" :path="errors.address1">
											<input type="text" class="form-control" id="address1" name="address1" v-model="registration.address1" :required="addressNeeded()" v-validate>
										</form-group>
									</div>
									<form-group name="address2" label="common.empty">
										<input type="text" class="form-control" id="address2" name="address2" v-model="registration.address2" v-validate>
									</form-group>
									<div class="required">
										<form-group name="city" label="registration.city" :path="errors.city">
											<input type="text" class="form-control" id="city" name="city" v-validate v-model="registration.city" :required="addressNeeded()">
										</form-group>
										<form-group name="zip" label="registration.zip" :path="errors.zip">
											<input type="text" class="form-control" id="zip" name="zip" v-validate v-model="registration.zip" :required="addressNeeded()">
										</form-group>
									</div>
									<form-group name="country" label="registration.country" :path="errors.country">
										<select class="form-control" id="country" name="country" v-model="registration.country" :required="addressNeeded()" v-validate>
										    <option value selected disabled hidden>{{ $t('common.fillout') }}</option>
											<option v-for="country in countries" :key="country" :value="country" v-t="'enum.country.'+country"></option>
										</select>
									</form-group>
								</div>


								<div v-if="phoneNeeded()">
									<form-group name="phone" label="registration.phone" :path="errors.phone">
										<input type="text" class="form-control" id="phone" name="phone" v-validate v-model="registration.phone">
									</form-group>
									<div class="required">
										<form-group name="mobile" label="registration.mobile_phone" :path="errors.mobile">
											<input type="text" class="form-control" id="mobile" name="mobile" v-validate v-model="registration.mobile" :required="phoneNeeded()">
										</form-group>
									</div>
								</div>

								<button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'changeaddress.update_btn'"></button>
								<div class="extraspace"></div>
								<error-box :error="error"></error-box>
							</form>

						</div>
                    </div>
                </div>

				<div v-if="tokenIncluded">
                    <error-box :error="error"></error-box>
				</div>
				<p v-show="codeSuccess" class="alert alert-success" v-t="'postregister.address_success'"></p>
				<p v-show="mailSuccess" class="alert alert-success" v-t="'postregister.email_success'"></p>
			</panel>
		</div>
		<div v-if="terms.active">
			<terms-modal :which="terms.which" @close="terms.active=false"></terms-modal>
		</div>
	</div>
</div>    
</template>
<script>
import Panel from 'components/Panel.vue'
import TermsModal from 'components/TermsModal.vue'

import server from "services/server.js";
import crypto from "services/crypto.js";
import users from "services/users.js";
import oauth from "services/oauth.js";
import dateService from "services/date.js";
import languages from "services/languages.js";
import { status, CheckBox, RadioBox, ErrorBox, FormGroup, Password } from 'basic-vue3-components';
import session from "services/session.js";
import ENV from "config";
import { setLocale } from "services/lang.js";

export default {
	data: () => ({
		registration : {},
		user : {},
		passphrase : {},
		setpw : { secure : true },
		progress : {},
		mailSuccess : false,
		codeSuccess : false,
		resentSuccess : false,
		isoauth : false,
		terms : { which : "", active : false },
		ENV : ENV,
		tokenIncluded : false,
		mode : null,
		countries : languages.countries,
		unlockCode : ""	
	}),

	props: ['preview'],

	components: { CheckBox, RadioBox, ErrorBox, FormGroup, Panel, TermsModal, Password },

	mixins : [ status ],

	methods : {
		addressNeeded() {
			const { $data } = this;
			return $data.progress.requirements && ($data.progress.requirements.indexOf('ADDRESS_ENTERED') >= 0  );
		},
	
		phoneNeeded() {
			const { $data } = this;
			return $data.progress.requirements && ($data.progress.requirements.indexOf('PHONE_ENTERED') >= 0  );
		},
	
		addAddressParams() {
			const { $data, $route } = this;
			if (this.addressNeeded()) {
				if ($route.query.street) $data.registration.address1 = $route.query.street;
				if ($route.query.city) $data.registration.city = $route.query.city;
				if ($route.query.zip) $data.registration.zip = $route.query.zip;
			}
			if (this.phoneNeeded()) {
				if ($route.query.phone) $data.registration.phone = $route.query.phone;
				if ($route.query.mobile) $data.registration.mobile = $route.query.mobile;
			}
		},

		setSecurityToken() {		
			this.retry(null, { securityToken : this.$data.setpw.securityToken });
		},
	
		noSecurityToken() {		
			this.retry(null, { securityToken : "_FAIL" });
		},

		requestMembership() {
			const { $data } = this, me = this;			
			me.doAction("requestmembership", users.requestMembership($data.user))
			.then(function() {
		   		me.retry();
			});
		},
		
		showTerms(def) {			
			const { $data }	= this;
			$data.terms = { which : def.which, active : true };
		},
	
		agreedToTerms(terms) {
			let me = this;
			let data = { terms : terms, app : oauth.getAppname() ? oauth.app._id : null };
			this.doAction('terms', server.post(jsRoutes.controllers.Terms.agreedToTerms().url, data ))
			.then(function(result) {
	    		me.retry(result);	    	
	    	});	
		},

		retry(funcresult, params) {			
			const { $data, $route, $router } = this, me = this;
	    	if (funcresult) {				
		   		if (funcresult.data.istatus === "ACTIVE") oauth.postLogin(funcresult);
		   		else session.postLogin(funcresult, $router, $route);		
	    	} else {
				let r = me.doAction("login",session.retryLogin(params));				
				r.then(function(result) {					
					if (result.data.istatus === "ACTIVE") {						
						oauth.postLogin(result);
					}
					else {
						session.postLogin(result, $router, $route);
					}					
				}, function(err) {
					$data.setpw = {};
									
					if (err.response.data && err.response.data.code == "error.expired.securitytoken") {
						$data.progress = { RELOGIN : true };
					}
				});				
			}			
		},
		
		setUnlockCode() {
		   oauth.setUnlockCode(this.$data.unlockCode);
		   this.retry(null, { unlockCode : this.$data.unlockCode });
		},

		
		pwsubmit() {
			const { $data, $route, $t } = this, me = this;
		
			if (!$data.setpw.passwordRepeat || $data.setpw.passwordRepeat !== $data.setpw.password) {
				this.setError("password", $t('error.invalid.password_repetition'));
				return;
			}
			let pwvalid = crypto.isValidPassword($data.setpw.password, $data.progress.role != "MEMBER"); 
        
        	if (!pwvalid) {
        		this.setError("password", ($data.progress.role != "MEMBER" ? $t('error.tooshort.password2') : $t('error.tooshort.password')));
				return;				
        	}
				
			crypto.generateKeys($data.setpw.password).then(function(keys) {
				
				if ($data.registration.lastname) {
					if ($data.registration.secure) {
						$data.registration.password = keys.pw_hash;	
						$data.registration.pub = keys.pub;
						$data.registration.priv_pw = keys.priv_pw;
						$data.registration.recoverKey = keys.recoverKey;
						$data.registration.recovery = keys.recovery;
					} else {
						$data.registration.password = $data.registration.password1;
					};	
				}
							
				var data = {};
				if ($route.query.token) data.token = $route.query.token;
				if ($data.mode) data.mode = $data.mode;	
				if ($data.setpw.secure) {
				data.password = keys.pw_hash;
				data.pub = keys.pub;
				data.priv_pw = keys.priv_pw;
				data.recovery = keys.recovery;
				data.recoverKey = keys.recoverKey;
				} else {
					data.password = $data.setpw.password;
				}
					
				me.doAction('setpw', server.post(jsRoutes.controllers.Application.confirmAccountEmail().url, data))
				.then(function(result) {
					if (result.data.challenge) {
					
					} else {				
					me.retry(result);
					}
				});
			});
		},

		changeAuthType() {		
			const { $data } = this, me = this;
			$data.registration.user = $data.registration._id;
			if ($data.registration.mobile === "") $data.registration.mobile = undefined;
			me.doAction("changeAddress", users.updateAddress({ user : $data.registration._id, authType : $data.registration.authType, mobile : $data.registration.mobile, emailnotify : $data.registration.emailnotify })).
			then(function(data) { 
				me.retry();
			});
		},
	
		confirm(forceConfirm) {
			const { $data, $route } = this,me = this;
		
			$data.resentSuccess = false;
			$data.codeSuccess = false;
			$data.mailSuccess = false;
			var data = { token : $route.query.token, mode : $data.mode };
			if (forceConfirm) {
				data.mode = "VALIDATED";

			}
	    	me.doAction('email', server.post(jsRoutes.controllers.Application.confirmAccountEmail().url, data ))
	    	.then(function(result) {
	    		$data.progress = result.data;	 
	    		if (result.data.emailStatus !== "UNVALIDATED" && (!result.data.requirements || result.data.requirements.indexOf("PASSWORD_SET") < 0)) {
	    	  		$data.mailSuccess = true;	  	    	  
	    		} else {
	    			me.setFlags();
	    		}
	    	});	    
		},
	
    	enterMailCode(code) {
			const { $data } = this, me = this;    	
			$data.resentSuccess = false;
			$data.codeSuccess = false;
			$data.mailSuccess = false;

			var data = { code : code, mode : "VALIDATED", userId : $data.progress.userId , role : $data.progress.role };
	    	me.doAction('email', server.post(jsRoutes.controllers.Application.confirmAccountEmail().url, data ))
	    	.then(function(result) {
	    		me.retry(result);	    	
	    	});	    
		},

		changeBirthday() {		
			const { $data, $t } = this, me = this;
			
		
        	let d = $data.registration.birthdayDate;
        	let pad = function(n){
		    	return ("0" + n).slice(-2);
			};
		
			let dparts = d.split("\.");
			if (dparts.length != 3 || !dateService.isValidDate(dparts[0],dparts[1],dparts[2])) {
		  		this.setError("birthday", $t("error.invalid.date"));
		  		return;
			} else {
				if (dparts[2].length==2) dparts[2] = "19"+dparts[2];
				$data.registration.birthday = dparts[2]+"-"+pad(dparts[1])+"-"+pad(dparts[0]);			
			}					
				
			let upd = { user : $data.registration._id, birthday : $data.registration.birthday};
											
			me.doAction("changeAddress", server.post(jsRoutes.controllers.admin.Administration.changeBirthday().url, upd)).
			then(function(data) { 
				me.retry();
			});
		},
		
		changeGender() {		
			const { $data, $t } = this, me = this;
						
			let upd = { user : $data.registration._id, gender : $data.registration.gender };
																	
			$data.registration.user = $data.registration._id;									
			me.doAction("changeAddress", users.updateAddress($data.registration)).
			then(function(data) { 
				me.retry();
			});
		},

		sendCode() {
			const { $data } = this, me = this;
			$data.resentSuccess = false;
			$data.codeSuccess = false;
			$data.mailSuccess = false;
		
			let data = { confirmationCode : $data.passphrase.passphrase };
			if (data.confirmationCode && data.confirmationCode.length > 0) {
	    		me.doAction('code', server.post(jsRoutes.controllers.Application.confirmAccountAddress().url, data ))
	    		.then(function(result) { 
	    			$data.codeSuccess = true;
	    			$data.progress.confirmationCode = true;
					me.retry(result);	    						
				});
			}
		},

		changeAddress() {		
			const { $data } = this, me = this;
        	if (!this.addressNeeded()) {
        		$data.registration = JSON.parse(JSON.stringify($data.registration));
				$data.registration.firstname = $data.registration.lastname = $data.registration.gender = $data.registration.city = $data.registration.zip = $data.registration.country = $data.registration.address1 = undefined;			
        	}
        	if (!this.phoneNeeded()) {
        	    $data.registration = JSON.parse(JSON.stringify($data.registration));
				$data.registration.phone = $data.registration.mobile = undefined;
        	}
        	$data.registration.authType = undefined;
					
			$data.registration.user = $data.registration._id;									
			me.doAction("changeAddress", users.updateAddress($data.registration)).
			then(function(data) { 
				me.retry();
			});
		},

		resend() {	
			const { $data } = this, me = this;
			$data.resentSuccess = false;
			$data.codeSuccess = false;
			$data.mailSuccess = false;
			me.doAction('resent', server.post(jsRoutes.controllers.Application.requestWelcomeMail().url, { userId : $data.progress.userId }))
	    	.then(function() {
	    		$data.resentSuccess = true;	    		    	
	    	});	    
		},

		setFlags() {
			const { $data } = this;
			if ($data.progress && $data.progress.requirements) {
				for (let i in $data.progress.requirements) {
					$data.progress[$data.progress.requirements[i]] = true;
				}
				$data.registration = $data.progress.user || {};
				this.addAddressParams();
			}
		},

		init() {
			const { $data, $route } = this, me = this;
			if (this.preview) {
				if (this.preview.requirement) {
				  $data.progress = { requirements : [ this.preview.requirement ] };
				} else {
				  $data.progress = { requirements : this.preview.requirements };
				}
			} else if ($route.query.feature) {
				$data.progress = { requirements : [ $route.query.feature ] };
		
				this.doBusy(session.currentUser.then(function (userId) {
					me.doBusy(users.getMembers({"_id": userId}, ["name", "email", "searchable", "language", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone", "emailStatus", "agbStatus", "contractStatus", "role", "subroles", "confirmedAt"])
					.then(function(results) {
						$data.registration = results.data[0];
						$data.progress.emailStatus = $data.registration.emailStatus;
						$data.progress.agbStatus = $data.registration.agbStatus;
						$data.progress.contractStatus = $data.registration.contractStatus;
						me.addAddressParams();
					}));
				}));
			}
		},

		prepare() {			
			const { $data, $route, $router }	= this;
			$data.progress = session.progress || {};
	
			this.init();			
			this.setFlags();		   
			if ($route.query.token && $route.meta.mode) {
				if ($data.mode == null) $data.mode = $route.meta.mode;
				$data.tokenIncluded = true;
				if ($data.mode=="REJECTED") {
		   			$data.progress = { REJECT : true };
				} else {
					$data.progress = { CONFIRM : true };
					this.confirm();					
				}
			}
	
			if (oauth.getAppname()) { $data.isoauth = true; }
			if (!$route.query.feature) this.loadEnd();		
			if (Object.keys($data.progress).length==0) {
				$router.go(-1);
			}
		}
	
	},

	watch : {
		$route(to, from) { 						
			if (to.path.indexOf("postregister") < 0 && to.path.indexOf("upgrade") < 0) return;
			this.prepare(); 
		}
	},

    mounted() {
       if (this.$data.progress && (this.$data.progress.AUTH2FACTOR || this.$data.progress.PHONE_VERIFIED)) {                
         this.$refs.tokenInput.focus();
       }
    },
    
	created() {
        if (this.$route.query.language) {
            setLocale(this.$route.query.language);
        }
		this.prepare();
	}
    
}
</script>