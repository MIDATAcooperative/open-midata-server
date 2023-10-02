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
        

        <form v-if="usergroup" name="myform" ref="myform" class="css-form form-horizontal" @submit.prevent="create()" novalidate role="form">
            <form-group id="name" label="provider_editusergroup.name" :path="errors.name">
                <p v-if="usergroup._id" class="form-control-plaintext">{{ usergroup.name }}</p>
                <input v-else id="name" name="name" type="text" class="form-control" v-validate v-model="usergroup.name" required>
            </form-group> 
            <form-group id="status" label="provider_editusergroup.searchable" v-if="usergroup._id && usergroup.type!='ORGANIZATION'">
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
                <Sorter v-t="'common.user.firstname'" sortby="user.firstname" v-model="members"></Sorter>
                <Sorter sortby="user.lastname" v-model="members" v-t="'common.user.lastname'"></Sorter>
                <Sorter v-t="'common.user.email'" sortby="user.email" v-model="members"></Sorter>
                <th></th>
                <th></th>   
            </tr>
            <tr class="clickable" @click="select(member)" v-for="member in members.filtered" :key="member._id">
                <td>{{ member.user.firstname }}</td>
                <td>{{ member.user.lastname }}</td>
                <td>{{ member.user.email }}</td>
                <td>
                    {{ matrix(member.role) }}
                </td>
                <td>
                    <button type="button" v-if="member.member != user._id" @click="removePerson(member)" :disabled="action!=null" class="close" aria-label="Delete">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </td>
            </tr>				
        </table>
        
         <form v-if="usergroup" class="css-form form-horizontal" role="form">
            
            <form-group name="selected" v-if="add.user" label="provider_editusergroup.selected">
               <p class="form-control-static">{{ (add.user || {}).email }}</p>
            </form-group>
            <form-group name="rights" label="provider_editusergroup.rights">
                <check-box v-for="req in rights" :name="req" :key="req" v-model="add.role[req]" :disabled="!mayChangeTeam()">
                    <span>{{ $t('provider_editusergroup.right.'+req) }}</span>
                </check-box>		 
            </form-group>                         
        </form>			
        <div v-else v-t="'provider_editusergroup.notexists'"></div>

        <error-box :error="error"></error-box>
        <div v-if="usergroup && usergroup._id">
            <a @click="$router.back()" href="javascript:" class="btn btn-default mr-1" v-t="'common.back_btn'"></a>
            <button v-if="add.user" :disabled="action != null || !mayChangeTeam()" type="button" class="btn btn-primary mr-1" v-t="'provider_editusergroup.update_btn'" @click="updateMember();"></button>
            <button type="button" class="btn btn-default mr-1" v-if="usergroup.status == 'ACTIVE'" @click="addPeople();" v-t="'editconsent.add_hp_btn'"></button>
            <button type="button" class="btn btn-default mr-1" v-if="usergroup.status == 'ACTIVE'" @click="addPeople2();" v-t="'editconsent.add_people_btn'"></button>
            <success :finished="finished" action="change" msg="common.save_ok"></success>
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
	
	<modal id="anyUserSearch" full-width="true" @close="setupAnyUserSearch=null" :open="setupAnyUserSearch!=null" :title="$t('addusers.title')">
	   <add-any-user :setup="setupAnyUserSearch" @add="addPerson"></add-any-user>
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
import AddAnyUser from "components/tiles/AddAnyUser.vue"
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
        setupAnyUserSearch : null,
	    form : {},        
        add : { user:null, role:{ unpseudo:true } },
        rights : [ "readData", "writeData", "changeTeam","setup","applications" ]       
    }),

    components: {  ErrorBox, FormGroup, Success, CheckBox, Panel, Modal, ProviderSearch, AddAnyUser },

    mixins : [ status, rl ],

    methods : {
        init() {
		    const { $data } = this, me = this;

            $data.add = { user:null, role:{ roleName:"hc", unpseudo:true, readData:true, writeData:true } };
            
            if ($data.groupId) {
            
            me.doBusy(usergroups.search({ "_id" : $data.groupId }, ["name", "status", "searchable", "type" ])
			.then(function(data) {		
			    if (data.data.length) $data.usergroup = data.data[0]; else $data.usergroup = null;								                						
			}));
                            
            me.doBusy(usergroups.listUserGroupMembers($data.groupId)
            .then(function(data) {                
                for (let member of data.data)  { member.role.unpseudo = true; }
                $data.members = me.process(data.data, { filter : { status : 'ACTIVE' }, sort : "user.lastname" });                
                $data.expired = me.process(data.data, { filter : { status : 'EXPIRED' }, sort : "user.lastname" });                
            }));

            } else {
                $data.usergroup = {};
                $data.members = me.process([], { filter : { status : 'ACTIVE' }, sort : "user.lastname" });     
                me.ready();
            }
                        
	    },

        getTitle() {
            const { $data, $t } = this, me = this;
            if ($data.groupId) return $t('provider_editusergroup.title');
            return $t('provider_newusergroup.title');
        },
        
        mayChangeTeam() {
          return true;
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
	    
	     matrix(role) {
            var r = "";            
            r += role.readData ? "R" : "-";
            r += role.writeData ? "W" : "-";          
            r += role.changeTeam ? "T" : "-";          
            r += role.applications ? "A" : "-";
            r += role.setup ? "S" : "-";	   
            return r;
	    },
	
        addPeople() {
            const { $data, $route, $router } = this, me = this;
		
		    $data.setupProvidersearch = {}; 
	    },
	    
	    addPeople2() {
            const { $data, $route, $router } = this, me = this;
		
		    $data.setupAnyUserSearch = {}; 
	    },

        addPerson(persons) {	
		    const { $data, $route, $router } = this, me = this;
				   
		    $data.setupProvidersearch = null;
		    $data.setupAnyUserSearch = null;

            if (!persons.length) persons = [ persons ];					
		    let personIds = [];
            for (let p of persons) personIds.push(p._id);
		
		    me.doAction("change", usergroups.addMembersToUserGroup($data.groupId, personIds, $data.add.role).
            then(function() { me.init(); }));		
        },
        
        updateMember() {
            const { $data } = this, me = this;
            me.doAction("change", usergroups.addMembersToUserGroup($data.groupId, [ $data.add.user._id ], $data.add.role).
            then(function() { me.init(); }));
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