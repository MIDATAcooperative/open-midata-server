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
    <study-nav page="study.actions"></study-nav>
    <tab-panel :busy="isBusy">
	
        <error-box :error="error"></error-box>

        <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="submit()" role="form">	
                
            <p class="lead" v-t="'studyactions.related_apps'"></p>

            <div v-if="!selection">
                <p v-t="'studyactions.empty'" v-if="!links.length"></p>
                <table class="table table-striped table-hover" v-if="links.length">
                    <tr>
                        <th v-t="'studyactions.app'"></th>
                        <th v-t="'studyactions.type'"></th>
                        <th v-t="'studyactions.validation'"></th>
                        <th></th>
                    </tr>
                    <tr v-for="link in links" :key="link._id">
                        <td @click="select(link);">{{ link.app.filename }}</td>
                        <td>
                            <div v-for="type in link.type" :key="type">{{ $t('studyactions.types_short.'+type) }}</div>
                        </td>
                        <td>
                            <div v-if="link.validationResearch != 'VALIDATED'">
                                <span class="fas fa-times text-danger mr-1"></span>
                                <span v-t="'studyactions.status.not_validated_research'"></span>
                            </div>
                            <div v-if="link.validationDeveloper != 'VALIDATED'">
                                <span class="fas fa-times text-danger mr-1"></span>
                                <span v-t="'studyactions.status.not_validated_developer'"></span>
                            </div>
                            <div v-if="link.validationDeveloper == 'VALIDATED' && link.validationResearch == 'VALIDATED'">
                                <span class="fas fa-check text-success mr-1"></span>
                                <span v-t="'studyactions.status.validated'"></span>
                            </div>
                            <div v-if="link.usePeriod.indexOf(study.executionStatus)<0">
                                <span class="fas fa-times text-danger mr-1"></span>
                                <span v-t="'studyactions.status.study_wrong_status'"></span>
                            </div>
                            <div v-if="link.type.indexOf('REQUIRE_P')>=0 && study.participantSearchStatus != 'SEARCHING'">
                                <span class="fas fa-times text-danger mr-1"></span>
                                <span v-t="'error.closed.study'"></span>
                            </div>
                            <div v-if="(link.type.indexOf('REQUIRE_P')>=0 || link.type.indexOf('OFFER_P')>=0) && link.study.joinMethods.indexOf('APP') < 0 && link.study.joinMethods.indexOf('APP_CODE') < 0">
                                <span class="fas fa-times text-danger mr-1"></span>
                                <span v-t="'studyactions.status.study_no_app_participation'"></span>
                            </div>	                
                        </td>
                        <td>
                            <button type="button" class="btn btn-sm btn-default mr-1" @click="select(link);" v-t="'studyactions.select_btn'"></button>
                            <button type="button" class="btn btn-sm btn-default mr-1" :disabled="action!=null" @click="validate(link);" v-t="'studyactions.validate_btn'"></button>
                            <button type="button" class="btn btn-sm btn-danger" :disabled="action!=null" @click="remove(link);" v-t="'common.delete_btn'"></button>
                        </td>
                    </tr>
                </table>
            
                <button type="button" class="btn btn-primary" v-t="'common.add_btn'" @click="addNew()"></button>
            </div>
            <div v-if="selection">
                <form-group label="studyactions.study" :path="errors.name">
                    <p class="form-control-plaintext">{{ study.name }}</p>
                </form-group> 
                <form-group name="app" label="studyactions.app" :path="errors.app">
                    <typeahead class="form-control" @selection="appselection()" v-model="selection.app.filename" field="filename" :suggestions="apps" />
                </form-group>
                <form-group name="apptype" label="common.empty" :path="errors.apptype">
                    <p class="form-control-plaintext" v-if="selection.app.type">
                        <span>{{ $t('enum.plugintype.' + selection.app.type) }}</span>
                        <span v-t="'studyactions.for'"></span>
                        <span>{{ $t('enum.userrole.'+selection.app.targetUserRole) }}</span>	               
                    </p>
                    <p class="form-control-plaintext" v-if="!selection.app.type" v-t="'studyactions.no_valid_app'"></p>
                </form-group>
                <form-group name="type" label="studyactions.type" :path="errors.type">	                
                    <check-box v-for="type in types" :key="type" :name="type" :disabled="checkType(selection.app, type)" :checked="selection.type.indexOf(type)>=0" @click="toggle(selection.type, type);" >
                        <span>{{ $t('studyactions.types.'+type) }}</span>
                    </check-box>                
                </form-group>
                <form-group name="usePeriod" label="studyactions.use_period" :path="errors.usePeriod">
                    <check-box v-for="period in periods" :key="period" :name="period" :checked="selection.usePeriod.indexOf(period)>=0" @click="toggle(selection.usePeriod, period);" >
                        <span>{{ $t('studyactions.use_periods.'+period) }}</span>
                    </check-box>	                        
                </form-group>
            
                <form-group label="common.empty">
                    <button class="btn btn-primary space" v-submit :disabled="action!=null" type="submit" v-t="'common.submit_btn'"></button>
                    <button class="btn btn-default space" type="button" v-t="'common.cancel_btn'" @click="cancel();"></button>
                </form-group>
            </div>	        		   
        </form>       
    </tab-panel>
	
	<div v-if="!selection">
        <study-actions2></study-actions2>
    </div>	   
	
</div>

</template>
<script>

import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import StudyActions2 from "components/tiles/StudyActions2.vue"
import server from "services/server.js"
import apps from "services/apps.js"
import studies from "services/studies.js"
import { status, ErrorBox, Success, CheckBox, RadioBox, FormGroup, Typeahead } from 'basic-vue3-components'
import _ from "lodash";

export default {
    data: () => ({	
        studyid : null,
        crit : { group : "" },
        types : studies.linktypes,
	    periods : studies.executionStati,
	    selection : undefined       
    }),

    components: {  Panel, TabPanel, ErrorBox, FormGroup, StudyNav, Success, CheckBox, RadioBox, StudyActions2, Typeahead },

    mixins : [ status ],

    methods : {
        reload() {
            const { $data } = this, me = this;
            $data.selection = null;
            
            me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
            .then(function(data) { 				
                $data.study = data.data;												
            }));			
            
            me.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("study", $data.studyid).url)
            .then(function(data) { 				
                $data.links = data.data;												
            }));	
            
            me.doBusy(apps.getApps({ }, ["_id", "filename", "name", "orgName", "publisher", "type", "targetUserRole"])
            .then(function(data) {
                $data.apps = data.data;
            }));
	    },
	
	    addNew() {
            const { $data } = this;
		    $data.selection = { app : {}, type:[], usePeriod:["PRE", "RUNNING"], linkTargetType : "STUDY" };
	    },
	
        select(link) {
            const { $data } = this;
		    $data.selection = link;
	    },
	
	    cancel() {
            const { $data } = this;
		    $data.selection = null;
	    },
	
	    toggle(array,itm) {          
            var pos = array.indexOf(itm);
            if (pos < 0) array.push(itm); else array.splice(pos, 1);
        },
   
        checkType(app, linktype) {
            
            if (!app || !app.type) return true;
            if (app.type === "mobile") {
                if (linktype === "AUTOADD_A" ) return true;
            }
            // "OFFER_P", "REQUIRE_P", "RECOMMEND_A", "AUTOADD_A", "DATALINK"
            return false;
        },
   
        appselection() {
            const { $data } = this, me = this;
            me.doSilent(apps.getApps({ filename : $data.selection.app.filename }, ["_id", "filename", "name", "orgName", "publisher", "type", "targetUserRole"])
            .then(function(data) {
                if (data.data && data.data.length == 1) {
                $data.selection.appId = data.data[0]._id;
                $data.selection.app = data.data[0];
                }
            }));
    },
   
        remove(link) {
            const me = this;
            me.doAction("delete", server.delete(jsRoutes.controllers.Market.deleteStudyAppLink(link._id).url)
            .then(function() {
                me.reload();
            }));
        },
   
        validate(link) {
            const me = this;
            me.doAction("validate", server.post(jsRoutes.controllers.Market.validateStudyAppLink(link._id).url)
            .then(function() {
                me.reload();
            }));
        },
   
        submit() {
            const { $data } = this, me = this;
            $data.selection.studyId = $data.studyid;

            var first;
            if ($data.selection._id) {
                first = me.doAction("delete", server.delete(jsRoutes.controllers.Market.deleteStudyAppLink($data.selection._id).url));
            } else first = Promise.resolve();
            first.then(function() { me.doAction("submit", server.post(jsRoutes.controllers.Market.insertStudyAppLink().url, $data.selection)
            .then(function() {
                me.reload();
            })); });
        }
		
       
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.studyid = $route.query.studyId;
        me.reload();
        
    }
}
</script>