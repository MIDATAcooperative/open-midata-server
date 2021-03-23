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

<panel :title="$t('changeemail.title')" :busy="isBusy">
    <error-box :error="error"></error-box>
			            
	<form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="changeEmail()" role="form">

        <input type="text" style="display: none" id="fakeUsername" name="fakeUsername" value="" />
        <input type="password" style="display: none" id="fakePassword" name="fakePassword" value="" />
	
        <form-group name="midataId" label="changeemail.midataId">
			<p class="form-control-plaintext">{{ member.midataId }}<span v-show="!member.midataId" v-t="'changeemail.none'"></span></p>
        </form-group>
		<form-group name="name" label="changeemail.name">
			<p class="form-control-plaintext">{{ member.firstname }} {{ member.lastname }}</p>
        </form-group>
		<form-group name="oldEmail" label="changeemail.old_email">
			<p class="form-control-plaintext">{{ member.email }}<span v-show="!member.email" v-t="'changeemail.none'"></span></p>
        </form-group>
		<form-group name="email" label="changeemail.new_email" :path="errors.email"> 
			<input type="text" class="form-control" id="email" name="email"
					:placeholder="$t('changeemail.new_email')" v-model="pw.email" required v-validate autocomplete="off">				   
		</form-group>
				
		<button type="submit" v-submit :disabled="status!=null" class="btn btn-primary" v-t="'changeemail.change_btn'"></button>						
        <success action="changeEmail" msg="changeemail.success" :finished="finished"></success>
    </form>
		
</panel>

</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import session from "services/session.js"
import users from "services/users.js"
import { status, ErrorBox, FormGroup, Success } from 'basic-vue3-components'

export default {
  
    data: () => ({
        pw : { email : "", email2 : "" },
        member : { midataId:"", firstname:"", lastname:"" },
        userId : null
	}),	

    components: {  Panel, FormGroup, ErrorBox, Success },

    mixins : [ status ],
  
    methods : {
        changeEmail() {		
            const { $data } = this, me = this;
            let data = { user : $data.userId , email : $data.pw.email };
		
		    me.doAction("changeEmail", server.post(jsRoutes.controllers.admin.Administration.changeUserEmail().url, data));        						 
        },
        
        init() {
            const { $data, $route } = this, me = this;
            this.doBusy(session.currentUser.then(function(userId) {
		        $data.userId = $route.query.userId || userId;
		        me.doBusy(users.getMembers({ _id : $data.userId }, [ "midataID", "firstname", "lastname", "email", "role" ]))
		        .then(function(data) {
			        $data.member = data.data[0];
		        });
            }));    
        }
    },

    created() {
        this.init();
    }
   
}
</script>