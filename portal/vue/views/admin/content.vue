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
				
		<p v-t="'content.intro'"></p>
		<div class="row">
		<div class="col-sm-8 mt-1">
		<input type="text" id="queryadd" name="queryadd" class="form-control" v-validate v-model="newentry.search">
		</div><div class="col-sm-4 mt-1">
		<button class="btn btn-default space" :disabled="action!=null" @click="search()" v-t="'common.search_btn'"></button>
		<button class="btn btn-default space" :disabled="action!=null" @click="createNew()" v-t="'content.createnew_btn'"></button>
		<button class="btn btn-default space" :disabled="action!=null" @click="createGroup()" v-t="'content.creategroup_btn'"></button>
		</div>
		</div>
		<div class="extraspace"></div>
		<p v-t="'content.make_selection'" v-if="newentry.choices"></p>
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
		<hr v-show="currentCode || currentContent || currentGroup">
		<form name="myform"	 ref="myform" novalidate @submit.prevent="saveContent()">			
        <div v-if="currentCode!=null">
			  <form-group label="common.empty" v-if="currentCode.isNew"><strong v-t="'content.new_code'">New code</strong></form-group>
			  <form-group label="common.empty" v-else><strong v-t="'content.edit_code'">Edit code</strong></form-group>
              <form-group name="system" label="content.system">
                <typeahead name="system" class="form-control" v-model="currentCode.system" :suggestions="systems"></typeahead>
              </form-group>
              <form-group name="version" label="content.version">
                <input name="version" class="form-control" type="text" v-validate v-model="currentCode.version" >
              </form-group>
              <form-group name="code" label="content.code">
                <input name="code" class="form-control" type="text" v-validate v-model="currentCode.code" >
              </form-group>
              <form-group name="display" label="content.display">
                 <input name="display" class="form-control" type="text" v-validate v-model="currentCode.display" >
              </form-group>             
        </div>		
        <div v-if="currentContent!=null">
			   <form-group v-if="currentContent.isNew" label="common.empty"><strong v-t="'content.new_content'">Edit Midata Content-Type</strong></form-group>
			   <form-group v-else label="common.empty"><strong v-t="'content.edit_content'">Edit Midata Content-Type</strong></form-group>
               <form-group name="content" label="content.content">
	             <input class="form-control" type="text" v-validate v-model="currentContent.content">
	           </form-group>
               <div v-for="lang in langs" :key="lang">
	           <form-group name="label" :label="'Label '+lang">                   
	               <input class="form-control" type="text" v-validate v-model="currentContent.label[lang]">                   
	           </form-group>
               </div>
	           <form-group name="security" label="content.security">
	             <select class="form-control" type="text" v-validate v-model="currentContent.security">
                     <option value="MEDIUM" v-t="'content.medium'">MEDIUM</option>
                     <option value="HIGH" v-t="'content.high'">HIGH</option>
                 </select>
	           </form-group>
              <form-group name="group" label="content.group">
	             <select class="form-control" v-validate v-model="currentContent.currentGroup">
                     <option v-for="group in groups" :key="group._id" :value="group.name">{{ group.name }}</option>
                  </select>
	           </form-group>
			   <form-group name="defaultCode" label="content.defaultCode">
	              <input class="form-control" type="text" v-validate v-model="currentContent.defaultCode">
				  <button class="btn btn-sm btn-primary" type="button" @click="makeDefault()">Make current code default</button>
	           </form-group>
	           <form-group name="resourceType" label="content.resource_type">
	             <typeahead class="form-control" v-model="currentContent.resourceType" :suggestions="formats" field="format"></typeahead>
	           </form-group>
	          
	          
	          <form-group name="source" label="Source">
	             <input type="text" class="form-control" v-validate v-model="currentContent.source">
	           </form-group>

			   <form-group label="common.empty">
				  <button class="btn btn-primary mr-1" type="submit" v-submit :disabled="action!=null" v-t="'content.save_btn'">Save</button>
				  <button class="btn btn-danger mr-1" type="button" :disabled="action!=null" @click="deleteCode(true)" v-t="'content.delete_content_btn'">Delete Content</button>
				  <button class="btn btn-danger" type="button" :disabled="action!=null" @click="deleteCode(false)" v-t="'content.delete_code_btn'">Delete Code</button>
			   </form-group>
        </div>

		<div v-if="currentGroup">	
			<form-group label="common.empty" v-if="currentGroup.isNew"><strong v-t="'content.new_group'">Edit Group</strong></form-group>	 
			<form-group label="common.empty" v-else><strong v-t="'content.edit_group'">Edit Group</strong></form-group>	 
                <form-group name="name" label="content.group_name">
	             <input class="form-control" type="text" v-validate v-model="currentGroup.name">
	           </form-group>
	           <div v-for="lang in langs" :key="lang">
	           <form-group name="label" :label="'Label '+lang">                   
	               <input class="form-control" type="text" v-validate v-model="currentGroup.label[lang]">                   
	           </form-group>
               </div>
	           <form-group name="parent" label="content.parent_group">
	             <select class="form-control" type="text" v-validate v-model="currentGroup.parent">
				   <option v-for="grp in groups" :key="grp.name" :value="grp.name">{{ grp.label[lang] }}</option>
				 </select>
	           </form-group>
	           <form-group name="system" label="content.group_system">
	             <input class="form-control" type="text" v-validate v-model="currentGroup.system">
	           </form-group>
			   <form-group label="common.empty">
				  <button class="btn btn-primary mr-1" type="submit" v-submit :disabled="action!=null" v-t="'content.save_group_btn'">Save Group</button>
				  <button class="btn btn-danger" type="button" :disabled="action!=null" @click="deleteGroup()" v-t="'content.delete_group_btn'">Delete Group</button>				  
			   </form-group>
		</div>
		</form>
 		     
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
import _ from 'lodash';
import { status, ErrorBox, FormGroup, Typeahead } from 'basic-vue3-components';

var lookupCodes = function(entry) {
		entry.codes = [];
		return formats.searchCodes({ content : entry.content },["_id", "code","system","version","display","content"])
		.then(function(result) {			
		    entry.codes = [];
			for (var i=0;i<result.data.length;i++) {
				entry.codes.push(result.data[i]);
			}
			return entry;
		});
	};
	
	var lookupContent = function(name) {
		return formats.searchContents({ content : name }, ["content", "defaultCode", "security","label", "resourceType", "comment", "source"])
		.then(function(result) {
			if (result.data.length == 1) return { key : result.data[0].content, format : result.data[0].resourceType, content : result.data[0].content, display : result.data[0].label[getLocale()] || result.data[0].label.en || result.data[0].content };
		});
	};
	

	
	

export default {
    
    data: () => ({
	   newentry : { search : "", choices:null },
	   groupSystem : "v1",
       currentCode : null,
       currentContent : null,
       currentGroup : null,
       langs : ["en","de","fr","it"],
	   lang : "en",
       groups : [],
       formats : [],
       systems : ["http://loinc.org","http://snomed.info/sct","http://hl7.org/fhir/sid/icd-10","http://midata.coop"]
	}),				

	components : { ErrorBox, Panel, FormGroup, Typeahead },

    mixins : [ status ],

    methods : {
        getTitle() {
            const { $data, $t } = this;
            return "Content Editor";
            
        },

		reload() {
			const { $data, $route, $router } = this, me = this;
			
			$data.newentry.choices = null;
			$data.currentCode = null;
			$data.currentContent = null;
			$data.currentGroup = null;
            me.doBusy(formats.listGroups().then((result) => $data.groups = _.orderBy(result.data, ["name"], ["asc"])));
            me.doBusy(formats.listFormats().then((result) => $data.formats = _.orderBy(result.data, ["format"], ["asc"])));
		},
	
			
		resourceName(format) {
			const { $data, $route, $router } = this, me = this;
			if (format.startsWith("fhir/")) return format.substr(5);
			return format;
		},

        addContent(content, code) {
            const { $data } = this, me = this;

			if (content.group) {
				formats.listGroups().then((result) => {
					$data.groups = _.orderBy(result.data, ["name"], ["asc"]);
					$data.currentGroup = me.getGroupByGroupname(content.system, content.group);
					$data.currentContent = null;
					$data.currentCode = null;
				});				
			} else {
				$data.currentGroup = null;
				formats.searchContents({ content : content.content }, ["_id", "content", "defaultCode", "security","label", "resourceType", "comment", "source"])
				.then((c) => { 
					
					let cnt = c.data[0];
					cnt.oldGroup = (me.getGroup($data.groupSystem, cnt.content) || {}).name;
					cnt.currentGroup = cnt.oldGroup;
					cnt.oldName = cnt.content;
					$data.currentContent = cnt; 
				});

				if (code) {
				$data.currentCode = code;
				} else $data.currentCode = { system : "", version : "", code:"", display : "", content:content.content, isNew : true };                        
			}
        },

        createNew() {
            const { $data } = this;
            $data.currentContent = { content : "", defaultCode : "", security : "MEDIUM", label : {}, "resourceType" : "", isNew : true };
            $data.currentCode = { system : "", version : "", code:"", display : "", content:"", isNew : true };                        
        },

		createGroup() {
			const { $data } = this;
			$data.currentContent = null;
			$data.currentCode = null;
			$data.currentGroup = {  system: $data.groupSystem, name : "", label : {}, parent : "", isNew : true };
		},

		getGroup(system, contentName) {
			const { $data } = this;
			for (let grp of $data.groups) {
				if (grp.system == system && grp.contents && grp.contents.indexOf(contentName)>=0) return grp;
			}
			return null;
		},

		makeDefault() {
			const { $data } = this;
			if ($data.currentCode && $data.currentContent) {
				$data.currentContent.defaultCode = $data.currentCode.system+" "+$data.currentCode.code;
			}
		},

		getGroupByGroupname(system, name) {
			const { $data } = this;
			for (let grp of $data.groups) {
				if (grp.system == system && grp.name == name) return grp;
			}
			return null;
		},

		saveContent() {
			const { $data } = this, me = this;

			if ($data.currentCode && $data.currentContent) {
				$data.currentCode.content = $data.currentContent.content;
			}

			function updateCode() {
				if ($data.currentCode) {
					if ($data.currentCode.isNew) {
						return me.doAction("save", formats.createCode($data.currentCode));
					} else {
						return me.doAction("save", formats.updateCode($data.currentCode));
					}
				} else return Promise.resolve();
			}

			function updateContent() {
				if ($data.currentContent) {
					if ($data.currentContent.isNew) {
						return me.doAction("save", formats.createContent($data.currentContent));
					} else {
						return me.doAction("save", formats.updateContent($data.currentContent));
					}
				} else return Promise.resolve();
			}

			function updateGroup() {
				if ($data.currentContent) {
					if ($data.currentContent.oldGroup != $data.currentContent.currentGroup) {
						let oldGroup = null;
						let newGroup = null;
						if ($data.currentContent.oldGroup) {
							oldGroup = me.getGroupByGroupname($data.groupSystem, $data.currentContent.oldGroup);
							if (oldGroup) {
								oldGroup.contents.splice(oldGroup.contents.indexOf($data.currentContent.oldName), 1);
							}
						}
						newGroup = me.getGroupByGroupname($data.groupSystem, $data.currentContent.currentGroup);
						if (newGroup) {
							if (!newGroup.contents) newGroup.contents = [];
							newGroup.contents.push($data.currentContent.content);
						}

						function saveOld() {
							if (oldGroup) return formats.updateGroup(oldGroup); else return Promise.resolve();
						}

						function saveNew() {
							if (newGroup) return formats.updateGroup(newGroup); else return Promise.resolve();
						}
						
						return me.doAction("save", saveOld().then(saveNew));
					}
				}
			}

			function done() {
				$data.currentCode = null;
				$data.currentContent = null;
				$data.currentGroup = null;
				return Promise.resolve();
			}

			function saveGroup() {
				if ($data.currentGroup) {
					if ($data.currentGroup.isNew) {
						return me.doAction("save", formats.createGroup($data.currentGroup));
					} else {
						return me.doAction("save", formats.updateGroup($data.currentGroup));
					}
				} else return Promise.resolve();
			}

			if ($data.currentGroup) {
				me.doAction("save", saveGroup().then(done).then(me.reload));
			} else {
				me.doAction("save", updateCode().then(updateContent).then(updateGroup).then(done));
			}
			
		},

		removeFromGroups(cnt) {
			const { $data } = this, me = this;
			let all = [];
			for (let grp of $data.groups) {
				if (grp.contents && grp.contents.indexOf(cnt) >= 0) {
					grp.contents.splice(grp.contents.indexOf(cnt), 1);
					all.push(me.doAction("delete", formats.updateGroup(grp)));
				}
			}
			return Promise.all(all);
		},

		deleteCode(withContent) {
			const { $data } = this, me = this;

			function deleteCode() {
				if ($data.currentCode && !$data.currentCode.isNew) {
					return me.doAction("delete", formats.deleteCode($data.currentCode));
				} else return Promise.resolve();
			}	

			function deleteContent() {
				if (withContent && $data.currentContent && !$data.currentContent.isNew) {
					return me.removeFromGroups($data.currentContent.oldName)
					.then(() => { formats.deleteContent($data.currentContent)});
				} else return Promise.resolve();
			}

			me.doAction("delete", deleteCode().then(deleteContent).then(me.reload));			
		},

		deleteGroup() {
			const { $data } = this, me = this;
			
			if ($data.currentGroup && !$data.currentGroup.isNew) {
				me.doAction("delete", formats.deleteGroup($data.currentGroup).then(me.reload));				
			}	
			
		},
		
		fullTextSearch(what) {
			const me = this;
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
								
								let grp = me.getGroupByGroupname(dat.children[i3].system, dat.children[i3].name);
								addgrp({ key : "grp "+grp.name, group : grp.name, system : grp.system, display : grp.label[getLocale()] || grp.label.en || grp.name, contents:[] });					    
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
		}
	
		
		
		
		
    },

    created() {
        const { $data, $route, $router } = this, me = this;
       	session.currentUser.then(function() { me.reload(); });	
    }
}
</script>