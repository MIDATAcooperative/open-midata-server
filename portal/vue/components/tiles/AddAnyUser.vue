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
    <div class="ignore autosize">
        <div class="body" v-if="!isBusy">
						       
	        <form class="form-horizontal" @submit.prevent="searchUsers()">
	            <p v-t="'addusers.search_by_email'"></p>
		        <div class="form-group form-row">
		           <div class="col-sm-3 mb-1">
		               <select class="form-control" v-if="!fixedRole" v-model="crit.role" v-validate required>
							<option value selected disabled hidden>{{ $t('common.fillout') }}</option>
                            <option v-for="role in roles" :key="role.value" :value="role.value">{{ $t(role.name) }}</option>
                       </select>
			        </div>
			        <div class="col-sm-7 mb-1">
				        <input type="text" class="form-control" :placeholder="$t('addusers.search_user')" v-model="crit.userQuery" required v-validate>
			        </div>
			        <div class="col-sm-2">
				        <button type="submit" :disabled="action!=null" class="btn btn-primary form-control" v-t="'common.search_btn'"></button>
			        </div>
		        </div>
	        </form>
	        <div v-if="foundUsers.all">
	        <p v-if="foundUsers.all.length == 0" v-t="'addusers.no_matching_users'"></p>
			<pagination v-model="foundUsers"></pagination>
	        <div v-if="foundUsers.all.length > 0">
		        <div class="form-check" v-for="user in foundUsers.filtered" :key="user._id">
			        <label class="form-check-label"> 
                        <input class="form-check-input" type="checkbox" v-model="user.checked"> {{user.firstname}} {{user.lastname}} &lt;{{user.email}}&gt;
			        </label>
		        </div>
		        <p v-if="foundUsers.all.length === 0" v-t="'addusers.no_new_found'"></p>
	        </div>
			</div>
        </div>
        <div class="footer">
	        <button type="button" class="btn btn-primary" @click="addUsers(circle)" v-t="'common.add_btn'"></button>&nbsp;
	        <button type="button" class="btn btn-default" @click="$emit('close')" v-t="'common.cancel_btn'"></button>
        </div>
    </div>
</template>
<script>

import server from "services/server"
import { status, rl, FormGroup, CheckBox } from 'basic-vue3-components'
import hc from "services/hc"
import _ from "lodash"

export default {

	props: [ "setup" ],
	emits : [ "add", "close" ],

    data : ()=>({      	
		foundUsers : {},
		crit : { userQuery : "", role : "PROVIDER" },
		roles : [        
		   { value : "PROVIDER" , name : "enum.userrole.PROVIDER"},
		   { value : "RESEARCH" , name : "enum.userrole.RESEARCH"},
		   { value : "DEVELOPER" , name : "enum.userrole.DEVELOPER"}
        ],
    }),
	
	components: { CheckBox, FormGroup },

	mixins : [ status, rl ],

	methods : {
		reload() {
			const { $data } = this, me = this;
			
			me.ready();
		},
					
		searchUsers() {
			const { $data } = this, me = this;
			
			me.doAction("search", server.post(jsRoutes.controllers.Users.get().url, { properties : { email : $data.crit.userQuery, role : $data.crit.role }, fields : ["_id", "firstname", "lastname", "status", "email" ] }).
			then(function(users) {				
				$data.foundUsers = me.process(users.data);
				
			}));		
		
		},
	
	
		addUsers() {
			const { $data, $emit } = this, me = this;
			var circle = me.setup.consent;
			// get the users that should be added to the circle
			
			var usersToAdd = _.filter($data.foundUsers.all, function(user) { return user.checked; });			
			var userIds = _.map(usersToAdd, function(user) { return user._id; });
			userIds = _.uniq(userIds, false, function(userId) { return userId; });
				
			if (usersToAdd.length === 0) return;
			$emit("add", usersToAdd);		
		}
		
	},

	created() {
		this.reload();
	}
    
}
</script>