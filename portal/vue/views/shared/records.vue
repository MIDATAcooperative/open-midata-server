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
    <panel :title="$t('records.title')" :busy="isBusy">
		
	    <div style="margin:10px">
		  
		    <div class="alert alert-info" v-if="consent">
		        <span v-t="'records.share_instructions1'"></span><strong>{{ consent.name }}</strong>.<br>
		        <span v-t="'records.share_instructions2'"></span><strong>{{ consent.authorized.length }}</strong> <span v-t="'records.share_instructions3'"></span><br><br>	
		        <router-link class="btn btn-default btn-sm" :to="{ path : './editconsent', query: { consentId : selectedAps._id } }" v-t="'common.done_btn'">Done</router-link>	     
		    </div>
		  
		    <div class="alert alert-info" v-if="allowDelete">
		        <p><span v-t="'records.delete_instructions1'"></span> <b v-t="'records.delete_instructions2'">delete</b> <span v-t="'records.delete_instructions3'"></span> <b v-t="'records.delete_instructions4'"></b> <span v-t="'records.delete_instructions5'"></span></p>
		        <p v-t="'records.delete_instructions6'"></p>	     
		    </div>

            <error-box :error="error"></error-box>
		  		 
		  	<div class="float-right col-sm-4" v-if="displayAps.owner && compare!=null">
			    <label for="selectedAps" v-t="'records.shared_with'"></label>
			    <select class="form-control" id="selectedAps" v-model="selectedApsId" @change="setSelectedAps()">
                    <optgroup v-for="(list,label) in compareGrouped" :label="$t('enum.consenttype.'+label)" :key="label">
                      <option v-for="c in list" :key="c._id" :value="c._id">{{ $t(c.i18n, { name : c.name })}}</option>
                    </optgroup>
                </select>
	        </div>	
	        <div class="float-right col-sm-4" v-if="compare==null && selectedAps!=null && selectedType=='spaces'">
			    <label for="selectedAps" v-t="'records.records_used_for_app'"></label>	
			    <router-link class="btn btn-default" :to="{ path : './spaces', query : { spaceId : selectedAps._id }}" v-t="'records.back_to_plugin'"></router-link>
	        </div>	  
								        
	        <div class="col-sm-4">
	            <label for="owner" @dblclick="showDebug()" v-t="'records.show'"></label>
	            <select class="form-control" id="owner" v-model="displayAps" @change="selectSet()">
                    <optgroup v-for="(list,label) in availableApsGrouped" :label="$t('records.type_'+label)" :key="label">
                      <option v-for="c in list" :key="c._id" :value="c">{{ $t(c.i18n, { name : c.name })}}</option>
                    </optgroup>
                </select>
	        </div>			
       
        
            <div class="margin-top">
                <div class="col-sm-12">
                    <form class="form">
		                <div class="form-check">
		                    <label class="form-check-label">
                                <input class="form-check-input" type="radio" name="treetype" value="group" v-model="treeMode" @click="setTreeMode('group');"> <span class="margin-left" v-t="'records.by_group'"></span> 
                            </label>
                        </div>
                        <div class="form-check">
                            <label class="form-check-label">
                                <input class="form-check-input" type="radio" name="treetype" value="plugin" v-model="treeMode" @click="setTreeMode('plugin');"> <span class="margin-left" v-t="'records.by_plugin'"></span>
                            </label>
                        </div>
                    </form>        
                </div>
            </div>
        
            <div class="alert alert-warning" v-if="tooManyConsents">
		        <p v-t="'records.too_many_consents'"></p>
		    </div>
        </div>
             		        
        <div class="tree" v-for="data in tree" :key="data._id">
            <record-tree-node :selectedAps="selectedAps" :action="action" :data="data" :sharing="sharing" @share="shareGroup" @unshare="unshareGroup"  @show="showRecords" @deleteGroup="deleteGroup"></record-tree-node>
        </div>
			
		<div class="panel-body" v-if="tree.length === 0" v-t="'records.empty'"></div>

        <modal :title="((selectedData || {}).fullLabel || {}).fullLabel" :open="selectedData && selectedData.allRecords" @close="selectedData=null" full-width="true" id="details">
	        
                    <pagination v-model="selectedData.allRecords" search="search"></pagination>
                    

                    <ul class="list-group nospaceafter" v-if="selectedData.allRecords.filtered.length > 0">
				        <li class="list-group-item" v-for="record in selectedData.allRecords.filtered" :key="record._id" :class="{ 'list-group-item-success' : ( isShared(record)) }">
                            <span class="badge badge-info">{{ $filters.date(record.created) }}</span>&nbsp;
                            <span v-if="record.owner != userId" class="badge badge-info">{{ record.ownerName }}</span>&nbsp;
                            <a href="javascript:;" @click="showDetails(record)">{{record.name}}</a>
                            <button v-show="(allowDelete && isOwnRecord(record)) || (allowDeletePublic && isPublicRecord(record))" @dblclick="deleteRecord(record, selectedData)" class="btn btn-danger btn-sm" v-t="'records.delete'"></button>
                            <div class="float-right" v-if="selectedAps!=null">					   
                                <button class="btn btn-sm btn-primary" :disabled="action!=null" @click="unshare(record, selectedData);" v-show="isShared(record)">
                                    <span class="fas fa-picture"></span><span v-t="'records.unshare'"></span>
                                </button>
                                <button class="btn btn-sm btn-primary" :disabled="action!=null" v-show="!isShared(record)" :class="{'disabled': !isOwnRecord(record)}" @click="share(record, selectedData);">
                                    <span class="fas fa-share"></span><span v-t="'records.share'"></span>
                                </button>
                            </div>										
				        </li>
			        </ul>
			        <div class="container" v-if="selectedData.allRecords.filtered.length === 0" v-t="'records.empty'"></div>
           
            <template v-slot:footer>
			    <button class="btn btn-default" @click="selectedData=null" v-t="'records.close_btn'"></button>
            </template>
        </modal>
    </panel>
</template>
<script>
import { status, rl, Modal, ErrorBox } from 'basic-vue3-components';
import server from "services/server"
import Panel from "components/Panel.vue"
import RecordTreeNode from "components/RecordTreeNode.vue"
import dateService from "services/date"
import { getLocale } from "services/lang"
import records from "services/records"
import circles from "services/circles"
import formats from "services/formats"
import apps from "services/apps"
import studies from "services/studies"
import session from "services/session"
import spaces from "services/spaces"
import _ from "lodash"

let contentLabels = {};
let loadLabels = {};
let loadPlugins = {};
let doLoadLabels = false;
let doLoadPlugins = false;
    


	var groups = {};
	var plugins = {};
		
	
	var getOrCreateGroup = function($data, group) {
	   	if (groups[group] != null) return groups[group];
	  
	   	var newgroup = _.filter($data.gi, function(x){  return x.name == group; })[0];
	   
	   	newgroup.children = [];
	   	newgroup.records = [];
	   	newgroup.infoCount = 0;
	   	newgroup.countShared = 0;
	   	newgroup.group = newgroup.name;
	   	newgroup.id = newgroup.name.replace(/[/\-]/g,'_');
	   	
	   	if (newgroup.parent == null || newgroup.parent === "") {
            newgroup.fullLabel = { fullLabel : newgroup.label[$data.lang] || newgroup.name };
            newgroup._parent = null;
	   		$data.tree.push(newgroup);
	   	} else {
            var prt = getOrCreateGroup($data, newgroup.parent);
            newgroup._parent = prt;   
	   		newgroup.fullLabel = { fullLabel : newgroup.label[$data.lang] || newgroup.name };
	   		prt.children.push(newgroup);
	   	}
	   	
	   	groups[group] = newgroup;
	   	return newgroup;
	};
	
	var getOrCreatePlugin = function($data, plugin) {
	   	if (plugins[plugin] != null) return plugins[plugin];
	  
	   	var newplugin = { id : "_"+plugin, plugin:plugin, _parent:$data.tree[0], parent:$data.tree[0] };
	   
	   	newplugin.children = [];
	   	newplugin.contents = {};
	   	newplugin.records = [];
	   	newplugin.infoCount = 0;
	   	newplugin.countShared = 0;	   	
	   		   	
	   	newplugin.fullLabel = { fullLabel : "Label "+newplugin.id };
	   	if (contentLabels[plugin]) {
	   		newplugin.fullLabel.fullLabel = contentLabels[plugin];
	   	} else {
	   		if (plugin) {
		   		loadPlugins[plugin] = newplugin.fullLabel;
		   		doLoadPlugins = true;
	   		}
	   	}
	   	
	   	
	   	$data.tree[0].children.push(newplugin);
	   		   	
	   	plugins[plugin] = newplugin;
	   	return newplugin;
	};
	
	 	
	var getOrCreateFormat = function($data, format, group) {
	   	if (groups["cnt:"+format] != null) return groups["cnt:"+format];
	   
	   	var grp = getOrCreateGroup($data, group);
	   	var newfmt = { name : "cnt:"+format, content:format, type:"group", fullLabel: { fullLabel : "Content: "+format }, parent:group, _parent:grp, children:[], records:[] };
	   	
	   	if (contentLabels[format]) {
	   		newfmt.fullLabel.fullLabel = contentLabels[format];
	   	} else if (loadLabels[format]) {
	   	    newfmt.fullLabel = loadLabels[format];		   	
	   	} else {
	   		loadLabels[format] = newfmt.fullLabel;
	   		doLoadLabels = true;
	   	}
	   	
	   	grp.children.push(newfmt);	   		   	
	   	groups["cnt:"+format] = newfmt;
	   	return newfmt;
	};
	
	var getOrCreatePluginContent = function($data, content, plugin) {
	   	if (plugin.contents[content] != null) return plugin.contents[content];
	   	
	   	var newfmt = { name : "cnt:"+content, type:"group", fullLabel:{ fullLabel : "Content: "+content }, parent:plugin, plugin:plugin.plugin, content:content, children:[], records:[] };
	   	
	   	if (contentLabels[content]) {
	   		newfmt.fullLabel.fullLabel = contentLabels[content];
	   	} else if (loadLabels[content]) {
	   		newfmt.fullLabel = loadLabels[content];
	   	} else {
	   		loadLabels[content] = newfmt.fullLabel;
	   		doLoadLabels = true;
	   	}
	   	
	   	plugin.children.push(newfmt);	   		   	
	   	plugin.contents[content] = newfmt;
	   	return newfmt;
	};
		
	var countRecords = function($data, group) {
		var c = group.infoCount || group.records.length;		
		for (let g of group.children) { c+= countRecords($data, g); }
		group.count = c;
		group.open =  $data.open[group.id] || group.open || (group.parent == null);
		return c;
	};
		
	
	var countShared = function(group) {
		var s = 0;			
		for (let g of group.children) { s+= countShared(g); }
		group.countShared += s;
		return group.countShared;
	};
	
	var resetShared = function(group) {		
		for (let g of group.children) { resetShared(g); }
		group.countShared = 0;		
    };
    
    var addToQuery = function($data, type, item) {
		if (!$data.sharing.query) $data.sharing.query = {};
		if (!$data.sharing.query[type]) $data.sharing.query[type] = [];
		
		if ($data.sharing.query[type].indexOf(item) < 0) {
		  $data.sharing.query[type].push(item);
		  return true;
		}
		
		return false;
	};
	
	var removeFromQuery = function($data, type, item) {
		if (!$data.sharing.query) return false;
		if (!$data.sharing.query[type]) return false;
		var idx = $data.sharing.query[type].indexOf(item);
		if (idx < 0) return false;		
		$data.sharing.query[type].splice(idx, 1);
		if ($data.sharing.query[type].length === 0 && type != "group") $data.sharing.query[type] = undefined;		
		return true;
	};
	

export default {
    data: () => ({
        userId : null,
	    lang : getLocale(),	
	    records : [],
	    infos : [],
	    tree : [],
	    compare : null,
        selectedAps : null,
        selectedApsId : null,
        displayAps : { owner : "" },
        allowDelete : false,
	    allowDeletePublic : false,
	    open : {},
	    treeMode : "group",
        tooManyConsents : false,
        sharing : null,
        gi : null,
        selectedData : null
	}),	

    computed : {
        compareGrouped() {
            return _.chain(this.compare).orderBy(['name'],['asc']).groupBy("type").value();
        },
        availableApsGrouped() {
            return _.chain(this.availableAps).orderBy(['name'],['asc']).groupBy("type").value();
        }
    },
    
    components: { Panel, RecordTreeNode, ErrorBox, Modal },

    mixins : [ status, rl ],

    methods : {
        init() {
            const { $data, $route } = this, me = this;
      
	        session.currentUser
	        .then(function(userId) {		
                $data.userId = userId;
                $data.availableAps = [{ i18n : "records.my_data" , name : "My Data", aps:userId, owner : "self", type : "global"  }, { i18n:"records.all_data", name : "All Data", aps:userId, owner : "all", type : "global" }, { i18n:"records.public_data", name : "Public Data", aps:userId, "public" : "only", type : "global" }];
                $data.displayAps = $data.availableAps[0];
                var n = "RecordsCtrl_"+$route.name;
                //session.load(n, $scope, ["open", "treeMode"]);
                
                if ($route.query.selected != null) {	
                    var selectedType = $route.query.selectedType;
                    var selected = $route.query.selected;
                    $data.selectedType = selectedType;
                    $data.selectedAps = { "_id" : selected , type : selectedType };
                    me.explainPreselection();
                }
                
                me.getAvailableSets(userId);
                me.loadGroups();
                var what = ($route.query.selected != null) ? null : "self";
                me.getInfos(userId, what)
                .then(function() {
                
                    if ($route.query.selected != null) {										 
                    $data.displayAps = $data.availableAps[1];				  
                    $data.compare = null;
                    me.loadSharingDetails();				 
                    } else me.loadShared(userId); 
                });
            });
		},
	
	    setTreeMode(mode) {
            const { $data, $route } = this, me = this;
				
            //$data.records = [];
            $data.infos = [];
            $data.tree = [ ];
            $data.compare = null;
            $data.selectedAps = null;
            $data.selectedApsId = null;
            
            
            $data.treeMode = mode;
            
            me.doBusy(me.getInfos($data.displayAps.aps, $data.displayAps.owner, $data.displayAps.study, $data.displayAps["public"]))
            .then(function() {				
                me.loadSharingDetails();				 			
            });
            
	    },
	    getRecords(userId, owner, group, study, public_mode, groupObj) {
            const { $data, $route, $filters } = this, me = this;
		
            var properties = {};
            if (owner) properties.owner = owner;
            if (study) properties.study = study;
            if (public_mode) properties["public"] = public_mode;
            if (groupObj && groupObj.plugin) {
                properties.app = groupObj.plugin;
                properties.content = groupObj.content;
            }
            else if (group) {
                properties.group = group;
                properties["group-system"] = "v1";
            }
            if ($data.debug) properties.streams = "true";
            properties.limit = 5000;
            return me.doAction("load", records.getRecords(userId, properties, ["id", "owner", "ownerName", "content", "created", "name", "group", "app"])).
            then(function(results) {
                //$data.records = results.data;
                for (let r of results.data) {
                    r.search = r.name+" "+r.ownerName+" "+$filters.date(r.created);
                }
                if (groupObj) groupObj.allRecords = me.process(results.data, { filter : { search : "" } });
                if ($data.gi != null) me.prepareRecords(groupObj.allRecords.all);				
            });
	    },
	
	    getInfos(userId, owner, study, public_mode) {
            const { $data, $route } = this, me = this;
            var properties = {};
            if (owner) properties.owner = owner;
            if (study) properties.study = study;
            if (public_mode) properties["public"] = public_mode;
            if ($data.debug) properties.streams = "true";
            return me.doBusy(records.getInfos(userId, properties, $data.treeMode === "plugin" ? "CONTENT_PER_APP" : "CONTENT")).
            then(function(results) {
                $data.tooManyConsents = results.status == 202;
                $data.infos = results.data;
                if ($data.gi != null) me.prepareInfos();				
            });
        },
        
        setOpen(group, open) {
            group.open = open;
            $data.open[group.id] = open;		
	    },
	
	
	    getAvailableSets(userId) {
            const { $data, $route } = this, me = this;
            if ($route.meta.role == "research") {
                
                studies.research.list()
                .then(function(results) {
                    for (let study of results.data) { 
                        $data.availableAps.push({ i18n:"records.study", name:study.name, aps:userId, study : study._id, type : "study" });
                    }
                });
                
            } else {
            
                circles.listConsents({ "member": userId, "status" : ["ACTIVE","FROZEN"] }, ["name","owner", "ownerName", "type"])
                .then(function(results) {
                    
                    for (let circle of results.data) { 
                        $data.availableAps.push({ i18n:"records.shared", name:circle.ownerName, aps:circle._id, type : circle.type.toLowerCase() });
                    }
                });
            }
	    },
	
	    selectSet() {
            const { $data, $route } = this, me = this;
            me.doBusy(me.getInfos($data.displayAps.aps, $data.displayAps.owner, $data.displayAps.study, $data.displayAps["public"])
            .then(function() { me.loadSharingDetails(); }));
            
        },
        
        setSelectedAps() {
            const { $data, $route } = this, me = this;
          
            $data.selectedAps = _.filter($data.compare, (x) => (x._id==$data.selectedApsId))[0];            
           
            me.loadSharingDetails();
        },
	
	    showDebug() {
            const { $data, $route } = this, me = this;
            $data.debug = true;
            me.getInfos($data.displayAps.aps, $data.displayAps.owner, $data.displayAps.study, $data.displayAps["public"])
            .then(function() { me.loadSharingDetails(); });
		
	    },
	
	    addAllGroups() {
            const { $data, $route } = this, me = this;
		    for (let grp of $data.gi) {
			    getOrCreateGroup($data, grp.name);
		    }
	    },
	
	    loadGroups() {
            const { $data, $route } = this, me = this;
            me.doBusy(formats.listGroups()).
            then(function(result) { 
                $data.gi = result.data;			
                
                if ($data.infos.length > 0) me.prepareInfos();	
                //if ($data.records.length > 0) me.prepareRecords();	
            });
	    },
	
	    loadContentLabels() {
            const { $data, $route } = this, me = this;
            me.loadPluginLabels();
            if (!doLoadLabels) return;
            doLoadLabels = false;
            formats.searchContents({ content : Object.keys(loadLabels) },["content","label"]).
            then(function(result) { 
            for (let c of result.data) {
                contentLabels[c.content] = loadLabels[c.content].fullLabel = c.label[$data.lang] || c.label.en;			 
            }
            loadLabels = {};
            
            });
	    },
	
	    loadPluginLabels() {
            const { $data, $route } = this, me = this;
            if (!doLoadPlugins) return;
            doLoadPlugins = false;
                    
            me.doBusy(apps.getApps({"_id" : Object.keys(loadPlugins) },["_id", "name", "i18n"])
            .then(function(result) {
                for (let c of result.data) {
                    var label = c.name;
                    if (c.i18n && c.i18n[$data.lang]) label = c.i18n[$data.lang].name;
                    contentLabels[c._id] = loadPlugins[c._id].fullLabel = label || c.name;				
                }
                loadPlugins = {};
            }));									
	    },
	
	    prepareRecords(records) {
            const { $data, $route } = this, me = this;
            if ($data.treeMode === "plugin") {
                for (let record of records) {
                    var format = record.content;
                    var pluginId = record.app;
                    var plugin = getOrCreatePlugin($data, pluginId);
                    var groupItem = getOrCreatePluginContent($data, format, plugin);
                    groupItem.records.push(record);
                    if (!record.name) record.name="no name";
                }
            } else {
                for (let record of records) {
                    var format = record.content;
                    var group = record.group;
                    var groupItem = getOrCreateFormat($data, format, group);
                    groupItem.records.push(record);
                    if (!record.name) record.name="no name";
                }
            }
            for (let t of $data.tree) { countRecords($data, t); }
        },
        
        prepareInfos() {
            const { $data, $route, $t } = this, me = this;    
            groups = {};
            plugins = {};		
            
            if ($data.treeMode === "plugin") {
                $data.tree = [ { name : "all",  fullLabel:{ fullLabel : "" }, parent:null, children:[], records:[] } ];
                $data.tree[0].fullLabel.fullLabel = $t("records.all");
                for (let info of $data.infos) {		    
                    var plugin = info.apps[0];
                    var pluginItem = getOrCreatePlugin($data, plugin);
                                    
                    pluginItem.open = $data.open[pluginItem.id] || false;
                    
                    var content = info.contents[0];
                    var contentItem = getOrCreatePluginContent($data, content, pluginItem);
                    contentItem.infoCount = info.count;
                    contentItem.loaded = false;
                    contentItem.records = [];
                    contentItem.open = $data.open[contentItem.id] || false;
                    
                }
            } else {
                $data.tree = [];
                me.addAllGroups();
                for (let info of $data.infos) {		    
                    var group = info.groups[0];
                    var groupItem = getOrCreateGroup($data, group);
                    
                    //groupItem.records = [];
                    //groupItem.loaded = false;
                    groupItem.open = $data.open[groupItem.id] || false;
                    
                    var content = info.contents[0];
                    var contentItem = getOrCreateFormat($data, content, group);
                    contentItem.infoCount = info.count;
                    contentItem.loaded = false;
                    contentItem.records = [];
                    contentItem.open = $data.open[contentItem.id] || false;
                    
                }
            }
            for (let t of $data.tree) { countRecords($data, t); }
            me.loadContentLabels();
	    },
	
	    deleteRecord(record, group) {
            const { $data, $route } = this, me = this;
            server.post(jsRoutes.controllers.Records["delete"]().url, { "_id" : record.id }).
            then(function(data) {			
                group.allRecords.all.splice(group.allRecords.all.indexOf(record), 1);			
            });
	    },
	
	    deleteGroup(group) {
            const { $data, $route } = this, me = this;
            var props = {};
            if (group.plugin) props.app = group.plugin;
            if (group.content) props.content = group.content;
            if (group.group) props.group = group.group;
            
            me.doBusy(server.post(jsRoutes.controllers.Records["delete"]().url, props))
            .then(function(data) {
                me.loadGroups();
                me.getInfos($data.userId, "self");
            });
	    },
			
	
	// show record details
	    showDetails(record) {
            const { $data, $router } = this, me = this;
            $data.selectedData = null;
            $router.push({ path : "./recorddetail", query : { recordId : record.id } });
        },
        
        showRecords(group) {
            const { $data  } = this, me = this;
            $data.selectedData = group;
            if (!group.loaded) {
                group.loaded = true;
                me.getRecords($data.displayAps.aps, $data.displayAps.owner, group.name, $data.displayAps.study, $data.displayAps["public"], group);
            }
            //$("#recdetailmodal").modal('show');
	    },
	
	// check whether the user is the owner of the record
	    isOwnRecord(record) {
            const { $data, $route } = this, me = this;
		    return $data.userId === record.owner;
	    },
	
	    isPublicRecord(record) {
            const { $data, $route } = this, me = this;
		    return record.ownerName=="-";
	    },
	
	    loadShared() {
            const { $data, $route } = this, me = this;
            if ($data.circles == null) {
                circles.listConsents({ owner : true }, ["name", "type", "status"])
                .then(function(results) {
                    
                    $data.loadingSharing = false;				
                    $data.compare = [];
                    for (let entry of results.data) { 
                        entry.i18n = "records.just_name";
                        $data.compare.push(entry);
                    }
                                                
                });		
            } 
	    },
	
	    loadSharingDetails() {
            const { $data, $route } = this, me = this;
            if ($data.selectedAps == null) return;
            
            me.doBusy(server.get(jsRoutes.controllers.Records.getSharingDetails($data.selectedAps._id).url)).
            then(function(results) {
                let sharing = results.data;
                
                sharing.ids = {};
                if (sharing.query) {
                    if (sharing.query["group-exclude"] && !Array.isArray(sharing.query["group-exclude"])) { sharing.query["group-exclude"] = [ sharing.query["group-exclude"] ]; }
                    if (sharing.query.group && !Array.isArray(sharing.query.group)) { sharing.query.group = [ sharing.query.group ]; }
                }
                for (let r of sharing.records)  { sharing.ids[r] = true; }
                for (let t of $data.tree)  { resetShared(t); }
                for (let s of sharing.summary)  {
                    getOrCreateFormat($data, s.contents[0],s.groups[0]).countShared = s.count;
                }
                for (let t of $data.tree) { countShared(t); }
                
                me.loadContentLabels();
             
                $data.sharing = sharing;
            });
	    },
	
	    isShared(record) {
            const { $data, $route } = this, me = this;
            if (record == null) return;
            if (!$data.sharing) return;
            return $data.sharing.ids[record._id];
	    },
	
	    isSharedGroup(group) {
            const { $data, $route } = this, me = this;
            if ($data.treeMode === "plugin") return false;
            if (group.parent != null && !groups[group.parent]) return false;
            
            group.parentShared = (group.parent != null && groups[group.parent].shared);
            group.parentExcluded = (group.parent != null && groups[group.parent].excluded);
            var excluded = $data.sharing && 
                $data.sharing.query &&
                $data.sharing.query["group-exclude"] && 	       
                $data.sharing.query["group-exclude"].indexOf(group.name) >= 0;
            group.excluded = group.parentExcluded || excluded;
            
            if (!$data.sharing || !$data.sharing.query || !$data.sharing.query.group) {
                group.shared = group.parentShared && !excluded;
                return group.shared;
            }
            
            var r = group.shared = ($data.sharing.query.group.indexOf(group.name) >= 0 || group.parentShared) && !excluded; 
            return r;
	    },
	
        share(record, group) {
            const { $data, $route } = this, me = this;
            removeFromQuery($data, "exclude-ids", record._id);
            me.doBusy(records.share($data.selectedAps._id, record._id, $data.selectedAps.type, $data.sharing.query));
            $data.sharing.ids[record._id] = true;
            
            while (group != null) {
            
                group.countShared++;
                group = groups[group.parent];
            }
	    },
		
        unshare(record, group) {
            const { $data, $route } = this, me = this;
		    if (group.shared) addToQuery($data, "exclude-ids", record._id);
            me.doBusy(records.unshare($data.selectedAps._id, record._id, $data.selectedAps.type, $data.sharing.query));
            $data.sharing.ids[record._id] = false;
            
            while (group != null) {
                group.countShared--;
                group = groups[group.parent];
            }
	    },
	
	    shareGroup(group) {
            const { $data, $route } = this, me = this;
            var type =  "group";
            if (!removeFromQuery($data, "group-exclude", group.name)) {
            addToQuery($data, "group", group.name);
            }
            
            var unselect = function(group) {			
                for (let c of group.children) {
                    removeFromQuery($data, "group", c.name);
                    removeFromQuery($data, "group-exclude", c.name);
                    unselect(c);
                }
            };
            unselect(group);
            
            me.doBusy(records.share($data.selectedAps._id, null, $data.selectedAps.type, $data.sharing.query)).
            then(function() { me.loadSharingDetails(); });
	    },
	
	    unshareGroup(group) {
            const { $data, $route } = this, me = this;
            var type =  "group";
            
            if (!removeFromQuery($data, "group", group.name)) {
                addToQuery($data, "group-exclude", group.name);
            }		
            var recs = [];
            
            var unselect = function(group) {
                for (let r of group.records) { recs.push(r._id); }
                for (let c of group.children) {
                    removeFromQuery($data, "group", c.name);
                    removeFromQuery($data, "group-exclude", c.name);
                    unselect(c);
                }
            };
            unselect(group);
            
        
            me.doBusy(records.unshare($data.selectedAps._id, recs, $data.selectedAps.type, $data.sharing.query)).
            then(function() { me.loadSharingDetails(); });
        },
        
        explainPreselection() {
            const { $data, $route } = this, me = this;
            if ($data.selectedType == "circles") {
                me.doBusy(circles.listConsents({ _id : $data.selectedAps._id }, ["name", "type", "authorized" ])
                .then(function(data) {
                    $data.consent = data.data[0];
                }));
            } else if ($data.selectedType == "spaces") {
                me.doBusy(spaces.get({ _id : $data.selectedAps._id }, ["name", "context"] )
                .then(function(data) {
                    $data.space = data.data[0];  
                }));
            }
	    }
    },

    created() {
        const { $data, $route } = this;
        $data.allowDelete = $route.meta.allowDelete;
        $data.allowDeletePublic = $route.meta.allowDeletePublic;
        this.init();
    }
    
}
</script>