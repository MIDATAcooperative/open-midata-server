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
    <study-nav page="study.subprojects" :study="study"></study-nav>
    <tab-panel :busy="isBusy">
        <error-box :error="error"></error-box>
                       
        <pagination v-model="members"></pagination>
        <table class="table table-striped table-hover" v-if="members.filtered.length">
            <tr>
                <Sorter v-t="'studies.code'" sortby="study.code" v-model="members"></Sorter>
                <Sorter v-t="'studies.name'" sortby="study.name" v-model="members"></Sorter>                                                
                <th></th>
                <th></th>              
            </tr>
            <tr class="clickable" @click="select(member)" v-for="member in members.filtered" :key="member._id">
                <td>{{ member.study.code }}</td>
                <td>{{ member.study.name }}</td>
                <td><span v-for="(m,s) in member.projectGroupMapping" :key="s" class="comma">{{ s+"="+m }}</span></td>                
                <td>
                    <button type="button" @click="removeProject(member.study)" :disabled="action!=null || !study.myRole.setup" class="close" aria-label="Delete">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </td>
            </tr>				
        </table>
        
        <p v-if="members.filtered.length==0" v-t="'studysubprojects.empty'"></p>
      
        <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="addProject()" role="form">
            <form-group name="project" label="studysubprojects.project" :path="errors.project">
                <typeahead class="form-control" @selection="studyselection()" v-model="add.code" :suggestions="studies" field="code" :display="codeName"/>
            </form-group>          
            <form-group name="projectName" label="studysubprojects.projectName" :path="errors.project">
                <p class="form-control-plaintext">{{ add.name }}</p>
            </form-group> 
            <form-group name="projectGroupMapping" label="studysubprojects.projectGroupMapping" :path="errors.projectGroupMapping">
              <div class="form-control-plaintext">
              <table>
                <thead>
                  <tr>
                    <th class="pr-3">{{ $t("studysubprojects.target") }}</th>
                    <th>{{ $t("studysubprojects.source") }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(grp,idx) in study.groups" :key="grp.name+idx">
                    <td>{{ grp.name }}</td>
                    <td>
                      <select class="form-control" v-model="grp.target">
                        <option v-for="group in add.groups" :key="group.name" :value="group.name">{{ group.name }}</option>
                      </select>               
                    </td>
                  </tr>
                </tbody>
              </table>
              </div>
            </form-group>
            <button :disabled="action != null || !study.myRole.setup || add.studyId==studyId || !add.studyId" type="submit" v-submit class="btn btn-primary" v-t="'studysubprojects.addproject_btn'"></button>
            <success :finished="finished" action="change" msg="common.save_ok"></success>              
        </form>                 
                                 
    </tab-panel>  
        			
</div>
   
</template>
<script>

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
    
        studyId : null,
        study : null,
           
        members : null,
             
	    add : { studyId : null, code : null, name : "", groups : [{ name : "*" }, { name : "-" }] }        
              
    }),

    components: {  ErrorBox, FormGroup, Success, CheckBox, TabPanel, StudyNav, Typeahead },

    mixins : [ status, rl ],

    methods : {
        init() {
		    const { $data } = this, me = this;

            me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyId).url)
            .then(function(data) { 				
                    for (let i=0;i<data.data.groups.length;i++) {
                      data.data.groups[i].target = "*";
                    }
            
                    $data.study = data.data;	                  
            }));

            me.doBusy(server.get(jsRoutes.controllers.research.Studies.list().url)
		    .then(function(data) {
			    $data.studies = data.data;
		    }));

            $data.add = { studyId : null, code : null, name : "", groups : [{ name : "*" }, { name : "-" }] };                       
                        			
			me.doBusy(server.post(jsRoutes.controllers.research.Studies.listSubprojects($data.studyId).url)
            .then(function(data) { 				
                 $data.members = me.process(data.data);		                
            }));
                                                               
	    },

        codeName(project) {
          return project.code+": "+project.name;
        },       
       			
	    removeProject(project) {
            const { $data } = this, me = this;
            me.doAction("change", server.post(jsRoutes.controllers.UserGroups.deleteUserGroupMembership().url, { member : $data.studyId, group : project._id })
            .then(function() {				
                me.init();
            }));
            
	    },
	    
	    select(row) {
	       let groups = this.$data.study.groups;
	       for (let grp of groups) {
	         grp.target = row.projectGroupMapping[grp.name];
	       }
	       this.$data.add = { studyId : row.study._id, code : row.study.code, name : row.study.name, groups : row.study.groups };
	       this.$data.add.groups.push({ name : "*" }, { name : "-" });
	    },
	    
	     studyselection() {
            const { $data } = this, me = this;
	        me.doSilent(studies.search({ code : $data.add.code }, ["_id", "code", "name", "groups" ])
		    .then(function(data) {
			    if (data.data && data.data.length == 1) {
			        $data.add.studyId = data.data[0]._id;
			        $data.add.name = data.data[0].name;
			        $data.add.groups = data.data[0].groups;		
			        
			        $data.add.groups.push({ name : "*" }, { name : "-" });
			        for (let i=0;i<$data.study.groups.length;i++) {
			          $data.study.groups[i].target = $data.add.groups.length > i ? $data.add.groups[i].name : "*";
			        }		  
			    }
		    }));
        },
        
        
	    	   	  	       	  	  
         addProject() {	
            const { $data } = this, me = this;			       			
            if ($data.add.studyId) {
                let projectGroupMapping = {};
                for (let i=0;i<$data.study.groups.length;i++) {
                  projectGroupMapping[$data.study.groups[i].name] = $data.study.groups[i].target; 
                }
                
	            me.doAction("change", usergroups.addProjectToUserGroup($data.add.studyId, [ $data.studyId ], projectGroupMapping).
	            then(function() {
	                $data.add = { studyId : null, code : null, name : "" };
	                me.init();					
	            }));
            }
         
	    }
                             				    	 		
    },

    created() {
        
        
        const { $data, $route } = this, me = this;
        $data.studyId = $route.query.studyId;
        
        me.init();
                            
    }
}
</script>
<style>
.comma:not(:last-child):after {
   content: ", ";
 }
</style>