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
    <study-nav page="study.sharing" :study="study"></study-nav>
    <tab-panel :busy="isBusy">
	
	    <p v-t="'researcher_sharing.description'"></p>
	    <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="search()" role="form">
	        <error-box :error="error"></error-box>
	            <form-group name="source" label="researcher_sharing.source" :path="errors.source"> 
				    <select class="form-control" name="source" @change="change()" v-validate v-model="crit.source" required>
                        <option v-for="source in sources"  :key="source" :value="source">{{ $t('researcher_sharing.'+source) }}</option>
                    </select>
			    </form-group>
	            <form-group name="format" label="researcher_sharing.format" :path="errors.format"> 
				    <select class="form-control" name="format" @change="change()" v-validate v-model="crit.format" required>
                        <option v-for="format in formats" :key="format" :value="format">{{ format }}</option>
                    </select>
			    </form-group>
			    <form-group name="content" label="researcher_sharing.content" :path="errors.content"> 
				    <select class="form-control" name="content" @change="change()" v-validate v-model="crit.content">
                        <option v-for="content in contents" :key="content" :value="content">{{ content }}</option>
                    </select>
			    </form-group>
			    <form-group name="app" label="researcher_sharing.app" :path="errors.app"> 
				    <select class="form-control" name="app" @change="change()" v-validate v-model="crit.app">
                        <option v-for="app in apps" :key="app" :value="app">{{ appNames[app] }}</option>
                    </select>
			    </form-group>
			    <form-group name="time" label="researcher_sharing.time" :path="errors.time">
			        <div class="row">
			            <div class="col-sm-4"> 
				            <select class="form-control" name="time" @change="change()" v-validate v-model="crit.timeCrit">
                                <option v-for="timeCrit in timeCrits" :key="timeCrit" :value="timeCrit">{{ timeCrit }}</option>
                            </select>
				        </div>
				        <div class="col-sm-8">
				            <input type="text" class="form-control" @change="change()" v-validate v-model="crit.time">
				        </div>
				    </div>
			    </form-group>
			    <form-group name="studyGroup" label="researcher_sharing.studyGroup" :path="errors.studyGroup">
			        <select class="form-control" name="studyGroup" @change="change()" v-validate v-model="crit.studyGroup" required>
                        <option v-for="studyGroup in studyGroups" :key="studyGroup.name" :value="studyGroup.name">{{ studyGroup.name }}</option>
                    </select>
			    </form-group>
			    <button class="btn btn-default space" type="submit" v-submit :disabled="action!=null" v-t="'researcher_sharing.search_btn'"></button>
			    <button class="btn btn-default space" type="button" :disabled="!found || action!=null" @click="share()" v-t="'researcher_sharing.share_btn'"></button>
			    <button class="btn btn-default space" type="button" :disabled="!found || action!=null" @click="unshare()" v-t="'researcher_sharing.unshare_btn'"></button>
			    <div class="extraspace">&nbsp;</div>
                <p v-if="found"><span>Records found: </span>{{ found }}</p>
                <table class="table table-striped" v-if="results.filtered.length">
                    <tr>
                        <th></th>
                        <Sorter sortby="name" v-model="results">Name</Sorter>
                        <Sorter sortby="format" v-model="results">Format</Sorter>		
                        <Sorter sortby="created" v-model="results">Created</Sorter>
                        <th></th>
                    </tr>
                    <tr v-for="result in results.filtered" :key="result._id" :class="{'table-success' : result.selected }">
                        <td><input type="checkbox" :checked="ids.indexOf(result._id)>=0" @click="toggle(ids, result._id);"></td>
                        <td>{{ result.name }}</td>
                        <td>{{ result.format }}</td>			  
                        <td>{{ $filters.date(result.created) }}</td>
                        <td>{{ result.selected }}</td>
                    </tr>
                </table>
	        </form>
    </tab-panel>	
</div>    
      	                 
</template>
<script>

import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import server from "services/server.js"
import session from "services/session.js"
import records from "services/records.js"
import apps from "services/apps.js"
import { rl, status, ErrorBox, Success, CheckBox, FormGroup } from 'basic-vue3-components'
import _ from "lodash";

export default {
    data: () => ({	
        studyid : null,
        crit : { },
        timeCrits : ["created-after","created-before", "updated-after","updated-before"],
	    sources : ["me", "project"],
	    ids : [],
        results : { filtered : [] },
        tooManyConsents : false,
        infos : [],
        contents : [],
        apps : [],
        formats : [],
        found : 0,
        study : null,
        studyGroups : []
    }),

    components: {  TabPanel, Panel, ErrorBox, FormGroup, StudyNav, Success, CheckBox },

    mixins : [ status, rl ],

    methods : {
        reload(userId) {
            const { $data } = this, me = this;
            $data.userId = userId;
            me.doBusy(records.getInfos(userId, { }, "ALL").
            then(function(results) {
                $data.tooManyConsents = results.status == 202;
                $data.infos = results.data;        
                if (results.data && results.data.length) {
	                $data.contents = results.data[0].contents;
	                $data.apps = results.data[0].apps;
	                $data.formats = results.data[0].formats;
                }
                
                me.doBusy(apps.getApps({ _id : $data.apps },["_id","name"]).then(function(result) {
                    let appNames = {};
                    for (var i=0;i<result.data.length;i++) appNames[result.data[i]._id]=result.data[i].name;
                    $data.appNames = appNames;
                }));
                
            }));
	    },

        init() {
            const { $data } = this, me = this;
            me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
            .then(function(data) { 				
                $data.study = data.data;
                $data.studyGroups = data.data.groups;
            }));	
        },
	
	    buildQuery() {
            const { $data } = this;
            var properties = {  };		
            var crit = $data.crit;
            if (crit.source=="me") properties.owner="self";
            else {
                properties.usergroup = $data.studyid;
                properties["force-local"] = true;
                //properties["study-related"] = true;
            }
            if (crit.content) properties.content = crit.content;
            if (crit.format) properties.format = crit.format;
            if (crit.app) properties.app = crit.app;
            if (crit.timeCrit == "created-after") properties["created-after"] = crit.time;
            if (crit.timeCrit == "created-before") properties["created-before"] = crit.time;
            if (crit.timeCrit == "updated-after") properties["updated-after"] = crit.time;
            if (crit.timeCrit == "updated-before") properties["updated-before"] = crit.time;
            return properties;
	    },
	
	    sharedQuery() {
            const { $data } = this, me = this;
            var properties = me.buildQuery();
            properties["force-local"] = undefined;
            properties.study = $data.studyid;
            properties["study-group"] = $data.crit.studyGroup;
            properties["study-related"] = "public";
            properties.owner = undefined;
            return properties;
	    },
	
	    sharingQuery() {
            const { $data } = this, me = this;
            var properties = me.buildQuery();
            if ($data.ids.length>0) {
                properties._id = $data.ids;			
            }
            return properties;
	    },
	      	
        search() {
            const { $data } = this, me = this;
            var properties = me.buildQuery();
            var sq = me.sharedQuery();
            $data.ids = [];
            
                  
            me.doAction("search", records.getRecords($data.userId, properties, ["_id", "name", "created","format","app"]))
            .then(function(result) {
                let results = result.data;
                $data.results = me.process(results);
                $data.found = results.length;
                
                me.doAction("search", records.getRecords($data.userId, sq, ["_id"])
                .then(function(result2) {
                    var map = {};
                    for (var i=0;i<results.length;i++) {
                        map[results[i]._id] = results[i]; 
                    }
                    for (var i2=0;i2<result2.data.length;i2++) {
                        var r = map[result2.data[i2]._id];
                        if (r) r.selected = true;
                    }	                 
                }));
                
            });
        },
	
        share() {
            const { $data } = this, me = this;
            var data = {
                "properties" : me.sharingQuery(), 
                "target-study" : $data.studyid, 
                "target-study-group" : $data.crit.studyGroup
            };
            me.doAction("share", server.post(jsRoutes.controllers.Records.shareRecord().url, data)
            .then(function(result) {
                me.search();
            }));
        },
	
        unshare() {         
            const { $data } = this, me = this;
            var data = {
                    "properties" : me.sharingQuery(), 
                    "target-study" : $data.studyid, 
                    "target-study-group" : $data.crit.studyGroup
                };
            me.doAction("share", server.post(jsRoutes.controllers.Records.unshareRecord().url, data)
            .then(function(result) {
                me.search();
            }));
        },
        
        change() {
            const { $data } = this, me = this;
            if ($data.results && $data.results.all) $data.results.all = [];
            $data.found = 0;
            $data.ids = [];
        },
	
        toggle(array,itm) {
            
            var pos = array.indexOf(itm);
            if (pos < 0) array.push(itm); else array.splice(pos, 1);
        }
	
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.studyid = $route.query.studyId;
        session.currentUser.then(function(userId) { me.init();me.reload(userId); });  
    }
}
</script>