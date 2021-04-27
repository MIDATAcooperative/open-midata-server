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
    <div  class="midata-overlay borderless">
        <panel :title="getTitle()" :busy="isBusy">
        <error-box :error="error"></error-box>
		
		<div v-if="app && (app.type=='mobile' || app.type=='service')" class="alert alert-warning">
		    <strong v-t="'manageapp.important'"></strong>		   
		    <p v-if="app.targetUserRole=='RESEARCH'" v-t="'manageapp.researchwarning'"></p>		    
            <p v-else v-t="'manageapp.logoutwarning'"></p>
	    </div>
		<div v-if="blocks.length && !currentBlock && !target.askresources && !expertmode">
		    <p v-t="'queryeditor.summary'"></p>
		    <div class="list-group">
		    <div v-for="block in blocks" :key="JSON.stringify(block)" :class="{'list-group-item-light':isFiltered(block) }" class="list-group-item" href="javascript:" @click="selectBlock(block);">
		    <b>{{ block.display }}</b> <span v-if="block.format" class="text-muted">- {{ resourceName(block.format) }}</span>
		    <div v-if="block.code">{{ block.code }}</div>
		    <div>
		      <span class="text-success" v-if="mode=='app' && block.owner && block.owner != 'all' && (!block.public || block.public=='no')">{{ $t('queryeditor.short_owner_'+block.owner) }}</span>		      
		      <span class="text-danger" v-if="mode=='app' && block.public == 'no' && (!block.owner || block.owner == 'all')" v-t="'queryeditor.short_owner_all'"></span>
		      <span class="text-info" v-if="block.public == 'only'">{{ $t('queryeditor.short_public_only') }}</span>
		      <span class="text-danger" v-if="block.public == 'also' && (!block.owner || block.owner == 'all')">{{ $t('queryeditor.short_public_also') }}</span> 
		      <span class="text-danger" v-if="block.public == 'also' && block.owner != 'all'">{{ $t('queryeditor.short_public_also_self') }}</span>
		      <span v-if="mode=='app'"> / </span>
		      <span class="text-danger" v-if="block.app && block.app != 'all'"><span v-t="'queryeditor.short_app_other'"></span> {{ block.appName }}</span>
		      <span class="text-success" v-if="!block.app || block.app == 'all'" v-t="'queryeditor.short_app_all'"></span>
		    </div>		   
		    
		    <div v-if="block.timeRestrictionMode">
		      <span>{{ $t('queryeditor.'+block.timeRestrictionMode) }}</span>: {{ $filters.date(block.timeRestrictionDate) }}
		    </div>
		    <div v-if="block.dataPeriodRestrictionMode">
		      <span>{{ $t('queryeditor.'+block.dataPeriodRestrictionMode) }}</span>: {{ $filters.date(block.dataPeriodRestrictionStart) }} - {{ $filters.date(block.dataPeriodRestrictionEnd) }} 
		    </div>
		    <div v-if="block.customFilter">
		      <span>Extra Filter: </span><span>{{ block.customFilterPath }}</span>: {{ block.customFilterValue }}
		    </div>
		    <div v-if="block.observer">
		      {{ block.observer }}
		    </div>
		  </div>
		</div>
		<div v-if="mode=='app'">
		  <div><label v-t="'manageapp.write_mode'"></label></div>
		  <select class="form-control" name="writes" v-validate v-model="app.writes" @change="requireLogout();">
              <option v-for="mode in writemodes" :key="mode" :value="mode">{{ $t('enum.writepermissiontype.'+mode) }}</option>
          </select>
		  <div class="extraspace"></div>
		</div>
		<hr>
				
       
						
		</div>
		
		<div v-if="expertmode">				
		<p v-t="'queryeditor.access_query'"></p>		
		<textarea class="form-control" @change="updateQuery()" v-validate v-model="query.queryStr"></textarea>
		<div class="extraspace"></div>
		<button class="btn btn-default" @click="expertModeDone()" v-t="'common.submit_btn'"></button>
		</div>
		
		<div v-if="target.askresources">
		  <p v-t="'queryeditor.select_resources'"></p>
		  <div v-for="resource in target.askresources" :key="resource.text" class="form-check">
		    <label class="form-check-label"><input class="form-check-input" type="checkbox" v-validate v-model="resource.selected">
		      <b>{{ resource.display }}</b> {{ resource.text }}
		    </label>
		  </div>
		  <button class="btn btn-default" v-t="'common.submit_btn'" @click="addPreselection()"></button>
		</div>
		
		<div v-if="!currentBlock && !newentry && !expertmode && !target.askresources">
		
		  
		
		  <p v-t="'queryeditor.check_okay'"></p>
		   <div v-if="mode=='app'" class="extraspace">
		    
		  
		   
		    
		  
		    <div class="form-check">
		      <label class="form-check-label">
		        <input class="form-check-input" type="checkbox" id="withLogout" name="withLogout" v-validate v-model="app.withLogout" value="true" :required="logoutRequired">
		        <span v-t="'manageapp.pleaseLogout1'"></span>
		        <span v-if="app.targetUserRole=='RESEARCH'"> / </span>
		        <span v-if="app.targetUserRole=='RESEARCH'" v-t="'manageapp.pleaseLogout2'"></span>		        
		      </label>
		    </div>  		  
		 </div>
		  
		  <button class="btn btn-default space" @click="addNew()" v-t="'queryeditor.add_data_btn'"></button>
		  <button class="btn btn-default space" :disabled="mode=='app' && !app.withLogout" @click="saveExit()" v-t="'queryeditor.save_exit_btn'"></button>
		  <button class="btn btn-default space" @click="cancel()" v-t="'queryeditor.cancel_btn'"></button>
		  <br><br>
		  <button class="btn btn-sm btn-default space" @click="enableExpertMode()" v-t="'queryeditor.expert_mode_btn'"></button>
		  <button class="btn btn-sm btn-default space" v-if="mode=='app'" @click="basicAppResources()" v-t="'queryeditor.add_basic_btn'"></button>
		</div>
		
		<div v-if="currentBlock">	
		
		<form class="form form-horizontal">
		  
		<h3>{{ currentBlock.display }}</h3>
		  
		  <form-group name="format" label="queryeditor.format" :path="errors.format">
		    <input type="text" class="form-control" name="format" v-validate v-model="currentBlock.format" placeholder="fhir/Observation">
		  </form-group>
		  <form-group name="content" label="queryeditor.content" v-if="currentBlock.content" :path="errors.content">
		    <p class="form-control-plaintext">{{ currentBlock.content }}</p>
		  </form-group>
		  <form-group name="code" label="queryeditor.code" v-if="currentBlock.code" :path="errors.code">
		    <p class="form-control-plaintext">{{ currentBlock.code }}</p>
		  </form-group>
		  <form-group name="public" label="queryeditor.public" v-if="mode=='app' && !currentBlock.flags.nopublic">
		    <div class="form-check">
		      <label class="form-check-label"><input class="form-check-input"  type="radio" v-validate v-model="currentBlock.public" value="no"><span v-t="'queryeditor.public_no'"></span></label>
		    </div>
		    <div class="form-check">
		      <label class="form-check-label"><input class="form-check-input"  type="radio" v-validate v-model="currentBlock.public"  value="only"><span v-t="'queryeditor.public_only'"></span></label>
		    </div>
		    <div class="form-check">
		      <label class="form-check-label"><input class="form-check-input"  type="radio" v-validate v-model="currentBlock.public" value="also"><span v-t="'queryeditor.public_also'"></span></label>
		    </div>
		  </form-group>
		  
		  <form-group name="owner" label="queryeditor.owner" v-if="mode=='app' && !currentBlock.flags.noowner">
		    <div class="form-check">
		      <label class="form-check-label"><input class="form-check-input" type="radio" v-validate v-model="currentBlock.owner" :disabled="currentBlock.public!='no'" value="self"><span v-t="'queryeditor.owner_self'"></span></label>
		    </div><div class="form-check">
		      <label class="form-check-label"><input class="form-check-input" type="radio" v-validate v-model="currentBlock.owner" :disabled="currentBlock.public!='no'" value="all"><span v-t="'queryeditor.owner_all'"></span></label>
		    </div>
		  </form-group>
		  <form-group name="app" label="queryeditor.source_app" v-if="!currentBlock.flags.noapp">
		    <div class="form-check" v-if="target.appname">
		      <label class="form-check-label"><input class="form-check-input"  type="radio" v-validate v-model="currentBlock.app"  value="self"><span v-t="'queryeditor.app_self'"></span></label>
		    </div><div class="form-check">
		      <label class="form-check-label"><input class="form-check-input"  type="radio" v-validate v-model="currentBlock.app" value="all"><span v-t="'queryeditor.app_all'"></span></label>
		    </div><div class="form-check">
		      <label class="form-check-label"><input class="form-check-input"  type="radio" v-validate v-model="currentBlock.app" value="other"><span v-t="'queryeditor.app_other'"></span><input type="text" v-validate v-model="currentBlock.appName"></label>
		    </div>
		  </form-group>
		  <form-group name="observer" label="queryeditor.observer" v-if="currentBlock.flags.observer" :path="errors.observer">
		    <input type="text" class="form-control" name="observer" v-validate v-model="currentBlock.observer">		    
		  </form-group>
		  <form-group name="category" label="queryeditor.category" v-if="currentBlock.flags.category" :path="errors.category">
		    <input type="text" class="form-control" v-validate v-model="currentBlock.category">	    
		  </form-group>
		  <div v-if="mode=='study'">
		  <form-group name="restrictions" label="queryeditor.restrictions" v-if="timeModes.length || dataPeriodModes.length || currentBlock.flags.custom" :path="errors.restrictions">
		    <div class="form-check" v-if="timeModes.length"><label class="form-check-label"><input class="form-check-input" type="checkbox" v-validate v-model="currentBlock.timeRestriction"><span v-t="'queryeditor.time_restriction'"></span></label></div>
		    <div class="form-check" v-if="dataPeriodModes.length"><label class="form-check-label"><input class="form-check-input" type="checkbox" v-validate v-model="currentBlock.dataPeriodRestriction"><span v-t="'queryeditor.data_period_restriction'"></span></label></div>
		    <div class="form-check" v-if="currentBlock.flags.custom"><label class="form-check-label"><input class="form-check-input" type="checkbox" v-validate v-model="currentBlock.customFilter"><span v-t="'queryeditor.custom_filter'"></span></label></div>
		  </form-group>
		  <form-group name="timeRestrictionDate" label="queryeditor.time_restriction" v-if="currentBlock.timeRestriction" :path="errors.timeRestrictionDate">
		     <div class="row"><div class="col-sm-3">		     
		     <select class="form-control" v-validate v-model="currentBlock.timeRestrictionMode">
                 <option v-for="timeMode in timeModes" :key="timeMode" :value="timeMode">{{ $t('queryeditor.'+timeMode) }}</option>
             </select>
		     </div>
		     <div class="col-sm-5">
	          <input id="timeRestrictionDate" name="timeRestrictionDate" type="date" class="form-control" placeholder="" v-validate v-model="currentBlock.timeRestrictionDate" />	          
             </div>
             </div>                 		       
		  </form-group>
		  <form-group name="dataPeriodRestriction" label="queryeditor.data_period_restriction" v-if="currentBlock.dataPeriodRestriction" :path="errors.dataPeriodRestriction">
		     <div class="row">
		     <div class="col-sm-5">		     
		       <select class="form-control" v-validate v-model="currentBlock.dataPeriodRestrictionMode">
                   <option v-for="timeMode in dataPeriodModes" :key="timeMode" :value="timeMode">{{ $t('queryeditor.'+timeMode) }}</option>
               </select>
		     </div>
		     <div class="col-sm-3">
		     
	          <input id="dataPeriodRestrictionStart" name="dataPeriodRestrictionStart" type="date" class="form-control" placeholder=""   v-validate v-model="currentBlock.dataPeriodRestrictionStart" />
	         
             </div><div class="col-sm-1"><p class="form-control-plaintext" v-t="'queryeditor.to'"></p></div>
             <div class="col-sm-3">
		     
	          <input id="dataPeriodRestrictionEnd" name="dataPeriodRestrictionEnd" type="date" class="form-control" placeholder=""  v-validate v-model="currentBlock.dataPeriodRestrictionEnd" is-open="datePickers.dataPeriodRestrictionEnd"/>
	         
             </div>
		     		    
		     </div>
		  </form-group>
		  <form-group id="customFilter"  label="queryeditor.custom_filter" v-if="currentBlock.customFilter">		   		     
		     <input type="text" class="form-control" v-validate v-model="currentBlock.customFilterValue"> 
		  </form-group>
		  </div>
          <form-group id="x" label="common.empty">								
		    <button class="btn btn-default space" @click="deleteBlock()" v-t="'queryeditor.remove_btn'"></button>
		    <button class="btn btn-default space" @click="applyBlock()" v-t="'queryeditor.apply_btn'"></button>
		  </form-group>
		</form>
		
		</div>
		
		<div v-if="newentry!=null">
		<p v-t="'queryeditor.newentry'"></p>
		<div class="row">
		<div class="col-sm-8">
		<input type="text" id="queryadd" name="queryadd" class="form-control" v-validate v-model="newentry.search">
		</div><div class="col-sm-4">
		<button class="btn btn-default space" :disabled="action!=null" @click="search()" v-t="'common.search_btn'"></button>
		<button class="btn btn-default space" :disabled="action!=null" @click="cancelsearch()" v-t="'common.cancel_btn'"></button>
		</div>
		</div>
		<div class="extraspace"></div>
		<p v-t="'queryeditor.make_selection'" v-if="newentry.choices"></p>
		<table class="table table-striped" v-if="newentry.choices">
		  <tr>
		    <th v-t="'queryeditor.resultgroup'"></th>
		    <th v-t="'queryeditor.resultdetail'"></th>
		  </tr>
		  <tr v-for="choice in newentry.choices" :key="JSON.stringify(choice)">
		    <td><a href="javascript:" @click="addContent(choice)">{{ choice.display }}</a>
		      <span v-if="choice.group" class="text-muted">(Group)</span>
		    </td>
		    <td>
		      <div v-for="code in choice.codes" :key="JSON.stringify(code)"><a href="javascript:" @click="addContent(choice, code)">{{ code.system }} {{ code.code }}</a></div>
		      <div v-for="content in choice.contents" :key="JSON.stringify(content)"><a href="javascript:" @click="addContent(content);">{{ content.display }}</a><span v-if="content.content" class="text-muted">(Content)</span></div>
		    </td> 
		  </tr>
		</table>
		<p v-if="newentry.choices && newentry.choices.length == 0" v-t="'queryeditor.search_empty'"></p>
		
		</div>


 		     
        </panel>	    
    </div>
</template>
<script>
import Panel from 'components/Panel.vue';
import { getLocale } from 'services/lang.js';
import server from 'services/server.js';
import apps from 'services/apps.js';
import session from 'services/session.js';
import formats from 'services/formats.js';
import labels from 'services/labels.js';
import { status, ErrorBox, FormGroup } from 'basic-vue3-components';

var lookupCodes = function(entry) {
		entry.codes = [];
		return formats.searchCodes({ content : entry.content },["code","system","version","display"])
		.then(function(result) {			
		    entry.codes = [];
			for (var i=0;i<result.data.length;i++) {
				entry.codes.push(result.data[i]);
			}
			return entry;
		});
	};
	
	var lookupContent = function(name) {
		return formats.searchContents({ content : name }, ["content", "label", "resourceType"])
		.then(function(result) {
			if (result.data.length == 1) return { key : result.data[0].content, format : result.data[0].resourceType, content : result.data[0].content, display : result.data[0].label[getLocale()] || result.data[0].label.en || result.data[0].content };
		});
	};
	

	
	

export default {
    
    data: () => ({
	
    	query : { queryStr : "", json : {} },
		app : null,
		study : null,
    	newentry :null,
    	target : { type : "study" },
    	blocks : [],
    	currentBlock : null,
    	writemodes : apps.writemodes,
		expertmode : false,
		mode : null,
		resourceOptions : {
			"fhir/AuditEvent" : ["noapp", "noowner", "notime", "nopublic"], 
			"fhir/Consent" : ["noapp", "noowner", "notime", "nopublic", "observer", "category"],
			"fhir/ResearchStudy" : ["noapp","noowner","initpublic","notime","nopublic"],
			"fhir/Organization" : ["noapp","noowner","initpublic","notime","nopublic"],
			"fhir/ValueSet" : ["noapp","noowner", "notime","initpublic"],
			"fhir/Group" : ["noowner"],
			"fhir/Patient" : ["noapp", "notime", "nopublic"],
			"fhir/Person" : ["noapp", "noowner", "notime", "nopublic"],
			"fhir/Practitioner" : ["noapp", "noowner", "notime"],
			"fhir/Subscription" : ["noapp", "noowner", "notime", "nopublic"],
			"fhir/Observation" : ["effective"],
			"fhir/QuestionnaireResponse" : ["custom"],
			"fhir/DocumentReference" : []	
		}
	}),				

	components : { ErrorBox, Panel, FormGroup },

    mixins : [ status ],

    methods : {
        getTitle() {
            const { $data, $t } = this;
            if ($data.app) return $t('queryeditor.app')+": "+$data.app.name;
            if ($data.study) return $t('queryeditor.study')+": "+$data.study.name;
            return " ";
        },

		reload() {
			const { $data, $route, $router } = this, me = this;
			if ($route.meta.mode == "study") {
				$data.mode = "study";
				me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($route.query.studyId).url)
				.then(function(data) { 				
					$data.study = data.data;	
					$data.query = { queryStr : JSON.stringify($data.study.recordQuery), json : $data.study.recordQuery };
					return me.parseAccessQuery($data.query.json).then(function(result) {
						$data.blocks = result;
						if ($data.blocks.length === 0) me.addNew();
					});										
				}));				
			} else if ($route.meta.mode == "app") {
				$data.mode = "app";
				me.doBusy(apps.getApps({ "_id" : $route.query.appId }, ["creator", "developerTeam", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "pluginVersion", "requirements", "termsOfUse", "orgName", "publisher", "unlockCode", "writes", "icons", "apiUrl", "noUpdateHistory","pseudonymize"])
				.then(function(data) { 
					$data.app = data.data[0];
					$data.target.appname = $data.app.filename;
					$data.query = { queryStr : JSON.stringify($data.app.defaultQuery), json : $data.app.defaultQuery };
					return me.parseAccessQuery($data.query.json).then(function(result) {
						$data.blocks = result;
						if ($data.blocks.length === 0) me.basicAppResources();
					});											
				}));
			}
		},
	
		isFiltered(block) {
			const { $data, $route, $router } = this, me = this;
			return labels.isFiltered(block, $data.app != null ? $data.app.filename : null, true);
		},
	
		basicAppResources() {
			const { $data, $route, $t } = this, me = this;
			var toadd = [ "Patient", "PseudonymizedPatient","Practitioner", "AuditEvent", "Consent", "Group", "Subscription"];
			$data.target.askresources = [];
			for (let x of toadd) {
				lookupContent(x).then(function(y) { 
					$data.target.askresources.push(y);
					let v =$t("queryeditor.ask."+y.content);
					if (v && v!=y.content) y.text = v;
				});
			}
		},
	
		resourceName(format) {
			const { $data, $route, $router } = this, me = this;
			if (format.startsWith("fhir/")) return format.substr(5);
			return format;
		},

		buildAccessQuery() {	
			const { $data, $route, $router } = this, me = this;	
			var finalblocks = [];
			var keys = {};			
			for (var i=0;i<$data.blocks.length;i++) {
				var block = $data.blocks[i];			
				var fb = {};
				if (block.format) fb.format = [ block.format ];		
				if (block.system) fb["group-system"] = block.system;
				if (block.owner && block.owner != "all") fb.owner = [ block.owner ];
				if (block["public"] && block["public"] != "no") fb["public"] = block["public"];
				if (block.app && block.app != "all") {
					if (block.app == "self") {
						fb.app = [ $data.target.appname ];					
					}
					else fb.app = [ block.appName ];
				}
				if (block.observer && block.observer != "") {
					fb.observer = [ block.observer ];
				}
				if (block.category && block.category != "") {
					fb.category = [ block.category ];
				}
				if (block.timeRestriction && block.timeRestrictionMode) {
					fb[block.timeRestrictionMode] = block.timeRestrictionDate;
				}
				if (block.dataPeriodRestriction && block.dataPeriodRestrictionMode) {
					if (!fb.data) fb.data = {};
					if (block.dataPeriodRestrictionMode === "effective") {
						if (block.dataPeriodRestrictionStart) fb.data["effectiveDateTime|effectivePeriod.start|null"] = { "!!!ge" : block.dataPeriodRestrictionStart };
						if (block.dataPeriodRestrictionEnd) fb.data["effectiveDateTime|effectivePeriod.end|null"] = { "!!!lt" : block.dataPeriodRestrictionEnd };
					}
				}
				if (block.customFilter && block.customFilterValue) {
					try {
					fb.data = JSON.parse(block.customFilterValue);
					} catch (e) {}
				}
				
				var k = JSON.stringify(fb);
				if (block.code) k+="code/"+block.content;
				if (block.content) k+="content";
				if (block.group) k+="group";
				
				if (keys[k]) {
					if (block.content) keys[k].content.push(block.content);
					if (block.code) keys[k].code.push(block.code);
					if (block.group) keys[k].group.push(block.group);
				} else {
					keys[k] = fb;
					finalblocks.push(fb);
					if (block.content) fb.content = [ block.content ];
					if (block.code) fb.code = [ block.code ];
					if (block.group) fb.group = [ block.group ];
				}									
									
			}		
			if (finalblocks.length > 1) {
				return { "$or" : finalblocks };
			} else if (finalblocks.length == 1) {
				return finalblocks[0];
			} else {
				return {};
			}
		},
	
	
		fullTextSearch(what) {
			return new Promise((resolve, reject) => {
				let waitFor = [];
				let waitFor2 = [];
				var searchresult = [];
				var already = {};
				
				var add = function(entry) {
					if (!already[entry.key]) { searchresult.push(entry); already[entry.key] = entry; return true; }
					return false;
				};
				
				var lookup = function(content) {
					return lookupCodes(content);		      
				};
				
				var addgroup = function(dat) {
					var grp = { key : "grp "+dat.name, group : dat.name, system : dat.system, display : dat.label[getLocale()] || dat.label.en || dat.name, contents:[] };
					var addgrp = function(what) {				
						grp.contents.push(what); 
					};
					var recproc = function(dat) {
						if (dat.contents && dat.contents.length > 1) {
							for (var i2=0;i2<dat.contents.length;i2++) {
								waitFor2.push(lookupContent(dat.contents[i2]).then(addgrp));
							}
						}
						if (dat.children) {
							for (var i3=0;i3<dat.children.length;i3++) {
								recproc(dat.children[i3]);					    
							}
						}
					};
					if (dat.contents && dat.contents.length == 1) return;
					if (add(grp)) {
						recproc(dat);
					}
				};
			
				waitFor.push(formats.listCodes()
				.then(function(result) {
					var l = result.data.length;		
					for (var i=0;i<l;i++) {
						var dat = result.data[i];
						//console.log(dat.code);
						if (dat.code.toLowerCase() == what) {
							waitFor2.push(lookupContent(dat.content)
							.then(lookup).then(add));
						}
					}
					//console.log(searchresult);
				}));
			
				waitFor.push(formats.listContents()
				.then(function(result) {
					var l = result.data.length;		
					for (var i=0;i<l;i++) {
						var dat = result.data[i];
						for (var lang in dat.label) {
						if (dat.label[lang].toLowerCase().indexOf(what) >= 0) {					 
							waitFor2.push(lookupCodes({ key : dat.content, content : dat.content, display : dat.label[getLocale()] || dat.label.en || dat.content, format : dat.resourceType })
							.then(add));					
						}
						}
					}
					//console.log(searchresult);
				}));	
			
			
				waitFor.push(formats.listGroups()
				.then(function(result) {
					var l = result.data.length; 		
					for (var i2=0;i2<l;i2++) { 
						var dat = result.data[i2];
						for (var lang in dat.label) {					
						if (dat.label[lang].toLowerCase().indexOf(what) >= 0 || dat.name.toLowerCase().indexOf(what) >= 0) {					 
							addgroup(dat);
							
						}
						} 
					}			
				}));
			
				waitFor.push(apps.getApps({ filename : what}, ["defaultQuery"])
				.then(function(r) {
					if (r.data && r.data.length == 1) {
						var q = r.data[0].defaultQuery;
						if (q.content) {
							
						}
					}
				}));
			
				Promise.all(waitFor).then(() => Promise.all(waitFor2).then(() => resolve(searchresult)));
			});
		},
	
	
	
		search() {
			const { $data, $route, $router } = this, me = this;
			//$data.newentry.choices = [];
			var what = $data.newentry.search.toLowerCase();
			me.doBusy(me.fullTextSearch(what).then((result) => $data.newentry.choices=result));
		},
	
		addNew() {
			const { $data, $route, $router } = this, me = this;
			$data.newentry = { search : "", choices:null };
		},
	
		addContent(content, code) {
			const { $data, $route, $router } = this, me = this;
			var newblock = { display : content.display, isnew : true, owner : "all", app : "all", "public" : "no"  };
			if (content.format) newblock.format = content.format;
			if (content.content) { newblock.content = content.content; }
			if (content.group) { newblock.group = content.group; newblock.system = content.system; }
			if (code) newblock.code = code.system+" "+code.code;
			//$data.blocks.push(newblock);
			
			me.selectBlock(newblock);		
			
			$data.newentry = null;
		},
	
		applyBlock() {
			const { $data, $route, $router } = this, me = this;
			if ($data.currentBlock["public"] == "only" || $data.currentBlock["public"] == "also" ) $data.currentBlock.owner = "all";
			if ($data.currentBlock.format && $data.currentBlock.format.lengh===0) $data.currentBlock.format = undefined;
			if ($data.currentBlock.isnew) {
				$data.blocks.push($data.currentBlock);
				$data.currentBlock.isnew = false;
			}
			
			if (!$data.currentBlock.timeRestriction) {
				$data.currentBlock.timeRestrictionDate = $data.currentBlock.timeRestrictionMode = undefined; 
			}
			if (!$data.currentBlock.dataPeriodRestriction) {
				$data.currentBlock.dataPeriodRestrictionStart = $data.currentBlock.dataPeriodRestrictionEnd = $data.currentBlock.dataPeriodRestrictionMode = undefined; 
			}
			if ($data.currentBlock.app == "self") {$data.currentBlock.appName = $data.target.appname; }
			$data.currentBlock = undefined;
			
			$data.query.json = me.buildAccessQuery();
			$data.query.queryStr = JSON.stringify($data.query.json);
			
		},
	
		selectBlock(block) {
			const { $data, $route, $router } = this, me = this;
			
			$data.currentBlock = block;
			
			$data.currentBlock.flags = {};		
			var ro = $data.resourceOptions[block.format];
			if (ro) {			
				for (let r of ro) { $data.currentBlock.flags[r] = true; }
			}
			
			if ($data.currentBlock.flags.initpublic && block.isnew) block.public = "only";

			if (!$data.currentBlock.flags.notime) {
			$data.timeModes = ["created-after", "updated-after" ];
			} else $data.timeModes = undefined;
			
			$data.dataPeriodModes = [];
			
			if ($data.currentBlock.flags.effective) {
				$data.dataPeriodModes.push("effective");
			}		
			
			$data.newentry = null;
		},
	
		deleteBlock() {
			const { $data, $route, $router } = this, me = this;
			if (!$data.currentBlock.isNew) {
				$data.blocks.splice($data.blocks.indexOf($data.currentBlock), 1);
			}
			$data.currentBlock = undefined;
			
			$data.query.json = me.buildAccessQuery();
			$data.query.queryStr = JSON.stringify($data.query.json);
			
			if ($data.blocks.length === 0) me.addNew();
		},
	
		enableExpertMode() {
			const { $data, $route, $router } = this, me = this;
			$data.expertmode = true;
			$data.currentBlock = undefined;
			$data.newentry = undefined;
		},
	
		expertModeDone() {
			const { $data, $route, $router } = this, me = this;
			try {				  
				$data.query.json = JSON.parse($data.query.queryStr);
				$data.error = null;
				$data.expertmode = false;
				me.doBusy(me.parseAccessQuery($data.query.json).then(function(result) {
					$data.blocks = result;
				}));
			} catch (e) {
				console.log(e);
				$data.error = e.message;
				
				return;
			}
										
		},
	
		addPreselection() {
			const { $data, $route, $router } = this, me = this;
			for (let r of $data.target.askresources) {
				if (r.selected) {
					r.selected = undefined;				
					me.addContent(r);
					me.applyBlock();
				}
			}
			$data.target.askresources = undefined;
		},
	
		saveExit() {
			const { $data, $route, $router } = this, me = this;
		//$data.query.json = buildAccessQuery();
			if ($data.mode == "study") {
				var data = { recordQuery : $data.query.json };
				me.doAction("update", server.put(jsRoutes.controllers.research.Studies.update($route.query.studyId).url, data)
				.then(function(data) { 				
						me.cancel();				    
				})); 
			} else if ($data.mode == "app") {
				$data.app.defaultQuery = $data.query.json;
				me.doAction('submit', apps.updatePlugin($data.app))
				.then(function() { 
					$router.push({ path : './manageapp', query : { appId : $route.query.appId } }); 
				});
			}
		},
	
		cancel() {
			const { $data, $route, $router } = this, me = this;
			if ($data.mode == "study") {
				$router.push({ path : './study.rules', query : { studyId : $route.query.studyId } });				
			} else if ($data.mode == "app") {
				$router.push({ path : './manageapp', query : { appId : $route.query.appId } }); 				
			}
		},
	
		cancelsearch() {
			const { $data, $route, $router } = this, me = this;
			$data.newentry = null;
		},

		parseAccessQuery(query, outerquery, rarray) {
			const { $data, $route, $router } = this, me = this;
			return new Promise((resolve, reject) => {
				let waitFor = [];
				var ac = function(path) {
					if (query[path] !== undefined) return query[path];
					if (outerquery && outerquery[path] !== undefined) return outerquery[path];
					return undefined;
				};
				var unwrap = function(arr, field) {
					var out = [];
					for (let elem of arr) {
						if (elem[field]) {
							if (Array.isArray(elem[field])) {						
								if (elem[field].length == 1) {
									var copy = JSON.parse(JSON.stringify(elem));
									copy.display = "x";
									copy[field] = copy[field][0];
									out.push(copy);
								} else {
									for (let v of elem[field]) {
										var copy1 = JSON.parse(JSON.stringify(elem));
										copy1.display = "y";
										copy1[field] = v;
										out.push(copy1);
									}
								}
							} else out.push(elem);
						} else out.push(elem);
					}
					return out;
				};
				var noarray = function(a) {
					if (Array.isArray(a) && a.length) return a[0];
					return a;
				};
				
				var result = rarray || [];
				
				if (query.$or) {
					for (var i = 0;i<query.$or.length;i++) waitFor.push(me.parseAccessQuery(query.$or[i], query, result));
				} else {
				
					var nblock = {};
					if (ac("format")) nblock.format = ac("format");
					if (ac("content")) nblock.content = ac("content");
					if (ac("code")) nblock.code = ac("code");		
					if (ac("group")) nblock.group = ac("group");
					if (ac("group-system")) nblock.system = ac("group-system");
					nblock["public"] = ac("public") || "no";
					if (ac("created-after")) {
						nblock.timeRestriction = true;
						nblock.timeRestrictionMode = "created-after";
						nblock.timeRestrictionDate = new Date(ac("created-after"));
					}
					if (ac("updated-after")) {
						nblock.timeRestriction = true;
						nblock.timeRestrictionMode = "updated-after";
						nblock.timeRestrictionDate = new Date(ac("updated-after"));
					}
					if (ac("data")) {
						var p = ac("data");
						if (p["effectiveDateTime|effectivePeriod.start|null"]) {
							nblock.dataPeriodRestriction = true;
							nblock.dataPeriodRestrictionMode = "effective";
							var d = p["effectiveDateTime|effectivePeriod.start|null"]["!!!ge"] || p["effectiveDateTime|effectivePeriod.start|null"].$ge; 
							nblock.dataPeriodRestrictionStart = new Date(d);					
						}
						if (p["effectiveDateTime|effectivePeriod.end|null"]) {
							nblock.dataPeriodRestriction = true;
							nblock.dataPeriodRestrictionMode = "effective";
							var d2 = p["effectiveDateTime|effectivePeriod.end|null"]["!!!lt"] || p["effectiveDateTime|effectivePeriod.end|null"].$lt;
							nblock.dataPeriodRestrictionEnd = new Date(d2);					
						}
						if (!nblock.dataPeriodRestriction) {
							nblock.customFilter = true;
							nblock.customFilterValue = JSON.stringify(p);
						}
					}
					if (ac("app")) {
						nblock.app = ac("app");
					}
					if (ac("observer")) {
						nblock.observer = noarray(ac("observer"));
					}
					if (ac("category")) {
						nblock.category = noarray(ac("category"));
					}
					if (ac("owner")) {
						nblock.owner = noarray(ac("owner"));
					}
				
					for (let r of unwrap(unwrap(unwrap(unwrap(unwrap([ nblock ],"group"),"code"),"content"),"app"),"format")) {
						(function(r) {
						if (!r.app) r.app = "all";
						if (r.app == $data.target.appname) { r.app = "self";r.appName = $data.target.appname; }
						else if (r.app !== "all") { r.appName = r.app; r.app = "other"; }
						if (!r.owner) r.owner = "all";
						if (r.content) {
							r.display="test";
							waitFor.push(labels.getContentLabel(getLocale(), r.content).then(function(v) { r.display = v; }));
						} else if (r.group) {
							waitFor.push(labels.getGroupLabel(getLocale(), r["group-system"] || "v1", r.group).then(function(v) { r.display = v; }));
						} else if (r.format) {
							r.display = r.format;
						}				
						if (r.content || r.group || r.code || r.format) { result.push(r); } 
						})(r);
					}
				}
				Promise.all(waitFor).then(() => resolve(result));
			});
		}
    },

    created() {
        const { $data, $route, $router } = this, me = this;
       	session.currentUser.then(function() { me.reload(); });	
    }
}
</script>