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
	    <p v-if="!orgs.length" v-t="'provider_organization.empty'"></p>
	    
	    <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="editorg()" role="form">
	        <div v-for="org in orgs" :key="org._id">
		        <form-group name="name" label="provider_organization.name" :path="errors.name"> 
			        <p class="form-control-plaintext">{{org.name}}</p>		    
	            </form-group>
	            <form-group name="description" label="provider_organization.description" :path="errors.description">
	                <p class="form-control-plaintext" >{{org.description}}</p>
	            </form-group>
	            <form-group name="x" label="common.empty">
	                <button type="button" class="btn btn-default mr-1" @click="editOrg(org)" v-t="'provider_organization.edit_btn'"></button>
                    <button type="button" class="btn btn-default" @click="editGroup(org)" v-t="'provider_organization.members'"></button>	                            
	            </form-group>
	            <hr>
            </div>
            <button type="button" class="btn btn-primary" @click="addOrg()" v-t="'common.create_btn'"></button>
        </form>	
    </panel>     
   <!-- <panel :title="$t('provider_organization.members')" :busy="isBusy">	 
        <pagination v-model="persons" search="search"></pagination>
	    <table class="table table-striped" v-if="persons.filtered.length">
	        <tr>
	            <Sorter v-t="'common.user.firstname'" sortby="firstname" v-model="persons"></Sorter>
	            <Sorter v-t="'common.user.lastname'" sortby="lastname" v-model="persons"></Sorter>
	            <Sorter v-t="'common.user.email'" sortby="email" v-model="persons"></Sorter>
	        </tr>
	        <tr v-for="person in persons.filtered" :key="person._id">
	            <td>{{ person.firstname }}</td>
	            <td>{{ person.lastname }}</td>
	            <td>{{ person.email }}</td>
	        </tr>
	    </table>
	  
	    <button class="btn btn-default" :disabled="!isMasterUser()" @click="add()" v-t="'provider_organization.addprovider'"></button>	  
    </panel> -->
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
        persons : null
    }),

    components: {  Panel, ErrorBox, FormGroup, Success },

    mixins : [ status, rl ],

    methods : {
        reload() {
            const { $data } = this, me = this;

/*
            me.doBusy(users.getMembers({  role : "PROVIDER", provider : session.org }, users.MINIMAL )
		    .then(function(data) {
                for (let user of data.data)	user.search = user.firstname+" "+user.lastname;
			    $data.persons = me.process(data.data, { filter : { search : "" }});
		    }));
*/
			me.doBusy(usergroups.search({ "member" : true, type : "ORGANIZATION", active : true, setup : true }, usergroups.ALLPUBLIC )
    	    .then(function(results) {
		        $data.orgs = [];
				for (let grp of results.data) {
					me.doBusy(server.get(jsRoutes.controllers.providers.Providers.getOrganization(grp._id).url)
		    		.then(function(data) { 	               
		        		$data.orgs.push(data.data);												
		    		}));	   
				}
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