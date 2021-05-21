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
     <panel :title="$t('applink.title')" :busy="isBusy">		  
	
        <error-box :error="error"></error-box>

        <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="submit()" role="form">	
                
            <p class="lead" v-t="'studyactions.related_apps'"></p>

            <div v-if="!selection">
                <p v-t="'studyactions.empty'" v-if="!links.length"></p>
                <table class="table table-striped table-hover" v-if="links.length">
                    <tr>
                        <th v-t="'applink.target'"></th>
                        <th v-t="'studyactions.type'"></th>
                        <th v-t="'studyactions.validation'"></th>
                        <th></th>
                    </tr>
                    <tr v-for="link in links" :key="link._id">
                        <td @click="select(link);">{{ (link.study || {}).code }} {{ (link.study || {}).name }} {{ (link.provider || {}).name }} {{ (link.serviceApp || {}).name }}<span v-if="link.userLogin">({{ link.userLogin }})</span></td>
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
                            <div v-if="link.study && link.usePeriod.indexOf(link.study.executionStatus)<0">
                                <span class="fas fa-times text-danger mr-1"></span>
                                <span v-t="'studyactions.status.study_wrong_status'"></span>
                            </div>
                            <div v-if="link.study && link.type.indexOf('REQUIRE_P')>=0 && link.study.participantSearchStatus != 'SEARCHING'">
                                <span class="fas fa-times text-danger mr-1"></span>
                                <span v-t="'error.closed.study'"></span>
                            </div>
                            <div v-if="link.study && (link.type.indexOf('REQUIRE_P')>=0 || link.type.indexOf('OFFER_P')>=0) && link.study.joinMethods.indexOf('APP') < 0 && link.study.joinMethods.indexOf('APP_CODE') < 0">
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
                <router-link :to="{ path : './manageapp', query :  {appId:app._id} }" class="btn btn-default mr-1" v-t="'common.back_btn'"></router-link>
			    <button type="button" class="btn btn-primary mr-1" v-t="'applink.add_research_btn'" @click="addNewResearch()"></button>
			    <button type="button" class="btn btn-primary mr-1" v-t="'applink.add_service_btn'" @click="addNewService()"></button>
	            <button type="button" class="btn btn-primary mr-1" v-t="'applink.add_provider_btn'" @click="addNewProvider()"></button>                
            </div>
            <div v-if="selection && selection.linkTargetType=='STUDY'">
                <form-group name="study" label="studyactions.study" :path="errors.study">
                    <div class="row">
	                    <div class="col-sm-3">
	                        <typeahead class="form-control" @selection="studyselection()" v-model="selection.study.code" :suggestions="studies" field="code" />
	                    </div>
	                    <div class="col-sm-9">
	                        <p class="form-control-plaintext">{{ selection.study.name }}</p>
	                    </div>
	                </div>
                </form-group> 
                <form-group name="app" label="studyactions.app" :path="errors.app">
                    <p class="form-control-plaintext">{{ app.filename }}</p>	   
                </form-group>
                <form-group name="apptype" label="common.empty" :path="errors.apptype">
                    <p class="form-control-plaintext" v-if="app.type">
                        <span>{{ $t('enum.plugintype.' + app.type) }}</span>
                        <span v-t="'studyactions.for'"></span>
                        <span>{{ $t('enum.userrole.'+app.targetUserRole) }}</span>	               
                    </p>
                    <p class="form-control-plaintext" v-if="!app.type" v-t="'studyactions.no_valid_app'"></p>
                </form-group>
                <form-group name="type" label="studyactions.type" :path="errors.type">	                
                    <check-box v-for="type in types" :key="type" :name="type" :disabled="checkType(app, type)" :checked="selection.type.indexOf(type)>=0" @click="toggle(selection.type, type);" >
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

            <div v-if="selection && selection.linkTargetType=='ORGANIZATION'">
	            <form-group name="provider" label="applink.provider" :path="errors.userLogin">	  				   
				   <input type="text" class="form-control" name="userLogin" v-validate v-model="selection.userLogin">	              	                  
	            </form-group> 	          
	            <form-group name="app" label="studyactions.app">
	               <p class="form-control-plaintext">{{ app.filename }}</p>	               
	            </form-group>	       
	            <form-group name="identifier" label="applink.identifier">
	               <input type="text" class="form-control" v-validate v-model="selection.identifier">
	            </form-group>
	            <form-group name="termsOfUse" label="applink.terms_of_use" >	    
	               <typeahead id="termsOfUse" name="termsOfUse" class="form-control" v-model="selection.termsOfUse" :suggestions="terms" field="id" display="fullname" />		    
	                  <p class="form-text text-muted" v-if="selection.termsOfUse"><router-link :to="{ path : './terms', query : { which:selection.termsOfUse }}" v-t="'applink.show_terms'"></router-link></p>
	                  <p class="form-text text-muted" v-t="'applink.terms_of_use_hint'"></p> 
	            </form-group>	            
	            <form-group name="type" label="studyactions.type">
	              <div v-for="type in types2" :key="type" class="form-check">
	                <label class="form-check-label">
	                  <input class="form-check-input" type="checkbox" :disabled="checkType(app, type)" :checked="selection.type.indexOf(type)>=0" @click="toggle(selection.type, type)" >
	                  <span>{{ $t('applink.types.'+type) }}</span>
	                </label>
	              </div>
	            </form-group>
	           	       
	            <form-group label="common.empty">
	                <button class="btn btn-primary mr-1" type="submit" v-submit v-t="'common.submit_btn'"></button>
	                <button class="btn btn-default mr-1" type="button" v-t="'common.cancel_btn'" @click="cancel();"></button>
	            </form-group>
			</div>
			   
			<div v-if="selection && selection.linkTargetType=='SERVICE'">
	            <form-group name="serviceApp" label="applink.service_app">	   
				   <typeahead class="form-control" @selection="serviceappselection()" v-model="selection.serviceApp.filename" :suggestions="apps" field="filename" />                       	               
	            </form-group> 	          
	            <form-group name="app" label="studyactions.app">
	               <p class="form-control-plaintext">{{ app.filename }}</p>	               
	            </form-group>	       	                  
	            <form-group name="type" label="studyactions.type">
	              <div v-for="type in types2" :key="type" class="form-check">
	                <label class="form-check-label">
	                  <input class="form-check-input" type="checkbox" :disabled="checkType(app, type)" :checked="selection.type.indexOf(type)>=0" @click="toggle(selection.type, type);" >
	                  <span>{{ $t('applink.types.'+type) }}</span>
	                </label>
	              </div>
	            </form-group>
	           	       
	             <form-group label="common.empty">
	               <button class="btn btn-primary mr-1" type="submit" v-submit v-t="'common.submit_btn'"></button>
	               <button class="btn btn-default mr-1" type="button" v-t="'common.cancel_btn'" @click="cancel();"></button>
	             </form-group>
	        </div>       		   
        </form>       
    </panel>
			
</div>

</template>
<script>

import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import server from "services/server.js"
import apps from "services/apps.js"
import studies from "services/studies.js"
import { status, ErrorBox, Success, CheckBox, RadioBox, FormGroup, Typeahead } from 'basic-vue3-components'
import _ from "lodash";

export default {
    data: () => ({	
        appId : null,
        crit : { group : "" },
        types : studies.linktypes,
        types2 : ["OFFER_P", "REQUIRE_P", "OFFER_EXTRA_PAGE", "OFFER_INLINE_AGB"],
	    periods : studies.executionStati,
	    selection : undefined,
        apps : [],
        studies : [],
        app : null
    }),

    components: {  Panel, TabPanel, ErrorBox, FormGroup, Success, CheckBox, RadioBox, Typeahead },

    mixins : [ status ],

    methods : {
        reload() {
            const { $data } = this, me = this;
            $data.selection = null;
            
            me.doBusy(apps.getApps({ "_id" : $data.appId }, ["creator", "filename", "name", "description", "icons", "type", "targetUserRole" ])
		    .then(function(data) { 
			    $data.app = data.data[0];			
		    }));	
                        
            me.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("app", $data.appId).url)
            .then(function(data) { 				
                $data.links = data.data;												
            }));	

            me.doBusy(studies.search({ validationStatus : "VALIDATED" }, ["_id", "code", "name" ])
		    .then(function(data) {
			    $data.studies = data.data;
		    }));
                                   								
		    me.doBusy(apps.getApps({ type : "external" }, ["_id", "filename", "name", "orgName", "publisher", "type", "targetUserRole"])
		    .then(function(data) {
			    $data.apps = data.data;
		    }));
	    },

        addNewResearch() {
            const { $data } = this;
		    $data.selection = { linkTargetType : "STUDY", app : {}, study:{}, type:[], usePeriod:["PRE", "RUNNING"], studyId : null };
	    },
	
	    addNewProvider() {
            const { $data } = this;
		    $data.selection = { linkTargetType : "ORGANIZATION", app : {}, provider:{}, type:[], providerId : null };
	    },

	    addNewService() {
            const { $data } = this;
		    $data.selection = { linkTargetType : "SERVICE", app : {}, serviceApp:{}, type:[], serviceAppId : null };
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
          
        studyselection() {
            const { $data } = this, me = this;
	        me.doSilent(studies.search({ code : $data.selection.study.code }, ["_id", "code", "name" ])
		    .then(function(data) {
			    if (data.data && data.data.length == 1) {
			        $data.selection.studyId = data.data[0]._id;
			        $data.selection.study = data.data[0];			  
			    }
		    }));
        },

        serviceappselection() {
            const { $data } = this, me = this;
	        me.doSilent(apps.getApps({ filename : $data.selection.serviceApp.filename }, ["_id", "filename", "name", "orgName", "publisher", "type", "targetUserRole"])
	        .then(function(data) {
		        if (data.data && data.data.length == 1) {
		            $data.selection.serviceAppId = data.data[0]._id;
		            $data.selection.serviceApp = data.data[0];
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
            $data.selection.appId = $data.appId;

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
        $data.appId = $route.query.appId;
        me.reload();
        
    }
}
</script>