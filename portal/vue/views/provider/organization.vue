<template>
    <panel :title="$t('provider_organization.title')" :busy="isBusy">	 
        <error-box :error="error"></error-box>
	    <p v-if="!isMasterUser()" class="alert alert-info" v-t="'provider_organization.master_user'"></p>
	    <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="editorg()" role="form">
	        <form-group name="name" label="provider_organization.name" :path="errors.name"> 
		        <input type="text" class="form-control" id="name" :readonly="!isMasterUser()" name="name" v-validate v-model="org.name" required>		    
            </form-group>
            <form-group name="description" label="provider_organization.description" :path="errors.description">
                <textarea class="form-control" id="description" :readonly="!isMasterUser()" name="description" rows="5" v-validate v-model="org.description" required></textarea>
            </form-group>
            <form-group name="x" label="common.empty">
                <button type="submit" :disabled="!isMasterUser() || action!=null" class="btn btn-primary" v-t="'common.submit_btn'"></button>
                <success :finished="finished" action="update" msg="common.save_ok"></success>                
            </form-group>
        </form>	
    </panel>     
    <panel :title="$t('provider_organization.members')" :busy="isBusy">	 
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
    </panel>
</template>
<script>

import ErrorBox from "components/ErrorBox.vue"
import Panel from "components/Panel.vue"
import FormGroup from "components/FormGroup.vue"
import Success from "components/Success.vue"
import server from "services/server.js"
import session from "services/session.js"
import users from "services/users.js"
import status from 'mixins/status.js'
import rl from 'mixins/resultlist.js'

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

            me.doBusy(users.getMembers({  role : "PROVIDER", provider : session.org }, users.MINIMAL )
		    .then(function(data) {
                for (let user of data.data)	user.search = user.firstname+" "+user.lastname;
			    $data.persons = me.process(data.data, { filter : { search : "" }});
		    }));

		    me.doBusy(server.get(jsRoutes.controllers.providers.Providers.getOrganization(session.org).url)
		    .then(function(data) { 	               
		        $data.org = data.data;												
		    }));
				    				
	    },
	
	    editorg() {									
            const { $data } = this, me = this;
	        me.doAction("update", server.post(server.post(jsRoutes.controllers.providers.Providers.updateOrganization(session.org).url, $data.org)));			
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