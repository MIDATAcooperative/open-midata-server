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
    <panel :titel="$t('admin_organizations.title')" :busy="isBusy">
		<error-box :error="error"></error-box>
		<form class="css-form form-horizontal">
		  
		    <form-group name="status" label="admin_members.status">
		        <select  class="form-control" id="status" @change="reload();" v-model="search.criteria.status">
		            <option value=""></option>
                    <option v-for="status in stati" :key="status" :value="status">{{ $t('enum.userstatus.'+status) }}</option>
                </select>
		    </form-group>
		    <form-group name="name" label="admin_organizations.name">
		        <div class="input-group">
		            <input type="text" class="form-control" id="name" v-model="search.criteria.name" v-validate>
		            <div class="input-group-append">
		                <button class="btn btn-primary" :disabled="action!=null" @click="reloadName()" v-t="'common.search_btn'"></button>
		            </div>
		        </div>
		    </form-group>
		    <form-group name="identifier" label="admin_organizations.identifier">
		        <div class="input-group">
		            <input type="text" class="form-control" id="identifier" v-model="search.criteria.identifier" v-validate>
		            <div class="input-group-append">
		                <button class="btn btn-primary" :disabled="action!=null" @click="reloadIdentifier()" v-t="'common.search_btn'"></button>
		            </div>
		        </div>
		    </form-group>			    
		</form>
		<div v-if="organizations && organizations.filtered">
		  
        <pagination v-model="organizations" search="name"></pagination>
					
		<table class="table table-striped" v-if="organizations.filtered.length">
              <thead>
				<tr>
					<Sorter sortby="name" v-model="organizations" v-t="'admin_organizations.name'"></Sorter>					
					<Sorter sortby="status" v-model="organizations" v-t="'admin_organizations.status'"></Sorter>
					<th></th>
				</tr>
			  </thead>
			<tbody>		
				<tr v-for="member in organizations.filtered" :key="member._id" >
					<td><router-link :to="{ path : './updateorganization', query :  { orgId : member._id } }">{{ member.name || 'none' }}</router-link></td>					
					<td><select @change="changeOrganization(member);" v-model="member.status" class="form-control">
                            <option v-for="status in stati" :key="status" :value="status">{{ $t('enum.userstatus.'+status) }}</option>
                        </select></td>
                    <td><router-link :to="{ path : './editusergroup', query :  { groupId : member._id } }" class="btn btn-sm btn-default">{{ $t('admin_organizations.members_btn') }}</router-link></td>                    
				</tr>
			</tbody>
		</table>

        <p v-if="organizations.filtered.length === 0" v-t="'admin_organizations.empty'"></p>
			
		</div>			
    </panel>  
</template>
<script>

import Panel from "components/Panel.vue"
import users from "services/users.js"
import administration from "services/administration.js"
import session from "services/session.js"
import server from "services/server.js"
import hc from "services/hc.js"
import { status, rl, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {
    data: () => ({	
       
	    stati : [ "NEW", "ACTIVE", "BLOCKED", "DELETED" ],
	 
        search : { criteria : { name : "", identifier : "", status:"" } },
        organizations : null,
        setup : { sort : "email", filter : { search : "" }, ignoreCase : true},
        dateLimit: null
        
    }),

    components: {  Panel, ErrorBox, FormGroup },

    mixins : [ status, rl ],

    methods : {
        reload(comeback) {		
            const { $data } = this, me = this;
              
            
            if (!$data.search.criteria.name) { delete $data.search.criteria.name; }
            if (!$data.search.criteria.identifier) { delete $data.search.criteria.identifier; }
                       
            $data.organizations = null;		
    		me.doBusy(server.post(jsRoutes.controllers.admin.Administration.searchOrganization().url, $data.search.criteria)
    		.then(function(data) {    		   
    			$data.organizations = me.process(data.data, { sort : "name", filter : { "name" : "" } });
    		}));
            
            $data.dateLimit = new Date();
            $data.dateLimit.setMonth($data.dateLimit.getMonth() - 1);
	    },
	    
	    reloadIdentifier() {
	      this.$data.search.criteria.name = "";
	      this.reload();
	    },
	    
	    reloadName() {
	      this.$data.search.criteria.identifier = "";
	      this.reload();
	    },
	
	    changeOrganization(org) {	
            const me = this;
		    me.doAction("change", administration.changeOrganizationStatus(org._id, org.status));
	    }
    },

    created() {
        const { $data } = this, me = this;
        session.load("OrganizationListCtrl", me, ["search", "setup"]);	    
	    me.reload(true);
    },

    unmounted() {
        session.save("OrganizationListCtrl", this, ["search", "setup"]);
    }
    
}
</script>