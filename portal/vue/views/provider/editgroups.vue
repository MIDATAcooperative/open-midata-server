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
    
    <panel :busy="isBusy" :title="getTitle()">	
        <error-box :error="error"></error-box>

        <form name="myform" ref="myform" class="css-form form-horizontal" @submit.prevent="create()" novalidate role="form">
            <form-group id="name" label="provider_editusergroup.name" :path="errors.name">
                <p v-if="usergroup._id" class="form-control-plaintext">{{ usergroup.name }}</p>
                <input v-else id="name" name="name" type="text" class="form-control" v-validate v-model="usergroup.name" required>
            </form-group> 
            <form-group id="status" label="provider_editusergroup.searchable" v-if="usergroup._id">
                <div class="form-check">
                    <input class="form-check-input" type="checkbox" v-validate v-model="usergroup.searchable" @change="edit();">
                    <div class="margin-left">
                        <success :finished="finished" action="change" msg="common.save_ok"></success>                        
                    </div>
                </div>                     
            </form-group>    
            <form-group id="status" label="provider_editusergroup.status" v-if="usergroup._id">
                <p class="form-control-plaintext" v-t="'enum.userstatus.'+usergroup.status"></p>               
            </form-group> 

            <button type="submit" v-submit v-if="!usergroup._id" class="btn btn-primary" v-t="'provider_newusergroup.create_btn'"></button>
        </form>	         
        
        <pagination v-model="members"></pagination>
        <table class="table table-striped table-hover" v-if="members.filtered.length">
            <tr>
                <Sorter v-t="'editgroups.entityName'" sortby="entityName" v-model="members"></Sorter>
                                
                <th></th>   
            </tr>
            <tr class="clickable" @click="select(member)" v-for="member in members.filtered" :key="member._id">
                <td>{{ member.entityName }}</td>
                
                <td>
                    <button type="button" v-if="member.member != user._id" @click="removePerson(member)" :disabled="action!=null" class="close" aria-label="Delete">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </td>
            </tr>				
        </table>

        <div v-if="usergroup._id">
            <router-link :to="{ path : './usergroups' }" class="btn btn-default mr-1" v-t="'common.back_btn'"></router-link>
            <button type="button" class="btn btn-default" v-if="usergroup.status == 'ACTIVE'" @click="addPeople();" v-t="'editconsent.add_people_btn'"></button>
        </div>                          
    </panel>  

    <div v-if="expired && expired.filtered.length">
    <panel :busy="isBusy" :title="$t('provider_editusergroup.former_members')">
        <pagination v-model="expired"></pagination>
        <table class="table table-striped table-hover" v-if="expired.filtered.length">
            <tr>
                <Sorter v-t="'common.user.firstname'" sortby="user.firstname" v-model="expired"></Sorter>
                <Sorter sortby="user.lastname" v-model="expired" v-t="'common.user.lastname'"></Sorter>
                <Sorter v-t="'common.user.email'" sortby="user.email" v-model="expired"></Sorter>
                <Sorter v-t="'provider_editusergroup.startDate'" sortby="startDate" v-model="expired"></Sorter>
                <Sorter v-t="'provider_editusergroup.endDate'" sortby="endDate" v-model="expired"></Sorter>
            </tr>
            <tr v-for="member in expired.filtered" :key="member._id">
                <td>{{ member.user.firstname }}</td>
                <td>{{ member.user.lastname }}</td>
                <td>{{ member.user.email }}</td>
                
                <td>{{ $filters.date(member.startDate) }}</td>
                <td>{{ $filters.date(member.endDate) }}</td> 
            </tr>				
        </table>

    </panel>
    </div>

    <modal id="provSearch" full-width="true" @close="setupProvidersearch=null" :open="setupProvidersearch!=null" :title="$t('providersearch.title')">
	   <provider-search :setup="setupProvidersearch" @add="addPerson"></provider-search>
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
import ProviderSearch from "components/tiles/ProviderSearch.vue"
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
	    form : {},        
        add : { role:{} }       
    }),

    components: {  ErrorBox, FormGroup, Success, CheckBox, Panel, Modal, ProviderSearch },

    mixins : [ status, rl ],

    methods : {
        init() {
		    const { $data } = this, me = this;

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

        getTitle() {
            const { $data, $t } = this, me = this;
            if ($data.usergroup && $data.usergroup._id) return $t('provider_editusergroup.title');
            return $t('provider_newusergroup.title');
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
	
	    edit() {	
            const { $data, $router } = this, me = this;	
		    me.doAction("change", usergroups.editUserGroup($data.usergroup));
	    },
			
	    removePerson(person) {
            const { $data } = this, me = this;
            me.doAction("change", server.post(jsRoutes.controllers.UserGroups.deleteUserGroupMembership().url, { member : person.member, group : $data.groupId })
            .then(function() {				
                me.init();
            }));
            
	    },
	
        addPeople() {
            const { $data, $route, $router } = this, me = this;
		
		    $data.setupProvidersearch = {}; 
	    },

        addPerson(persons) {	
		    const { $data, $route, $router } = this, me = this;
				   
		    $data.setupProvidersearch = null;

            if (!persons.length) persons = [ persons ];					
		    let personIds = [];
            for (let p of persons) personIds.push(p._id);
		
		    me.doAction("change", usergroups.addMembersToUserGroup($data.groupId, personIds).
            then(function() { me.init(); }));		
        }
				    	 		 
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.groupId = $route.query.groupId;

        session.currentUser.then(function(userId) {			
			$data.user = session.user;		
            me.init();
		});        
    }
}
</script>