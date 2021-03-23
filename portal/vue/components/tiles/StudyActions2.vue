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
<panel :title="$t('studyactions.datalink')" :busy="isBusy">
	    
	<p class="alert alert-info" v-t="'studyactions.description'"></p>  		          	       
	<div>
	    <p class="lead" v-t="'studyactions.aggregator_apps'"></p>  
	    <form name="myform" ref="myform" role="form" class="css-form form-horizontal" novalidate @submit.prevent="addApplication(myform)">
	        
	        <form-group name="group" label="Group" :path="errors.group">
	            <select v-validate v-model="crit.group" class="form-control" @change="changedGroup()">
                    <option v-for="group in study.groups" :key="group.name" :value="group.name">{{ group.name }}</option>
	                <option value="" v-t="'studyactions.allgroups'"></option>
	            </select>
	        </form-group>
	        
            <div>
                <form-group name="plugin" label="studyactions.plugin" :path="errors.plugin">
                    <select name="plugin" id="plugin" v-validate v-model="crit.plugin" @change="setPluginType" class="form-control" required>
                        <option v-for="plugin in plugins" :key="plugin._id" :value="plugin._id">{{ plugin.name }}</option>
                    </select>
                </form-group>
                <form-group name="device" label="studyactions.device" v-if="crit.pluginType=='mobile'" :path="errors.device">
                    <input type="text" id="device" name="device"  v-validate v-model="crit.device" class="form-control" required>
                </form-group>
                <form-group name="endpoint" label="studyactions.endpoint" v-if="crit.pluginType=='endpoint'" :path="errors.endpoint">
                    <input type="text" id="endpoint" name="endpoint"  v-validate v-model="crit.endpoint" class="form-control" required>
                </form-group>
                <form-group name="onlyAggregated" label="studyactions.only_aggregated" v-if="crit.pluginType=='endpoint'" :path="errors.onlyAggregated">
                    <check-box name="onlyAggregated" v-validate v-model="crit.onlyAggregated" :path="errors.onlyAggregated">                              
					</check-box>
                </form-group>
                <form-group name="shareback" label="studyactions.shareback" v-if="crit.pluginType!='endpoint'">
                    <check-box name="shareback" id="shareback" v-validate v-model="crit.shareback" :path="errors.shareback"> 
					</check-box>
                </form-group>
                <form-group name="restrictread" label="studyactions.restrict" v-if="crit.group">
                    <check-box name="restrictread" v-validate v-model="crit.restrictread" :path="errors.restrictread">
					</check-box>                               
                </form-group>
            </div>
                       
            <button type="submit" v-submit class="btn btn-default" :disabled="action!=null || (crit.pluginType=='mobile' || crit.pluginType=='service' || crit.pluginType=='analyzer') && group == null" v-t="'studyactions.add_application_btn'"></button>          
     
        </form>                   
           
    </div>
</panel>
<panel v-if="me_menu.length" :title="$t('studyactions.linked_spaces')" :busy="isBusy">
	<table class="table table-striped"> 							
	    <tr v-for="entry in me_menu" :key="entry._id">
			<td>
			    <a href="javascript:" @click="showSpace(entry)" v-t="entry.name"></a>
			    <a href="javascript:" @click="deleteSpace(entry)" :disabled="action!=null" class="float-right btn btn-danger btn-sm" v-t="'common.delete_btn'"></a>
			</td>
		</tr>
	</table>
</panel>
<panel v-if="consents.length" :title="$t('studyactions.linked_apps')" :busy="isBusy">

	<table class="table table-striped" v-if="consents.length">

		<tr>
			<th v-t="'consents.name'"></th>					
			<th v-t="'consents.status'"></th>					
			<th v-t="'consents.number_of_records'"></th>
			<th></th>
		</tr>
		<tr v-for="consent in consents" :key="consent._id" ng-repeat="consent in consents | orderBy : 'name'" :class="{ 'table-warning' : consent.status == 'UNCONFIRMED' }">
			<td><a @click="editConsent(consent);" href="javascript:">{{ consent.name }}</a></td>					
			<td>{{ $t('enum.consentstatus.'+consent.status) }}</td>					
			<td>{{ consent.records }}</td>
			<td><button @click="deleteConsent(consent)" :disabled="action!=null" class="btn btn-danger btn-sm" v-t="'common.delete_btn'"></button></td>
		</tr>
	</table>
</panel>
<apikeys></apikeys>
</template>
<script>

import Panel from "components/Panel.vue"
import Apikeys from "views/shared/apikeys.vue"

import server from "services/server.js"
import apps from "services/apps.js"
import circles from "services/circles.js"
import spaces from "services/spaces.js"
import { status, ErrorBox, Success, CheckBox, FormGroup } from 'basic-vue3-components'

export default {
    props : [],

    data : ()=>({      
		crit : { group : "", pluginType : "" },
		study : null,
		showKeys : false,
		plugins : [],
		aps : null,
		group : null,
		me_menu : [],
		consents : []
    }),

	components: {  Panel, ErrorBox, FormGroup, Success, CheckBox, Apikeys },

    mixins : [ status ],

    methods : {        
        reload() {
			const { $data, $route } = this, me = this;
			me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
			.then(function(data) { 				
				$data.study = data.data;	
				
				me.updateConsents();
				$data.showKeys = true;		
			}));
			
			var apptypes = $route.meta.allowPersonalApps ? ["analyzer","visualization","mobile","endpoint"] : ["analyzer", "endpoint"]; 
			
			me.doBusy(apps.getApps({ "targetUserRole" : "RESEARCH", type : apptypes }, ["filename", "name","type"])
			.then(function(data) {
				$data.plugins = data.data;
			}));
		
	    },
	
	    setGroup() {
			const { $data } = this, me = this;
			$data.group = $data.crit.group;
			me.doBusy(server.post(jsRoutes.controllers.research.Studies.shareWithGroup($data.studyid, $data.group).url))
			.then(function(result) {
				if (result.data.length) {
				$data.aps = result.data[0]._id;
				views.setView("group_records", { aps : $data.aps, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type : "studyrelated" });
				}
			});
			
			me.updateConsents();
		
	    },
	
	    changedGroup() {
			const { $data } = this, me = this;
			
			$data.crit.device = "";
			$data.group = $data.crit.group;
			me.updateConsents();
	    },
			   	
		updateConsents() {
			const { $data } = this, me = this;
			me.doBusy(circles.listConsents({ "sharingQuery.link-study" : $data.studyid, "sharingQuery.link-study-group" : $data.crit.group }, [ "name", "authorized", "type", "status", "records" ])
			.then(function(data) {
				$data.consents = data.data;						
			}));
			
			me.doBusy(spaces.getSpacesOfUserContext($data.userId, $data.study.code + ":" + ($data.crit.group ? $data.crit.group : ""))
			.then(function(results) {
				$data.me_menu = results.data;
			}));	
		},
	
		editConsent(consent) {
			const { $router } = this;
			$router.push({ path : './editconsent' , query : { consentId : consent._id }});			
		},

		setPluginType() {
			const { $data } = this;
			let plugin = _.filter($data.plugins, (x) => x._id == $data.crit.plugin)[0];
			$data.crit.pluginType = plugin.type;
		},
	
		addApplication() {	
			const { $data } = this, me = this;
	    
			$data.showKeys = false;	
			
			var c = $data.crit;		
			me.doAction("addapplication", server.post(jsRoutes.controllers.research.Studies.addApplication($data.studyid, $data.group).url, { plugin : c.plugin, restrictread : c.restrictread, shareback : c.shareback, device : c.device, onlyAggregated : c.onlyAggregated, endpoint : c.endpoint })
			.then(function(result) {				
				me.updateConsents();
				$data.showKeys = true;
			}));
		},
	
		deleteConsent(consent) {
			const me = this;
			me.doAction("deleteConsent", server.delete(jsRoutes.controllers.Circles["delete"](consent._id).url).
			then(function() {
				me.updateConsents();
			}));
		},
	
		showSpace(space) {
			const { $data, $router } = this;
			$router.push({ path : './spaces', query : { spaceId : space._id, study : $data.studyid }});		
		},
	
		deleteSpace(space) {
			const me = this;
			me.doAction("deleteConsent", spaces.deleteSpace(space._id).then(function() { me.updateConsents(); }));
		}
    },
    
    created() {
		const { $data, $route } = this;
		$data.studyid = $route.query.studyId;
        this.reload();
    }
        
}
</script>