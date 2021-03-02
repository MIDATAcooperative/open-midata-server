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
		<panel style="max-width:630px; padding-top:30px; margin:0 auto;" :title="$t('registration.sign_up')" :busy="isBusy">
			
			<div class="midatalogo">
				<img src="/images/logo.png" style="height: 36px;" alt="">
			</div>

            <error-box :error="error"></error-box>
            <p v-if="offline" class="alert alert-danger" v-t="'error.offline'"></p>
			<form ref="myform" class="css-form form-horizontal" @submit.prevent="register()" role="form" novalidate>
			    <span v-t="'registration.mandantory_fields'"></span>
				<span v-t="'registration.password_policy'"></span>
 
				<div v-if="role=='research'">
					<h3 v-t="'researcher_registration.research_organization'"></h3>
					<div class="required">
						<form-group name="name" label="researcher_registration.name" :path="errors.name"> 
							<input type="text" class="form-control" name="name" :placeholder="$t('researcher_registration.name')" v-model="registration.name" required>
						</form-group>
						<form-group name="description" label="researcher_registration.description" :path="errors.description">
							<textarea class="form-control" name="description" rows="5" v-model="registration.description" required></textarea> 
						</form-group>
						<h3 v-t="'researcher_registration.research_user'">Research User</h3>
					</div>
				</div>

                 <div class="required">								  
				    <form-group name="email" label="registration.email" :path="errors.email">
						<input type="email" class="form-control" id="email" name="email" :placeholder="$t('registration.email')" v-model="registration.email" required v-validate>									
							    
						<router-link v-if="isNew" :to="{ path : './login', query : {action:action, login:login} }" v-t="'registration.already_have_account'"></router-link>
					</form-group>
					<form-group name="password" label="registration.password" :path="errors.password"> 
						<input type="password" class="form-control" id="password" name="password" :placeholder="$t('registration.password')" v-model="registration.password1" required v-validate>							  
				    </form-group>
					<form-group name="password2" label="registration.password_repetition" :path="errors.password2">
                        <input type="password" class="form-control" id="password2" name="password2" :placeholder="$t('registration.password')" v-model="registration.password2" required v-validate>
                    </form-group>
                </div>
                <form-group name="secure" label="registration.secure" v-if="secureChoice()">
                    <div class="form-check">
                        <label class="form-check-label">
                            <input class="form-check-input" type="checkbox" v-model="registration.secure">
                            <span v-t="'registration.secure2'"></span>
                        </label>
                    </div>
                </form-group>
                <div class="required">
                    <form-group name="firstname" label="registration.firstname" :path="errors.firstname">
                        <input type="text" class="form-control" id="firstname" name="firstname" :placeholder="$t('registration.firstname')" v-model="registration.firstname" required v-validate>
                    </form-group>
                    <form-group name="lastname" label="registration.lastname" :path="errors.lastname">
                        <input type="text" class="form-control" id="lastname" name="lastname" :placeholder="$t('registration.lastname')" v-model="registration.lastname" required v-validate>
                    </form-group>
                                
                    <form-group name="gender" label="registration.gender" :path="errors.gender">
                        <select class="form-control" id="gender" name="gender" v-model="registration.gender" required v-validate>
                            <option value="FEMALE" v-t="'enum.gender.FEMALE'">female</option>
                            <option value="MALE" v-t="'enum.gender.MALE'"></option>
                            <option value="OTHER" v-t="'enum.gender.OTHER'"></option>
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
                            <option v-for="lang in languages" :key="lang.value" :value="lang.value">{{ $t(lang.name) }}</option>
                        </select>
                    </form-group>
                </div>
                <div v-if="addressNeeded()" class="required">
                    <form-group name="address1" label="registration.address" :path="errors.address">
                        <input type="text" class="form-control" id="address1" name="address1" :placeholder="$t('registration.address_line1')" v-model="registration.address1" required v-validate>
                    </form-group>
                    <form-group name="address2" label="common.empty">
                        <input type="text" class="form-control" id="address2" name="address2" :placeholder="$t('registration.address_line2')" v-model="registration.address2" v-validate>
                    </form-group>
                                
                    <form-group name="city" label="registration.city" :path="errors.city">
                        <input type="text" class="form-control" id="city" name="city" :placeholder="$t('registration.city')" v-model="registration.city" required v-validate>
                    </form-group>
                    <form-group name="zip" label="registration.zip" :path="errors.zip">
                        <input type="text" class="form-control" id="zip" name="zip" :placeholder="$t('registration.zip')" v-model="registration.zip" required v-validate>
                    </form-group>
                </div>
                <div v-if="countryNeeded()" class="required">
                    <form-group name="country" label="registration.country" :path="errors.country">
                        <select class="form-control" id="country" name="country" v-model="registration.country" required v-validate>
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
               <!-- <div class="required" v-if="app.unlockCode">
                    <form-group name="unlockCode" label="registration.unlock_code" :path="errors.unlockCode">
                        <input type="text" class="form-control" id="unlockCode" name="unlockCode" :placeholder="$t('registration.unlock_code')" v-model="registration.unlockCode" required v-validate>
                        <formerror myid="unlockCode" type="invalid" message="error.invalid.unlock_code"></formerror>
                    </form-group>
                </div> -->
                <form-group name="agb" label="registration.agb" >
                    <check-box v-model="registration.agb" name="agb" :path="errors.agb">                                 
                        <span v-t="'registration.agb2'"></span>&nbsp;
                        <a @click="showTerms(currentTerms.member.termsOfUse);" href="javascript:" v-t="'registration.agb3'"></a>&nbsp;
                        <span v-t="'registration.privacypolicy2'"></span>&nbsp;
                        <a @click="showTerms(currentTerms.member.privacyPolicy);" href="javascript:" v-t="'registration.privacypolicy3'"></a>
                    </check-box>                        
                </form-group>                                                          

                <button class="btn btn-primary btn-block" type="submit" :disabled="action!=null" v-t="'registration.sign_up_btn'" v-submit>					
                </button>
			
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
        </div></div>
  
</template>
<script>
import server from "services/server.js";
import crypto from "services/crypto.js";
import oauth from "services/oauth.js";
import dateService from "services/date.js";
import languages from "services/languages.js";
import status from "mixins/status.js";
import session from "services/session.js";
import ENV from "config";
import { getLocale, setLocale, addBundle } from "services/lang.js";
import FormGroup from 'components/FormGroup.vue';
import ErrorBox from 'components/ErrorBox.vue';
import Panel from 'components/Panel.vue';
import TermsModal from 'components/TermsModal.vue';
import CheckBox from 'components/CheckBox.vue';

export default {
  data: () => ({
    registration : { language : getLocale(), confirmStudy : [], unlockCode : null },
	languages : languages.all,
	countries : languages.countries,	
	flags : { optional : false },
    genders : ["FEMALE","MALE","OTHER"],
    offline : false,
    currentTerms : "",
    terms : { which : "", active : false },
    welcomemsg : false,

    action : null,
    login : null,
	role : "user"
  }),

  components : {
     FormGroup, ErrorBox, Panel, TermsModal, CheckBox 
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
		setLocale(lang);
	},

	addressNeeded() {
        const { $data } = this;
		if ($data.role == "research") return true;
		return $data.app && $data.app.requirements && ($data.app.requirements.indexOf('ADDRESS_ENTERED') >= 0 ||  $data.app.requirements.indexOf('ADDRESS_VERIFIED') >=0 );
	},
	
	phoneNeeded() {
        const { $data } = this;
		if ($data.role == "research") return true;
		return $data.app && $data.app.requirements && ($data.app.requirements.indexOf('PHONE_ENTERED') >= 0 ||  $data.app.requirements.indexOf('PHONE_VERIFIED') >=0 );
	},

	birthdayNeeded() {
		const { $data } = this;
		return $data.app && $data.app.requirements && ($data.app.requirements.indexOf('BIRTHDAY_SET') >= 0);
	},

	countryNeeded() {
		const { $data, $route } = this;
		return this.addressNeeded() || !$route.query.country;
	},

	languageNeeded() {
		const { $data, $route } = this;
		return !$route.query.lang;
	},

	secureChoice() {
		const { $data } = this;
		if ($data.role == "research") return false;
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

	mustAccept(v) {
		return v==true ? "" : $t('error.missing.agb')
	},

    register() {		        
		const { $data, $router, $route, $t } = this, me = this;
		console.log($data.registration.agb);

		if ($data.registration.password1 != $data.registration.password2) {
			this.setError("password", $t("error.invalid.password_repetition"));
			return;
		}

		var pwvalid = crypto.isValidPassword($data.registration.password1);         
        if (!pwvalid) {
			this.setError("password", $t("error.tooshort.password"));
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
		
		if ($route.query.developer) {
			data.developer = $route.query.developer;
		}
		
		
		var finishRegistration = function() { 
			if (oauth.getAppname()) {		  
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
				  $data.welcomemsg = true;	
				  
				  if ($data.app && $data.app.requirements && $data.app.requirements.indexOf('EMAIL_VERIFIED') >= 0) {
					  me.confirmWelcome(); 
				  }
			  });
			} else if ($data.role == "research") {
				me.doAction("register", server.post(jsRoutes.controllers.research.Researchers.register().url, data))
		        .then(function(data) { session.postLogin(data, $router, $route); });			
			} else {
			
				me.doAction("register", server.post(jsRoutes.controllers.Application.register().url, data)).
				then(function(data) { session.postLogin(data, $router, $route); });
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
	 $data.role = $route.meta.role;
	 if ($data.role == "research") {
		 addBundle("researchers");
		 $data.registration.secure = true;
	 }
     this.doBusy(server.get(jsRoutes.controllers.Terms.currentTerms().url).then(function(result) { $data.currentTerms = result.data; }));

     $data.days = [];
	 $data.months = [];
	 var i = 0;
	 for (i=1;i <= 9; i++ ) { $data.months.push("0"+i); }
	 for (i=10;i <= 12; i++ ) $data.months.push(""+i);
	
	if (oauth.getAppname()) {		
	   $data.app = oauth.app;
	   $data.links = oauth.links;
	}
	
	
	
	if ($route.query.login) {
		$data.registration.email = $route.query.login;
        
        $data.action = $route.query.action;
		$data.login = $route.query.login;
		$data.isNew = true;
	}
	if ($route.query.given) $data.registration.firstname = $route.query.given;	
	if ($route.query.family) $data.registration.lastname = $route.query.family;
	if ($route.query.gender) $data.registration.gender = $route.query.gender;
	if ($route.query.country) $data.registration.country = $route.query.country;
	
	if ($route.query.language) {
		$data.registration.language = $route.query.language;
		me.changeLanguage($route.query.language);
	}
	if ($route.query.birthdate) {
		var d = new Date($route.query.birthdate);
		$data.registration.birthdayDate = d.getDate()+"."+(1+d.getMonth())+"."+d.getFullYear();		
	}
	if (me.addressNeeded()) {
		if ($route.query.city) $data.registration.city = $route.query.city;
		if ($route.query.zip) $data.registration.zip = $route.query.zip;
		if ($route.query.street) $data.registration.address1 = $route.query.street;
	}
	if (me.addressNeeded() || me.phoneNeeded()) {
		if ($route.query.phone) $data.registration.phone = $route.query.phone;
		if ($route.query.mobile) $data.registration.mobile = $route.query.mobile;
	}
	
  }
}
</script>
