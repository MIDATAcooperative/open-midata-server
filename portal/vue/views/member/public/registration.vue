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
	<div v-if="!terms.active && !welcomemsg">
		<!-- Registration -->
		<panel style="max-width:630px; padding-top:20px; margin:0 auto;" :title="$t('registration.sign_up')" :busy="isBusy">
			
			<div class="midatalogo">
				<img src="/images/logo.png" style="height: 36px;" alt="">
			</div>
           
            <p v-if="offline" class="alert alert-danger" v-t="'error.offline'"></p>
			<form ref="myform" class="css-form form-horizontal" @submit.prevent="register()" role="form" novalidate>
				<div v-if="short">
					<form-group name="personalDetails" label="registration.personal_details" :path="errors.personalDetails"> 
						<p class="form-control-plaintext">
							{{ registration.firstname }} {{ registration.lastname }}<br>							
     		    			<span v-if="registration.address1">{{ registration.address1 }}<br/></span>
     		    			<span v-if="registration.address2">{{ registration.address2 }}<br/></span>
     		    			<span v-if="registration.zip">{{ registration.zip }} {{ registration.city }}<br></span>
							{{ getCountry(registration.country) }}
						</p>
					</form-group>						
					<form-group v-if="registration.birthdayDate" name="birthday" label="registration.birthday" :path="errors.birthday">
						<p class="form-control-plaintext">{{ registration.birthdayDate }}</p>
					</form-group>	
					<hr>
				</div>

				<p>
			    <span v-if="reducedInput()" v-t="'registration.mandantory_fields2'"></span><span v-else v-t="'registration.mandantory_fields'"></span> <span v-if="advancedPassword()" v-t="'registration.password_policy2'"></span><span v-else v-t="'registration.password_policy'"></span>
 				</p>
				<div v-if="role=='research'">
					<h3 v-t="'researcher_registration.research_organization'"></h3>
					<div class="required">
						<form-group name="name" label="researcher_registration.name" :path="errors.name"> 
							<input type="text" class="form-control" name="name" :placeholder="$t('researcher_registration.name')" v-validate v-model="registration.name" required>
						</form-group>
						<form-group name="description" label="researcher_registration.description" :path="errors.description">
							<textarea class="form-control" name="description" rows="5" v-validate v-model="registration.description" required></textarea> 
						</form-group>
						<h3 v-t="'researcher_registration.research_user'">Research User</h3>
					</div>
				</div>
				<!-- <div v-if="role=='provider'">
					<h3 v-t="'provider_registration.provider'"></h3>
					<div class="required">
						<form-group name="name" label="provider_registration.name" :path="errors.name"> 
							<input type="text" class="form-control" name="name" :placeholder="$t('provider_registration.name')" v-validate v-model="registration.name" required>							    
						</form-group>	
					</div>
					<form-group name="description" label="provider_registration.description" :path="errors.description">
						<textarea class="form-control" name="description" rows="5" v-validate v-model="registration.description"></textarea>
					</form-group>
					<form-group name="url" label="provider_registration.url" :path="errors.url">
						<input type="text" class="form-control" name="url" placeholder="https://www.example.com" v-validate v-model="registration.url">
					</form-group>  						  
					<h3 v-t="'provider_registration.user'"></h3>
				</div> -->
			
                 <div class="required">								  
				    <form-group name="email" label="registration.email" :path="errors.email">						
						<input type="email" class="form-control" id="email" name="email" :placeholder="$t('registration.email')" v-model="registration.email" required v-validate>																    
						<a href="javascript:" v-if="isNew" @click="showLogin()" v-t="'registration.already_have_account'"></a>
					</form-group>
					<form-group name="password" label="registration.password" :path="errors.password"> 
						<password class="form-control" id="password" name="password" :placeholder="$t('registration.password')" v-model="registration.password1" required></password>							  
				    </form-group>
					<form-group name="password2" label="registration.password_repetition" :path="errors.password2">
                        <password class="form-control" id="password2" name="password2" :placeholder="$t('registration.password')" v-model="registration.password2" required></password>
                    </form-group>
                </div>
                <!--<form-group name="secure" label="registration.secure" v-if="secureChoice()" class="midata-checkbox-row">
                    <div class="form-check">
                        <label class="form-check-label">
                            <input class="form-check-input" type="checkbox" v-model="registration.secure" disabled>
                            <span v-t="'registration.secure2'"></span>
                        </label>
                    </div>
                </form-group>-->
                <div class="required" v-if="!short">
                    <form-group name="firstname" label="registration.firstname" :path="errors.firstname">
                        <input type="text" class="form-control" id="firstname" name="firstname" :placeholder="$t('registration.firstname')" v-model="registration.firstname" required v-validate>
                    </form-group>
                    <form-group name="lastname" label="registration.lastname" :path="errors.lastname">
                        <input type="text" class="form-control" id="lastname" name="lastname" :placeholder="$t('registration.lastname')" v-model="registration.lastname" required v-validate>
                    </form-group>
                                
                    <form-group name="gender" label="registration.gender" :path="errors.gender" v-if="genderNeeded()">
                        <select class="form-control" id="gender" name="gender" v-model="registration.gender" required v-validate>
                            <option value selected disabled hidden>{{ $t('common.fillout') }}</option>
                            <option value="FEMALE" v-t="'enum.gender.FEMALE'">female</option>
                            <option value="MALE" v-t="'enum.gender.MALE'">male</option>
                            <option value="OTHER" v-t="'enum.gender.OTHER'">other</option>
                        </select>
                    </form-group>
				</div>
				<div class="required" v-if="birthdayNeeded()">
                    <form-group name="birthday" label="registration.birthday" :path="errors.birthdayDate">
                        <input type="text" class="form-control"   name="birthdayDate" v-model="registration.birthdayDate" required v-validate>                                                       
                    </form-group>
				</div>
				<div class="required" v-if="languageNeeded()">
                    <form-group myid="language" label="registration.language" :path="errors.language">
                        <select class="form-control" id="language" v-model="registration.language" @change="changeLanguage(registration.language);">
                            <option value selected disabled hidden>{{ $t('common.fillout') }}</option>
                            <option v-for="lang in languages" :key="lang.value" :value="lang.value">{{ $t(lang.name) }}</option>
                        </select>
                    </form-group>
                </div>
                <div v-if="addressNeeded()">
                    <div class="required">
	                    <form-group name="address1" label="registration.address" :path="errors.address">
	                        <input type="text" class="form-control" id="address1" name="address1" :placeholder="$t('registration.address_line1')" v-model="registration.address1" required v-validate>
	                    </form-group>
                    </div>
                    <form-group name="address2" label="common.empty">
                        <input type="text" class="form-control" id="address2" name="address2" :placeholder="$t('registration.address_line2')" v-model="registration.address2" v-validate>
                    </form-group>
                    <div class="required">            
	                    <form-group name="city" label="registration.city" :path="errors.city">
	                        <input type="text" class="form-control" id="city" name="city" :placeholder="$t('registration.city')" v-model="registration.city" required v-validate>
	                    </form-group>
	                    <form-group name="zip" label="registration.zip" :path="errors.zip">
	                        <input type="text" class="form-control" id="zip" name="zip" :placeholder="$t('registration.zip')" v-model="registration.zip" required v-validate>
	                    </form-group>
                    </div>
                </div>
                <div v-if="countryNeeded()" class="required">
                    <form-group name="country" label="registration.country" :path="errors.country">
                        <select class="form-control" id="country" name="country" v-model="registration.country" required v-validate>
                            <option value selected disabled hidden>{{ $t('common.fillout') }}</option>
                            <option v-for="country in countries" :key="country" :value="country">{{ $t('enum.country.'+country) }}</option>
                        </select>
                    </form-group>
                </div>
                <div v-if="phoneNeeded()" class="required">
                    <form-group name="phone" label="registration.phone" :path="errors.phone">
                        <input type="text" class="form-control" id="phone" name="phone" :placeholder="$t('registration.phone')" v-model="registration.phone" v-validate>
                    </form-group>
                                
                    <form-group name="mobile" label="registration.mobile_phone" :path="errors.mobile">
                        <input type="text" class="form-control" id="mobile" name="mobile" :placeholder="$t('registration.mobile_phone')" v-model="registration.mobile" required v-validate>
                    </form-group>
                </div>
                <div class="required" v-if="app && app.unlockCode">
                    <form-group name="unlockCode" label="registration.unlock_code" :path="errors.unlockCode">
                        <input type="text" class="form-control" id="unlockCode" name="unlockCode" :placeholder="$t('registration.unlock_code')" v-model="registration.unlockCode" required v-validate>                        
                    </form-group>
                </div>

                <form-group name="coach" label="registration.coach" :path="errors.coach" v-if="role=='developer'">
					<input type="text" class="form-control" id="coach" name="coach" v-validate v-model="registration.coach">
				</form-group>
                <form-group name="reason" label="registration.reason" :path="errors.reason" v-if="role=='developer'">
                    <textarea class="form-control" rows="5" id="reason" name="reason" v-validate v-model="registration.reason" required></textarea>
                    <p class="form-text text-muted" translate="developer_registration.reason_fillout"></p>
                </form-group>
                <hr>
                <form-group name="agb" label="registration.agb" class="midata-checkbox-row">
                    <check-box v-model="registration.agb" name="agb" required :path="errors.agb">                                 
                        <span v-t="'registration.agb2'"></span>&nbsp;
                        <a @click="showTerms(currentTerms.member.termsOfUse);" href="javascript:" v-t="'registration.agb3'"></a>&nbsp;
                        <span v-t="'registration.privacypolicy2'"></span>&nbsp;
                        <a @click="showTerms(currentTerms.member.privacyPolicy);" href="javascript:" v-t="'registration.privacypolicy3'"></a>
                    </check-box>   

					<div v-if="app && app.loginTemplate == 'REDUCED'">					
					<section v-if="app.termsOfUse">
						<div class="form-check">
							<input id="appAgb" name="appAgb" class="form-check-input" type="checkbox" required v-model="registration.appAgb" />
							
							<label for="appAgb" class="form-check-label">
						   		<span v-t="'registration.app_agb2'"></span>&nbsp;
						   		<a @click="showTerms(app.termsOfUse)" href="javascript:" v-t="'registration.app_agb3'"></a>
						 	</label>							 					
						 
						</div>					
					</section>
					<section v-for="link in links" :key="link._id">
						<div class="form-check" v-if="link.type.indexOf('OFFER_P')>0">
							<input type="checkbox" class="form-check-input" :id="link._id" :name="link._id" value="" :checked="registration.confirmStudy.indexOf(link.studyId || link.userId || link.serviceAppId)>=0" @click="toggle(registration.confirmStudy, link.studyId || link.userId || link.serviceAppId)" /> 
							<label :for="link._id" class="form-check-label">
							<span>{{ $t(getLinkLabel(link)) }}</span>:
							<a v-if="link.termsOfUse" @click="showTerms(link.termsOfUse)" href="javascript:">{{ (link.study || {}).name }} {{ (link.provider || {}).name }} {{ (link.serviceApp || {}).name }}</a>
							<span v-if="!(link.termsOfUse)">{{ getLinkName(link) }}</span>
							</label>
						</div>					
					</section>                           
				</div>

                </form-group>   

				 <error-box :error="error">
				   <div v-if="error=='error.exists.user'">
				      <a href="javascript:" @click="showLogin()" v-t="'registration.already_have_account2'"></a>				      
				   </div>
				 </error-box>
				 <div class="d-grid gap-2 mt-3 mb-2">	
                   <button class="mt-1 btn btn-primary" type="submit" :disabled="action!=null" v-t="'registration.sign_up_btn'" v-submit>				 				
                   </button>
				</div>
			
			</form>
		</panel>
	</div>

    <div v-if="terms.active">        
      <terms-modal :which="terms.which" @close="terms.active=false"></terms-modal>
    </div>
	
    <div v-if="welcomemsg">
        <panel :title="$t('registration.welcome_title')" style="max-width: 600px; padding-top: 20px; margin: 0 auto;">
      	    <p v-t="'registration.welcome_text1'"></p>
		    <div class="extraspace"></div>
		    <p v-t="'registration.welcome_text2'"></p>
		    <div class="extraspace"></div>
		    <p v-t="'registration.welcome_text3'"></p>
		    <button class="btn btn-primary" type="button" v-t="'registration.welcome_btn'" @click="confirmWelcome();" :disabled="action!=null"></button>
        </panel>						  
    </div>

</div>
        </div>
	 </div>
  
</template>

<script>
import server from "services/server.js";
import crypto from "services/crypto.js";
import oauth from "services/oauth.js";
import dateService from "services/date.js";
import languages from "services/languages.js";
import { status, FormGroup, ErrorBox, CheckBox, Password } from 'basic-vue3-components';
import session from "services/session.js";
import ENV from "config";
import { getLocale, setLocale, addBundle } from "services/lang.js";
import Panel from 'components/Panel.vue';
import TermsModal from 'components/TermsModal.vue';


export default {
  data: () => ({
    registration : { language : getLocale(), confirmStudy : [], secure : true, unlockCode : "", country : languages.countries[0], gender:"", confirm : false },
	languages : languages.all,
	countries : languages.countries,	
	flags : { optional : false },
    genders : ["FEMALE","MALE","OTHER"],
    offline : false,
    currentTerms : "",
    terms : { which : "", active : false },
    welcomemsg : false,

    actions : null,
    login : null,
	role : "user",
	links : [],
	app : null,
	isNew : false,
	short : false,
	queryParams : null
  }),

  props: ['preview', 'previewlinks', 'query'],

  components : {
     FormGroup, ErrorBox, Panel, TermsModal, CheckBox, Password 
  },

  mixins : [ status ],
    
  methods : {
    confirmWelcome() {
        const { $data, $router, $route } = this, me = this;
		me.doAction("register", oauth.login(false)) 
	    .then(function(result) {			 
			  if (result === "CONFIRM" || result === "CONFIRM-STUDYOK") {
				$router.push({ path : "./oauthconfirm", query : $route.query });
			  }
			  if (result !== "ACTIVE") { session.postLogin({ data : result}, $router, $route);}
		})
		.catch(function(err) { 			
			session.failurePage($router, $route, err.response.data);
		});			
	},
	
	changeLanguage(lang) {
		if (!this.$route.query.developer) {
	  	  setLocale(lang);
		}
	},

	addressNeeded() {
        const { $data } = this;
		if ($data.short) return false;
		if ($data.role == "research" || $data.role == "provider" || $data.role == "developer" ) return true;
		return $data.app && $data.app.requirements && ($data.app.requirements.indexOf('ADDRESS_ENTERED') >= 0 ||  $data.app.requirements.indexOf('ADDRESS_VERIFIED') >=0 );
	},
	
	phoneNeeded() {
        const { $data } = this;
		if ($data.role == "research" || $data.role == "provider" || $data.role == "developer") return true;
		return $data.app && $data.app.requirements && ($data.app.requirements.indexOf('PHONE_ENTERED') >= 0 ||  $data.app.requirements.indexOf('PHONE_VERIFIED') >=0 );
	},

	birthdayNeeded() {
		const { $data, $route } = this;
		if ($data.short) return false;
		if (this.$data.queryParams.birthdate) return true;
		if ($data.role == "member") return true;
		return $data.app && $data.app.requirements && ($data.app.requirements.indexOf('BIRTHDAY_SET') >= 0);
	},
	
	genderNeeded() {
		const { $data, $route } = this;
		if (this.$data.queryParams.gender) return true;
		return $data.app && $data.app.requirements && ($data.app.requirements.indexOf('GENDER_SET') >= 0);
	},

	countryNeeded() {
		const { $data, $route } = this;
		if ($data.short) return false;
		return this.addressNeeded() || !this.$data.queryParams.country;
	},

	languageNeeded() {
		const { $data, $route } = this;	
		return !this.$data.queryParams.language;
	},

	secureChoice() {
		const { $data } = this;
		if ($data.role == "research" || $data.role == "provider") return false;
		return true;
	},
	
	showTerms(def) {		
        const { $data } = this;       
        $data.terms = { which : def, active : true };		
	},
	
	toggle(array,itm) {		
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
    },
    
    back() {
      this.$router.go(-1);	
	},
	
	pwValid(v) {
	   const { $data, $t } = this;	   
	   return $data.registration.password1 ==  $data.registration.password2 ? "" : $t('error.invalid.password_repetition')
	},
	
	advancedPassword() {
	   return this.$data.role != "member";
	},

	reducedInput() {
       return this.$data.short && !this.languageNeeded() && !this.phoneNeeded() && !this.countryNeeded() && !this.birthdayNeeded() /*& this.$data.role == "member"*/;
	},

	mustAccept(v) {
		return v==true ? "" : $t('error.missing.agb')
	},
	
	showLogin() {
	   const { $route, $router } = this;
	   let query = $route.query || {};	 
	   let params = JSON.parse(JSON.stringify(query));
	   
	   delete params.isnew;
	    
	   params.login = this.$data.registration.email;
		if (query.client_id) {
		  $router.push({ path : "./oauth2", query : params });
		} else {
		  $router.push({ path : "./login", query : params });
		} 		  
	},

	getLinkHeading(link) {
		let t = (link.study && link.study.type) ? link.study.type : (link.linkTargetType || "STUDY");
		
		return this.$t('oauth2.link_'+t+"_"+((link.type.indexOf("CHECK_P") >= 0) ? "required" : "optional"));
	},
   
    getLinkLabel(link) {
	    if (link.linkTargetType == "ORGANIZATION") {
			if (link.type.indexOf("CHECK_P") >= 0) return "oauth2.confirm_provider";
			return "oauth2.confirm_provider_opt";
		} 
		if (link.linkTargetType == "SERVICE") {
			if (link.type.indexOf("CHECK_P") >= 0) return "oauth2.confirm_service";
			return "oauth2.confirm_service_opt";
		} 
		if (link.study.type == "CLINICAL" || link.study.type == "REGISTRY") {
			if (link.type.indexOf("CHECK_P") >= 0 /*&& !(link.type.indexOf("OFFER_EXTRA_PAGE") >=0)*/) return "oauth2.confirm_study";
			return "oauth2.confirm_study_opt";
		}
		if (link.study.type == "CITIZENSCIENCE") return "oauth2.confirm_citizen_science";		
		if (link.study.type == "COMMUNITY") {
			if (link.type.indexOf("CHECK_P") >= 0 /*&& !(link.type.indexOf("OFFER_EXTRA_PAGE") >=0)*/) return "oauth2.confirm_community";
			return "oauth2.confirm_community_opt";
		}		
	},

	getLinkName(link) {
		if (link.study) return link.study.name;
		if (link.provider) return link.provider.name;
		if (link.serviceApp) 
			return (link.serviceApp.i18n[getLocale()] && link.serviceApp.i18n[getLocale()].name) ? link.serviceApp.i18n[getLocale()].name : link.serviceApp.name;
		return "???";
    },

	getCountry(c) {
		let { $data, $t } = this;
		for (let c1 of $data.countries) {
			if (c1 == c) return $t("enum.country."+c1);
		}
		return "-";		
	},

    register() {		        
		const { $data, $router, $route, $t } = this, me = this;
		

		if ($data.registration.password1 != $data.registration.password2) {
			this.setError("password", $t("error.invalid.password_repetition"));
			return;
		}

		var pwvalid = crypto.isValidPassword($data.registration.password1, $data.role && $data.role != "member");         
        if (!pwvalid) {
			this.setError("password", $t(($data.role && $data.role != "member") ? "error.tooshort.password2" : "error.tooshort.password"));
        	return;
        }

		if (!$data.registration.agb) {
			this.setError("agb", $t("error.missing.agb"));			
			return;
		}
                                                    		
		var pad = function(n){
		    return ("0" + n).slice(-2);
		};
		
		var d = $data.registration.birthdayDate;
				
		
		if (d) {
			var dparts = d.split("\.");
			if (dparts.length != 3 || !dateService.isValidDate(dparts[0],dparts[1],dparts[2])) {
			  this.setError("birthdayDate", $t("error.invalid.date"));			  
			  return;
			} else {
				if (dparts[2].length==2) dparts[2] = "19"+dparts[2];
				$data.registration.birthday = dparts[2]+"-"+pad(dparts[1])+"-"+pad(dparts[0]);				
			}
						
		} else if ($data.registration.birthdayYear) {
			$data.registration.birthday = $data.registration.birthdayYear + "-" + 
										pad($data.registration.birthdayMonth) + "-" +
										pad($data.registration.birthdayDay);
		} else $data.registration.birthday = undefined;
		// send the request
		var data = $data.registration;
		if (!data.gender) data.gender = "UNKNOWN";	
		
		if ($route.query.developer) {
			data.developer = $route.query.developer;
		}
		
		
		var finishRegistration = function() { 
			if (oauth.getAppname()) {	
			  oauth.setDuringRegistration(true);	  
			  data.app = oauth.getAppname();
			  data.device = oauth.getDevice();
			  if ($data.registration.unlockCode) oauth.setUnlockCode($data.registration.unlockCode);
			  if ($route.query.joincode) {
				  oauth.setJoinCode($route.query.joincode);
				  data.joinCode = $route.query.joincode;
			  }
			  
			  me.doAction("register", server.post(jsRoutes.controllers.QuickRegistration.register().url, data)).
			  then(function(datax) { 			 
				  oauth.setUser($data.registration.email, $data.registration.password1);			  
				  				
				  session.postLogin(datax, $router, $route);	  
				  /*if ($data.app && $data.app.requirements && $data.app.requirements.indexOf('EMAIL_VERIFIED') >= 0) {
					  me.confirmWelcome(); 
				  } else {
				      $data.welcomemsg = true;
				  }*/
			  });
			} else if ($data.role == "research") {
				me.doAction("register", server.post(jsRoutes.controllers.research.Researchers.register().url, data))
		        .then(function(data) { 
					if (!me.$route.query.developer) {
					  session.postLogin(data, $router, $route);
					} else me.$router.go(-1);
				});
			} else if ($data.role == "provider") {
				me.doAction("register", server.post(jsRoutes.controllers.providers.Providers.register().url, data))
		        .then(function(data) { 
					if (!me.$route.query.developer) {
					  session.postLogin(data, $router, $route);
					} else me.$router.go(-1);
				});			
			} else if ($data.role == "developer") {
				me.doAction("register", server.post(jsRoutes.controllers.Developers.register().url, data))
		        .then(function(data) {
					if (!me.$route.query.developer) { 
					  session.postLogin(data, $router, $route);
					} else me.$router.go(-1);
				});	
			} else {			
				me.doAction("register", server.post(jsRoutes.controllers.Application.register().url, data)).
				then(function(data) { 
					if (!me.$route.query.developer) {
					  session.postLogin(data, $router, $route);
			  	    } else me.$router.go(-1);
				});
			}
			
		};
				
		me.doAction("register", crypto.generateKeys($data.registration.password1)).then(function(keys) {				
			if ($data.registration.secure) {
			  $data.registration.password = keys.pw_hash;	
			  $data.registration.pub = keys.pub;
			  $data.registration.priv_pw = keys.priv_pw;
			  $data.registration.recoverKey = keys.recoverKey;
			  $data.registration.recovery = keys.recovery;
			} else {
			  $data.registration.password = $data.registration.password1;
			};			
		    finishRegistration();						
		});
		
		
				
	}
	
  },

  created() {    
     const { $data, $route } = this, me = this;
     $data.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	 $data.role = $route.query.role || $route.meta.role;
	 if ($data.role == "research") {
		 addBundle("researchers");
		 $data.registration.secure = true;
	 } else if ($data.role == "provider") {
		 addBundle("providers");
		 $data.registration.secure = true;
	 } else if ($data.role == "developer") {
		 addBundle("developers");
		 $data.registration.secure = true;
	 }
     this.doBusy(server.get(jsRoutes.controllers.Terms.currentTerms().url).then(function(result) { $data.currentTerms = result.data; }));

     $data.days = [];
	 $data.months = [];
	 var i = 0;
	 for (i=1;i <= 9; i++ ) { $data.months.push("0"+i); }
	 for (i=10;i <= 12; i++ ) $data.months.push(""+i);
     if (this.preview) {
		$data.app = this.preview;
		$data.links = this.previewlinks;
	 } else if (oauth.getAppname()) {		
	    $data.app = oauth.app;
		$data.links = oauth.links;
		if ($data.app.loginTemplate=="REDUCED") {
		    $data.registration.confirm = true;
			me.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("app-use", $data.app._id).url)
			.then(function(data) {	
			
				let links = [];
				for (var l=0;l<data.data.length;l++) {
					var link = data.data[l];					
					if (link.type.indexOf("OFFER_P")>=0) links.push(link);

				}
				$data.links = oauth.links = links;
			}));
		}
	    
	 } else if ($route.query.client_id) {
	     this.back();
	     return;
	 }
	
	$data.queryParams = this.query || $route.query;
	
	if (this.$data.queryParams.login) {
		$data.registration.email = this.$data.queryParams.login;
        
        $data.actions = this.$data.queryParams.actions;
		$data.login = this.$data.queryParams.login;
		$data.isNew = true;
	}
	if (this.$data.queryParams.given) $data.registration.firstname = this.$data.queryParams.given;	
	if (this.$data.queryParams.family) $data.registration.lastname = this.$data.queryParams.family;
	if (this.$data.queryParams.gender) $data.registration.gender = this.$data.queryParams.gender;
	if (this.$data.queryParams.country) $data.registration.country = this.$data.queryParams.country;
	
	if (this.$data.queryParams.language) {
		$data.registration.language = this.$data.queryParams.language;
		me.changeLanguage(this.$data.queryParams.language);
	}
	if (this.$data.queryParams.birthdate) {
		var d = new Date(this.$data.queryParams.birthdate);
		$data.registration.birthdayDate = d.getDate()+"."+(1+d.getMonth())+"."+d.getFullYear();		
	}
	if (me.addressNeeded()) {
		if (this.$data.queryParams.city) $data.registration.city = this.$data.queryParams.city;
		if (this.$data.queryParams.zip) $data.registration.zip = this.$data.queryParams.zip;
		if (this.$data.queryParams.street) $data.registration.address1 = this.$data.queryParams.street;
	}
	if (me.addressNeeded() || me.phoneNeeded()) {
		if (this.$data.queryParams.phone) $data.registration.phone = this.$data.queryParams.phone;
		if (this.$data.queryParams.mobile) $data.registration.mobile = this.$data.queryParams.mobile;
	}
	if (this.$data.queryParams.ro) {
		let r = $data.registration;
		
		if (r.firstname && r.lastname && (r.gender || !me.genderNeeded()) && r.country && (r.birthdayDate || !me.birthdayNeeded())) {
			if (!me.addressNeeded() || (r.city && r.zip && r.address1)) {				
			   $data.short = true;
			}
		}		
	}

  }
}
</script>
