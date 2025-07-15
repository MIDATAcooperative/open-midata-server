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
     <panel :title="$t('developer_testusers.title')" :busy="isBusy">		  
	
        <error-box :error="error"></error-box>
       
            <pagination v-model="members"></pagination>

			<table class="table table-striped" v-if="members.filtered.length">
               <thead>
				<tr>
				    <Sorter v-model="members" sortby="email" v-t="'common.user.email'"></Sorter>
					<Sorter v-model="members" sortby="firstname" v-t="'common.user.firstname'"></Sorter>
					<Sorter v-model="members" sortby="lastname" v-t="'common.user.lastname'"></Sorter>
					<Sorter v-model="members" sortby="role" v-t="'developer_testusers.role'"></Sorter>
					<th></th>					
				</tr>
               </thead>
			   <tbody>
								
				<tr v-for="member in members.filtered" :key="member._id">
				    <td>{{ member.email }}</td>					
					<td>{{ member.firstname }}</td>
					<td>{{ member.lastname }}</td>					
					<td>{{ $t('enum.userrole.'+member.role) }}</td>
					<td><button type="button" class="btn btn-sm btn-default" @click="resetPassword(member);" v-t="'developer_testusers.resetpw'"></button></td>
				</tr>
			   </tbody>
			</table>

            <p v-if="members.filtered.length == 0" v-t="'developer_testusers.empty'"></p>
			
			<div class="alert alert-info" v-t="'developer_testusers.description'"></div>
			
			<router-link class="btn btn-default me-1" :to="{ path : './registration' ,query : { developer : userId, role : 'member' } }" v-t="'developer_testusers.register_member_btn'"></router-link>
			<router-link class="btn btn-default me-1" :to="{ path : './registration' ,query :  { developer : userId, role : 'provider' } }" v-t="'developer_testusers.register_hp_btn'"></router-link>
			<router-link class="btn btn-default" :to="{ path : './registration', query : { developer : userId, role : 'research' } }" v-t="'developer_testusers.register_researcher_btn'"></router-link>
					 					
    </panel>
			
</div>

</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import users from "services/users.js"
import session from "services/session.js"
import { status, ErrorBox, Success, FormGroup, rl } from 'basic-vue3-components'

export default {
    data: () => ({	      
       members : null,
       userId : null
    }),

    components: {  Panel, ErrorBox, FormGroup, Success },

    mixins : [ status, rl ],

    methods : {
        reload(userId) {		
            const { $data } = this, me = this;
            me.doBusy(users.getMembers({ "developer" : userId }, [ "firstname", "lastname", "email", "role" ])
            .then(function(data) {
                $data.members = me.process(data.data);						
            }));
	    },
	
	    resetPassword(member) {
            const me = this;
            me.doBusy(server.post(jsRoutes.controllers.Developers.resetTestAccountPassword().url, { user : member._id }))
            .then(function(data) {
                document.location.href=data.data;
            });
	    }
    
    },

    created() {
        const { $data } = this, me = this;
        session.currentUser.then(function(userId) { $data.userId = userId; me.reload(userId); });
    }
}
</script>