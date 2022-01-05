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
    <study-nav page="study.team"></study-nav>
    <tab-panel :busy="isBusy">
	
        <error-box :error="error"></error-box>
        <p v-if="members.all.length < 2" v-t="'studyteam.defineself'" class="alert alert-info"></p>
        <pagination v-model="members"></pagination>
        <table class="table table-striped table-hover">
            <tr>
                <Sorter v-t="'common.user.firstname'" sortby="user.firstname" v-model="members"></Sorter>
                <Sorter sortby="user.lastname" v-model="members" v-t="'common.user.lastname'"></Sorter>
                <Sorter v-t="'common.user.email'" sortby="user.email" v-model="members"></Sorter>
                <Sorter colspan="3" v-t="'studyteam.rolehint'" sortby="role.roleName" v-model="members"></Sorter>			      
            </tr>
            <tr class="clickable" @click="select(member)" v-for="member in members.filtered" :key="member._id">
                <td>{{ member.user.firstname }}</td>
                <td>{{ member.user.lastname }}</td>
                <td>{{ member.user.email }}</td>
                <td>{{ member.role.roleName }}</td>
                <td>
                    {{ matrix(member.role) }}
                </td>
                <td>
                    <button type="button" v-if="member.member != user._id && study.myRole.changeTeam" @click="removePerson(member)" :disabled="action!=null" class="close" aria-label="Delete">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </td>
            </tr>				
        </table>
        
        <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="addPerson()" role="form">
            <form-group name="person" label="studyteam.person" :path="errors.person">
                <typeahead class="form-control" id="person" :disabled="lockChanges" name="person" v-model="add.personemail" field="email" :suggestions="persons" required />
            </form-group>
            <form-group name="_role" label="studyteam.role">
                <select name="_role" id="_role" class="form-control" :disabled="lockChanges" v-validate v-model="add.roleTemplate" @change="updateRole();" required>
                    <option v-for="role in roles" :key="role.id" :value="role.id">{{ $t('enum.researcherrole.'+role.id) }}</option>
                </select>			    			    
            </form-group>
            <div v-if="add.roleTemplate == 'OTHER'">
                <form-group name="roleName" label="studyteam.roleName" :path="errors.roleName">			    
                    <input type="text" class="form-control" id="roleName" :disabled="lockChanges" name="roleName" v-validate v-model="add.role.roleName">			    
                </form-group>
            </div>
            <form-group name="rights" label="studyteam.rights" :path="errors.rights">
                <check-box v-for="req in rights" :name="req" :key="req" v-model="add.role[req]" :disabled="add.roleTemplate != 'OTHER' || lockChanges">
                    <span>{{ $t('studyteam.right.'+req) }}</span>
                </check-box>		 
            </form-group>
            <button :disabled="action != null || !study.myRole.changeTeam" type="submit" v-submit class="btn btn-primary" v-t="'studyteam.addperson_btn'"></button>
            <success :finished="finished" action="change" msg="common.save_ok"></success>              
        </form>						
                                
    </tab-panel>    
</div>
   
</template>
<script>

import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import server from "services/server.js"
import usergroups from "services/usergroups.js"
import studies from "services/studies.js"
import session from "services/session.js"
import users from "services/users.js"
import { rl, status, ErrorBox, Success, CheckBox, FormGroup, Typeahead } from 'basic-vue3-components'
import _ from "lodash";

export default {
    data: () => ({	
        studyid : null,
        groupId : null,
        study : null,
        user : null,
        members : null,
	    form : {},
        roles : studies.roles,
        rights : ["setup", "readData", "writeData", "unpseudo", "export", "changeTeam", "participants", "auditLog" ],
        add : { role:{} }       
    }),

    components: {  TabPanel, Panel, ErrorBox, FormGroup, StudyNav, Success, CheckBox, Typeahead },

    mixins : [ status, rl ],

    methods : {
        init() {
		    const { $data } = this, me = this;
            $data.groupId = $data.studyid;
            
            me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
            .then(function(data) { 				
                    $data.study = data.data;	
                    $data.lockChanges = !$data.study.myRole.changeTeam;
            }));
                            
            me.doBusy(usergroups.listUserGroupMembers($data.studyid)
            .then(function(data) {                
                for (let member of data.data)  { member.role.unpseudo = !member.role.pseudo; }
                $data.members = me.process(data.data, { filter : { status : 'ACTIVE' }, sort : "user.lastname" });                
            }));
            
            me.doBusy(users.getMembers({ role : "RESEARCH", organization : session.org, status : ["ACTIVE", "NEW", "BLOCKED"] }, users.MINIMAL )
            .then(function(data) {
                $data.persons = data.data;
            }));                                				
	    },
			
	    removePerson(person) {
            const { $data } = this, me = this;
            me.doAction("change", server.post(jsRoutes.controllers.UserGroups.deleteUserGroupMembership().url, { member : person.member, group : $data.groupId })
            .then(function() {				
                $data.members.all.splice($data.members.all.indexOf(person), 1);
            }));
            
	    },
	
	    updateRole() {
            const { $data } = this;            
            var role = _.filter($data.roles, (x) => x.id == $data.add.roleTemplate)[0];
            $data.add.role.roleName = role.roleName;
            $data.add.role.id = role.id;
            for (var i in $data.rights) {
                $data.add.role[$data.rights[i]] = role[$data.rights[i]];
            }
	    },
			
	    addPerson() {	
            const { $data } = this, me = this;			       			
            me.doAction("change", users.getMembers({ email : $data.add.personemail, role : "RESEARCH" },["email", "role"]))
            .then(function(result) {
                if (result.data && result.data.length) {
                    $data.add.person = result.data[0];
                
                
                    me.doAction("change", usergroups.addMembersToUserGroup($data.groupId, [ $data.add.person._id ], $data.add.role).
                    then(function() {
                        $data.add = { role:{}, personemail : "", roleTemplate:null };
                        me.init();					
                    }));
                } else {
                    $data.error = { code : "error.unknown.user" };
                }
            });
	    },
	
	    matrix(role) {
            var r = "";
            r += role.setup ? "S" : "-";
            r += role.readData ? "R" : "-";
            r += role.writeData ? "W" : "-";
            r += role.unpseudo ? "U" : "-";
            r += role["export"] ? "E" : "-";
            r += role.changeTeam ? "T" : "-";
            r += role.participants ? "P" : "-";
            r += role.auditLog ? "L" : "-";	   
            return r;
	    },
	
	    select(member) {
            const { $data } = this;
            $data.add = { personemail : member.user.email, role : JSON.parse(JSON.stringify(member.role)), roleTemplate : "Other" };            
            for (let r of $data.roles) if (r.id==member.role.id) $data.add.roleTemplate = r.id;
	    }
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.studyid = $route.query.studyId;

        session.currentUser.then(function(userId) {			
			$data.user = session.user;		
            me.init();
		});        
    }
}
</script>