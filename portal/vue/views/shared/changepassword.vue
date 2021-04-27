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

<panel :title="$t('changepassword.title')" :busy="isBusy">
    <error-box :error="error"></error-box>
			            
	<form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="changePassword()" role="form">

        <input type="text" style="display: none" id="fakeUsername" name="fakeUsername" value="" />
        <input type="password" style="display: none" id="fakePassword" name="fakePassword" value="" />
	
		<form-group name="oldPassword" label="changepassword.old_password" :path="errors.oldPassword">
			<password class="form-control" id="oldPassword" name="oldPassword" :placeholder="$t('changepassword.old_password')" v-model="pw.oldPassword"
					required autocomplete="off" />
        </form-group>
		<form-group name="password" label="changepassword.new_password" :path="errors.password">
			<password class="form-control" id="password" name="password"
					:placeholder="$t('changepassword.new_password')" v-model="pw.password" required autocomplete="off" />				   
		</form-group>
		<form-group name="password2" label="changepassword.repeat_password" :path="errors.password2">
			<password class="form-control" id="password2" name="password2" :placeholder="$t('changepassword.new_password')" v-model="pw.password2"
					required autocomplete="off" />
        </form-group>
		<form-group name="secure" label="registration.secure">
            <check-box name="secure" v-model="pw.secure" :path="errors.secure">
                <span v-t="'registration.secure2'"></span>
            </check-box>
        </form-group>				
		<button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'changepassword.change_btn'"></button>		
        <success action="changePassword" msg="changeaddress.success" :finished="finished"></success>
    </form>
		
</panel>

</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import session from "services/session.js"
import crypto from "services/crypto.js"
import { status, ErrorBox, FormGroup, CheckBox, Success, Password } from 'basic-vue3-components'

export default {
  
    data: () => ({
        pw : { oldPassword:"", password:"", password2:"", secure : false }
	}),	

    components: {  Panel, FormGroup, ErrorBox, Success, CheckBox, Password },

    mixins : [ status ],
  
    methods : {
        changePassword() {		
            const { $data } = this, me = this;
            let pwvalid = crypto.isValidPassword($data.pw.password); 
            
            if ($data.pw.password != $data.pw.password2) {
			    this.setError("password", $t("error.invalid.password_repetition"));
			    return;
		    }

		
            if (!pwvalid) {
			    this.setError("password", $t("error.tooshort.password"));
        	    return;
            }
	
		    let data = $data.pw;
		
		    me.doAction("changePassword", crypto.generateKeys($data.pw.password).then(function(keys) {
			
			    if ($data.pw.secure) {
				    data = { oldPassword : $data.pw.oldPassword, oldPasswordHash : crypto.getHash($data.pw.oldPassword) };
				    data.password = keys.pw_hash;
				    data.pub = keys.pub;
				    data.priv_pw = keys.priv_pw;
				    data.recovery = keys.recovery;
				    data.recoverKey = keys.recoverKey;
			    } else {
				    data = { oldPassword : $data.pw.oldPassword, oldPasswordHash : crypto.getHash($data.pw.oldPassword) };
				    data.password = $data.pw.password;
			    }
			    return server.post(jsRoutes.controllers.PWRecovery.changePassword().url, data);			
		    })).then(function() { session.login(); });
						 
        },
        
        init() {
            const { $data } = this;
            this.doBusy(session.currentUser.then(function() {
		        $data.pw.secure = session.user.security == "KEY_EXT_PASSWORD";
	        }));
        }
    },

    created() {
        this.init();
    }
   
}
</script>