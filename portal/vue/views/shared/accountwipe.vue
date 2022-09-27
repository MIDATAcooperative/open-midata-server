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
    <div>		
		<panel :title="$t('accountwipe.title')" :busy="isBusy">
            <div class="alert alert-warning">
                <strong v-t="'accountwipe.warning'"></strong>
                <p v-t="'accountwipe.final'">This cannot be undone!</p>
            </div>
			<p>{{ $t(getHello('accountwipe.hello' ), { name : user.name }) }}</p>
			<p v-t="'accountwipe.paragraph1'"></p>
			<p v-t="'accountwipe.paragraph2'"></p>	
			<ul>
			  <li v-t="'accountwipe.point1'"></li>
			  <li v-t="'accountwipe.point2'"></li>
			  <li v-t="'accountwipe.point3'"></li>
			</ul>		
			<p v-t="'accountwipe.paragraph3'"></p>
			<form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="accountWipe()" role="form">
			    <input type="text" style="display: none" id="fakeUsername" name="fakeUsername" value="" />
                <input type="password" style="display: none" id="fakePassword" name="fakePassword" value="" />
	
	            <form-group label="user.name" name="name">
	                <p class="form-control-plaintext">
	                    {{ user.name }} ({{ $t("enum.userrole."+user.role) }})<br>
	                    {{ user.email }}	                
	                </p>
	            </form-group>
	           
	            <form-group name="password" label="accountwipe.password" :path="errors.password">
				    <password class="form-control" id="password" name="password" v-model="user.password" 
					required autocomplete="off" />
				</form-group>
				
				<form-group name="reason" label="accountwipe.reason" :path="errors.reason">
				    <p class="form-control-plaintext" v-t="'accountwipe.reason2'"></p>
				    <textarea class="form-control" id="reason" name="reason" v-model="user.reason"></textarea>
				</form-group>
                <error-box :error="error"></error-box>
	           	            
				<button type="submit" v-submit :disabled="action!=null" class="btn btn-danger mt-3 btn-block" v-t="'accountwipe.wipe_btn'"></button>				
				<button type="button" class="btn btn-default mt-3 btn-block" @click="skip()" v-t="'common.cancel_btn'"></button>
			</form>
							
        </panel>				   				
	</div>
	
</template>
<script>
import Panel from "components/Panel.vue"
import server from "services/server.js"
import crypto from "services/crypto.js"
import session from "services/session.js"
import users from "services/users.js"
import actions from "services/actions.js"
import languages from "services/languages.js"
import ENV from "config"
import { setLocale } from 'services/lang.js';
import { status, CheckBox, ErrorBox, FormGroup, RadioBox, Password } from 'basic-vue3-components'

export default {
  
    data: () => ({
        user : {},        
        isSelf : false        
	}),	

    components: { RadioBox, Panel, FormGroup, ErrorBox, Password },

    mixins : [ status ],
  
    methods : {
        accountWipe() {
            const { $data, $route } = this, me = this;
		    if (!$data.user.password) {
			    $data.error = { code : "accountwipe.error" };
			    return;
		    }
		    $data.user.passwordHash = crypto.getHash($data.user.password);
		    me.doAction("wipe", server.post("/api/shared/users/wipe", $data.user)).then(function() {
				//if ($route.query.actions) window.close();
				session.logout();
		        document.location.href="/#/public/login"; 
	        });
	    },

		skip() {
        const { $data, $route, $router } = this, me = this;
			if (!actions.showAction($router, $route)) {
				$router.go(-1);
			}
	    },
	
	    getHello(label) {
            const { $data } = this, me = this;
		    if ($data.user.gender === "FEMALE") return label+"_w";
		    if ($data.user.gender === "MALE") return label+"_m";
		    return label;
	    },
        
        init() {
            const { $data, $route } = this, me = this;
            let userId = $route.query.userId;	
            me.doBusy(session.currentUser.then(function(myUserId) {
                userId = userId || myUserId;
                $data.isSelf = myUserId == userId;
	            me.doBusy(users.getMembers({"_id": userId}, ["name", "email", "searchable", "language", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone", "emailStatus", "agbStatus", "contractStatus", "role", "subroles", "confirmedAt", "birthday", "midataID", "status", "gender", "authType", "notifications"]))
		        .then(function(results) {
                    let user = results.data[0];                    
			        $data.user = user;
                });		
            }));
        }
						
    },

    created() {
        this.init();
    }
}
</script>
