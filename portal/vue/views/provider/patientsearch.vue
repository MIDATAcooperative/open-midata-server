<template>
    <panel :title="$t('provider_patientsearch.title')" :busy="isBusy">    
        <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="dosearch()" role="form">
			<div class="row">
			    <div class="col-sm-6">
			        <form-group name="email" label="provider_patientsearch.email" :path="errors.email">
				        <input type="text" class="form-control" id="midataID" name="midataID" @change="changedCriteria()" v-validate v-model="criteria.email" :required="!criteria.midataID">
				    </form-group>
			    </div>
			    <div class="col-sm-1">
			        <p class="form-control-plaintext" v-t="'provider_patientsearch.or'"></p>
			    </div>
			    <div class="col-sm-5">
				    <form-group name="midataID" label="provider_patientsearch.midataid" :path="errors.midataID">
				        <input type="text" class="form-control" name="midataID" @change="changedCriteria()" v-validate v-model="criteria.midataID" :required="!criteria.email">
				    </form-group>
				    <form-group name="birthday" label="provider_patientsearch.birthday" :path="errors.birthday">
				        <input type="text" class="form-control" id="birthday" name="birthday" @change="changedCriteria()" v-validate v-model="criteria.birthday" :required="criteria.midataID">
				    </form-group>
				</div>
			</div>
			
			<div v-if="member">
		
				<form-group name="team" label="provider_patientsearch.usergroup" :path="errors.team">
				    <select class="form-control" name="team" v-validate v-model="newconsent.usergroup">
                        <option v-for="ug in usergroups" :key="ug._id" :value="ug._id">{{ ug.name}}</option>
                    </select>
				</form-group>
				<form-group name="passcode" label="provider_patientsearch.passcode" :path="errors.passcode">
					<div class="input-group">
				        <input type="text" class="form-control" id="passcode" name="passcode" v-validate v-model="criteria.passcode">				          
				    </div>
				</form-group>				    									    					 						
			</div>		
			
			<button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'common.search_btn'"></button>
			<router-link v-if="searched && !member._id" :to="{ path : './addpatient', query : { email : criteria.email } }" class="btn btn-default" v-t="'provider_patientsearch.add_new_account_btn'"></router-link>
			<button v-if="searched" @ng-click="addConsent();" :disabled="action!=null" class="btn btn-default" v-t="'provider_patientsearch.add_new_consent_btn'"></button>
				
		</form>
		
    </panel>
	
	<div v-if="member">
	    <div class="row">
            <div class="col-sm-6">
                <panel :title="$t('provider_patientsearch.address')" :busy="isBusy">
                            
                    <address>
                        <strong>{{ member.firstname }} {{ member.lastname }}</strong><br> {{ member.address1 }}<br> {{ member.address2 }}<br> {{
                        member.zip }} {{ member.city }}<br> {{ member.country }}<br> <span v-if="member.phone"><span v-t="'common.user.phone'"></span>: {{ member.phone }}</span><br>
                        <span v-if="member.mobile"><span v-t="'common.user.mobile_phone'"></span>: {{ member.mobile }}</span><br> <span v-if="member.ssn"><span v-t="'common.user.ssn'"></span>: {{ member.ssn }}</span><br>
                        <span v-if="member.email"><span v-t="'common.user.email'"></span>: {{ member.email }}</span><br>
                    </address>
                                                                    
                    <button class="btn btn-primary" v-if="consents.length>0" @click="selectPatient();" v-t="'provider_patientsearch.show_patient_btn'"></button>
                </panel>
            </div>		
            <div class="col-sm-6">
                <panel :title="$t('provider_patientsearch.consents')" :busy="isBusy">		      		       
                    <ul>
                        <li v-for="consent in consents" :key="consent._id"><router-link :to="{ path : './editconsent', query :  {consentId:consent._id}}">{{ consent.name }} (<span>{{ $t('enum.consentstatus.'+consent.status) }}</span>)</router-link></li>
                    </ul>							
                </panel>
                
            </div>
		</div>
	</div>
		
</template>
<script>
import ErrorBox from "components/ErrorBox.vue"
import Success from "components/Success.vue"
import Panel from "components/Panel.vue"
import CheckBox from "components/CheckBox.vue"
import RadioBox from "components/RadioBox.vue"
import FormGroup from "components/FormGroup.vue"

import provideraccess from "services/provideraccess.js"
import circles from "services/circles.js"
import usergroups from "services/usergroups.js"
import status from 'mixins/status.js'

export default {
    data: () => ({	
        criteria : {},
	    newconsent : {},
	    member : null,
        consents : null
    }),

    components: {  Panel, ErrorBox, FormGroup,  Success, CheckBox, RadioBox },

    mixins : [ status ],

    methods : {
        dosearch() {
            const { $data } = this, me = this;
            $data.member = null;
            $data.consents = null;
                        
            me.init();
                        
            me.doBusy(provideraccess.search($data.criteria)
            .then(function(data) { 				
                    $data.member = data.data.member;
                    $data.consents = data.data.consents;                                  
            }));            
            
	    },
	
	    changedCriteria() {
            const { $data } = this;
		    $data.member = null;
		    $data.consents = null;				
            $data.error = null;
	    },
	
	    selectPatient() {
            const { $data, $router } = this, me = this;
		    $router.push({ path : './memberdetails', query : { user : $data.member._id } });		
	    },
	
	    addConsent() {	
            const { $data, $router } = this, me = this;
            if ($data.criteria.passcode) {
                me.usePasscode();
                } else {		
                if ($data.member) {
                    $router.push({ path : './newconsent', query : { "authorize" : $data.newconsent.usergroup, "owner" : $data.member._id, "request" : true } });
                } else {
                    $router.push({ path : './newconsent', query : { "authorize" : $data.newconsent.usergroup, "extowner" : $data.criteria.email, "request" : true } });
                }
            }
	    },
	
	    usePasscode() {
            const { $data, $router } = this, me = this;
		    me.doAction("usepasscode", circles.joinByPasscode($data.member._id, $data.criteria.passcode, $data.newconsent.usergroup ))
	        .then(function(data) {
	    	    $router.push({ path : './editconsent', query : { consentId : data.data._id } });
	        });
	    },
	
	    init() {
            const { $data, $route } = this, me = this;
		    me.doBusy(usergroups.search({ "member" : true, "active" : true }, usergroups.ALLPUBLIC )
    	    .then(function(results) {
		        $data.usergroups = results.data;
    	    }));
	    }
       
    },

    created() {
        const { $data, $route } = this, me = this;
        if ($route.query.email) {
		    $data.criteria.email = $route.query.email;
	    }
        me.ready();        
    }
}
</script>