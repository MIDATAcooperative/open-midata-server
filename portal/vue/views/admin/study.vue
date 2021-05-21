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
    <panel :title="$t('admin_study.title')" :busy="isBusy">
    
        <error-box :error="error"></error-box>
        
	<table class="table">
	  <tr>
	    <td v-t="'admin_study.name'"></td>
	    <td>{{ study.name }}</td>
	  </tr>
	   <tr>
	    <td v-t="'admin_study.type'"></td>
	    <td>{{ $t('enum.studytype.' + study.type) }}</td>
	  </tr>
	  <tr>
        <td v-t="'admin_study.created_at'"></td>
        <td>{{ $filters.date(study.createdAt) }}</td>
      </tr>
       <tr>
        <td v-t="'admin_study.created_by'"></td>
        <td>{{ creator.firstname }} {{ creator.lastname }} ({{ creator.email }})</td>
      </tr>    
      <tr>
        <td v-t="'admin_study.description'"></td>
        <td>{{ study.description }}</td>
      </tr>      
      <tr>
	    <td v-t="'admin_study.code'"></td>
	    <td>{{ study.code }}</td>
	  </tr>
	  <tr>
	    <td v-t="'admin_study.startDate'"></td>
	    <td>{{ $filters.date(study.startDate) }}</td>
	  </tr>       
	  <tr>
	    <td v-t="'admin_study.endDate'"></td>
	    <td>{{ $filters.date(study.endDate)}}</td>
	  </tr>
	  <tr>
	    <td v-t="'admin_study.dataCreatedBefore'"></td>
	    <td>{{ $filters.date(study.dataCreatedBefore) }}</td>
	  </tr>
	  <tr>
	    <td v-t="'admin_study.joinmethods'"></td>
	    <td>
	      <ul>
		    <li v-for="method in study.joinMethods" :key="method">{{ $t('enum.joinmethod.'+method) }}</li>
		  </ul>
	    </td>
	  </tr>
	   <tr>
	    <td v-t="'admin_study.consent_observers'"></td>
	    <td>
	      <ul>
		    <li v-for="appName in study.consentObserverNames" :key="appName">{{ appName }}</li>
		  </ul>
	    </td>
	  </tr>
    </table>	
    </panel> 
		  
    <panel :title="$t('admin_study.infos')" :busy="isBusy">
		  <div v-for="(info,idx) in study.infos" :key="idx">
		    <h4>{{ $t('enum.infos.'+info.type) }}</h4>
		    <div v-for="(v,k) in info.value" :key="k">
		      <p><b>{{k}}:</b><br>{{v}}</p>
		    </div>
		  </div>
    </panel>

    <panel v-if="study && study.infosInternal && study.infosInternal.length" :title="$t('admin_study.infos_internal')" :busy="isBusy">

		<div v-for="(info,idx) in study.infosInternal" :key="idx">
		    <h4>{{ $t('enum.infos.'+info.type) }}"></h4>
		    <div v-for="(v,k) in info.value" :key="k">
		      <p><b>{{k}}:</b><br>{{v}}</p>
		    </div>
		</div>
    </panel>
	     
	<panel v-if="study && study.infosPart && study.infosPart.length" :title="$t('admin_study.infos_part')" :busy="isBusy">
		  <div v-for="(info,idx) in study.infosPart" :key="idx">
		    <h4>{{ $t('enum.infos.'+info.type) }}"></h4>
		    <div v-for="(v,k) in info.value" :key="k">
		      <p><b>{{k}}:</b><br>{{v}}</p>
		    </div>
		  </div>
	</panel>
    
    <panel :title="$t('admin_study.required_information')" :busy="isBusy">
		  <p><span v-t="'admin_study.required_information'"></span>: <b>{{ study.requiredInformation }}</b></p>
		  <p><span v-t="'admin_study.anonymous'"></span>: <b>{{ $t('common.yesno.'+study.anonymous) }}</b></p>
		  <p><span v-t="'admin_study.required_assistance'"></span>: <b>{{ study.assistance }}</b></p>
		  <p><span v-t="'admin_study.termsOfUse'"></span>: <b><router-link :to="{ path : './terms', query : { which:study.termsOfUse} }">{{ study.termsOfUse }}</router-link></b></p>
		  <p><span v-t="'admin_study.sharing_query'"></span>:</p>
		  <access-query :query="study.recordQuery" details="true"></access-query>
		  <pre>{{ JSON.stringify(study.recordQuery, null, 2) }}
		  </pre>
		  <p v-t="'admin_study.requirements'"></p>
		  <ul>
		    <li v-for="req in study.requirements" :key="req">{{ $t('enum.userfeature.'+req) }}</li>
		  </ul>
    </panel>

    <panel :title="$t('admin_study.groups')" :busy="isBusy">
		<table class="table table-striped">
		      <tr>
		        <th v-t="'admin_study.group_name'"></th>
		        <th v-t="'admin_study.group_description'"></th>		        
		      </tr>
		      <tr v-for="(group,idx) in study.groups" :key="idx">
		        <td>{{ group.name }}</td>
		        <td>{{ group.description }}</td>		        
		      </tr>
         </table>
    </panel>
	
    <panel :title="$t('admin_study.team')" :busy="isBusy">
        <pagination v-model="members"></pagination>
		<table class="table table-striped">
			    <tr>
			      <Sorter v-model="members" sortby="user.firstname" v-t="'common.user.firstname'"></Sorter>
			      <Sorter v-model="members" sortby="user.lastname" v-t="'common.user.lastname'"></Sorter>
			      <Sorter v-model="members" sortby="user.email" v-t="'common.user.email'"></Sorter>
			      <Sorter v-model="members" sortby="role" v-t="'admin_study.role'"></Sorter>
			      <th></th>
			     
			    </tr>
				<tr v-for="member in members.filtered" :key="member.user._id">
					<td>{{ member.user.firstname }}</td>
					<td>{{ member.user.lastname }}</td>
					<td>{{ member.user.email }}</td>
					<td>{{ member.role.roleName }}</td>
					<td>
					  {{ matrix(member.role) }}
					</td>					
				</tr>				
		</table>
    </panel>
	
    <panel :title="$t('admin_study.links')" :busy="isBusy">
	
	  <p v-t="'studyactions.empty'" v-if="!links.length"></p>
	    <table class="table table-striped table-hover" v-if="links.length">
	            <tr>
	              <th v-t="'studyactions.app'"></th>
	              <th v-t="'studyactions.type'"></th>
	              <th v-t="'studyactions.validation'"></th>
	              <th></th>
	            </tr>
	            <tr v-for="(link,idx) in links" :key="idx">
	              <td @click="select(link);">{{ link.app.filename }}</td>
	                <td>
	                <div v-for="type in link.type" :key="type">{{ $t('studyactions.types_short.'+type) }}</div>
	              </td>
	              <td>
	                <div v-if="link.validationResearch != 'VALIDATED'">
	                  <span class="fas fa-times text-danger"></span>
	                  <span v-t="'studyactions.status.not_validated_research'"></span>
	                </div>
	                <div v-if="link.validationDeveloper != 'VALIDATED'">
	                  <span class="fas fa-times text-danger"></span>
	                  <span v-t="'studyactions.status.not_validated_developer'"></span>
	                </div>
	                <div v-if="link.validationDeveloper == 'VALIDATED' && link.validationResearch == 'VALIDATED'">
	                  <span class="fas fa-check text-success"></span>
	                  <span v-t="'studyactions.status.validated'"></span>
	                </div>
	                <div v-if="link.usePeriod.indexOf(study.executionStatus)<0">
	                  <span class="fas fa-times text-danger"></span>
	                  <span v-t="'studyactions.status.study_wrong_status'"></span>
	                </div>
	                <div v-if="link.type.indexOf('REQUIRE_P')>=0 && study.participantSearchStatus != 'SEARCHING'">
	                  <span class="fas fa-times text-danger"></span>
	                  <span v-t="'error.closed.study'"></span>
	                </div>
	                <div v-if="(link.type.indexOf('REQUIRE_P')>=0 || link.type.indexOf('OFFER_P')>=0) && link.study.joinMethods.indexOf('APP') < 0">
	                  <span class="fas fa-times text-danger"></span>
	                  <span v-t="'studyactions.status.study_no_app_participation'"></span>
	                </div>
	                
	              </td>
	              <td>	               
	                <button type="button" class="btn btn-sm btn-default" @click="validate(link);" v-t="'studyactions.validate_btn'"></button>
	                <button type="button" class="btn btn-sm btn-danger" @click="remove(link);" v-t="'common.delete_btn'"></button>
	              </td>
	            </tr>
	          </table>
	</panel>
	
	<panel :title="$t('admin_study.history')" :busy="isBusy">
      <audit-log :entity="study._id"></audit-log>
    </panel>
          
    <router-link class="btn btn-default mr-1" :to="{ path : './astudies' }" v-t="'common.back_btn'"></router-link>
    <button v-if="study.validationStatus == 'VALIDATION'" class="btn btn-primary mr-1" @click="finishValidation()" v-t="'admin_study.end_validation_btn'"></button>    
    <button v-if="study.validationStatus == 'VALIDATION'" class="btn btn-default mr-1" @click="backToDraft()" v-t="'admin_study.back_to_draft_btn'"></button>
    <button v-if="study.validationStatus != 'DRAFT'" class="btn btn-default mr-1" @click="exportStudy()" v-t="'admin_study.export_btn'"></button>
    <button v-if="readyForDelete()" class="btn btn-danger mr-1" @click="doDelete()" v-t="'admin_study.delete_study_btn'"></button>
       
</template>
<script>

import Panel from "components/Panel.vue"
import AuditLog from "components/AuditLog.vue"
import AccessQuery from "components/tiles/AccessQuery.vue"
import server from "services/server.js"
import users from "services/users.js"
import usergroups from "services/usergroups.js"
import ENV from "config"

import { status, rl, ErrorBox } from 'basic-vue3-components'

export default {

    data: () => ({	
        studyid : null,
	    study : {},
        creator : null,
        members : null,
        links : null
    }),

    components: {  Panel, ErrorBox, AuditLog, AccessQuery },

    mixins : [ status, rl ],

    methods : {
        reload() {
			const { $data, $route } = this, me = this;
            me.doBusy(server.get(jsRoutes.controllers.research.Studies.getAdmin($data.studyid).url)
            .then(function(data) { 				
                $data.study = data.data.study;
                
                me.doBusy(users.getMembers({ _id : $data.study.createdBy, "role" : ["RESEARCH","DEVELOPER","ADMIN"] }, users.MINIMAL)
                .then(function(data2) {
                    $data.creator = data2.data[0];
                }));
                
                me.doBusy(usergroups.listUserGroupMembers($data.studyid)
                .then(function(data) {
                    let members = data.data;
                    for (let member of members) { member.role.unpseudo = !member.role.pseudo; }
                    $data.members = me.process(members, { filter : { status : 'ACTIVE'} });
                }));
                
                me.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("study", $data.studyid).url)
                .then(function(data) { 				
                    $data.links = data.data;												
                }));	
                
            }));
	    },
	
        finishValidation() {	
            const { $data, $router } = this, me = this;			
            me.doAction("validation", server.post(jsRoutes.controllers.research.Studies.endValidation($data.studyid).url).
            then(function(data) { 				
                $router.push({ path : './astudies' });
            }));
	    },
	
	    backToDraft() {
			const { $data, $router } = this, me = this;
            me.doAction("back", server.post(jsRoutes.controllers.research.Studies.backToDraft($data.studyid).url).
            then(function(data) { 				
                $router.push({ path : './astudies' });
            }));
	    },
	
        doDelete() {
            const { $data, $router } = this, me = this;
            me.doAction("delete", server.post(jsRoutes.controllers.admin.Administration.deleteStudy($data.studyid).url).
            then(function(data) { 				
                $router.push({ path : './astudies' });
            }));
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
	
	    readyForDelete() {
		    return true;
	    },
	
        remove(link) {
            const { $data, $route } = this, me = this;
            me.doAction("delete", server.delete(jsRoutes.controllers.Market.deleteStudyAppLink(link._id).url)
            .then(function() {
                me.reload();
            }));
	    },
	   
	    validate(link) {
            const { $data, $route } = this, me = this;
            me.doAction("validate", server.post(jsRoutes.controllers.Market.validateStudyAppLink(link._id).url)
            .then(function() {
                me.reload();
            }));
	    },
	
	    exportStudy() {
            const { $data, $route } = this, me = this;
            me.doAction("download", server.token())
            .then(function(response) {
                document.location.href = ENV.apiurl + jsRoutes.controllers.research.Studies.exportStudy($data.studyid).url + "?token=" + encodeURIComponent(response.data.token);
            });
	    }
    },
    
    created() {
        const { $data, $route } = this, me = this;
        $data.studyid = $route.query.studyId;
        me.reload();
    }
}
</script>