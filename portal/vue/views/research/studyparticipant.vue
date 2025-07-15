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
    <panel :title="participation.ownerName" :busy="isBusy">
        <error-box :error="error"></error-box>
	    <div>
	        <span v-t="'studyparticipant.participant'"></span> {{ participation.ownerName }}<br>
	        <div v-if="participation.partName">
	            <span v-t="'studyparticipants.partName'"></span>: {{ participation.partName}}<br>
	        </div>	
	    </div>
	    <div v-if="member">
	        <address>	
	            <strong>{{ member.firstname }} {{ member.lastname }}</strong><br>
	            {{ member.address1 }}<br>
                {{ member.address2 }}<br>
	            {{ member.zip }} {{ member.city }}<br>
	            {{ member.country }}<br>	
	            <span v-if="member.phone"><span v-t="'common.user.phone'"></span>: {{ member.phone }}</span><br>
	            <span v-if="member.mobile"><span v-t="'common.user.mobile'"></span>: {{ member.mobile }}</span><br>
            </address>		
        </div>
        <p><span v-t="'studyparticipant.status'"></span> <span>{{ $t('enum.participationstatus.'+participation.pstatus) }}</span></p>
        <p v-if="member"><b>{{ member.email }}</b></p>
        <p><span v-t="'studyparticipant.group'"></span>{{ participation.group }}</p>
        <p><span v-t="'studyparticipant.shared_records'"></span>: <b>{{ participation.records }}</b></p>        
        <div class="row">
            <div class="col-3" v-if="mayApproveParticipation(participation)">
                <select v-model="participation.group" class="form-control" @change="changeGroup(participation)">
                    <option v-for="group in study.groups" :key="group.name">{{ group.name }}</option>
                </select>
            </div>
            <div class="col-9">
                <button v-if="mayApproveParticipation(participation)" @click="approveParticipation(participation)" class="btn btn-success space" v-t="'studyparticipants.accept_btn'"></button>
                <button v-if="mayRejectParticipation(participation)" @click="rejectParticipation(participation)" class="btn btn-danger space" v-t="'studyparticipants.reject_btn'"></button>
                    
                <div class="btn-group">
                    <button type="button" class="btn btn-default dropdown-toggle space" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        Action <span class="caret"></span>
                    </button>
                    <div class="dropdown-menu">
                        <a class="dropdown-item" v-if="!me_menu.length" href="javascript:" @click="showApp('fhir-observation');" v-t="'dashboard.observations'"></a>							
                        <a class="dropdown-item" v-for="(entry,idx) in me_menu" :key="idx" href="javascript:" @click="showSpace(entry)" v-t="entry.name"></a>
                        <div role="separator" class="dropdown-divider"></div>
                        <a class="dropdown-item" href="javascript:" @click="addSpace()" v-t="'dashboard.install_btn'"></a>
                    </div>
                </div>

                <success :finished="finished" action="change" msg="common.save_ok"></success>
            </div>
        </div>
    </panel>
    <panel :title="$t('studyparticipant.history')" :busy="isBusy">
	    <auditlog :entity="participation._id"></auditlog>
    </panel>
    
    <router-link :to="{ path : './study.participants', query : { studyId : studyid }}" class="btn btn-default" v-t="'common.back_btn'"></router-link>

</template>
<script>

import Panel from "components/Panel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import Auditlog from "components/AuditLog.vue"
import server from "services/server.js"
import spaces from "services/spaces.js"
import session from "services/session.js"
import { status, ErrorBox, Success, FormGroup } from 'basic-vue3-components'
import _ from "lodash";

export default {
    data: () => ({	
        studyid : null,
        memberid : null,
        userid : null,
	    member : {},
	    participation : {}
    }),

    components: {  Panel, ErrorBox, FormGroup, StudyNav, Success, Auditlog },

    mixins : [ status ],

    methods : {
        reload() {
			const { $data } = this, me = this;
		    me.doBusy(server.get(jsRoutes.controllers.research.Studies.getParticipant($data.studyid, $data.memberid).url).
			then(function(data1) {
				var data = data1.data;
				$data.participation = data.participation;			
				$data.member = data.member;			
				
				me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
				.then(function(data) { 				
					$data.study = data.data;
					
					if ($data.study.myRole.readData) {
						me.doBusy(spaces.getSpacesOfUserContext($data.userid, $data.study.code)
				    	.then(function(results) {
				    		$data.me_menu = results.data;
				    	}));						
					}
				}));
																			
			}));
	    },
			
	    showSpace(space) {
            const { $data, $router } = this, me = this;
		    $router.push({ path : './spaces', query : { spaceId : space._id, user : $data.memberid, study : $data.study._id }});
	    },
	
	    showApp(app) {
            const { $data, $router, $route } = this, me = this;
		    spaces.openAppLink($router, $route, $data.userid, { app : app, user : $data.memberid, context : $data.study.code, study : $data.study._id });
	    },
	
	    addSpace() {
            const { $data, $router } = this, me = this;
		    $router.push({ path : './market', query : { next : document.location.href , context : $data.study.code, study : $data.study._id, user : $data.memberid }});
	    },
	
	    mayApproveParticipation(participation) {
            const { $data } = this, me = this;
		    return $data.study && $data.study.myRole.participants && participation.pstatus == "REQUEST";		
		},
		
		mayAddParticipants() {
            const { $data } = this, me = this;
			return $data.study && $data.study.myRole.participants && $data.study.participantSearchStatus == "SEARCHING";			
		},
		
	    mayRejectParticipation(participation) {
            const { $data } = this, me = this;
	        return $data.study && $data.study.myRole.participants && participation.pstatus == "REQUEST";
		},
		
		rejectParticipation(participation) {
			const { $data } = this, me = this;
			var params = { member : participation._id };
			
			me.doAction("reject", server.post(jsRoutes.controllers.research.Studies.rejectParticipation($data.studyid).url, params))
			.then(function(data) { 				
			    me.reload();
			});
		},
		
		approveParticipation(participation) {
            const { $data } = this, me = this;		
			var params = { member : participation._id };			
			me.doAction("approve", server.post(jsRoutes.controllers.research.Studies.approveParticipation($data.studyid).url, params))
			.then(function(data) { 				
			    me.reload();
			});
		},
		
		changeGroup(participation) {
            const { $data } = this, me = this;
			var params = { member : participation._id, group : participation.group };
			me.doAction("change", server.post(jsRoutes.controllers.research.Studies.updateParticipation($data.studyid).url, params))
			.then(function(data) { 				
			    //$scope.reload();
			});
		}
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.studyid = $route.query.studyId;
        $data.memberid = $route.query.participantId;
        session.currentUser.then(function(userid) { $data.userid=userid;me.reload(); });	
        
    }
}
</script>