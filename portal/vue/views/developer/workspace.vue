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

        <div class="preview" v-if="usedApps.length || usedProjects.length || usedAccounts.length">
            <table>
                <tr>
                    <td></td>
                    <td class="center">
                         <div @click="goapp(app)" class="clickable mobileapp" v-for="app in mobileapps" :key="app._id">
                            <i class="fas big fa-mobile-alt"></i>
                            <div>{{ app.filename }}</div>
                            <i class="fas arrow fa-arrow-down"></i>
                         </div>
                    </td>
                    <td class="center">
                        <div class="browser" v-if="plugins.length">
                            <center>{{ $t('workspace.browser') }}</center>
                            <div @click="goapp(app)" class="clickable plugin" v-for="app in plugins" :key="app._id">
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
                        <div @click="goapp(app)" class="clickable imports" v-for="app in imports" :key="app._id">
                            <i class="fas arrow fa-arrow-right right"></i>
                            <i class="fas big fa-upload"></i>
                            <div>{{ app.filename }}</div>                            
                        </div>
                    </td>
                    <td colspan="2" class="server">
                        
                            <center>{{ $t('workspace.midata') }}</center>
                            <div class="useraccount">{{ $t('workspace.useraccount') }}
                                <div v-for="user in usedAccounts" :key="user.sessionToken">{{ user.role }}: {{ user.name }}</div>
                                <!-- <div v-for="user in usedAccounts" :key="user.sessionToken">{{ user }}</div> -->
                            </div>

                            <div @click="goapp(app)" class="clickable service" v-for="app in services" :key="app._id">
                                <i class="fas arrow fa-arrow-left left"></i>
                                <i class="fas big fa-cog"></i>
                                <div>{{ app.filename }}</div>                                
                            </div>
                        
                    </td>
                    <td>
                        <div @click="goapp(app)" class="clickable external" v-for="app in external" :key="app._id" >
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
                        <div @click="goproject(project)" class="clickable project" v-for="project in usedProjects" :key="project._id">
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
                         <div @click="goapp(app)" class="clickable analyzer" v-for="app in analyzers" :key="app._id" >
                            <i class="fas arrow fa-arrow-up"></i><br>
                            <i class="fas big fa-cog"></i>
                            <div>{{ app.filename }}</div>                            
                        </div>
                    </td>
                    <td></td>
                </tr>


            </table>
                     
        </div>
        <p v-else v-t="'workspace.add_something'"></p>
        <div class="mt-3">
            <button class="btn btn-primary mr-1 mb-1" type="button" @click="showAddApp" v-t="'workspace.newapp_btn'"></button>
            <button class="btn btn-primary mr-1 mb-1" type="button" @click="showAddProject" v-t="'workspace.newproject_btn'"></button>
            <button class="btn btn-primary mr-1 mb-1" type="button" @click="showAddUser" v-t="'workspace.newuser_btn'"></button>
            <button class="btn btn-primary mr-1 mb-1" type="button" @click="reset()" v-t="'workspace.reset_btn'"></button>
        </div>
    </panel>
    
    <panel :title="$t('workspace.issues')" :busy="isBusy">
        <pagination v-model="allissues"></pagination>
        <table class="table table-striped table-sm table-hover" v-if="allissues.filtered.length">
            <thead>
                <tr>
                    <Sorter v-model="allissues" sortby="type" v-t="'workspace.severity.severity'"></Sorter>
                    <Sorter v-model="allissues" sortby="component" v-t="'workspace.component'"></Sorter>
                    <Sorter v-model="allissues" sortby="issue" v-t="'workspace.issue'"></Sorter>
                    <th v-t="'workspace.param'"></th>
                </tr>
            </thead>
            <tbody>
            <tr @click="selectIssue(issue)" v-for="issue in allissues.filtered" :key="issue.index" :class="{ 'table-danger' : issue.type == 'error', 'table-warning' : issue.type == 'warning' }">
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
        <p v-else v-t="'workspace.no_issues'"></p>
    </panel>

    <panel :title="$t('workspace.summary')" :busy="isBusy" v-if="summary.length">        
        <div v-for="inp of input" :key="inp.letter">
            <b>{{ inp.letter }}</b> : <span v-if="inp.mode">{{ $t(inp.mode) }} <i class="fas fa-arrow-right"></i></span> {{ inp.system }} ({{ inp.short}}) {{ inp.target }}
        </div>
        <table class="table table-sm mt-2">
            <tr>
                <th v-t="'oauth2.requests_access_short'"></th>
                <th class="d-none d-sm-table-cell" v-for="sh in short" :key="sh">{{ sh }}</th>
                <!-- <td></td> -->
            </tr>
            <tr v-for="line in summary" :key="line.label">
                <td>{{ line.label }}
                    <div class="d-inline-block d-sm-none float-right text-muted">{{ line.letters }}</div>
                </td>
                <td class="d-none d-sm-table-cell" v-for="(sh,idx) in short" :key="idx"><i class="fas fa-check" v-if="line.checks[idx]"></i></td>
                <!-- <td>{{ line.summary }}</td> -->
            </tr>
        </table>

    </panel>

    <modal id="addapp" :full-width="true" :open="addApp.open" @close="addApp.open = false" :title="$t('workspace.add_app')">
         <div class="body">
        <form name="myform" ref="myform" @submit.prevent="submitAddApp">
            <form-group name="appname" label="workspace.appname" :path="errors.appname">
                <typeahead name="appname" class="form-control" v-model="addApp.appname" :suggestions="availableApps" field="filename"></typeahead>
            </form-group>
            <form-group label="common.empty">
                <button type="submit" v-submit class="btn btn-primary" v-t="'workspace.add_btn'"></button>
            </form-group>
        </form>
         </div>
    </modal>

     <modal id="adduser" :full-width="true" :open="addUser.open" @close="addUser.open = false" :title="$t('workspace.add_user')">
         <div class="body">
            <p class="alert alert-warning" v-t="'workspace.testusers_only'"></p>
            <form name="myform" ref="myform" @submit.prevent="submitAddUser">
                <form-group label="login.email_address">
                    <input type="email" class="form-control" :placeholder="$t('login.email_address')" required v-validate v-model="addUser.email" style="margin-bottom:5px;" autofocus>
                </form-group>
                <form-group label="login.password">
                    <password class="form-control" :placeholder="$t('login.password')" required v-model="addUser.password" style="margin-bottom:5px;"></password>
                </form-group>
                <form-group label="workspace.role">
                    <select class="form-control" v-model="addUser.role" v-validate required>
                        <option v-for="role in roles" :key="role.value" :value="role.value">{{ $t(role.name) }}</option>
                    </select>
                </form-group>
                <form-group label="common.empty">
                    <button type="submit" v-submit class="btn btn-primary" v-t="'workspace.add_btn'"></button>
                </form-group>
            </form>
         </div>
    </modal>

    <modal id="addproject" :full-width="true" :open="addProject.open" @close="addProject.open = false" :title="$t('workspace.add_project')">
        <div class="body">
            <form name="myform" ref="myform" @submit.prevent="submitAddProject">
                <form-group name="projectname" label="workspace.projectname" :path="errors.projectname">
                    <typeahead name="projectname" class="form-control" v-model="addProject.projectname" :suggestions="availableProjects" field="code"></typeahead>
                </form-group>
                <form-group label="common.empty">
                    <button type="submit" v-submit class="btn btn-primary" v-t="'workspace.add_btn'"></button>
                </form-group>
            </form>
        </div>
    </modal>


</template>
<style scoped>
.preview {
    display:block;
    margin-left: auto;
    margin-right: auto;
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

.center { text-align: center; }

.server {    
    border-top:1px solid grey;
    border-left:1px solid grey;
    border-right:1px solid grey;
    background-color: #e0e0e0;
    padding: 4px 4px 4px 4px;
    text-align: center;
}

.server2 {    
    border-left:1px solid grey;
    border-right:1px solid grey;
    background-color: #e0e0e0;
    padding: 4px 4px 4px 4px;
    text-align: center;
}

.server3 {
    border-bottom:1px solid grey;
    border-left:1px solid grey;
    border-right:1px solid grey;
    background-color: #e0e0e0;
    padding: 4px 4px 4px 4px;
    text-align: center;
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
import crypto from "services/crypto.js"
import Panel from "components/Panel.vue"
import apps from "services/apps.js"
import studies from "services/studies.js"
import server from "services/server.js"
import labels from "services/labels.js"
import _ from "lodash";
import Axios from 'axios';
import ENV from 'config';

import {  rl, status, ErrorBox, FormGroup, Typeahead, Modal, Password } from 'basic-vue3-components'

let index = 0;
let issues = [];

function analyze(usedApps, usedProjects, usedAccounts, issues, accprojects) {

    const applink = function(page, id) {       
        return { path : "./"+page, query : { appId : id }};
    }

    const projectlink = function(page, id) {
       
        if (_.filter(accprojects, (x) => x._id == id).length > 0) {
          return { path : "./"+page, query : { studyId : id }};
        } else return null;
    }

    const add = function(type, key, component, param, link) {            
            issues.push({ index : index, type : type, key : key, component : component, param : param, link : link });
            index++;
    }

    let appConsents = {};
    let participations = {};
    let projects = {};

    for (let account of usedAccounts) {
        if (account.message) add("error", "workspace.error.login_failed", account.name, account.message);
        if (account.apps) {
            for (let appInstance of account.apps) {
                if (!appConsents[appInstance.applicationId]) appConsents[appInstance.applicationId] = [];
                appConsents[appInstance.applicationId].push(appInstance);
            }
        }
        if (account.projects) {
            for (let participation of account.projects) {
                if (!participations[participation.study]) participations[participation.study] = [];
                participations[participation.study].push(participation);
            }
        }
    }

    for (let app of usedApps) {
        let type = app.type;

        if (app.status=="DEVELOPMENT" || app.status=="BETA") {
            add("zinfo", "workspace.info.app_in_development", app.filename, null, applink("manageapp", app._id));
        } else if (app.status == "DEPRECATED" || app.status == "DELETED") {
            add("warning", "workspace.warning.app_deprecated", app.filename, null, applink("manageapp", app._id));  
        }

        if (!app.orgName) add("warning", "workspace.warning.no_orgname", app.filename, null, applink("editapp", app._id));
        if (!app.publisher) add("warning" , "workspace.warning.no_publisher", app.filename, null, applink("editapp", app._id));

        if (!app.description || app.description.length < 10) add("warning", "workspace.warning.no_app_description", app.filename, null, applink("editapp", app._id));

        if (app.i18n) {

        }

        if (type == "visualization" || type == "oauth1" || type == "oauth2" ) {
            if (!app.tags || app.tags.length == 0) add("error", "workspace.error.no_tags", app.filename, null, applink("editapp", app._id));

            if (!app.spotlighted) add("warning", "workspace.warning.not_spotlighted", app.filename, null, applink("manageapp", app._id));

            if (!app.url || app.url.length == 0) add("error", "workspace.error.no_url", app.filename);
            
        }

        if (type == "service") {
           if (!app.repositoryUrl) add("warning", "workspace.warning.no_repository", app.filename, null, applink("repository", app._id));
           
        }

        if (app.repositoryUrl && !app.repositoryDate) add("error", "workspace.error.never_deployed", app.filename, null, applink("repository", app._id));

        if (app.requirements != null && app.requirements.length == 0) add("zinfo", "workspace.info.no_requirements", app.filename, null, applink("editapp", app._id));

        if (!app.defaultQuery || Object.keys(app.defaultQuery).length == 0 || (Object.keys(app.defaultQuery).length == 1 && app.defaultQuery.content && app.defaultQuery.content.length == 0))
        add("error", "workspace.error.no_access_filter", app.filename, null, applink("query", app._id));

        if (!app.icons || app.icons.length == 0) add("zinfo", "workspace.info.no_icons", app.filename, null, applink("appicon", app._id));

        if (!app.sendReports) add("warning", "workspace.warning.no_send_reports", app.filename, null, applink("editapp", app._id));

        let hasComments = false;
        if (app.stats == null) {
           add("warning", "workspace.warning.no_access", app.filename, null);
        } else for (let stat of app.stats) {
            if (stat.comments) {
                for (let c of stat.comments) {
                    if (c.toLowerCase().indexOf("error")>=0) {
                        add("error", "See Stats: "+c, app.filename, null, applink("appstats", app._id));
                    } else hasComments = true;                    
                }
            }
        }
        if (hasComments) add("warning", "workspace.warning.has_comments", app.filename, null, applink("appstats", app._id));

        if (usedAccounts.length) {
            let instances = appConsents[app._id];
            if (!instances || instances.length==0) {
                add("warning", "workspace.warning.no_user_for_app", app.filename);
            } else {
                for (let instance of instances) {
                    if (instance.status == "REJECTED") add("error", "workspace.error.appinstance_rejected", app.filename);
                    else if (instance.status != "ACTIVE") add("warning", "workspace.warning.appinstance_status", app.filename);

                    if (instance.appVersion < app.pluginVersion) add("warning", "workspace.warning.appinstance_outdated", app.filename);
                }
            }
        }
    }

    for (let project of usedProjects) {
        projects[project._id] = project;
        if (!project.joinMethods || project.joinMethods.length == 0) add("error","workspace.error.no_join_method", project.name, null, projectlink("study.overview", project._id));
        if (project.validationStatus == "DRAFT" || project.validationStatus == "PATCH") add("warning", "workspace.warning.project_not_validated", project.name, null, projectlink("study.overview", project._id));
        if (project.validationStatus == "VALIDATION") add("error", "workspace.error.project_in_validation", project.name, null, projectlink("study.overview", project._id));
        if (project.validationStatus == "REJECTED") add("error", "workspace.error.project_rejected", project.name, null, projectlink("study.overview", project._id));

        if (project.participantSearchStatus == "PRE") add("warning", "workspace.warning.project_not_searching", project.name, null, projectlink("study.overview", project._id));
        if (project.participantSearchStatus == "CLOSED") add("error", "workspace.warning.project_not_searching", project.name, null, projectlink("study.overview", project._id));

        if (project.executionStatus == "PRE") add("zinfo", "workspace.info.project_not_running", project.name, null, projectlink("study.overview", project._id));
        if (project.executionStatus == "FINISHED") add("error", "workspace.error.project_finished", project.name, null, projectlink("study.overview", project._id));
        if (project.executionStatus == "ABORTED") add("error", "workspace.error.project_aborted", project.name, null, projectlink("study.overview", project._id));
        // if (!project.groups || project.groups.length==0) add("error", "workspace.error.project_no_groups", project.name);
        if (!project.requirements || project.requirements.length==0) add("zinfo", "workspace.info.project_no_requirements", project.name, null, projectlink("study.overview", project._id));

        if (usedAccounts.length) {
            let parts = participations[project._id];
            if (!parts || parts.length==0) {
                add("warning", "workspace.warning.no_user_for_project", project.name);
            } else {
                for (let part of parts) {
                    if (part.pstatus != "ACCEPTED") add("warning", "workspace.warning.not_accepted", project.name, part.pstatus);
                }
            }
        }
    }
    
    for (let app of usedApps) {
        for (let link of app.links) {
            if (link.study) {
                
              if (link.validationResearch != "VALIDATED") add("error", "workspace.link_not_validated_research", app.filename, link.study.name, applink("applink", app._id));
              if (link.validationDeveloper != "VALIDATED") add("error", "workspace.link_not_validated_developer", app.filename, link.study.name, applink("applink", app._id));
              if (link.usePeriod.indexOf(link.study.executionStatus)<0) add("warning", "studyactions.status.study_wrong_status", link.study.name, null, projectlink("study.overview", link.study._id));
              if (link.type.indexOf('REQUIRE_P')>=0 && link.study.participantSearchStatus != 'SEARCHING') add("error","error.closed.study", link.study.name, null, projectlink("study.overview", link.study._id));
              if ((link.type.indexOf('REQUIRE_P')>=0 || link.type.indexOf('OFFER_P')>=0) && link.study.joinMethods.indexOf('APP') < 0 && link.study.joinMethods.indexOf('APP_CODE') < 0) add("error", "studyactions.status.study_no_app_participation", link.study.name, null, projectlink("study.rules", link.study._id));
            }	
        }
    }
    
    
}

function postWithSession(token, url, body) {
		return Axios.post(ENV.apiurl + url, body, { headers : { "X-Session-Token" : token, "Prefer" : "return=representation" } })
        .catch((result) => result.response);
};

function getWithSession(token, url) {
		return Axios.get(ENV.apiurl + url, { headers : { "X-Session-Token" : token, "Prefer" : "return=representation" } });
};

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
       addUser : { open : false, email : "" },
      
        roles : [
                { value : "MEMBER", name : "enum.userrole.MEMBER" },
                { value : "PROVIDER" , name : "enum.userrole.PROVIDER"},
                { value : "RESEARCH" , name : "enum.userrole.RESEARCH"},
                { value : "DEVELOPER" , name : "enum.userrole.DEVELOPER"},
        ]
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

    

    components: {  Panel, ErrorBox, FormGroup, Typeahead, Modal, Password },

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

        selectIssue(issue) {
            const { $router } = this;
            if (issue.link) {
                $router.push(issue.link);
            }
        },

        goapp(app) {
            const { $router } = this;
            $router.push({ path : './manageapp', query : { appId : app._id }});
        },

        goproject(prj) {
            const { $router, $data } = this;
            if (_.filter($data.availableProjects, (x) => x._id == prj._id).length > 0) {
                $router.push({ path : './study.overview', query : { studyId : prj._id }});
            }
        },

        redo() {
            const { $data, $t } = this, me = this;
            localStorage.workspace = JSON.stringify($data.setup);
            $data.usedApps = [];
            $data.usedProjects = [];
            $data.usedAccounts = [];
            issues = [];
            let waitFor = [];
            waitFor.push(server.get(jsRoutes.controllers.research.Studies.list().url)
            .then((st) => { $data.availableProjects = st.data }));
            for (let entry of $data.setup) {
                if (entry.type == "app") {
                    waitFor.push(apps.getApps({ filename : entry.name }, ["creator", "creatorLogin", "developerTeam", "developerTeamLogins", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "refreshTkExchangeParams", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "pluginVersion", "requirements", "termsOfUse", "orgName", "publisher", "unlockCode", "codeChallenge", "writes", "icons", "apiUrl", "noUpdateHistory", "pseudonymize", "predefinedMessages", "defaultSubscriptions", "sendReports", "consentObserving", "loginTemplate", "loginButtonsTemplate", "usePreconfirmed", "accountEmailsValidated", "allowedIPs", "decentral", "organizationKeys"])
                    .then(function(result) {
                        if (result.data.length==0) {
                            me.addError("workspace.error.app_no_exist", entry.name);
                        } else {
                            let app = result.data[0];
                            app.labels = [];
                            $data.usedApps.push(app);                            
                            return labels.prepareQuery($t, app.defaultQuery, null, app.labels, null).then(function() {
                                      
                                return server.get(jsRoutes.controllers.Market.getStudyAppLinks("app", app._id).url)
                                .then(function(links) {
                                    app.links = links.data;
                                    return server.get(jsRoutes.controllers.Market.getPluginStats(app._id).url)
                                    .then(function(stats) {	    	
                                        app.stats = stats.data;       
                                    }).catch(function() { app.stats = null; });
                                });	
                            });
                        }
                    }));
                } else if (entry.type == "project") {
                    waitFor.push(studies.search({ code : entry.name }, ["name", "code", "_id"])
                    .then(function(result) {
                        
                        if (result.data.length==0) {
                            me.addError("workspace.error.project_no_exist", entry.name);
                        } else {
                            return server.get(jsRoutes.controllers.members.Studies.get(result.data[0]._id).url)
                            .then(function(result2) {
                                let project = result2.data.study;
                                project.labels = [];
                                $data.usedProjects.push(project);                                  
                                return labels.prepareQuery($t, project.recordQuery, null, project.labels, project.requiredInformation)
                                .then(function() {
                                    return server.get(jsRoutes.controllers.Services.listServiceInstancesStudy(project._id).url)
                                    .then(function(services) {
                                        project.services = services.data;

                                        let rapp = function(serv) {
                                            return apps.getApps({ _id : serv.appId }, ["filename"])
                                            .then((a) => { if (a.data.length>0) serv.filename = a.data[0].filename; });
                                        }
                                        let allService = [];
                                        for (let serv of project.services) {
                                            allService.push(rapp(serv));
                                            
                                        }
                                        return Promise.all(allService);
                                    }).catch(function() { project.services = []; return Promise.resolve(); });
                                });
                            });
                        }
                    }));
                } else if (entry.type == "user") {
                    let data = {"email": entry.name, "password": crypto.getHash(entry.password), "role" : entry.role  };
                    let func = function(data) {                        
                        return postWithSession(null, jsRoutes.controllers.Application.authenticate().url, data);
                    };                    
		            waitFor.push(session.performLogin(func, data, entry.password)
                    .then(function(result) {                      
                        let account = result.data;
                        account.name = entry.name;
                        account.role = entry.role;
                        account.user = null;
                        account.apps = null;
                        account.consents = null;
                        account.projects = null;
                        if (account.sessionToken) {

                            return getWithSession(account.sessionToken, jsRoutes.controllers.Users.getCurrentUser().url).
                            then(function(result1) {
                                //account.user = result1.data;

                                let data = {"properties": { "_id" : result1.data.user }, "fields": ["email", "visualizations", "apps", "name", "role", "subroles", "developer", "security", "language", "status", "contractStatus", "agbStatus", "emailStatus", "authType", "city", "mobile", "searchable", "termsAgreed", "flags"] };					
                                return postWithSession(account.sessionToken, jsRoutes.controllers.Users.get().url, data)
                                .then(function(data) {
                                    account.user = data.data[0];	
                                
                                    return getWithSession(account.sessionToken, jsRoutes.controllers.members.Studies.list().url)
                                    .then(function(results4) {
                                        account.projects = results4.data;
                                        return postWithSession(account.sessionToken, jsRoutes.controllers.Circles.listApps().url, { fields : ["applicationId", "appVersion", "licence", "serviceId", "deviceId", "type", "status", "dataupdate", "writes", "validUntil", "createdBefore", "observers"] })
                                        .then(function(result2) {                                            
                                            account.apps = result2.data;

                                            return postWithSession(account.sessionToken, jsRoutes.controllers.Circles.listConsents().url, { properties : {}, fields : ["type", "status","writes", "validUntil", "createdBefore","createdAfter", "authorized", "entityType"] })
                                            .then(function (result3) {
                                                account.consents = result3.data;
                                                $data.usedAccounts.push(account);
                                            });                                
                                        });
                                    });
                                });
                            });
                        } else $data.usedAccounts.push(account);
                    }));			
                }
            }
            me.doBusy(
            Promise.all(waitFor).then(function() {
                let mredo = me.autocomplete();

                if (mredo) me.redo();
                else {
                    me.prepareQuerySummary();
                    me.analyze();
                    $data.allissues = me.process(issues, { sort : "type", filter : { text : "" } });
                }
            }));
        },

        analyze() {
            const { $data, $t } = this, me = this;
            analyze($data.usedApps, $data.usedProjects, $data.usedAccounts, issues, $data.availableProjects);
        },

        tryadd(entry) {
            const { $data } = this;
            for (let s of $data.setup) {
                if (s.name == entry.name && s.type == entry.type) return false;
            }
            $data.setup.push(entry);
            return true;
        },

        autocomplete() {
            const { $data } = this;
            let mustredo = false;
            for (let app of $data.usedApps) {
                if (app.links) {
                    for (let link of app.links) {
                        if (link.linkTargetType == "STUDY") {
                            mustredo = this.tryadd({ name : link.study.code, type : "project" }) || mustredo;
                        } else if (link.linkTargetType == "SERVICE") {
                            
                            mustredo = this.tryadd({ name : link.serviceApp.filename, type : "app" }) || mustredo;
                        }
                    }
                }
            }
            for (let project of $data.usedProjects) {
                if (project.services) {
                    for (let service of project.services) {
                        if (service.status == "ACTIVE") {
                            
                            mustredo = this.tryadd({ name : service.filename, type : "app" }) || mustredo;
                        }
                    }
                }
            }
           
            return mustredo;
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
            issues.push({ index : index, type : "zinfo", key : key, component : component, param : param });
            index++;
        },

        showAddApp() {
            const { $data, $t } = this, me = this;
            $data.addApp = { open : true, appname : "" };
        },

        showAddUser() {
            const { $data, $t } = this, me = this;
            $data.addUser = { open : true, email : "", password : "", role : "MEMBER" };
        },

        submitAddUser() {
            const { $data, $t } = this, me = this;
            me.tryadd({ name : $data.addUser.email, password : $data.addUser.password, type : "user" });
            $data.addUser.open = false;
            me.redo();
        },

        submitAddApp() {
            const { $data, $t } = this, me = this;
            me.tryadd({ name : $data.addApp.appname, type : "app" });
            $data.addApp.open = false;            
            me.redo();
        },

        showAddProject() {
            const { $data, $t } = this, me = this;
            $data.addProject = { open : true, projectname : "" };
        },

        submitAddProject() {
            const { $data, $t } = this, me = this;
            me.tryadd({ name : $data.addProject.projectname, type : "project" });
            $data.addProject.open = false;
            me.redo();
        },
      
        prepareQuerySummary() {
            const { $data, $t } = this, me = this;
            let short = [];
            let letters = ["", " A"," B"," C"," D"," E"," F"," G"," H"," I"];
            let idx = 1;          
            let input = [];
            	
           
		
		  for (let app of $data.usedApps) {
                let shortName =(app.type == "service" || app.type == "external") ? $t('oauth2.short_service') : $t('oauth2.short_app');
                input.push({ system : app.name, letter : letters[idx], labels:app.labels, short : shortName });                
                short.push(letters[idx]); 
                idx++;           
            }

            for (let project of $data.usedProjects) {
                input.push({ system : project.name, letter : letters[idx], labels:project.labels, short : $t('oauth2.short_'+(project.type.toLowerCase())) });
                short.push(letters[idx]);
                idx++;	
            }

	
		$data.summary = labels.joinQueries(this.$t, input);
		$data.short = short;
		$data.input = input;
            	
                                            
        }
                   
    },

    created() {
        const me = this;    
        this.$data.setup = JSON.parse(localStorage.workspace || "[]");
	    session.currentUser.then(function(userId) { me.init(userId); });        
    }
}
</script>