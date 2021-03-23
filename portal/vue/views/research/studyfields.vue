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
    <study-nav page="study.fields"></study-nav>
    <tab-panel :busy="isBusy">

        <error-box :error="error"></error-box>

        <h2 v-t="'studyfields.required_data'"></h2>
        <p class="alert alert-info" v-t="'studyfields.no_change_warning'"></p>
        <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="setRequiredInformation()" role="form">
                    
            <form-group name="identity" label="studyfields.member_identity" :path="errors.identity">
                <radio-box name="identity" :disabled="studyLocked()" value="DEMOGRAPHIC" v-model="information.identity">
                    <div class="margin-left">
                        <strong v-t="'studyfields.not_anonymous1'"></strong><span v-t="'studyfields.not_anonymous2'"></span>
                    </div>
                </radio-box>
                <radio-box :disabled="studyLocked()" name="identity" value="RESTRICTED" v-model="information.identity">
                    <div class="margin-left">
                        <strong v-t="'studyfields.pseudonymous1'"></strong><span v-t="'studyfields.pseudonymous2'"></span>
                    </div>
                </radio-box>
                <radio-box :disabled="studyLocked()" name="identity" value="NONE"  v-model="information.identity">
                    <div class="margin-left">
                        <strong v-t="'studyfields.pseudonymous1b'"></strong><span v-t="'studyfields.pseudonymous2b'"></span>
                    </div>
                </radio-box>
                <check-box :disabled="studyLocked()" name="anonymous" v-model="information.anonymous" :path="errors.anonymous">
                    <div class="margin-left">
                        <strong v-t="'studyfields.anonymous'"></strong><span v-t="'studyfields.anonymous2'"></span>
                    </div>
                </check-box>
                                    
            </form-group>
            <form-group name="assistance" label="studyfields.assistance" :path="errors.assistance">
                <radio-box :disabled="studyLocked()" name="assistance" value="NONE" v-model="information.assistance">
                    <div class="margin-left">
                        <strong v-t="'studyfields.no_assistance1'"></strong><span v-t="'studyfields.no_assistance2'"></span>
                    </div>
                </radio-box>
                <radio-box name="assistance" :disabled="studyLocked()" value="HCPROFESSIONAL" v-model="information.assistance">
                    <div class="margin-left">
                        <strong v-t="'studyfields.assistance1'"></strong><span v-t="'studyfields.assistance2'"></span>
                    </div>
                </radio-box>                
            </form-group>
            <form-group label="common.empty">
                <button type="submit" v-submit class="btn btn-primary" :disabled="studyLocked()" v-t="'common.change_btn'"></button>
                <success :finished="finished" action="change" msg="common.save_ok"></success>                
            </form-group>
        </form>
        
        <h2 v-t="'studyfields.groups'"></h2>
        <p v-if="!study.groups.length" v-t="'studyfields.groups_empty'"></p>
        <table v-else class="table table-striped">
            <tr>
                <th v-t="'studyfields.group_name'"></th>
                <th v-t="'studyfields.group_description'"></th>
                <th>&nbsp;</th>
            </tr>
            <tr v-for="(group,idx) in study.groups" :key="idx">
                <td><input class="form-control" type="text"  :disabled="studyLocked()" v-validate v-model="group.name"></td>
                <td><input class="form-control" type="text"  :disabled="studyLocked()" v-validate v-model="group.description"></td>
                <td><button class="btn btn-danger btn-sm" @click="deleteGroup(group);" v-t="'common.delete_btn'" :disabled="studyLocked()">Delete</button></td>
            </tr>
        </table>
        <button class="btn btn-default space" @click="addGroup();" v-t="'studyfields.add_group_btn'" :disabled="studyLocked()"></button>
        <button class="btn btn-primary space" @click="saveGroups()" :disabled="action != null || studyLocked()" v-t="'studyfields.save_changes_btn'"></button>
        <success :finished="finished" action="groups" msg="common.save_ok"></success>
                                
    </tab-panel>
   
</div>
   
</template>
<script>


import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import server from "services/server.js"
import { status, ErrorBox, Success, CheckBox, RadioBox, FormGroup } from 'basic-vue3-components'
import _ from "lodash";

export default {
    data: () => ({	
        studyid : null,
        information : {},
        study : null
    }),

    components: {  Panel, TabPanel, ErrorBox, FormGroup, StudyNav, Success, CheckBox, RadioBox },

    mixins : [ status ],

    methods : {
        reload() {
            const { $data } = this, me = this;
	        me.doBusy(server.get(jsRoutes.controllers.research.Studies.getRequiredInformationSetup($data.studyid).url)
		    .then(function(data) { 								
			    $data.information = data.data;							
		    }));
	   
	        me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
	        .then(function(data) { 				
                let study = data.data;
                if (!study.groups) { study.groups = []; }
			    $data.study = study;
			    $data.study.recordQuery = undefined;
		    }));
        },
   
        setRequiredInformation() {
            const { $data } = this, me = this;
	        var params = JSON.parse(JSON.stringify($data.information));
	   		   
	        me.doAction("change", server.post(jsRoutes.controllers.research.Studies.setRequiredInformationSetup($data.studyid).url, params).
		    then(function(data) { 				
		        me.reload();		        
		    }));  
        },
   
        requiredDataChange() {
            const { $data } = this;
	        if ($data.information.identity == "DEMOGRAPHIC") {
		        $data.information.anonymous = false;		  
	        }	   
        },
   
        addGroup() {
            const { $data } = this;
            $data.study.groups.push({ name:"", description:"" });
        },
   
        deleteGroup(group) {	  
            const { $data } = this; 
	        $data.study.groups.splice($data.study.groups.indexOf(group), 1);
        },

        saveGroups() {	 
            const { $data } = this, me = this;
	        me.doAction("groups", server.put(jsRoutes.controllers.research.Studies.update($data.studyid).url, { "groups" : $data.study.groups }))	 
        },
   
        studyLocked() {
            const { $data } = this;
		    return (!$data.study) || ($data.study.validationStatus !== "DRAFT" && $data.study.validationStatus !== "REJECTED") || !$data.study.myRole.setup;
        }
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.studyid = $route.query.studyId;
        me.reload();       
    }
}
</script>