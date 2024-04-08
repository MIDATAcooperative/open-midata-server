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
        				
		<p v-t="'content.intro'"></p>
		<div class="row">
		<div class="col-sm-8 mt-1">
		<input type="text" id="queryadd" name="queryadd" class="form-control" v-validate v-model="newentry.search">
		</div><div class="col-sm-4 mt-1">
		<div class="btn-group space mb-1">
		  <button class="btn btn-default" :disabled="action!=null" @click="search()" v-t="'common.search_btn'"></button>
		  <button type="button" class="btn btn-default dropdown-toggle dropdown-toggle-split" data-toggle="dropdown" aria-expanded="false">
            <span class="sr-only">Toggle Dropdown</span>
          </button>
          <div class="dropdown-menu">
            <a class="dropdown-item" href="javascript:" @click="searchSpecial('notApproved')" v-t="'content.search_not_approved'">All not approved</a>
            <a class="dropdown-item" href="javascript:" @click="searchSpecial('approved')" v-t="'content.search_approved'">All not approved</a>
            <a class="dropdown-item" href="javascript:" @click="searchSpecial('modified')" v-t="'content.search_modified'">All modified</a>
            <a class="dropdown-item" href="javascript:" @click="searchSpecial('deleted')" v-t="'content.search_deleted'">Recently deleted</a>         
          </div>
        </div>
		
		<button class="btn btn-default space mb-1" :disabled="action!=null" @click="createNew()" v-t="'content.createnew_btn'"></button>
		<button class="btn btn-default space mb-1" :disabled="action!=null" @click="createGroup()" v-t="'content.creategroup_btn'"></button>
		<button class="btn btn-default space mb-1" :disabled="action!=null" @click="exporter()" v-t="'content.export_btn'"></button>
		<button class="btn btn-default space mb-1" :disabled="action!=null" @click="importer()" v-t="'content.import_btn'"></button>
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
		      <span v-else class="text-muted">(Content)</span>
		      <span v-if="choice.approvedByName" class="text-success">✓</span>
		      <span v-else-if="choice.autoAddedByName" class="text-warning">?</span>
		    </td>
		    <td>
		      <div v-for="code in choice.codes" :key="JSON.stringify(code)"><a href="javascript:" @click="addContent(choice, code)">{{ code.system }} {{ code.code }}</a> <span class="text-muted">(Code)</span></div>
		      <div v-for="content in orderDisplay(choice.contents)" :key="JSON.stringify(content)"><a href="javascript:" @click="addContent(content);">{{ content.display }}</a><span v-if="content.content" class="text-muted">(Content)</span><span v-else>(Group)</span></div>
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
	             <input class="form-control" name="content" type="text" v-validate v-model="currentContent.content">
	           </form-group>
               <div v-for="lang in langs" :key="lang">
	           <form-group name="label" :label="'Label '+lang">                   
	               <input class="form-control" type="text" v-validate v-model="currentContent.label[lang]">                   
	           </form-group>
               </div>
               <form-group v-if="currentContent.autoAddedAt" name="autoAddedAt" label="content.autoAddedAt">
                 <p class="form-control-plaintext">{{ $filters.dateTime(currentContent.autoAddedAt) }} {{ $t("common.by") }} {{ currentContent.autoAddedByName }}</p>
               </form-group>
               <form-group v-if="currentContent.approvedAt" name="approvedAt" label="content.approvedAt">
                 <p class="form-control-plaintext">{{ $filters.dateTime(currentContent.approvedAt) }} {{ $t("common.by") }} {{ currentContent.approvedByName }}</p>
               </form-group>
	           <form-group name="security" label="content.security">
	             <select class="form-control" name="security" v-validate v-model="currentContent.security">
                     <option value="MEDIUM" v-t="'content.medium'">MEDIUM</option>
                     <option value="HIGH" v-t="'content.high'">HIGH</option>
                 </select>
	           </form-group>
              <form-group name="group" label="content.group">
	             <select class="form-control" v-validate v-model="currentContent.currentGroup">
                     <option v-for="group in orderedGroups" :key="group.name" :value="group.name">{{ group.label[lang] }}</option>
                  </select>
	           </form-group>
			   <form-group name="defaultCode" label="content.defaultCode">
	              <input class="form-control" name="defaultCode" type="text" v-validate v-model="currentContent.defaultCode">
				  <button class="btn btn-sm btn-primary" type="button" @click="makeDefault()">Make current code default</button>
	           </form-group>
	           <form-group name="resourceType" label="content.resource_type">
	             <typeahead class="form-control" name="resourceType" v-model="currentContent.resourceType" :suggestions="formats" field="format"></typeahead>
	           </form-group>
	          
	          
	          <form-group name="source" label="Source">
	             <input type="text" class="form-control" name="source" v-validate v-model="currentContent.source">
	           </form-group>

			   <form-group label="common.empty">
				  <button class="btn btn-primary mr-1 mb-1" type="submit" v-submit :disabled="action!=null" v-t="'content.save_btn'">Save</button>
				  <button class="btn btn-danger mr-1 mb-1" type="button" :disabled="action!=null" @click="deleteCode(true)" v-t="'content.delete_content_btn'">Delete Content</button>
				  <button class="btn btn-danger mb-1" type="button" :disabled="action!=null" @click="deleteCode(false)" v-t="'content.delete_code_btn'">Delete Code</button>
			   </form-group>
        </div>

		<div v-if="currentGroup">	
			<form-group label="common.empty" v-if="currentGroup.isNew"><strong v-t="'content.new_group'">Edit Group</strong></form-group>	 
			<form-group label="common.empty" v-else><strong v-t="'content.edit_group'">Edit Group</strong></form-group>	 
                <form-group name="name" label="content.group_name">
	             <input class="form-control" name="name" type="text" v-validate v-model="currentGroup.name">
	           </form-group>
	           <div v-for="lang in langs" :key="lang">
	           <form-group name="label" :label="'Label '+lang">                   
	               <input class="form-control" type="text" name="label" v-validate v-model="currentGroup.label[lang]">                   
	           </form-group>
               </div>
	           <form-group name="parent" label="content.parent_group">
	             <select class="form-control" v-validate v-model="currentGroup.parent">
				   <option v-for="grp in orderedGroups" :key="grp.name" :value="grp.name">{{ grp.label[lang] }}</option>
				 </select>
	           </form-group>
	           <form-group name="system" label="content.group_system">
	             <input class="form-control" type="text" name="system" v-validate v-model="currentGroup.system">
	           </form-group>
			   <form-group label="common.empty">
				  <button class="btn btn-primary mr-1 mb-1" type="submit" v-submit :disabled="action!=null" v-t="'content.save_group_btn'">Save Group</button>
				  <button class="btn btn-danger mb-1" type="button" :disabled="action!=null" @click="deleteGroup()" v-t="'content.delete_group_btn'">Delete Group</button>				  
			   </form-group>
		</div>
		</form>
		
		<error-box :error="error"></error-box>
 		     
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
import ENV from 'config';
import labels from 'services/labels.js';
import _ from 'lodash';
import { status, ErrorBox, FormGroup, Typeahead } from 'basic-vue3-components';

var lookupCodes = function(entry) {
		entry.codes = [];
		return formats.searchCodes({ content : entry.content },["_id", "code","system","version","display","content"])
		.then(function(result) {			
		    let codes = [];		    
			for (var i=0;i<result.data.length;i++) {			    
				codes.push(result.data[i]);
			}
			entry.codes = _.orderBy(codes, ["system", "code"], ["asc", "asc"]);
			return entry;
		});
	};
	
	var lookupContent = function(name) {
		return formats.searchContents({ content : name }, ["content", "defaultCode", "security","label", "resourceType", "comment", "source", "autoAddedBy", "autoAddedAt", "approvedBy", "approvedAt"])
		.then(function(result) {
			if (result.data.length == 1) return { key : result.data[0].content, format : result.data[0].resourceType, autoAddedByName : result.data[0].autoAddedByName, autoAddedAt : result.data[0].autoAddedAt, approvedByName : result.data[0].approvedByName, approvedAt : result.data[0].approvedAt,  content : result.data[0].content, display : result.data[0].label[getLocale()] || result.data[0].label.en || result.data[0].content };
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
	
	computed: {
       orderedGroups: function () {         
         return _.orderBy(this.$data.groups, (x) => x.label[this.$data.lang]);
       }
    },		

	components : { ErrorBox, Panel, FormGroup, Typeahead },

    mixins : [ status ],

    methods : {
    
        orderDisplay(inp) {
          return _.orderBy(inp, ["display"],["asc"]);
        },
        
        getTitle() {
            const { $data, $t } = this;
            return "Content Editor";
            
        },
        
        reloadDelayed() {
           setTimeout(() => this.reload(), 1000);
        },

		reload() {
			const { $data, $route, $router } = this, me = this;
			
			$data.newentry.choices = null;
			$data.currentCode = null;
			$data.currentContent = null;
			$data.currentGroup = null;
            me.doBusy(formats.listGroups().then((result) => { $data.groups = result.data; } ));
            me.doBusy(formats.listFormats().then((result) => { $data.formats = _.orderBy(result.data, ["format"], ["asc"]) }));
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
				formats.searchContents({ content : content.content }, ["_id", "content", "defaultCode", "security","label", "resourceType", "comment", "source", "autoAddedAt", "autoAddedBy", "approvedAt","approvedBy"])
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
            $data.currentGroup = null;                        
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
				if (grp.system == system && grp.contents && grp.contents.indexOf(contentName)>=0) {				  
				  return grp;
				}
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
				if ($data.currentCode && $data.currentCode.code) {
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
							oldGroup = {
								system : $data.groupSystem,
								name : $data.currentContent.oldGroup,
								content : $data.currentContent.content,
								deleted : true
							};							
						}
						newGroup = {
							system : $data.groupSystem,
							name : $data.currentContent.currentGroup,
							content : $data.currentContent.content,								
						};
							
						function saveOld() {
							if (oldGroup) return formats.updateGroupContent(oldGroup); else return Promise.resolve();
						}

						function saveNew() {
							if (newGroup) return formats.updateGroupContent(newGroup); else return Promise.resolve();
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
				me.doAction("save", saveGroup().then(done).then(me.reloadDelayed));
			} else {
				me.doAction("save", updateCode().then(updateContent).then(updateGroup).then(done).then(me.reloadDelayed));
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
					return formats.deleteContent($data.currentContent);
				} else return Promise.resolve();
			}

			me.doAction("delete", deleteCode().then(deleteContent).then(me.reloadDelayed));			
		},

		deleteGroup() {
			const { $data } = this, me = this;
			
			if ($data.currentGroup && !$data.currentGroup.isNew) {
				me.doAction("delete", formats.deleteGroup($data.currentGroup).then(me.reloadDelayed));				
			}	
			
		},
		
		fullTextSearch(what) {
			const me = this;
			return new Promise((resolve, reject) => {
				let waitFor = [];
				let waitFor2 = [];
				let searchresult = [];
				let already = {};
				
				let add = function(entry) {				    				    
					if (!already[entry.key]) { searchresult.push(entry); already[entry.key] = entry; return true; }
					return false;
				};
				
				let lookup = function(content) {
					return lookupCodes(content);		      
				};
				
				let addgroup = function(dat) {
					let grp = { key : "grp_"+dat.name, group : dat.name, system : dat.system, display : dat.label[getLocale()] || dat.label.en || dat.name, contents:[] };
					let addgrp = function(what) {				
						grp.contents.push(what); 
					};
					let recproc = function(dat) {
						if (dat.contents && dat.contents.length > 0) {
							for (let i2=0;i2<dat.contents.length;i2++) {
								waitFor2.push(lookupContent(dat.contents[i2]).then(addgrp));
							}
						}
						if (dat.children) {
							for (let i3=0;i3<dat.children.length;i3++) {
								
								let grp = me.getGroupByGroupname(dat.children[i3].system, dat.children[i3].name);
								addgrp({ key : "grp_"+grp.name, group : grp.name, system : grp.system, display : grp.label[getLocale()] || grp.label.en || grp.name, contents:[] });					    
							}
						}
					};
					//if (dat.contents && dat.contents.length == 1 && (!dat.children || dat.children.length==0)) return;
					if (add(grp)) {
						recproc(dat);
					}
				};
			
				waitFor.push(formats.listCodes()
				.then(function(result) {
					let l = result.data.length;		
					for (let i=0;i<l;i++) {
						let dat = result.data[i];						
						if (dat.code.toLowerCase() == what) {
							waitFor2.push(lookupContent(dat.content)
							.then(lookup).then(add));
						}
					}					
				}));
			
				waitFor.push(formats.listContents()
				.then(function(result) {
					let l = result.data.length;		
					for (let i=0;i<l;i++) {
						let dat = result.data[i];
						for (let lang in dat.label) {
						if (dat.label[lang].toLowerCase().indexOf(what) >= 0) {					 
							waitFor2.push(lookupCodes({ key : dat.content, content : dat.content, display : dat.label[getLocale()] || dat.label.en || dat.content, format : dat.resourceType, autoAddedByName:dat.autoAddedByName, autoAddedAt:dat.autoAddedAt, approvedByName:dat.approvedByName, approvedAt:dat.approvedAt })
							.then(add));					
						}
						}
					}
				
				}));	
			
			
				waitFor.push(formats.listGroups()
				.then(function(result) {
					let l = result.data.length; 		
					for (let i2=0;i2<l;i2++) { 
						let dat = result.data[i2];
						for (let lang in dat.label) {	
									
						if (dat.label[lang].toLowerCase().indexOf(what) >= 0 || dat.name.toLowerCase().indexOf(what) >= 0) {
						    console.log(dat);							    					
							addgroup(dat);
							
						}
						} 
					}			
				}));
			
				waitFor.push(apps.getApps({ filename : what}, ["defaultQuery"])
				.then(function(r) {
					if (r.data && r.data.length == 1) {
						let q = r.data[0].defaultQuery;
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
			let what = $data.newentry.search.toLowerCase();
			me.doBusy(me.fullTextSearch(what).then((result) => $data.newentry.choices=_.orderBy(result, ["display"],["asc"])));
		},
		
		searchSpecial(mode) {
            const { $data, $route, $router } = this, me = this;
            
            let searchresult = [];
            let waitFor2 = [];
            let already = {};
            let add = function(entry) {                                     
                    if (!already[entry.key]) { searchresult.push(entry); already[entry.key] = entry; return true; }
                    return false;
             };
            
            
            me.doBusy(formats.listContentsSpecial(mode)
            .then(function(result) {
               let l = result.data.length;     
               for (let i=0;i<l;i++) {
                 let dat = result.data[i];                  
                 waitFor2.push(lookupCodes({ key : dat.content, content : dat.content, display : dat.label[getLocale()] || dat.label.en || dat.content, format : dat.resourceType, autoAddedByName: dat.autoAddedByName, autoAddedAt:dat.autoAddedAt, approvedAt: dat.approvedAt, approvedByName: dat.approvedByName })
                 .then(add));                    
               }        
            }).then(() => Promise.all(waitFor2).then(() => $data.newentry.choices=_.orderBy(searchresult, ["display"],["asc"]))));
            
        },

        exporter() {
            this.doAction("download", server.token())
		    .then(function(response) {
		        document.location.href = ENV.apiurl + jsRoutes.controllers.FormatAPI.exportChanges().url + "?token=" + encodeURIComponent(response.data.token);
		    });
		},

		importer() {
			this.$router.push({ path : './importcontent'});
		}
	
		
		
		
		
    },

    created() {
        const { $data, $route, $router } = this, me = this;
       	session.currentUser.then(function() { me.reload(); });	
    }
}
</script>