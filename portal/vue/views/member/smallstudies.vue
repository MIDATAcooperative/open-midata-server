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
    <div  class="autosize body" v-if="!isBusy">    
        <div class="mb-2">
            <ul class="borderless nav nav-pills flex-column flex-sm-row">
                <li class="nav-item" role="presentation"><a :class="{'active' : tab==0 }" class="nav-link" href="javascript:" @click="setTab(0)" v-t="'smallstudies.recruiting'">Home</a></li>
                <li class="nav-item" role="presentation"><a :class="{'active' : tab==1 }" class="nav-link" href="javascript:" @click="setTab(1)" v-t="'smallstudies.ongoing'">Profile</a></li>
                <li class="nav-item" role="presentation"><a :class="{'active' : tab==2 }" class="nav-link" href="javascript:" @click="setTab(2)" v-t="'smallstudies.completed'">Messages</a></li>
                <li class="nav-item" role="presentation"><a :class="{'active' : tab==3 }" class="nav-link" href="javascript:" @click="setTab(3)" v-t="'smallstudies.stopped'">Messages</a></li>
            </ul>
        </div>
        <div class="tab-content">
	        <div class="tab-pane active">	
                <pagination v-model="results" search="studyName"></pagination>
                <div class="" v-if="results && results.filtered && results.filtered.length > 0">                    
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th v-t="'smallstudies.studies'"></th>
                                <th class="status-column" v-t="'smallstudies.status'"></th>	      	     
                            </tr>
                        </thead>
                        <tbody>
                            <tr class="clickable" @click="showDetails(study)" v-for="study in results.filtered" :key="study._id">
                                <td>	         
                                    <b>{{ study.studyName }}</b> - <span class="small text-muted">{{ $t('enum.studytype.'+study.type) }}</span>
                                    <p>{{ getSummary(study) }}</p>
                                </td>	  
                                <td class="status-column">
                                    <span class="icon fas fa-check-circle" v-show="study.pstatus == 'ACCEPTED'"></span>
                                    <span class="icon fas fa-question-circle" v-show="study.pstatus == 'REQUEST'"></span>
                                    <span class="icon fas fa-times-circle" v-show="study.pstatus == 'MEMBER_REJECTED' || study.pstatus == 'RESEARCH_REJECTED'"></span>
                                    <router-link :to="{ path : './studydetails' ,query : { studyId : study.study } }" class="btn btn-small btn-default" v-show="study.pstatus == 'MATCH'" v-t="'smallstudies.infoapply'">apply</router-link>&nbsp;
                                    <router-link :to="{ path : './studydetails' ,query : { studyId : study.study } }" class="btn btn-small btn-default" v-show="study.pstatus == 'INFO'" v-t="'smallstudies.infoonly'">info</router-link>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div class="section" v-if="results.filtered.length == 0">
                    <div class="extraspace"></div>
                    <span v-t="'flexiblestudies.empty'"></span>	  
                </div>	
	        </div>	
        </div>
    </div>
</template>
<script>

import Panel from 'components/Panel.vue';
import studies from 'services/studies.js';
import server from 'services/server.js';
import { rl, status, ErrorBox } from 'basic-vue3-components';
import { getLocale } from 'services/lang.js';


let studyById = {};
	
let tabs = [
    function(study) {
        return study.participantSearchStatus == "SEARCHING";
    },
    function(study) {
        return study.pstatus == "ACCEPTED" && study.executionStatus == "RUNNING";
    },
    function(study) {
        return study.pstatus == "ACCEPTED" && study.executionStatus == "FINISHED";
    },
    function(study) {
        return study.pstatus == "MEMBER_REJECTED" || study.pstatus == "RESEARCH_REJECTED" || study.executionStatus == "ABORTED" || study.pstatus == "MEMBER_RETREATED";
    }		
];

export default {
    data: () => ({       
        tab : 0,
        selection : null,
        results : null
	}),				

	components : { ErrorBox, Panel },

    mixins : [ status, rl ],

    methods : {
        reload() {		
			const { $data }	= this, me = this;
		    me.doBusy(server.get(jsRoutes.controllers.members.Studies.list().url).
		    then(function(results) { 				
		        $data.results = me.process(results.data, { filter : { selection : tabs[$data.tab], studyName : "" }});
		        var ids = [];
		        for (let study of results.data) {
		    	    studyById[study.study] = study;
                    ids.push(study.study);
                }
		  
		    
                me.doBusy(studies.search({ participantSearchStatus : "SEARCHING" }, ["name", "type", "infos", "description", "participantSearchStatus", "executionStatus", "createdAt", "joinMethods"]).
                then(function (result) {
                    for (let study of result.data) {
                        let part = studyById[study._id];
                        if (!part) {
                            part = study;
                            part.pstatus = study.joinMethods.indexOf('PORTAL') >= 0 ? "MATCH" : "INFO";
                            part.studyName = part.name;
                            part.study = study._id;
                            $data.results.all.push(part);
                        } else {
                            part.description = study.description;
                            part.infos = study.infos;
                            part.type = study.type;
                            part.participantSearchStatus = study.participantSearchStatus;
                            part.executionStatus = study.executionStatus;
                            part.createdAt = study.createdAt;
                        }
                    }
                }));
                
                me.doBusy(studies.search({ _id : ids  }, ["name", "description", "participantSearchStatus", "executionStatus", "type", "infos", "joinMethods"]).
                then(function (result) {
                    for (let study of result.data) {				
                        let part = studyById[study._id];
                        if (!part) {
                            part = study;
                            part.pstatus = "MATCH";
                            part.studyName = part.name;
                            part.study = study._id;
                            $data.results.all.push(part);
                        } else {
                            part.description = study.description;
                            part.participantSearchStatus = study.participantSearchStatus;
                            part.executionStatus = study.executionStatus;
                            
                        }
                    }			 
                }));
            }));
		},
	
	
	    setTab(tabnr) {
            const { $data } = this;
		    $data.tab = tabnr;
            $data.selection = tabs[tabnr];	
            if ($data.results && $data.results.filter) $data.results.filter.selection = tabs[tabnr];
	    },
	
	    showDetails(study) {
            const { $router } = this;
		    $router.push({ path : './studydetails', query : { studyId : study.study } });		
	    },
	
	    getSummary(study) {
		    if (study.infos) {
			    for (var i=0;i<study.infos.length;i++) {
				    if (study.infos[i].type === "SUMMARY") {
					    return study.infos[i].value[getLocale()] || study.infos[i].value.int || study.description;
				    }
			    }
		    }
		    return study.description;
	    }
    },

    created() {
        this.setTab(0);
        this.reload();	
    }
}
</script>