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
    <panel :title="$t('researcher_organization.title')" :busy="isBusy">	 
        <error-box :error="error"></error-box>
	    <p v-if="!isMasterUser()" class="alert alert-info" v-t="'researcher_organization.master_user'"></p>
	    <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="editorg()" role="form">
	        <form-group name="name" label="researcher_organization.name" :path="errors.name"> 
		        <input type="text" class="form-control" id="name" :readonly="!isMasterUser()" name="name" v-validate v-model="org.name" required>		    
            </form-group>
            <form-group name="description" label="researcher_organization.description" :path="errors.description">
                <textarea class="form-control" id="description" :readonly="!isMasterUser()" name="description" rows="5" v-validate v-model="org.description" required></textarea>
            </form-group>
            <form-group name="x" label="common.empty">
                <button type="submit" :disabled="!isMasterUser() || action!=null" class="btn btn-primary" v-t="'common.submit_btn'"></button>
                <success :finished="finished" action="update" msg="common.save_ok"></success>                
            </form-group>
        </form>	
    </panel>     
    <panel :title="$t('researcher_organization.researchers')" :busy="isBusy">	 
        <pagination v-model="persons" search="search"></pagination>
	    <table class="table table-striped" v-if="persons.filtered.length">
		  <thead>
	        <tr>
	            <Sorter v-t="'common.user.firstname'" sortby="firstname" v-model="persons"></Sorter>
	            <Sorter v-t="'common.user.lastname'" sortby="lastname" v-model="persons"></Sorter>
	            <Sorter v-t="'common.user.email'" sortby="email" v-model="persons"></Sorter>
	        </tr>
		  </thead>
		  <tbody>
	        <tr v-for="person in persons.filtered" :key="person._id">
	            <td>{{ person.firstname }}</td>
	            <td>{{ person.lastname }}</td>
	            <td>{{ person.email }}</td>
	        </tr>
		  </tbody>
	    </table>
	  
	    <button class="btn btn-default" :disabled="!isMasterUser()" @click="add()" v-t="'researcher_navbar.addresearcher'"></button>	  
    </panel>
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import session from "services/session.js"
import users from "services/users.js"
import { rl, status, ErrorBox, FormGroup, Success } from 'basic-vue3-components'

export default {
    data: () => ({	
        org : null,
        persons : null
    }),

    components: {  Panel, ErrorBox, FormGroup, Success },

    mixins : [ status, rl ],

    methods : {
        reload() {
            const { $data } = this, me = this;

            me.doBusy(users.getMembers({ role : "RESEARCH", organization : session.org }, users.MINIMAL )
		    .then(function(data) {
                for (let user of data.data)	user.search = user.firstname+" "+user.lastname;
			    $data.persons = me.process(data.data, { filter : { search : "" }});
		    }));

		    me.doBusy(server.get(jsRoutes.controllers.research.Researchers.getOrganization(session.org).url)
		    .then(function(data) { 	               
		        $data.org = data.data;												
		    }));
				    				
	    },
	
	    editorg() {									
            const { $data } = this, me = this;
	        me.doAction("update", server.post(jsRoutes.controllers.research.Researchers.updateOrganization(session.org).url, $data.org));			
	    },
	
	    isMasterUser() {
		    return session.hasSubRole('MASTER');
	    },

        add() {
            this.$router.push({ path : './addresearcher' });
        }
	
    },

    created() {
        const me = this;
        session.currentUser.then(function() { me.reload(); });	    
    }
    
}
</script>