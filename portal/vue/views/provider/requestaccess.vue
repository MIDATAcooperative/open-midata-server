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
    <panel :title="$t('provider_requestaccess.title')" :busy="isBusy">	 
        <error-box :error="error"></error-box>
	    		    
	    <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="requestaccess()" role="form">	     
	        <form-group name="name" label="provider_organization.name" :path="errors.name"> 
		        <input type="text" class="form-control" id="name" :readonly="true" name="name" v-model="org.name">		    
            </form-group>
            <form-group name="reason" label="provider_requestaccess.reason" :path="errors.reason">
                <textarea class="form-control" id="reason" name="reason" rows="5" v-validate v-model="org.reason" required></textarea>
            </form-group>
            <form-group name="x" label="common.empty">
                <button type="submit" :disabled="action!=null" class="btn btn-primary" v-t="'common.submit_btn'"></button>
                <success :finished="finished" action="update" msg="common.save_ok"></success>                
            </form-group>
            <div class="alert alert-info" v-if="waitForConfirmation">
              <p v-t="'provider_requestaccess.wait'"></p>
              <button class="btn btn-default" @click="reload()" v-t="'provider_requestaccess.retry_btn'"></button>
            </div>
        </form>	
    </panel>  
</template>
<script>


import Panel from "components/Panel.vue"
import server from "services/server.js"
import session from "services/session.js"
import users from "services/users.js"
import usergroups from "services/usergroups.js"
import { status, ErrorBox, FormGroup, Success, Modal } from 'basic-vue3-components'

export default {
    data: () => ({	
        orgId : null,
        org : null,
        waitForConfirmation : false
    }),

    components: {  Panel, ErrorBox, FormGroup, Success },

    mixins : [ status ],
        
    methods : {
        reload() {
            const { $data, $route } = this, me = this;
				    
			me.doBusy(server.get(jsRoutes.controllers.UserGroups.getUserGroup($data.orgId).url)
    		.then(function(data) { 	               		        		
        		$data.org = data.data;	        		
        		if ($data.org.currentUserAccessUntil && $data.org.currentUserAccessUntil > new Date().getTime()) {
        		   me.$router.back();
        		}
    		}));
	    			      	    			  				    		
	    },
	
	    requestaccess() {									
            const { $data } = this, me = this;
                                            
       	    me.doAction("update", server.post(jsRoutes.controllers.UserGroups.requestConfirmation().url, { group : $data.orgId, reason : $data.org.reason }))
       	    .then((res) => {
       	        $data.waitForConfirmation = true;
       		    me.reload();
       		});       	       	
	    }	
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.orgId = $route.query.orgId;
        
        session.currentUser.then(function(user) { $data.currentUser = user; me.reload(); });	    
    }
    
}
</script>