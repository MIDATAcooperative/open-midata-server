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
    
    <panel :busy="isBusy" :title="$t('provider_editgroups.title')">	
                       
        <pagination v-model="members"></pagination>
        <table class="table table-striped table-hover" v-if="members.filtered.length">
		   <thead>
            <tr>
                <Sorter v-t="'provider_editgroups.entityName'" sortby="entityName" v-model="members"></Sorter>
                <Sorter v-t="'provider_editgroups.entityType'" sortby="entityType" v-model="members"></Sorter>                                
                <Sorter v-t="'provider_editgroups.roleName'" sortby="role.roleName" v-model="members"></Sorter>
                <th></th>
                <th></th>
                <th></th>   
            </tr>
		   </thead>
		   <tbody>
            <tr class="clickable" @click="select(member)" v-for="member in members.filtered" :key="member._id">
                <td>{{ member.entityName }}</td>
                <td>{{ $t('enum.entitytype.'+member.entityType) }}</td>
                <td>{{ $t('provider_editgroups.role.'+member.role.roleName) }}</td>
                <td>{{ matrix(member.role) }}</td>
                <td width="20">
                  <a v-if="member.entityType=='SERVICES'" href="javascript:" @click="manageKeys(member)"><i class="fas fa-key"></i></a>
                </td>
                <td>
                    <button type="button" v-if="member.member != user._id" @click="removeGroup(member)" :disabled="action!=null" class="close" aria-label="Delete">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </td>
            </tr>
		   </tbody>				
        </table>
        
        <p v-if="members.filtered.length==0" v-t="'provider_editgroups.empty'"></p>
        
         <form class="css-form form-horizontal" role="form">
            
            <form-group name="selected" v-if="add.entityName" label="provider_editusergroup.selected">
               <p class="form-control-static">{{ add.entityName }}</p>
            </form-group>
            <form-group name="rights" label="provider_editusergroup.rights">
                <check-box v-for="req in rights" :name="req+'_1'" :key="req" v-model="add.role[req]" :disabled="!mayChangeTeam()">
                    <span>{{ $t('provider_editusergroup.right.'+req) }}</span>
                </check-box>		 
            </form-group>                         
        </form>			
        <error-box :error="error"></error-box>
        <div v-if="usergroup && usergroup._id">
            <a @click="$router.back()" href="javascript:" class="btn btn-default me-1" v-t="'common.back_btn'"></a>
            <button v-if="add.entityName" :disabled="action != null || !mayChangeTeam()" type="button" class="btn btn-primary me-1" v-t="'provider_editusergroup.update_btn'" @click="updateMember();"></button>
            <button type="button" class="btn btn-default me-1" v-if="usergroup.status == 'ACTIVE'" @click="addOrganizations();" v-t="'provider_editgroups.add_organization_btn'"></button>
            <button type="button" class="btn btn-default me-1" v-if="usergroup.status == 'ACTIVE'" @click="addOrganizations2();" v-t="'provider_editgroups.add_organization2_btn'"></button>
            <button type="button" class="btn btn-default me-1" v-if="usergroup.status == 'ACTIVE'" @click="addBroker();" v-t="'provider_editgroups.add_broker_btn'"></button>
            <button type="button" class="btn btn-default me-1" v-if="usergroup.status == 'ACTIVE'" @click="addUserGroups();" v-t="'provider_editgroups.add_group_btn'"></button>            
        </div>                          
    </panel>  

    <div v-if="expired && expired.filtered.length">
    <panel :busy="isBusy" :title="$t('provider_editgroups.former_members')">
        <pagination v-model="expired"></pagination>
        <table class="table table-striped table-hover" v-if="expired.filtered.length">
			<thead>
            <tr>
               <Sorter v-t="'provider_editgroups.entityName'" sortby="entityName" v-model="expired"></Sorter>            
               <Sorter v-t="'provider_editgroups.entityType'" sortby="entityType" v-model="expired"></Sorter>
               <Sorter v-t="'provider_editusergroup.startDate'" sortby="startDate" v-model="expired"></Sorter>
               <Sorter v-t="'provider_editusergroup.endDate'" sortby="endDate" v-model="expired"></Sorter>
            </tr>
			</thead>
			<tbody>
            <tr v-for="member in expired.filtered" :key="member._id">
                <td>{{ member.entityName }}</td>
                <td>{{ $t('enum.entitytype.'+member.entityType) }}</td>                
                <td>{{ $filters.date(member.startDate) }}</td>
                <td>{{ $filters.date(member.endDate) }}</td> 
            </tr>	
			</tbody>			
        </table>

    </panel>
    </div>

    <modal id="providerSearch" :full-width="true" @close="setupProvidersearch=null" :open="setupProvidersearch!=null" :title="$t('usergroupsearch.title')">
	   <user-group-search :setup="setupProvidersearch" @add="addGroup"></user-group-search>
	</modal>
	
	<modal id="organizationSearch" :full-width="true" @close="setupOrganizationSearch=null" :open="setupOrganizationSearch!=null" :title="$t('organizationsearch.title')">
	   <organization-search :setup="setupOrganizationSearch" @add="addGroup"></organization-search>
	</modal>
	
	<modal id="databrokerSearch" :full-width="true" @close="setupBrokerSearch=null" :open="setupBrokerSearch!=null" :title="$t('databrokersearch.title')">
	   <data-broker-search :setup="setupBrokerSearch" @add="addGroup"></data-broker-search>
	</modal>
	
</div>
   
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import usergroups from "services/usergroups.js"
import studies from "services/studies.js"
import session from "services/session.js"
import users from "services/users.js"
import UserGroupSearch from "components/tiles/UserGroupSearch.vue"
import OrganizationSearch from "components/tiles/OrganizationSearch.vue"
import DataBrokerSearch from "components/tiles/DataBrokerSearch.vue"
import { rl, status, ErrorBox, Success, CheckBox, FormGroup, Modal } from 'basic-vue3-components'
import _ from "lodash";

export default {
    data: () => ({	       
        groupId : null, 
        usergroup : null,       
        user : null,
        members : null,
        expired : null,
        setupProvidersearch : null,
        setupOrganizationSearch : null,
        setupBrokerSearch : null,
	    form : {},        
        add : { user:null, role:{} },
        rights : [ "readData", "writeData", "participants", "changeTeam", "setup","applications" ]       
    }),

    components: {  ErrorBox, FormGroup, Success, CheckBox, Panel, Modal, UserGroupSearch, OrganizationSearch, DataBrokerSearch },

    mixins : [ status, rl ],

    methods : {
        init() {
		    const { $data } = this, me = this;

            $data.add = { user:null, role:{ roleName:"hc" } };
            
            if ($data.groupId) {
            
            me.doBusy(usergroups.search({ "_id" : $data.groupId }, ["name", "status", "searchable" ])
			.then(function(data) {				
				$data.usergroup = data.data[0];								                						
			}));
                            
            me.doBusy(usergroups.listUserGroupGroups($data.groupId)
            .then(function(data) {                                
                $data.members = me.process(data.data, { filter : { status : 'ACTIVE' }, sort : "entityName" });                
                $data.expired = me.process(data.data, { filter : { status : 'EXPIRED' }, sort : "entityName" });                
            }));

            } else {
                $data.usergroup = {};
                $data.members = me.process([], { filter : { status : 'ACTIVE' }, sort : "entityName" });     
                me.ready();
            }
                        
	    },
       

        create() {	
			const { $data, $router } = this, me = this;
		
		    me.doAction("create", usergroups.createUserGroup($data.usergroup).
		    then(function(data) {			
			    $router.push({ path : "./editusergroup", query : { groupId : data.data._id } });	
                $data.groupId = data.data._id;				 
                me.init();
		    }));				
	    },
	    
	    select(who) {
	       this.$data.add = who;
	       console.log(who);
	    },
	
	    edit() {	
            const { $data, $router } = this, me = this;	
		    me.doAction("change", usergroups.editUserGroup($data.usergroup));
	    },
			
	    removeGroup(person) {
            const { $data } = this, me = this;
            me.doAction("change", server.post(jsRoutes.controllers.UserGroups.deleteUserGroupMembership().url, { member : person.member, group : $data.groupId })
            .then(function() {				
                me.init();
            }));
            
	    },
	    
	    mayChangeTeam() {
	      return true;
	    },
	    
	     matrix(role) {
            var r = "";            
            r += role.readData ? "R" : "-";
            r += role.writeData ? "W" : "-";          
            r += role.participants ? "C" : "-";
            r += role.changeTeam ? "T" : "-";          
            r += role.applications ? "A" : "-";
            r += role.setup ? "S" : "-";	   
            return r;
	    },
	
        addUserGroups() {
            const { $data, $route, $router } = this, me = this;
		
		    $data.setupProvidersearch = {}; 
	    },
	    
	    addOrganizations() {
            const { $data, $route, $router } = this, me = this;
		 
            this.$router.push({ path : './addorganization', query : { parentId : $data.groupId} });      		   
	    },
	    
	    addOrganizations2() {
            const { $data, $route, $router } = this, me = this;		                     		
		    $data.setupOrganizationSearch = {}; 
	    },
	    
	    addBroker() {
	        const { $data, $route, $router } = this, me = this;		                     		
		    $data.setupBrokerSearch = {};
	    },
	    
	    manageKeys(ugm) {
	       this.$router.push({ path : "./servicekeys", query : { groupId : ugm.userGroup, serviceId : ugm.member } }); 
	    },

        addGroup(groups) {	
		    const { $data, $route, $router } = this, me = this;
		    console.log("TRIGGER");
		    console.log(groups);
		    $data.setupProvidersearch = null;
		    $data.setupBrokerSearch = null;

            if (!groups.length) groups = [ groups ];					
		    let groupsIds = [];
		    let isPeople = false;
		    let isService = false;
            for (let p of groups) {
               if (p.email) isPeople = true;
               if (p.filename || p.executorAccount) isService = true;
               groupsIds.push(p._id || p.id);
            }
		
		   if (isPeople) {
		      me.doAction("change", usergroups.addMembersToUserGroup($data.groupId, groupsIds).
              then(function() { me.init(); }));
		   } else if (isService) {
		      me.doAction("change", usergroups.addBrokerToUserGroup($data.groupId, groupsIds).
              then(function() { me.init(); }));
		   } else {
		      me.doAction("change", usergroups.addGroupsToUserGroup($data.groupId, groupsIds).
              then(function() { me.init(); }));
           }		
        },
               
        
        updateMember() {
            const { $data } = this, me = this;
            if ($data.add.entityType == "SERVICES") {
	            me.doAction("change", usergroups.addBrokerToUserGroup($data.groupId, [ $data.add.member ], $data.add.role).
	            then(function() { me.init(); }));
            } else {
	            me.doAction("change", usergroups.addGroupsToUserGroup($data.groupId, [ $data.add.member ], $data.add.role).
	            then(function() { me.init(); }));
            }
        }
				    	 		 
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.groupId = $route.query.groupId || $route.query.orgId;

        session.currentUser.then(function(userId) {			
			$data.user = session.user;		
            me.init();
		});        
    }
}
</script>