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
    <panel :title="$t('workspace.title')" :busy="isBusy">
    
        <error-box :error="error"></error-box>  

        <div class="preview">
            <table>
                <tr>
                    <td></td>
                    <td>
                         <div class="mobileapp" v-for="app in mobileapps" :key="app._id">
                            <i class="fas big fa-mobile-alt"></i>
                            <div>{{ app.filename }}</div>
                            <i class="fas arrow fa-arrow-down"></i>
                         </div>
                    </td>
                    <td>
                        <div class="browser">
                            <center>{{ $t('workspace.browser') }}</center>
                            <div class="plugin" v-for="app in plugins" :key="app._id">
                                <i class="fas big fa-desktop"></i>
                                <div>{{ app.filename }}</div>
                                <i class="fas arrow fa-arrow-down"></i>
                            </div>
                        </div>
                    </td>
                    <td></td>
                </tr>
                <tr>
                    <td>
                        <div class="imports" v-for="app in imports" :key="app._id">
                            <i class="fas arrow fa-arrow-right right"></i>
                            <i class="fas big fa-upload"></i>
                            <div>{{ app.filename }}</div>                            
                        </div>
                    </td>
                    <td colspan="2" class="server">
                        
                            <center>{{ $t('workspace.midata') }}</center>
                            <div class="useraccount">{{ $t('workspace.useraccount') }}</div>

                            <div class="service" v-for="app in services" :key="app._id">
                                <i class="fas arrow fa-arrow-left left"></i>
                                <i class="fas big fa-cog"></i>
                                <div>{{ app.filename }}</div>                                
                            </div>
                        
                    </td>
                    <td>
                        <div class="external" v-for="app in external" :key="app._id" >
                            <i class="fas arrow fa-arrow-left left"></i>
                            
                            <div>
                                <i class="fas big fa-desktop"></i><br>
                                {{ app.filename }}
                            </div>
                            
                        </div>
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td colspan="2" class="server2">
                        <div class="project" v-for="project in usedProjects" :key="project._id">
                            <i class="fas arrow fa-arrow-down"></i><br>
                            <i class="fas big fa-flask"></i>
                            <div>{{ project.name }}</div>                
                        </div>
                    </td>
                    <td></td>
                </tr>
                <tr>
                    <td></td>
                    <td colspan="2" class="server3">
                         <div class="analyzer" v-for="app in analyzers" :key="app._id" >
                            <i class="fas arrow fa-arrow-up"></i><br>
                            <i class="fas big fa-cog"></i>
                            <div>{{ app.filename }}</div>                            
                        </div>
                    </td>
                    <td></td>
                </tr>


            </table>
                     
        </div>
        <button class="btn btn-primary mr-1" type="button" @click="showAddApp" v-t="'workspace.newapp_btn'"></button>
        <button class="btn btn-primary mr-1" type="button" @click="showAddProject" v-t="'workspace.newproject_btn'"></button>
        <button class="btn btn-primary mr-1" type="button" @click="reset()" v-t="'workspace.reset_btn'"></button>
    </panel>
    
    <panel :title="$t('workspace.issues')" :busy="isBusy">
        <pagination v-model="allissues"></pagination>
        <table class="table table-striped table-sm">
            <thead>
                <tr>
                    <Sorter v-model="allissues" sortby="type" v-t="'workspace.severity.severity'"></Sorter>
                    <Sorter v-model="allissues" sortby="component" v-t="'workspace.component'"></Sorter>
                    <Sorter v-model="allissues" sortby="issue" v-t="'workspace.issue'"></Sorter>
                    <th v-t="'workspace.param'"></th>
                </tr>
            </thead>
            <tbody>
            <tr v-for="issue in allissues.filtered" :key="issue.index" :class="{ 'table-danger' : issue.type == 'error', 'table-warning' : issue.type == 'warning' }">
                <td>
                    {{ $t('workspace.severity.'+issue.type) }}
                </td>
                <td>
                    {{ issue.component }}
                </td>
                <td>
                    {{ $t(issue.key) }}
                </td>
                <td>
                    {{ issue.param }}
                </td>
            </tr>
            </tbody>
        </table>
    </panel>

    <panel :title="$t('workspace.summary')" :busy="isBusy">
        <table class="table table-sm">
			<tr>
				<th v-t="'oauth2.requests_access_short'"></th>
				<th v-for="sh in short" :key="sh">{{ sh }}</th>			
			</tr>
			<tr v-for="line in summary" :key="line.label">
				<td>{{ line.label }}</td>
				<td v-for="(sh,idx) in short" :key="idx"><i class="fas fa-check" v-if="line.checks[idx]"></i></td>
			
			</tr>
		</table>    

    </panel>

    <modal id="addapp" full-width="true" :open="addApp.open" @close="addApp.open = false" :title="$t('workspace.add_app')">
        <form name="myform" ref="myform" @submit.prevent="submitAddApp">
            <form-group name="appname" label="workspace.appname" :path="errors.appname">
                <typeahead name="appname" class="form-control" v-model="addApp.appname" :suggestions="availableApps" field="filename"></typeahead>
            </form-group>
            <form-group label="common.empty">
                <button type="submit" v-submit class="btn btn-primary" v-t="'workspace.add_btn'"></button>
            </form-group>
        </form>
    </modal>

    <modal id="addpproject" full-width="true" :open="addProject.open" @close="addProject.open = false" :title="$t('workspace.add_project')">
        <form name="myform" ref="myform" @submit.prevent="submitAddProject">
            <form-group name="projectname" label="workspace.projectname" :path="errors.projectname">
                <typeahead name="projectname" class="form-control" v-model="addProject.projectname" :suggestions="availableProjects" field="code"></typeahead>
            </form-group>
            <form-group label="common.empty">
                <button type="submit" v-submit class="btn btn-primary" v-t="'workspace.add_btn'"></button>
            </form-group>
        </form>
    </modal>


</template>
<style scoped>
.preview {
    display:block;
    position:relative;
    min-height: 500px;
    min-width: 700px;
}

.big { font-size: 40px; }
.arrow { font-size: 20px; color:red; }
.right { float:right; padding-top:15px; padding-left:10px; }
.left { float:left; padding-top:15px; }

.browser {
    border:1px solid grey;
    margin-left:10px;
    margin-bottom:20px;
    padding:10px 10px 10px 10px;
    background-color:#e0e0e0;
}

.server {    
    border-top:1px solid grey;
    border-left:1px solid grey;
    border-right:1px solid grey;
    background-color: #e0e0e0;
    padding: 4px 4px 4px 4px;
}

.server2 {    
    border-left:1px solid grey;
    border-right:1px solid grey;
    background-color: #e0e0e0;
    padding: 4px 4px 4px 4px;
}

.server3 {
    border-bottom:1px solid grey;
    border-left:1px solid grey;
    border-right:1px solid grey;
    background-color: #e0e0e0;
    padding: 4px 4px 4px 4px;
}

.useraccount {
    display: block;
    margin:10px 10px 10px 10px;
    padding:10px 10px 10px 10px;
    border: 1px solid grey;
    background-color: #a0a0ff;    
}

.mobileapp {
    display:inline-block;    
    text-align: center;    
}

.mobileapp div {    
    padding: 4px 4px 4px 4px;
    font-size: 0.75em;
}

.plugin {
    display:inline-block;    
    text-align: center;
}

.plugin div {
    font-size: 0.75em;
}

.imports {
    display:block;  
    padding-right:40px;  
}

.imports div {
    font-size: 0.75em;
}

.analyzer {
    display:inline-block;    
    text-align: center;
}

.analyzer div {
    font-size: 0.75em;
}


.project {    
    text-align: center;
    display: inline-block;
}

.project div {
    
    height: 20px;
    
    font-size: 0.75em;
}

.service {
    display:inline-block;
    padding-left: 40px;

}

.service div {
   
    height: 20px;
    padding: 4px 4px 4px 4px;
    display: inline-block;
    font-size: 0.75em;
}

.external {
    display:block;
    padding-left: 40px;
    text-align: center;
}

.external div {
    font-size: 0.75em;
    padding-left:30px;
}

</style>
<script>

import session from "services/session.js"
import Panel from "components/Panel.vue"
import apps from "services/apps.js"
import studies from "services/studies.js"
import server from "services/server.js"
import labels from "services/labels.js"
import _ from "lodash";

import {  rl, status, ErrorBox, FormGroup, Typeahead, Modal } from 'basic-vue3-components'

let index = 0;
let issues = [];

function analyze(usedApps, usedProjects, usedAccounts, issues) {

    const add = function(type, key, component, param, link) {            
            issues.push({ index : index, type : type, key : key, component : component, param : param, link : link });
            index++;
    }

    for (let app of usedApps) {
        let type = app.type;

        if (app.status=="DEVELOPMENT" || app.status=="BETA") {
            add("info", "workspace.info.app_in_development", app.filename);
        } else if (app.status == "DEPRECATED" || app.status == "DELETED") {
            add("warning", "workspace.warning.app_deprecated", app.filename);  
        }

        if (!app.orgName) add("warning", "workspace.warning.no_orgname", app.filename);
        if (!app.publisher) add("warning" , "workspace.warning.no_publisher", app.filename);

        if (!app.description || app.description.length < 10) add("warning", "workspace.warning.no_app_description", app.filename);

        if (app.i18n) {

        }

        if (type == "visualization" || type == "oauth1" || type == "oauth2" ) {
            if (!app.tags || app.tags.length == 0) add("error", "workspace.error.no_tags", app.filename);

            if (!app.spotlighted) add("warning", "workspace.warning.not_spotlighted", app.filename);

            if (!app.url || app.url.length == 0) add("error", "workspace.error.no_url", app.filename);
            
        }

        if (type == "service") {
           if (!app.repositoryUrl) add("warning", "workspace.warning.no_repository", app.filename);
           
        }

        if (app.repositoryUrl && !app.repositoryDate) add("error", "workspace.error.never_deployed", app.filename);

        if (app.requirements != null && app.requirements.length == 0) add("info", "workspace.info.no_requirements", app.filename);

        if (!app.defaultQuery || Object.keys(app.defaultQuery).length == 0 || (Object.keys(app.defaultQuery).length == 1 && app.defaultQuery.content && app.defaultQuery.content.length == 0))
        add("error", "workspace.error.no_access_filter", app.filename);

        if (!app.icons || app.icons.length == 0) add("info", "workspace.info.no_icons", app.filename);

        if (!app.sendReports) add("warning", "workspace.warning.no_send_reports", app.filename);

        let hasComments = false;
        for (let stat of app.stats) {
            if (stat.comments) {
                for (let c of stat.comments) {
                    if (c.toLowerCase().indexOf("error")>=0) {
                        add("error", "See Stats: "+c, app.filename);
                    } else hasComments = true;                    
                }
            }
        }
        if (hasComments) add("warning", "workspace.warning.has_comments", app.filename);
    }

    for (let project of usedProjects) {
        if (!project.joinMethods || project.joinMethods.length == 0) add("error","workspace.error.no_join_method", project.name);
        if (project.validationStatus == "DRAFT") add("warning", "workspace.warning.project_not_validated", project.name);
        if (project.validationStatus == "VALIDATION") add("error", "workspace.error.project_in_validation", project.name);
        if (project.validationStatus == "REJECTED") add("error", "workspace.error.project_rejected", project.name);

        if (project.participantSearchStatus == "PRE") add("warning", "workspace.warning.project_not_searching", project.name);
        if (project.participantSearchStatus == "CLOSED") add("error", "workspace.warning.project_not_searching", project.name);

        if (project.executionStatus == "PRE") add("info", "workspace.info.project_not_running", project.name);
        if (project.executionStatus == "FINISHED") add("error", "workspace.error.project_finished", project.name);
        if (project.executionStatus == "ABORTED") add("error", "workspace.error.project_aborted", project.name);
        if (!project.groups || project.groups.length==0) add("error", "workspace.error.project_no_groups", project.name);
        if (!project.requirements || project.requirements.length==0) add("info", "workspace.info.project_no_requirements", project.name);
    }

    for (let app of usedApps) {
        for (let link of app.links) {

        }
    }
    
}


export default {

    data: () => ({	  
       setup : [],      
       usedApps : [], 
       usedProjects : [],
       usedAccounts : [],
       
       allissues : {},
       summary : [],
       short : [],
       availableApps : [],
       availableProjects : [],
       addApp : { open : false, appname : "" },
       addProject : { open : false, projectname : "" },
       addAccount : { open : false, email : "" }
    }),

    computed : {
        mobileapps() {
            return _.filter(this.$data.usedApps, (a) => a.type=="mobile");
        },

        plugins() {
            return _.filter(this.$data.usedApps, (a) => a.type=="visualization");
        },

        imports() {
            return _.filter(this.$data.usedApps, (a) => (a.type=="oauth1" || a.type=="oauth2"));
        },

        analyzers() {
            return _.filter(this.$data.usedApps, (a) => a.type=="analyzer");
        },

        services() {
            return _.filter(this.$data.usedApps, (a) => a.type=="service");
        },

        external() {
            return _.filter(this.$data.usedApps, (a) => a.type=="external");
        }
    },

    

    components: {  Panel, ErrorBox, FormGroup, Typeahead, Modal },

    mixins : [ rl, status ],

    methods : {
        init(userId) {	
            const { $data, $t } = this, me = this;
            me.doBusy(apps.getApps({}, ["filename","name"]).then(function(result) { $data.availableApps = result.data }));
            me.redo();
	    },

        reset() {
            const { $data, $t } = this, me = this;
            $data.setup = [];
            me.redo();
        },

        left(idx, base) {
            let c  = [0,50,-50,100,-100];
            return (base+c[idx])+"px";
        },

        top(idx, base) {
            let c = [0,40,-40,80,-80];
            return (base+c[idx])+"px";
        }, 

        redo() {
            const { $data, $t } = this, me = this;
            localStorage.workspace = JSON.stringify($data.setup);
            $data.usedApps = [];
            $data.usedProjects = [];
            $data.usedAccounts = [];
            issues = [];
            let waitFor = [];
            for (let entry of $data.setup) {
                if (entry.type == "app") {
                    waitFor.push(apps.getApps({ filename : entry.name }, ["creator", "creatorLogin", "developerTeam", "developerTeamLogins", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "pluginVersion", "requirements", "termsOfUse", "orgName", "publisher", "unlockCode", "writes", "icons", "apiUrl", "noUpdateHistory", "pseudonymize", "predefinedMessages", "defaultSubscriptions", "sendReports", "consentObserving"])
                    .then(function(result) {
                        if (result.data.length==0) {
                            me.addError("workspace.errors.app_no_exist", entry.name);
                        } else {
                            let app = result.data[0];
                            app.labels = [];
                            $data.usedApps.push(app);                            
                            return labels.prepareQuery($t, app.defaultQuery, null, app.labels, null).then(function() {
                                 return server.get(jsRoutes.controllers.Market.getPluginStats(app._id).url)
                                .then(function(stats) {	    	
                                    app.stats = stats.data;     
                                    return server.get(jsRoutes.controllers.Market.getStudyAppLinks("app", app._id).url)
                                    .then(function(links) {
                                        app.links = links.data;
                                    });
                                });	
                            });
                        }
                    }));
                } else if (entry.type == "project") {
                    waitFor.push(studies.search({ code : entry.name }, ["name", "code", "_id"])
                    .then(function(result) {
                        
                        if (result.data.length==0) {
                            me.addError("workspace.errors.project_no_exist", entry.name);
                        } else {
                            return server.get(jsRoutes.controllers.research.Studies.get(result.data[0]._id).url)
                            .then(function(result2) {
                                let project = result2.data;
                                project.labels = [];
                                $data.usedProjects.push(project);                                  
                                return labels.prepareQuery($t, project.recordQuery, null, project.labels, project.requiredInformation);
                            });
                        }
                    }));
                }
            }
            me.doBusy(
            Promise.all(waitFor).then(function() {
                me.prepareQuerySummary();
                me.analyze();
                $data.allissues = me.process(issues, { sort : "type", filter : { text : "" } });
            }));
        },

        analyze() {
            const { $data, $t } = this, me = this;
            analyze($data.usedApps, $data.usedProjects, $data.usedAccounts, issues);
        },

        addError(key, component, param) {
            const { $data, $t } = this, me = this;
            issues.push({ index : index, type : "error", key : key, component : component, param : param });
            index++;
        },

        addWarning(key, component, param) {
            const { $data, $t } = this, me = this;
            issues.push({ index : index, type : "warning", key : key, component : component, param : param });
            index++;
        },

        addInfo(key, component, param) {
            const { $data, $t } = this, me = this;
            issues.push({ index : index, type : "info", key : key, component : component, param : param });
            index++;
        },

        showAddApp() {
            const { $data, $t } = this, me = this;
            $data.addApp = { open : true, appname : "" };
        },

        submitAddApp() {
            const { $data, $t } = this, me = this;
            $data.setup.push({ name : $data.addApp.appname, type : "app" });
            $data.addApp.open = false;
            me.redo();
        },

        showAddProject() {
            const { $data, $t } = this, me = this;
            $data.addProject = { open : true, projectname : "" };
        },

        submitAddProject() {
            const { $data, $t } = this, me = this;
            $data.setup.push({ name : $data.addProject.projectname, type : "project" });
            $data.addProject.open = false;
            me.redo();
        },
      
        prepareQuerySummary() {
            const { $data, $t } = this, me = this;
            let short = [];
            let letters = ["", " A"," B"," C"," D"," E"," F"," G"," H"," I"];
            let idx = 1;
            let projectIdx = 0;
            let input = [];
            	
            	
            for (let app of $data.usedApps) {
                input.push({ system : app.name, labels:app.labels });
                if (app.type == "service" || app.type == "external") { short.push($t('oauth2.short_service')+letters[idx]); }
                else short.push($t('oauth2.short_app')+letters[idx]); 
                idx++;           
            }

            for (let project of $data.usedProjects) {
                input.push({ system : project.name, labels:project.labels });
                short.push($t('oauth2.short_'+(project.type.toLowerCase()))+letters[idx]);
                idx++;	
            }
                                    
            $data.summary = labels.joinQueries(this.$t, input);
            $data.short = short;
        }
                   
    },

    created() {
        const me = this;    
        this.$data.setup = JSON.parse(localStorage.workspace || "[]");
	    session.currentUser.then(function(userId) { me.init(userId); });        
    }
}
</script>