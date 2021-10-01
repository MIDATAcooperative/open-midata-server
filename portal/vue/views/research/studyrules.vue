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
    <study-nav page="study.rules"></study-nav>
    <tab-panel :busy="isBusy">
	        
        <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="submit()" role="form">		
            <h2 v-t="'studyrules.title'"></h2>
            <p class="alert alert-info" v-if="!error" v-t="'studyrules.no_change_warning'"></p>
            <error-box :error="error"></error-box>
            <form-group name="recordQuery" label="studyrules.sharing_query">
                <access-query :query="study.recordQuery" details="true"></access-query>	  
                <div class="margin-top">   
                    <router-link :to="{ path : './study.query', query : { studyId : studyid } }" class="btn btn-default" v-t="'studyrules.queryeditor_btn'"></router-link>
                </div>
            </form-group>

            <form-group name="startDate" label="studyrules.startDate" :path="errors.startDate">	    
                <input id="startDate" name="startDate" type="date" class="form-control" :disabled="studyLocked()" v-validate v-model="study.startDate"  />                 
            </form-group>
    
            <form-group name="endDate" label="studyrules.endDate" :path="errors.endDate">	    
                <input id="endDate" name="endDate" type="date" class="form-control" :disabled="studyLocked()" v-validate v-model="study.endDate" />                  
            </form-group>
    
            <form-group name="dataCreatedBefore" label="studyrules.dataCreatedBefore" :path="errors.dataCreatedBefore">	    
                <input id="dataCreatedBefore" name="dataCreatedBefore" type="date" class="form-control" :disabled="studyLocked()" v-validate v-model="study.dataCreatedBefore"/>        
            </form-group>
        
            <form-group name="termsOfUse" label="studyrules.terms_of_use" :path="errors.termsOfUse">	    
                <typeahead id="termsOfUse" name="termsOfUse" class="form-control" :disabled="studyLocked()" v-model="study.termsOfUse" field="id" display="fullname" :suggestions="terms" />		    
                <p class="form-text text-muted" v-if="study.termsOfUse"><router-link :to="{ path : './terms', query :  { which:study.termsOfUse }}" v-t="'studyrules.show_terms'"></router-link></p>
                <p class="form-text text-muted" v-t="'studyrules.terms_of_use_hint'"></p> 
            </form-group>
    
            <form-group name="consentObserver" label="studyrules.consent_observers" :path="errors.consentObserver">	    
                <typeahead id="consentObserver" name="consentObserver" class="form-control" :disabled="studyLocked()" v-model="study.consentObserverStr" field="filename" display="name" :suggestions="observers" />		    	    
            </form-group>
    
            <form-group name="joinMethods" label="studyrules.join_methods" :path="errors.joinMethods">
                <check-box v-for="method in joinmethods" :key="method" :name="method" :disabled="studyLocked()" :checked="study.joinMethods.indexOf(method)>=0" @click="toggle(study.joinMethods, method);">
                    <span class="margin-left" v-t="'enum.joinmethod.'+method"></span>
                </check-box>		 
            </form-group>
    
            <form-group name="requirements" label="studyrules.requirements" :path="errors.requirements">
                <check-box v-for="req in requirements" :key="req" :name="req" :disabled="studyLocked()" :checked="study.requirements.indexOf(req)>=0" @click="toggle(study.requirements, req);">
                    <span class="margin-left" v-t="'enum.userfeature.'+req"></span>
                </check-box>		 
            </form-group>

            <form-group name="leavePolicy" label="studyrules.leavePolicy" :path="errors.leavePolicy">
                <select id="leavePolicy" name="leavePolicy" class="form-control" :disabled="studyLocked()" v-validate v-model="study.leavePolicy" required>
                    <option v-for="policy in leavePolicies" :key="policy" :value="policy">{{ $t('enum.projectleavepolicy.'+policy) }}</option>
                </select>
            </form-group>

            <form-group name="rejoinPolicy" label="studyrules.rejoinPolicy" :path="errors.rejoinPolicy">
                <select id="rejoinPolicy" name="rejoinPolicy" class="form-control" :disabled="studyLocked()" v-validate v-model="study.rejoinPolicy" required>
                    <option v-for="policy in rejoinPolicies" :key="policy" :value="policy">{{ $t('enum.rejoinpolicy.'+policy) }}</option>
                </select>
            </form-group>

            <form-group label="common.empty">
                <button type="submit" :disabled="action !=null || studyLocked()" class="btn btn-primary" v-t="'common.change_btn'"></button>
                <success :finished="finished" action="change" msg="common.save_ok"></success>        
            </form-group>        
        </form>
    </tab-panel>    
</div>
               
</template>
<script>

import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import AccessQuery from "components/tiles/AccessQuery.vue"
import server from "services/server.js"
import studies from "services/studies.js"
import terms from "services/terms.js"
import apps from "services/apps.js"
import formats from "services/formats.js"
import { status, ErrorBox, Success, CheckBox, RadioBox, FormGroup, Typeahead } from 'basic-vue3-components'
import _ from "lodash";

export default {
    data: () => ({	
        studyid : null,        
        study : null,
        requirements : apps.userfeatures,
        query : {},
        codesystems : formats.codesystems,
        joinmethods : studies.joinmethods,
        leavePolicies : studies.leavePolicies,
        rejoinPolicies : studies.rejoinPolicies,
        terms : [],
        observers : null
    }),

    components: {  TabPanel, Panel, ErrorBox, FormGroup, StudyNav, Success, CheckBox, RadioBox, AccessQuery, Typeahead },

    mixins : [ status ],

    methods : {
        reload() {
            const { $data, $filters } = this, me = this;
            me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
            .then(function(data) { 				
                let study = data.data;	
                if (!study.requirements) study.requirements = [];
                if (!study.joinMethods) study.joinMethods = [];
                if (study.consentObserverNames && study.consentObserverNames.length) {
                    study.consentObserverStr = study.consentObserverNames.join(",");
                }
                if (study.startDate) study.startDate = $filters.usDate(study.startDate);
                if (study.endDate) study.endDate = $filters.usDate(study.endDate);
                if (study.dataCreatedBefore) study.dataCreatedBefore = $filters.usDate(study.dataCreatedBefore);
                study.recordQueryStr = JSON.stringify(study.recordQuery); 
                
                $data.study = study;	
            }));
            
            me.doBusy(apps.getApps({ type : "external", consentObserving : true }, ["_id", "filename", "name", "orgName", "publisher", "type", "targetUserRole"])
            .then(function(data) {
                $data.observers = data.data;                
            }));
        },
   
        submit() {
	    	const { $data } = this, me = this;
            let observersStr = $data.study.consentObserverStr;
            let observers = [];
            if (observersStr) {
                var plugins = observersStr.split(",");			
                $data.study.consentObserverNames = plugins;
            } else $data.study.consentObserverNames = [];
                        
            let data = { joinMethods : $data.study.joinMethods, termsOfUse : $data.study.termsOfUse, requirements: $data.study.requirements, startDate : $data.study.startDate, endDate : $data.study.endDate, dataCreatedBefore : $data.study.dataCreatedBefore, consentObserverNames : $data.study.consentObserverNames, leavePolicy : $data.study.leavePolicy, rejoinPolicy : $data.study.rejoinPolicy };
            me.doAction("change", server.put(jsRoutes.controllers.research.Studies.update($data.studyid).url, data)
            .then(function(data) { 				
                me.reload();            
            })); 
        },
   
        studyLocked() {
            const { $data } = this;
	        return (!$data.study) || ($data.study.validationStatus !== "DRAFT" && $data.study.validationStatus !== "REJECTED") || !$data.study.myRole.setup;    
        },
   
        toggle(array,itm) {		
		    var pos = array.indexOf(itm);
		    if (pos < 0) array.push(itm); else array.splice(pos, 1);
        }
           
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.studyid = $route.query.studyId;
        me.doBusy(terms.search({}, ["name", "version", "language", "title"])
	    .then(function(result) {
		    $data.terms = result.data;
	    }));
        me.reload();       
    }
}
</script>