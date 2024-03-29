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
			
		
	        <div v-if="contacts.all && contacts.all.length > 0">
	            <p v-t="'addusers.contacts'"></p>
				<pagination v-model="contacts"></pagination>
		        <div class="form-check" v-for="contact in contacts.filtered" :key="contact._id">
			        <label class="form-check-label"> 
                        <input class="form-check-input" type="checkbox" v-model="contact.checked"> 
                        <span class="margin-left">{{contact.firstname}} {{contact.lastname}} &lt;{{contact.email}}&gt;</span>
			        </label>
		        </div>
		        <p v-if="contacts.filtered.length === 0" v-t="'addusers.no_contacts_to_add'"></p>
	        </div>
	        <hr>
	        <form class="form-horizontal" @submit.prevent="searchUsers()">
	            <p v-t="'addusers.search_by_email'"></p>
		        <div class="form-group form-row">
			        <div class="col-sm-10 mb-1">
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
                        <input class="form-check-input" type="checkbox" v-model="user.checked"> {{user.name}}
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
		contacts : {},
		foundUsers : {},
		crit : { userQuery : "" }
    }),
	
	components: { CheckBox, FormGroup },

	mixins : [ status, rl ],

	methods : {
		reload() {
			const { $data } = this, me = this;
			
			me.doBusy(server.get(jsRoutes.controllers.Users.loadContacts().url).
			then(function(result) {	
				var f = [];
				for (let contact of result.data) {
					if (me.isntMember(contact)) f.push(contact);
				}
				$data.contacts = me.process(f);
			}));
		},
	
	
		isntMember(user) {
			const { $data } = this, me = this;
			if (!me.setup) return true;
			let activeCircle = me.setup.consent;
			let memberIds = _.map(activeCircle.authorized, function(member) { return member; });
			return !_.includes(memberIds, user._id);
		},
	
	
		searchUsers() {
			const { $data } = this, me = this;
			
			me.doAction("search", server.get(jsRoutes.controllers.Users.search($data.crit.userQuery).url).
			then(function(users) {				
				$data.foundUsers = me.process(users.data);
				
			}));		
		
		},
	
	
		addUsers() {
			const { $data, $emit } = this, me = this;
			var circle = me.setup.consent;
			// get the users that should be added to the circle
			var contactsToAdd = _.filter($data.contacts.all, function(contact) { return contact.checked; });
			var foundUsersToAdd = _.filter($data.foundUsers.all, function(user) { return user.checked; });
			var usersToAdd = _.union(contactsToAdd, foundUsersToAdd);
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