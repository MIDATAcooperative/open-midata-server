<template>

	
		<!-- Registration -->
		<panel :title="getTitle()" :busy="isBusy">
						
            <error-box :error="error"></error-box>
            
			<form ref="myform" class="css-form form-horizontal" @submit.prevent="register()" role="form" novalidate>
			    
                 <div :class="{ 'required' : emailNeeded() }">								  
				    <form-group name="email" label="registration.email" :path="errors.email">
						<input type="email" class="form-control" id="email" name="email" :placeholder="$t('registration.email')" v-model="registration.email" :required="emailNeeded()" v-validate>																    						
					</form-group>		
                </div>
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
                    <div class="required" v-if="birthdayNeeded()">
                        <form-group name="birthday" label="registration.birthday" :path="errors.birthdayDate">
                            <input type="text" class="form-control"   name="birthdayDate" v-model="registration.birthdayDate" required v-validate>                                                       
                        </form-group>
				    </div>			
                    <form-group myid="language" label="registration.language" :path="errors.language">
                        <select class="form-control" id="language" v-model="registration.language">
                            <option v-for="lang in languages" :key="lang.value" :value="lang.value">{{ $t(lang.name) }}</option>
                        </select>
                    </form-group>    
                    <div v-if="addressNeeded()" class="required">
                        <form-group name="address1" label="registration.address" :path="errors.address">
                            <input type="text" class="form-control" id="address1" name="address1" :placeholder="$t('registration.address_line1')" v-model="registration.address1" required v-validate>
                        </form-group>
                        <form-group name="address2" label="">
                            <input type="text" class="form-control" id="address2" name="address2" :placeholder="$t('registration.address_line2')" v-model="registration.address2" v-validate>
                        </form-group>
                                    
                        <form-group name="city" label="registration.city" :path="errors.city">
                            <input type="text" class="form-control" id="city" name="city" :placeholder="$t('registration.city')" v-model="registration.city" required v-validate>
                        </form-group>
                        <form-group name="zip" label="registration.zip" :path="errors.zip">
                            <input type="text" class="form-control" id="zip" name="zip" :placeholder="$t('registration.zip')" v-model="registration.zip" required v-validate>
                        </form-group>
                    </div>            
                    <form-group name="country" label="registration.country" :path="errors.country">
                        <select class="form-control" id="country" name="country" v-model="registration.country" required v-validate>
                            <option v-for="country in countries" :key="country" :value="country">{{ $t('enum.country.'+country) }}</option>
                        </select>
                    </form-group>  
                     <div v-if="phoneNeeded()" class="required">
                        <form-group name="phone" label="registration.phone" :path="errors.phone">
                            <input type="text" class="form-control" id="phone" name="phone" :placeholder="$t('registration.phone')" v-model="registration.phone" v-validate>
                        </form-group>
                                    
                        <form-group name="mobile" label="registration.mobile_phone" :path="errors.mobile">
                            <input type="text" class="form-control" id="mobile" name="mobile" :placeholder="$t('registration.mobile_phone')" v-model="registration.mobile" required v-validate>
                        </form-group>
                    </div>                  
                </div>           
                <form-group name="x" label="common.empty">
                    <button class="btn btn-primary" type="submit" :disabled="action!=null" v-t="'registration.sign_up_btn'" v-submit>					
                    </button>
                    <success :finished="finished" action="register" msg="common.save_ok"></success>  
                </form-group>
			
			</form>
		</panel>

</template>
<script>
import server from "services/server.js";
import languages from "services/languages.js";
import fhir from "services/fhir.js";
import status from "mixins/status.js";
import { getLocale } from "services/lang.js";
import FormGroup from 'components/FormGroup.vue';
import ErrorBox from 'components/ErrorBox.vue';
import Success from 'components/Success.vue';
import Panel from 'components/Panel.vue';
import CheckBox from 'components/CheckBox.vue';
import dateService from "services/date.js";

export default {
  data: () => ({
    registration : { language : getLocale(), confirmStudy : [], unlockCode : null },
	languages : languages.all,
	countries : languages.countries,	
	flags : { optional : false },
    genders : ["FEMALE","MALE","OTHER"],  
    mode : null,
    study : null
  }),

  components : {
     FormGroup, ErrorBox, Panel, CheckBox, Success
  },

  mixins : [ status ],
    
  methods : {
   	getTitle() {
        const { $data, $t } = this;
        if ($data.mode=="researcher") return $t('addresearcher.sign_up');
        return $t('addparticipant.sign_up');
    },

    reload() {
		const { $data, $route } = this, me = this;	
		me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($route.query.studyId).url))
		.then(function(data) { 				
			$data.study = data.data;	
		});
				
	},

    emailNeeded() {
        const { $data } = this;				
        if ($data.mode != "participant") return true;
		return $data.study && $data.study.requirements && ($data.study.requirements.indexOf('EMAIL_ENTERED') >= 0 ||  $data.study.requirements.indexOf('EMAIL_VERIFIED') >=0 );
	},
	
	addressNeeded() {
        const { $data } = this;				
		return $data.study && $data.study.requirements && ($data.study.requirements.indexOf('ADDRESS_ENTERED') >= 0 ||  $data.study.requirements.indexOf('ADDRESS_VERIFIED') >=0 );
	},
	
	phoneNeeded() {
        const { $data } = this;				
		return $data.study && $data.study.requirements && ($data.study.requirements.indexOf('PHONE_ENTERED') >= 0 ||  $data.study.requirements.indexOf('PHONE_VERIFIED') >=0 );
	},

    birthdayNeeded() {
		const { $data } = this;
		return $data.study != null; //&& $data.study.requirements && ($data.study.requirements.indexOf('BIRTHDAY_SET') >= 0);
	},

    register() {		        
		const { $data, $route, $router } = this, me = this;				
		var data = $data.registration;			        	
        if ($route.meta.mode == "researcher") {
		    me.doAction("register", server.post(jsRoutes.controllers.research.Researchers.registerOther().url, data));						
        } else if ($route.meta.mode == "participant") {
            
                                             		
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

            var user = {
                "resourceType" : "Patient",
                "active" : true,
                "name" : [{
                    "family" : data.lastname,
                    "given" : [ data.firstname ]
                }],
                "telecom" : [],
                "gender" : data.gender.toLowerCase(),
                "birthDate" : data.birthday,
                "address" : [{
                    "line" : [],			   
                    "country" : data.country
                }],
                "communication" : [{
                    "language" : {
                    "coding" : { "code" : data.language, "system" : "urn:ietf:bcp:47" }
                    },
                    "preferred" : true
                }],
                "extension" : [			   
                { 
                    "url" : "http://midata.coop/extensions/terms-agreed",
                    "valueString" : "midata-terms-of-use--1.0"
                    },
                { 
                    "url" : "http://midata.coop/extensions/terms-agreed",
                    "valueString" : "midata-privacy-policy--1.0"
                    },
                    { 
                    "url" : "http://midata.coop/extensions/join-study",
                    "valueCoding" : { "code" : $data.study.code, "system" : "http://midata.coop/codesystems/study-code" }
                    }
                ]
        
                };
            if (data.email) user.telecom.push({
                "system" : "email",
                "value" : data.email
            });
            if (data.phone) user.telecom.push({
                "system" : "phone",
                "value" : data.phone
            });
            if (data.mobile) user.telecom.push({
                "system" : "phone",
                "value" : data.mobile
            });
            if (data.address1) user.address[0].line.push(data.address1);
            if (data.address2) user.address[0].line.push(data.address2);
            if (data.city) user.address[0].city = data.city;
            if (data.zip) user.address[0].postalCode = data.zip;
            
            me.doAction("register", fhir.postR4("Patient", user)).
            then(function(data) { 
            
            var ids = data.data.identifier;
            for (let identifier of ids) {
                if (identifier.system == "http://midata.coop/identifier/participant-id") {
                    $router.push({ path : './study.participant' , query : { participantId : identifier.value, studyId : $route.query.studyId } });        		   
                }
            }
            
            });
        }
	}
	
  },

  created() {    
    const { $data, $route } = this, me = this;
    $data.mode = $route.meta.mode;
    if ($data.mode == "participant") {
        me.reload();
    } else me.ready();
  }
}
</script>
