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
    <study-nav page="study.overview"></study-nav>
    <tab-panel :busy="isBusy || study==null">
	
        <error-box :error="error"></error-box>

        <div class="alert alert-warning" v-if="role!='research'">
	      <strong>{{ $t('createstudy.developer_nostart') }}</strong>
	      <div>{{ $t('createstudy.developer_nostart2') }}</div>
	    </div>	  
	    
        <div class="alert alert-info">
            <p><strong v-t="'studyoverview.workflow'"></strong></p>
            <div v-if="lastCheck"> 	  
                <p>
                    <span v-t="'studyoverview.lastdone'"></span>: <a href="javascript:" @click="go(lastCheck.page);" class="alert-link" v-t="lastCheck.title"></a>
                </p>
                <p><i>{{ $t(lastCheck.title + '_help') }}</i></p> 	                   
            </div>
            <div v-if="primaryCheck">
                <p><span v-t="'studyoverview.nextstep'"></span>: <a href="javascript:" @click="go(primaryCheck.page);" class="alert-link" v-t="primaryCheck.title"></a></p>
                <p><i>{{ $t(primaryCheck.title + '_help') }}</i></p>
                <a v-if="primaryCheck.page != '.'" href="javascript:" @click="go(primaryCheck.page);" class="btn btn-default mr-1" v-t="'studyoverview.visitnow_btn'" ></a>               
                <button v-if="primaryCheck.flag && !primaryCheck.done" @click="addProcessTag(primaryCheck.flag)" class="btn btn-default mr-1" v-t="'studyoverview.markdone_btn'"></button>
                <button @click="primaryCheck.action()" class="btn btn-default mr-1" v-if="primaryCheck.action && primaryCheck.check()" v-t="'studyoverview.donow_btn'"></button>
            </div>
    
        </div>

        <table class="table">
            <tr>
                <td v-t="'studyoverview.name'"></td>
                <td>{{ study.name }}
                    <a v-if="readyForValidation()" class="btn btn-default btn-sm" @click="go('description')" v-t="'studyoverview.edit_description_btn'"></a>
                </td>
            </tr>
            <tr>
                <td v-t="'studyoverview.type'"></td>
                <td>{{ $t('enum.studytype.'+study.type) }}</td>
            </tr>
            <tr>
                <td v-t="'studyoverview.created_at'"></td>
                <td>{{ $filters.date(study.createdAt) }}</td>
            </tr>
            <tr>
                <td v-t="'studyoverview.created_by'"></td>
                <td>{{ study.creatorName }}</td>
            </tr>
            <tr>
                <td v-t="'studyoverview.description'"></td>
                <td>{{ study.description }}
                    <a v-if="readyForValidation()" class="btn btn-default btn-sm" @click="go('description')" v-t="'studyoverview.edit_description_btn'"></a>
                </td>
            </tr>      
            <tr>
                <td v-t="'studyoverview.code'"></td>
                <td>{{ study.code }}</td>
            </tr>
            <tr>
                <td v-t="'studyoverview.validation_status'"></td>
                <td>{{ $t('enum.studyvalidationstatus.'+study.validationStatus) }}</td>
            </tr>
            <tr>
                <td v-t="'studyoverview.participant_search_status'"></td>
                <td>{{ $t('enum.participantsearchstatus.'+study.participantSearchStatus) }}</td>
            </tr>
            <tr>
                <td v-t="'studyoverview.execution_status'"></td>
                <td>{{ $t('enum.studyexecutionstatus.'+study.executionStatus) }}</td>
            </tr>
        </table>
    </tab-panel>
    
    <panel :title="$t('studyoverview.checklist')" :busy="isBusy">
        <table class="table table-hover">
            <tr class="clickable" v-for="item in checklist" :key="item.title" :class="{ 'table-success' : item.done, 'table-danger' : item.required && !item.done, 'table-warning' : !item.required && !item.done && !item.heading, 'table-heading' : item.heading }">
                <td @click="go(item.page)">
                    <span uib-tooltip="item.title+'_help'" v-t="item.title"></span>
                </td><td>
                    <div class="float-right">           
                        <span v-if="item.done" class="fas fa-check"></span>
                        <span v-if="!item.done && item.required" class="fas fa-exclamation-sign"></span>
                        <span v-if="!item.done && !item.required && !item.heading" v-t="'study_checklist.optional'"></span>
                    </div>  
                </td>          
            </tr>
        </table>
    </panel>

    <panel :title="$t('studyoverview.history')" :busy="isBusy || study==null">
        <auditlog :entity="study._id"></auditlog>  
    </panel>

    <div v-if="!isBusy && study!=null">
        <router-link class="btn btn-default space" :to="{ path : './studies' }" v-t="'common.back_btn'"></router-link>
        <button class="btn btn-default space" @click="clone(false)" v-t="'studyoverview.clone_study_btn'"></button>
        <button v-if="readyForValidation()" class="btn btn-primary space" @click="startValidation()" v-t="'studyoverview.start_validation_btn'"></button>
        <button v-if="readyForParticipantSearch()" class="btn btn-primary space" :disabled="role!='research'" @click="startParticipantSearch()" v-t="'studyoverview.start_participant_search_btn'"></button>
        <button v-if="readyForEndParticipantSearch()" class="btn btn-primary space" @click="endParticipantSearch()" v-t="'studyoverview.end_participant_search_btn'"></button>
        <button v-if="readyForStartExecution()" class="btn btn-primary space" @click="startExecution()" v-t="'studyoverview.start_study_execution_btn'"></button>
        <button v-if="readyForFinishExecution()" class="btn btn-primary space" @click="finishExecution()" v-t="'studyoverview.finish_study_btn'"></button>
        <button v-if="readyForDelete()" class="btn btn-danger space" @click="dodelete()" v-t="'studyoverview.delete_study_btn'"></button>
        <button v-if="readyForAbort()" class="btn btn-danger space" @click="abortExecution()" v-t="'studyoverview.abort_study_btn'"></button>   
    </div>
                                
    <modal id ="studyinf" :full-width="true" :open="confirm" @close="cancel()" :title="(study || {}).name">
        <div class="body">
            <p><span v-t="'studyoverview.name'"></span>: <b>{{ study.name }}</b></p>	          
            <p>{{ $t('studyoverview.confirm.'+confirm.id) }}</p>
            <p v-if="confirm.id!='clone'" v-t="'studyoverview.confirm.are_you_sure'"></p>
            <div class="extraspace"></div>
        </div>
        <template v-slot:footer>      
            <button v-if="confirm.id=='validation'" class="btn btn-primary space" @click="startValidation(true)" v-t="'studyoverview.start_validation_btn'"></button>      
            <button v-if="confirm.id=='end_participant_search'" class="btn btn-primary space" @click="endParticipantSearch(true)" v-t="'studyoverview.end_participant_search_btn'"></button>      
            <button v-if="confirm.id=='finish_execution'" class="btn btn-primary space" @click="finishExecution(true)" v-t="'studyoverview.finish_study_btn'"></button>
            <button v-if="confirm.id=='delete'" :disabled="action!=null" class="btn btn-danger space" @click="dodelete(true)" v-t="'studyoverview.delete_study_btn'"></button>
            <button v-if="confirm.id=='abort'" class="btn btn-danger space" @click="abortExecution(true)" v-t="'studyoverview.abort_study_btn'"></button>            
            <button v-if="confirm.id=='clone'" class="btn btn-default space" @click="clone(true)" v-t="'studyoverview.clone_study_btn'"></button>
            <button class="btn btn-default space" v-t="'common.cancel_btn'" @click="cancel()"></button>
        </template>
    </modal>
</div>
   
</template>
<script>

import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import Auditlog from "components/AuditLog.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import server from "services/server.js"
import usergroups from "services/usergroups.js"
import { status, ErrorBox, Modal } from 'basic-vue3-components'
import ENV from 'config';

export default {
    data: () => ({	
        studyid : null,
        study : null,
        confirm : null,
        tests : {},
        checklist: [],
        lastCheck : null,
        primaryCheck : null,
        role : null
    }),

    components: {  TabPanel, Panel, ErrorBox, Modal, Auditlog, StudyNav },

    mixins : [ status ],

    methods : {
        readyForValidation() {
            const { $data } = this;
		    return $data.study.myRole && $data.study.myRole.setup && ($data.study.validationStatus == "DRAFT" || $data.study.validationStatus == "REJECTED");
	    },
	
	    readyForParticipantSearch() {
            const { $data } = this;
            return $data.study.myRole && $data.study.myRole.setup && $data.study.validationStatus == "VALIDATED" &&
                $data.study.executionStatus == "PRE" &&
                (
                    $data.study.participantSearchStatus == "PRE" ||
                    $data.study.participantSearchStatus == "CLOSED"
                );
	    },
	
	    readyForEndParticipantSearch() {
            const { $data } = this;
		    return $data.study.myRole && $data.study.myRole.setup && $data.study.validationStatus == "VALIDATED" && $data.study.participantSearchStatus == "SEARCHING";
	    },
	
	    readyForDelete() {
            const { $data } = this;
		    return $data.study.myRole && $data.study.myRole.setup && ($data.study.executionStatus == "PRE" || $data.study.executionStatus == "ABORTED");
	    },
	
	    readyForAbort() {
            const { $data } = this;
		    return $data.study.myRole && $data.study.myRole.setup && ($data.study.validationStatus == "VALIDATED" && $data.study.executionStatus != "ABORTED" && $data.study.participantSearchStatus != "SEARCHING");
	    },
	
	    readyForStartExecution() {
            const { $data } = this;
            return $data.study.validationStatus == "VALIDATED" && 
                $data.study.participantSearchStatus != "PRE" &&
                $data.study.executionStatus == "PRE";
	    },
	
	    readyForFinishExecution() {
            const { $data } = this;
            return $data.study.myRole && $data.study.myRole.setup && $data.study.validationStatus == "VALIDATED" && 
                $data.study.participantSearchStatus == "CLOSED" &&
                $data.study.executionStatus == "RUNNING";
	    },
	
	    startValidation(conf) {
            const { $data } = this, me = this;
            if (!conf) {
                me.showConfirm("validation");
                return;
            }
            $data.confirm = null;
                        
            me.doAction("startValidation", server.post(jsRoutes.controllers.research.Studies.startValidation($data.studyid).url).
            then(function(data) { 				
                me.reload();
                //$scope.auditlog.reload();
            }));
	    },
	
	    startParticipantSearch() {
            const { $data } = this, me = this;
            if ($data.role != 'research') return;        
            me.doAction("startParticipantSearch", server.post(jsRoutes.controllers.research.Studies.startParticipantSearch($data.studyid).url).
            then(function(data) { 				
                me.reload();                
            }));
	    },
	
	    endParticipantSearch(conf) {
            const { $data } = this, me = this;
            if (!conf) {
                me.showConfirm("end_participant_search");
                return;
            }
            $data.confirm = null;
            
            me.doAction("endParticipantSearch", server.post(jsRoutes.controllers.research.Studies.endParticipantSearch($data.studyid).url).
            then(function(data) { 				
                me.reload();
                
            }));
	    },
	
	    startExecution(conf) {
            const { $data } = this, me = this;
                        
            me.doAction("startExecution", server.post(jsRoutes.controllers.research.Studies.startExecution($data.studyid).url).
            then(function(data) { 				
                me.reload();                
            }));
	    },
	
	    finishExecution(conf) {
            const { $data } = this, me = this;
            if (!conf) {
                me.showConfirm("finish_execution");
                return;
            }
            $data.confirm = null;
                        
            me.doAction("finishExecution", server.post(jsRoutes.controllers.research.Studies.finishExecution($data.studyid).url).
            then(function(data) { 				
                me.reload();                
            }));
	    },
	
	    abortExecution(conf) {
            const { $data } = this, me = this;
            if (!conf) {
                me.showConfirm("abort");
                return;
            }
            $data.confirm = null;
                        
            me.doAction("abortExecution", server.post(jsRoutes.controllers.research.Studies.abortExecution($data.studyid).url).
            then(function(data) { 				
                me.reload();                
            }));
	    },
	
	    clone(conf) {
            const { $data, $router } = this, me = this;
            if (!conf) {
                me.showConfirm("clone");
                return;
            }
            $data.confirm = null;
                        
            //$timeout(1000).then(function() {
            me.doAction("clone", server.post(jsRoutes.controllers.research.Studies.cloneToNew($data.studyid).url).
            then(function(data) { 				
                $router.push({ path : "./description" , query : { studyId : data.data._id }});
            }));            
	    },
	
	    showConfirm(what) {
            const { $data } = this, me = this;
            $data.confirm = { id : what };            
	    },
	
	    addProcessTag(tag) {
            const { $data } = this, me = this;
            if (!$data.study.processFlags) $data.study.processFlags = [];
            if ($data.study.processFlags.indexOf(tag) < 0) {
                $data.study.processFlags.push(tag);
                
                var data = { processFlags : $data.study.processFlags };
                me.doAction("update", server.post(jsRoutes.controllers.research.Studies.updateNonSetup($data.studyid).url, data))
                .then(function(data) { 				
                    me.reload();
                }); 
            }
	    },
	
	    dodelete(conf) {
            const { $data, $router } = this, me = this;
            if (!conf) {
                me.showConfirm("delete");
                return;
            }
            $data.confirm = null;
            
            me.doAction("delete", server.post(jsRoutes.controllers.research.Studies.delete($data.studyid).url)
            .then(function(data) { 				
                $router.push({ path : './studies' });
            }));
            
	    },
	
	    reload() {
            const { $data } = this, me = this;
            me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url))
            .then(function(data) { 				
                    let study = data.data;

                    var data = {"properties": {"_id": [study.createdBy]}, "fields": ["firstname", "lastname"]};
		            me.doBusy(server.post(jsRoutes.controllers.Users.get().url, data).
			        then(function(users1) {
                        var users = users1.data;
                        _.each(users, function(user) {
                            if (study.createdBy === user._id) { study.creatorName = (user.firstname+" "+user.lastname).trim(); }					
                        });
			        }).then(function() {
                    
                        $data.tests = { team : false, applinked : false, allassigned : false };
                        
                        return me.doBusy(usergroups.listUserGroupMembers($data.studyid)
                        .then(function(data) {					
                            $data.tests.team = data.data.length > 1;
                        }));
                    }).then(function() {
                    
                        return me.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("study", $data.studyid).url)
                        .then(function(data) { 				
                            for (var i=0;i<data.data.length;i++) {
                                var sal = data.data[i];
                                if (sal.type.indexOf("OFFER_P")>=0 ||sal.type.indexOf("REQUIRE_P")>=0 ) $data.tests.applinked = true;
                            }												
                        }));	
                                                            
                    }).then(function() {
                    
                    me.doBusy(server.post(jsRoutes.controllers.research.Studies.countParticipants($data.studyid).url, { properties : { pstatus : "REQUEST" } })
                    .then(function(data) {
                        $data.tests.allassigned = data.data.total === 0;
                    }).then(function()  {
                        if (!study.processFlags) study.processFlags = [];
                
                        $data.checklist = [
                            { title : "study_checklist.phase1", page : ".", heading : true  },
                            { title : "study_checklist.name", page : "study.description", required : true, done : study.name && study.description },
                            { title : "study_checklist.teamsetup", page : "study.team", flag : "team", done : $data.tests.team || study.processFlags.indexOf("team")>=0 },
                            { title : "study_checklist.information", page : "study.info", required : false, done : (study.infos && study.infos.length) },
                            { title : "study_checklist.groups", page : "study.fields", required : true, done : study.groups.length },
                            { title : "study_checklist.sharingQuery", page : "study.rules", required : true, done : (study.recordQuery && ( JSON.stringify(study.recordQuery) !== "{}")  ) },
                            { title : "study_checklist.dates", page : "study.rules", required : true, done : study.startDate || study.endDate || study.dataCreatedBefore },
                            { title : "study_checklist.terms", page : "study.rules" , flag : "termsofuse", done : study.termsOfUse || study.processFlags.indexOf("termsofuse")>=0 },
                            { title : "study_checklist.validation", action : me.startValidation, check : me.readyForValidation, page : ".", required : true, done : study.validationStatus !== "DRAFT" },
                            { title : "study_checklist.validation_passed", page : ".", required : true, done : study.validationStatus == "VALIDATED" },
                            { title : "study_checklist.phase2", page : ".", heading : true },
                            { title : "study_checklist.applications", page : "study.actions", flag : "applications", done : study.processFlags.indexOf("applications") >= 0 },
                            { title : "study_checklist.applinked", page : ".", flag : "applinked", done : $data.tests.applinked || study.processFlags.indexOf("applinked") >= 0 },
                            { title : "study_checklist.partsearchstart", action : me.startParticipantSearch, check : me.readyForParticipantSearch, page : ".", required : true, done : study.participantSearchStatus != "PRE" },
                            { title : "study_checklist.phase3", page : ".", heading : true },
                            { title : "study_checklist.executionstart", action : me.startExecution, check : me.readyForStartExecution, page : ".", required : true, done : study.executionStatus != "PRE"  },
                            { title : "study_checklist.acceptedpart", page : "study.participants", required : true, done : study.participantSearchStatus != "PRE" && $data.tests.allassigned },
                            { title : "study_checklist.phase4", page : ".", heading : true },
                            { title : "study_checklist.partsearchend", action : me.endParticipantSearch, check : me.readyForEndParticipantSearch, page : ".", required : true, done : study.participantSearchStatus == "CLOSED" },
                            { title : "study_checklist.execend", action : me.finishExecution, check : me.readyForFinishExecution, page : ".", required : true, done : study.executionStatus == "FINISHED"  },
                            { title : "study_checklist.exportdata", page : "study.records", flag : "export", done : study.processFlags.indexOf("export") >= 0 }
                        ];
                    
                        for (var i = 0;i<$data.checklist.length;i++) {
                            if (! $data.checklist[i].done && ! $data.checklist[i].heading) {
                                $data.primaryCheck = $data.checklist[i];
                                break;
                            }
                            $data.lastCheck = $data.checklist[i];
                        }
                    
                        $data.study = study;
                    }));
                }));                                        
            });
	    },
	
	    go(what) {
            const { $data, $router } = this;
            if (what==".") return;
		    $router.push({ path : './'+what, query : { studyId : $data.studyid }});
	    },
	
	    cancel() {
            const { $data } = this;
		    $data.confirm = null;
	    },
	
	    exportStudy() {
            const { $data } = this, me = this;
            me.doAction("download", server.token())
            .then(function(response) {
            document.location.href = ENV.apiurl + jsRoutes.controllers.research.Studies.exportStudy($data.studyid).url + "?token=" + encodeURIComponent(response.data.token);
            });
	    }
    },

    created() {
        const { $data, $route } = this;
        this.$data.role = this.$route.meta.role;
        $data.studyid = $route.query.studyId;
        this.reload();
    }
}
</script>
