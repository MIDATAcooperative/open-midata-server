<template>

	
		<!-- Registration -->
		<panel :title="$t('addresearcher.sign_up')" :busy="isBusy">
						
            <error-box :error="error"></error-box>
            
			<form ref="myform" class="css-form form-horizontal" @submit.prevent="register()" role="form" novalidate>
			    
                 <div class="required">								  
				    <form-group name="email" label="registration.email" :path="errors.email">
						<input type="email" class="form-control" id="email" name="email" :placeholder="$t('registration.email')" v-model="registration.email" required v-validate>									
							    
						<router-link v-if="isNew" :to="{ path : './login', query : {action:action, login:login} }" v-t="'registration.already_have_account'"></router-link>
					</form-group>					                
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
                    <form-group myid="language" label="registration.language" :path="errors.language">
                        <select class="form-control" id="language" v-model="registration.language">
                            <option v-for="lang in languages" :key="lang.value" :value="lang.value">{{ $t(lang.name) }}</option>
                        </select>
                    </form-group>                
                    <form-group name="country" label="registration.country" :path="errors.country">
                        <select class="form-control" id="country" name="country" v-model="registration.country" required v-validate>
                            <option v-for="country in countries" :key="country" :value="country">{{ $t('enum.country.'+country) }}</option>
                        </select>
                    </form-group>                    
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
import status from "mixins/status.js";
import { getLocale } from "services/lang.js";
import FormGroup from 'components/FormGroup.vue';
import ErrorBox from 'components/ErrorBox.vue';
import Success from 'components/Success.vue';
import Panel from 'components/Panel.vue';
import CheckBox from 'components/CheckBox.vue';

export default {
  data: () => ({
    registration : { language : getLocale(), confirmStudy : [], unlockCode : null },
	languages : languages.all,
	countries : languages.countries,	
	flags : { optional : false },
    genders : ["FEMALE","MALE","OTHER"],        
  }),

  components : {
     FormGroup, ErrorBox, Panel, CheckBox, Success
  },

  mixins : [ status ],
    
  methods : {
   		    
    register() {		        
		const { $data } = this, me = this;				
		var data = $data.registration;			        	
		me.doAction("register", server.post(jsRoutes.controllers.research.Researchers.registerOther().url, data));						
	}
	
  },

  created() {    
    const me = this;
    me.ready();
  }
}
</script>
