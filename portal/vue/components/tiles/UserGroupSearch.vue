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
    
	<div class="panel panel-primary">
		<div class="panel-heading" v-t="'dashboard.usergroupsearch'"></div>
		<div class="body">

			<error-box :error="error"></error-box>
			<form name="myform" ref="myform" class="css-form form-horizontal" @submit.prevent="search()">
				<form-group name="name" label="usergroupsearch.name" :path="errors.name">
				  <input type="text" class="form-control" :placeholder="$t('usergroupsearch.name')" v-model="criteria.name" v-validate> 
				</form-group>
				
				<form-group name="x" label="common.empty"> 
				   <button type="submit" v-submit :disabled="action!=null" class="btn btn-default" v-t="'common.search_btn'"></button>
				</form-group>
			</form>

			<div v-if="usergroups && usergroups.filtered">
				<pagination v-model="usergroups"></pagination>

				<div v-for="usergroup in usergroups.filtered" :key="usergroup._id" class="card margin-top">
					<div class="card-header">{{ usergroup.name }}</div>
					<div class="card-body">	  
					    <div class="table-responsive">       
						<table class="table">
							<tbody>
							<tr v-for="member in usergroup.members" :key="member._id">
								<td>{{ member.user.firstname }}</td>
								<td>{{ member.user.lastname }}</td>
								<td>{{ member.user.email }}</td>
							</tr>
							</tbody>
						</table>
						</div>
						<button class="btn btn-default col-5 me-1" @click="addIndividuals(usergroup)" v-t="'usergroupsearch.add_individuals_btn'"></button>
						<button class="btn btn-default col-5" @click="addGroup(usergroup)" v-t="'usergroupsearch.add_group_btn'"></button>
					</div>
				</div>
			
				<p v-if="usergroups.filtered.length == 0" v-t="'usergroupsearch.empty_result'"></p>
			</div>
        </div>		
	</div>
</template>
<script>

import Panel from "components/Panel.vue"
import { rl, status, CheckBox, FormGroup, ErrorBox } from 'basic-vue3-components'
import usergroups from "services/usergroups"

export default {

	props: [ "setup" ],
	emits : [ "add" ],

    data : ()=>({      
        criteria : { name : "" },
        usergroups : {}
    }),
	
	components: { CheckBox, FormGroup, Panel, ErrorBox },

	mixins : [ status, rl ],

	methods : {
		dosearch(crit) {
            const { $data } = this, me = this;
    	    me.doAction("search", usergroups.search(crit, ["name"])
    	    .then(function(data) {    
				let work = [];
    		    for (let usergroup of data.data) {    			
					usergroup.members = [];
    			    work.push(usergroups.listUserGroupMembers(usergroup._id)
    			    .then(function(result) {
    				    usergroup.members = result.data;
    			    }));
                }
				return Promise.all(work).then(function() {
					$data.usergroups = me.process(data.data);
				});				
    		}));
    	},

        search() {
            const { $data } = this, me = this;
    	    var crit = {};
    	    if ($data.criteria.name !== "") crit.name = $data.criteria.name;    	
    	    this.dosearch(crit);	    	    	    	
        },
    
        addIndividuals(prov) {
            const { $data, $emit } = this, me = this;
    	    var toAdd = [];
    	    for (let member of prov.members) {
                toAdd.push(member.user);
            }
    	    $emit("add", toAdd);
        },
        
        addGroup(group) {
            this.$emit("add", group);
        }    
    
	},

	created() {
		this.ready();
	}
    
}

</script>