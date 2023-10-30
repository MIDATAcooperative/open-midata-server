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
    <panel :title="$t('provider_organization.title')" :busy="isBusy">	 
        <error-box :error="error"></error-box>
	    <p v-if="!isMasterUser()" class="alert alert-info" v-t="'provider_organization.master_user'"></p>
	    <p v-if="allOrgs && !allOrgs.length" v-t="'provider_organization.empty'"></p>
	    
	    <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="editorg()" role="form">
	       <pagination v-model="orgs" search="name"></pagination>
	       
	       	<table class="table table-striped" v-if="orgs.filtered.length">
                <thead>
				<tr>
					<Sorter sortby="name" v-model="orgs" v-t="'provider_organization.name'"></Sorter>					
					<Sorter sortby="address" v-model="orgs" v-t="'provider_organization.address'"></Sorter>
					<th></th>
				</tr>
				</thead>
						
				<tbody>		
				<tr v-for="org in orgs.filtered" :key="org._id" >
					<td>{{ org.name }}</td>					
					<td>
					    <address>                                      
		    {{ org.address1 }}<br>
			{{ org.address2 }}<br>
			{{ org.zip }} {{ org.city }}<br>
			{{ org.country }}<br><br>			
			<span v-if="org.phone"><span v-t="'common.user.phone'"></span>: {{ org.phone }}</span>
		                </address>
					</td>
                    <td>
                    <button type="button" class="btn btn-default mr-1 btn-sm mb-1" @click="editOrg(org)" v-t="'provider_organization.edit_btn'"></button>
                    <button type="button" class="btn btn-default btn-sm mb-1" @click="editGroup(org)" v-t="'provider_organization.members'"></button>	                           
                    </td>                    
				</tr>
				</tbody>
		</table>
	       
	    
            <button type="button" class="btn btn-primary" @click="addOrg()" v-t="'common.create_btn'"></button>
        </form>	
    </panel>     
  
</template>
<script>


import Panel from "components/Panel.vue"
import server from "services/server.js"
import session from "services/session.js"
import users from "services/users.js"
import usergroups from "services/usergroups.js"
import { rl, status, ErrorBox, FormGroup, Success } from 'basic-vue3-components'

export default {
    data: () => ({	
        orgs : [],
        allOrgs : null,
        persons : null
    }),

    components: {  Panel, ErrorBox, FormGroup, Success },

    mixins : [ status, rl ],

    methods : {
        reload() {
            const { $data } = this, me = this;


			me.doBusy(usergroups.search({ "member" : true, type : "ORGANIZATION", active : true, setup : true }, usergroups.ALLPUBLIC )
    	    .then(function(results) {
		        let orgs = [];
		        let wait = [];
				for (let grp of results.data) {
					wait.push(me.doBusy(server.get(jsRoutes.controllers.providers.Providers.getOrganization(grp._id).url)
		    		.then(function(data) { 	 
		    		    if (data.data) {              
		        		   orgs.push(data.data);
		        		}												
		    		}, function(err) {})));	   
				}
				return Promise.all(wait).then(() => {
				   $data.allOrgs = orgs;
				   $data.orgs = me.process(orgs, { sort : "name", filter : { "name" : "" } });
				});
    	    }));		    
				    				
	    },
	
	    editOrg(org) {									
            const { $data } = this, me = this;
            this.$router.push({ path : './updateorganization', query : { orgId : org._id } });		
	    },
	    
	    editGroup(org) {									
            const { $data } = this, me = this;
            this.$router.push({ path : './editusergroup', query : { groupId : org._id } });		
	    },
	    
	    addOrg() {									
            const { $data } = this, me = this;
            this.$router.push({ path : './addorganization' });		
	    },
	
	    isMasterUser() {
		    return session.hasSubRole('MASTER');
	    },

        add() {
            this.$router.push({ path : './addprovider' });
        }
	
    },

    created() {
        const me = this;
        session.currentUser.then(function() { me.reload(); });	    
    }
    
}
</script>