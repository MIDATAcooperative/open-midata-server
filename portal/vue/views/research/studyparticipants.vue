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
<div >
    <study-nav page="study.participants"></study-nav>
    <tab-panel :busy="isBusy">
	
            <form class="css-form form-horizontal">
		        <form-group name="search" label="studyparticipants.search_type">
		            <select class="form-control" id="search" @change="reload(searchName);" v-model="searchName">
                        <option v-for="search in searches" :key="search.name" :value="search.name">{{ $t(search.name) }}</option>
                    </select>
		        </form-group>	
            </form>

            <error-box :error="error"></error-box>
    	
            <p>{{ $t('studyparticipants.total', { count : total }) }}</p>
            <p v-t="'studyparticipants.only_first_shown'" v-if="total > 1000"></p>
            
            <pagination v-model="results"></pagination>
            
            <table class="table table-hover" v-if="results.filtered.length">
                <thead>
                    <tr>
                        <th v-t="'studyparticipants.name'"></th>
                        <th v-if="!study.myRole.pseudo" v-t="'studyparticipants.partName'"></th>
                        <th v-t="'studyparticipants.group'"></th>
                        <th v-t="'studyparticipants.status'"></th>
                        <th v-t="'studyparticipants.recruiter'"></th>
                        <th></th>	      	     
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="participant in results.filtered" :key="participant._id">
                        <td><router-link :to="{ path : './study.participant', query : { studyId : studyid, participantId : participant._id} }">{{ participant.ownerName }}</router-link></td>
                        <td v-if="!study.myRole.pseudo">{{ participant.partName }}</td>	  
                        <td><select v-model="participant.group" class="form-control" v-if="mayApproveParticipation(participant)" @change="changeGroup(participant)">
                            <option v-for="group in study.groups" :key="group.name" :value="group.name">{{ group.name }}</option>
                            </select><span v-else>{{ participant.group }}</span></td>
                        <td>{{ $t('enum.participationstatus.'+ participant.pstatus) }}</td>
                        <td>{{ participant.recruiterName }}</td>
                        <td>
                            <button v-if="mayApproveParticipation(participant)" :disabled="action!=null" @click="approveParticipation(participant)" class="btn btn-success btn-sm space" v-t="'studyparticipants.accept_btn'"></button>
                            <button v-if="mayRejectParticipation(participant)" :disabled="action!=null" @click="rejectParticipation(participant)" class="btn btn-danger btn-sm space" v-t="'studyparticipants.reject_btn'"></button>
                        </td>	      	        
                    </tr>
                </tbody>
            </table>

            <p v-if="results.filtered.length == 0 && mayViewParticipants()" v-t="'studyparticipants.empty'"></p>	
            <p v-if="!mayViewParticipants()" v-t="'studyparticipants.not_allowed'"></p>
            <router-link v-if="mayAddParticipants()" :to="{ path : './study.addparticipant', query : { studyId : studyid }}"  class="btn btn-default" v-t="'studyparticipants.add_participant_btn'"></router-link>
            
    </tab-panel>    

    <panel :title="$t('studyparticipants.acceptall')" :busy="isBusy">	
	    <form class="form form-horizontal" novalidate name="myform" ref="myform" @submit.prevent="acceptAll()">	 
	        <form-group name="autoJoinGroup" label="studyparticipants.group" :path="errors.autoJoinGroup">
	            <select v-validate v-model="acceptall.autoJoinGroup" class="form-control">
	                <option :value="null">&nbsp;</option>
                    <option v-for="group in study.groups" :key="group.name" :value="group.name">{{ group.name }}</option>
                </select>
	        </form-group>
	        <form-group name="autoJoin" label="studyparticipants.autojoin" :path="errors.autoJoin">
                <check-box v-model="acceptall.autoJoin" name="autoJoin">
	                <span v-t="'studyparticipants.autojoin2'"></span>
                </check-box>	    
	        </form-group>
	        <form-group label="common.empty">
	            <button type="submit" v-submit class="btn btn-primary" :disabled="action!=null || (acceptall.autoJoin && !acceptall.autoJoinGroup)" v-t="'common.submit_btn'"></button>
	            <success :finished="finished" action="autojoin" msg="common.save_ok"></success>
	        </form-group>
	    </form>
    </panel>
</div>
	                 
</template>
<script>

import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import server from "services/server.js"
import session from "services/session.js"
import { rl, status, ErrorBox, Success, CheckBox, FormGroup } from 'basic-vue3-components'
import _ from "lodash";

export default {
    data: () => ({	
        studyid : null,
        results : null,
        total : 0,
        acceptall : { autoJoin:"", autoJoinGroup:"" },
        searches : [ 
            { 
                name : "studyparticipants.all",
                criteria : {  }
            },
            {
                name : "studyparticipants.request",
                criteria : { pstatus : "REQUEST"  }
            },	 
            {
                name : "studyparticipants.rejected",
                criteria : { pstatus : ["MEMBER_REJECTED", "RESEARCH_REJECTED"] }
            },
            {
                name : "studyparticipants.retreated",
                criteria : { pstatus : "MEMBER_RETREATED" }
            }
  	    ],
        searchName : "studyparticipants.request",
        search : {}
    }),

    components: {  TabPanel, Panel, ErrorBox, FormGroup, StudyNav, Success, CheckBox },

    mixins : [ status, rl ],

    methods : {
        reload(searchName, comeback) {
            const { $data } = this, me = this;
            if (searchName) $data.search = _.filter($data.searches, (x) => x.name == searchName)[0];
          
            me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
            .then(function(data) { 				
                $data.study = data.data;
                if ($data.study.autoJoinGroup) {
                    $data.acceptall = { autoJoinGroup : $data.study.autoJoinGroup, autoJoin : true };
                }
            }));
            
            me.doBusy(server.post(jsRoutes.controllers.research.Studies.countParticipants($data.studyid).url, { properties : $data.search.criteria })
            .then(function(cdata) {
                $data.total = cdata.data.total;
                me.doBusy(server.post(jsRoutes.controllers.research.Studies.listParticipants($data.studyid).url, { properties : $data.search.criteria })
                .then(function(data) { 				
                    $data.results = me.process(data.data);		                
                }));
            
            }));
	    },
	
	
	    mayApproveParticipation(participation) {
            const { $data } = this;
	        return $data.study && $data.study.myRole.participants && participation.pstatus == "REQUEST";	
	    },
	
	    mayAddParticipants() {
            const { $data } = this;
		    return $data.study && $data.study.myRole.participants && $data.study.participantSearchStatus == "SEARCHING";		
	    },
	
	    mayViewParticipants() {
            const { $data } = this;
		    return $data.study && $data.study.myRole.participants && !$data.study.myRole.pseudo;		
	    },
	
        mayRejectParticipation(participation) {
            const { $data } = this;
            return $data.study && $data.study.myRole.participants && participation.pstatus == "REQUEST";
	    },
        rejectParticipation(participation) {
		    const { $data } = this, me = this;
            let params = { member : participation._id };		
            me.doAction("reject", server.post(jsRoutes.controllers.research.Studies.rejectParticipation($data.studyid).url, params)
            .then(function(data) { 				
                me.reload();
            }));
	    },
	
	    approveParticipation(participation) {
            const { $data } = this, me = this;	
		    let params = { member : participation._id };
		
		    me.doAction("approve", server.post(jsRoutes.controllers.research.Studies.approveParticipation($data.studyid).url, params)
            .then(function(data) { 				
                me.reload();
            }));
	    },
	
	    changeGroup(participation) {
            const { $data } = this, me = this;
		    let params = { member : participation._id, group : participation.group };
		    me.doAction("change", server.post(jsRoutes.controllers.research.Studies.updateParticipation($data.studyid).url, params)
		    .then(function(data) { 				
		    //$data.reload();
		    }));
	    },
	
	    acceptAll() {		
            const { $data } = this, me = this;
            me.doAction("autojoin", server.post(jsRoutes.controllers.research.Studies.updateNonSetup($data.studyid).url, $data.acceptall)
            .then(function(data) { 				
                me.reload();		   
            }));
	    }
	
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.studyid = $route.query.studyId;
        me.reload($data.searchName, true);         
    }
}
</script>