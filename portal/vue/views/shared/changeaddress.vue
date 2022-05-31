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
<panel :title="$t('changeaddress.title')" :busy="isBusy">
    <error-box :error="error"></error-box>
			                
	<form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="changeAddress()" role="form">
		<form-group name="email" label="registration.email">
		    <p class="form-control-plaintext">{{  registration.email }} <button class="btn btn-sm btn-default" v-t="'common.change_btn'" @click="changeEmail();"></button></p>
		</form-group>
		<form-group name="firstname" label="registration.firstname" :path="errors.firstname">
            <input type="text" class="form-control" id="firstname" name="firstname" :placeholder="$t('registration.firstname')" @change="adrChange();" v-model="registration.firstname" required v-validate>
        </form-group>
		<form-group name="lastname" label="registration.lastname" :path="errors.lastname">
            <input type="text" class="form-control" id="lastname" name="lastname" :placeholder="$t('registration.lastname')" @change="adrChange();" v-model="registration.lastname" required v-validate>
        </form-group>
			  
		<form-group name="gender" label="registration.gender" :path="errors.gender">
            <select class="form-control" id="gender" name="gender" @change="adrChange();" v-model="registration.gender" v-validate>
                <option value selected disabled hidden>{{ $t('common.fillout') }}</option>
                <option value="FEMALE" v-t="'enum.gender.FEMALE'">female</option>
                <option value="MALE" v-t="'enum.gender.MALE'"></option>
                <option value="OTHER" v-t="'enum.gender.OTHER'"></option>
            </select>
        </form-group>
		<form-group name="birthday" label="registration.birthday" :path="errors.birthdayDate">
            <input type="text" class="form-control" name="birthdayDate" @change="birthChange()" v-model="registration.birthdayDate" v-validate>
        </form-group>		
			   
		<form-group name="address1" label="registration.address" :path="errors.address1">
            <input type="text" class="form-control" id="address1" name="address1" :placeholder="$t('registration.address_line1')" @change="adrChange();" v-model="registration.address1" v-validate>
        </form-group>
		<form-group name="address2" label="common.empty" :path="errors.address2">
            <input type="text" class="form-control" id="address2" name="address2" :placeholder="$t('registration.address_line2')" @change="adrChange();" v-model="registration.address2" v-validate>
        </form-group>
		<form-group name="city" label="registration.city" :path="errors.city">
            <input type="text" class="form-control" id="city" name="city" :placeholder="$t('registration.city')" @change="adrChange();" v-model="registration.city" v-validate>
        </form-group>
		<form-group name="zip" label="registration.zip" :path="errors.zip">
            <input type="text" class="form-control" id="zip" name="zip" :placeholder="$t('registration.zip')" @change="adrChange();" v-model="registration.zip" v-validate>
        </form-group>
		<form-group name="country" label="registration.country" :path="errors.country">
            <select class="form-control" id="country" name="country" @change="adrChange();" v-model="registration.country" required v-validate>
                <option value selected disabled hidden>{{ $t('common.fillout') }}</option>
                <option value="CH" v-t="'enum.country.CH'"></option>
            </select>
        </form-group>
                        
        <form-group name="phone" label="registration.phone" :path="errors.phone">
            <input type="text" class="form-control" id="phone" name="phone" :placeholder="$t('registration.phone')" @change="adrChange();" v-model="registration.phone" v-validate>
        </form-group>
        <form-group name="mobile" label="registration.mobile_phone" :path="errors.mobile">
            <input type="text" class="form-control" id="mobile" name="mobile" :placeholder="$t('registration.mobile_phone')" @change="adrChange();" v-model="registration.mobile" v-validate>
        </form-group>
                         
        <button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'changeaddress.update_btn'"></button>
        <success action="changeAddress" msg="changeaddress.success" :finished="finished"></success>
    </form>
		
</panel>

</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import session from "services/session.js"
import users from "services/users.js"
import languages from "services/languages.js"
import { status, ErrorBox, FormGroup, Success } from 'basic-vue3-components'
import dateService from "services/date.js"

var pad = function(n){
	return ("0" + n).slice(-2);
};

export default {
  
    data: () => ({
        registration : {},
        addressChanged : false,
        birthdayChanged : false,
        actions : null
	}),	

    components: {  Panel, FormGroup, ErrorBox, Success },

    mixins : [ status ],
  
    methods : {
        changeEmail() {
            const { $router, $data } = this;
		    $router.push({ path : "./changeemail", query : { userId : $data.registration._id, actions : $data.actions } });
        },
        
        adrChange() {
            const { $data } = this;
		    $data.addressChanged = true;
	    },
	
	    birthChange() {
            const { $data } = this;
		    $data.birthdayChanged = true;
        },

        changeAddress() {		
		    const { $data, $t, $router } = this, me = this;
            let q = Promise.resolve();
		
            let data = $data.registration;
            data.user = data._id;

            var d = data.birthdayDate;
		
		    if (d) {
			    var dparts = d.split("\.");
			    if (dparts.length != 3 || !dateService.isValidDate(dparts[0],dparts[1],dparts[2])) {
			        this.setError("birthdayDate", $t("error.invalid.date"));			  
			        return;
			    } else {
				    if (dparts[2].length==2) dparts[2] = "19"+dparts[2];
				    data.birthday = dparts[2]+"-"+pad(dparts[1])+"-"+pad(dparts[0]);				
                }
            }
		
			if ($data.birthdayChanged) {		
			    q = q.then(function() { return me.doAction("changeAddress", users.updateBirthday(data)); });
		    }
		    if ($data.addressChanged) {
			    q = q.then(function() { return me.doAction("changeAddress", users.updateAddress(data)); });
		    }
            q.then(function() {	
        	    $data.success = true;	
                if ($data.actions) $router.go(-1);
		    });
	    },
	
        
        init() {
            const { $data, $route, $filters } = this, me = this;
            $data.actions = $route.query.actions;
            me.doBusy(session.currentUser.then(function(myUserId) { 
		        let userId = $route.query.userId || myUserId;
		        me.doBusy(users.getMembers({"_id": userId }, ["name", "email", "gender", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone", "birthday"]))
		        .then(function(results) {
                    let data = results.data[0];
                    data.birthdayDate = $filters.date(data.birthday);
                    $data.registration = data;                    
                });
		    }));		
	    }        
    },

    created() {
        this.init();
    }
   
}
</script>