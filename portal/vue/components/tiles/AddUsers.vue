<template>
    <div class="ignore autosize">
        <div class="body" v-if="!isBusy">
			
		
	        <div v-if="contacts.all.length > 0">
	            <p v-t="'addusers.contacts'"></p>
				<pagination v-model="contacts"></pagination>
		        <div class="form-check" v-for="contact in contacts.filtered" :key="contact._id">
			        <label class="form-check-label"> 
                        <input class="form-check-input" type="checkbox" v-model="contact.checked"> 
                        <span class="margin-left">{{contact.firstname}} {{contact.lastname}} &lt;{{contact.email}}&gt;</span>
			        </label>
		        </div>
		        <p v-if="filteredContacts.length === 0" v-t="'addusers.no_contacts_to_add'"></p>
	        </div>
	        <hr>
	        <form class="form-horizontal" @submit.prevent="searchUsers()">
	            <p v-t="'addusers.search_by_email'"></p>
		        <div class="form-group form-row">
			        <div class="col-10">
				        <input type="text" class="form-control" :placeholder="$t('addusers.search_user')" v-model="crit.userQuery" required v-validate>
			        </div>
			        <div class="col-2">
				        <button type="submit" :disabled="status!=null" class="btn btn-primary form-control" v-t="'common.search_btn'"></button>
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

import CheckBox from "components/CheckBox.vue"
import FormGroup from "components/FormGroup.vue"
import server from "services/server"
import status from "mixins/status"
import rl from "mixins/resultlist"
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
			console.log("RELOAD!");
			me.doBusy(server.get(jsRoutes.controllers.Users.loadContacts().url).
			then(function(result) {	
				var f = [];
				for (let contact in result.data) {
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
			return !_.contains(memberIds, user._id);
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
			console.log(usersToAdd);		

			if (usersToAdd.length === 0) return;
			$emit("add", usersToAdd);
		/*
		} else {
		
		var data = {"users": userIds};
		server.post(jsRoutes.controllers.Circles.addUsers(circle._id).url, JSON.stringify(data)).
			then(function() {
				$scope.error = null;
				$scope.foundUsers = [];
				_.each($scope.contacts, function(contact) { contact.checked = false; });
				_.each(userIds, function(userId) { circle.authorized.push(userId); });
				//_.each(usersToAdd, function(user) { $scope.userNames[user._id] = user.name; });
			},function(err) { $scope.error = "Failed to add users: " + err; });
		}*/
		}
		
	},

	created() {
		this.reload();
	}
    
}
</script>