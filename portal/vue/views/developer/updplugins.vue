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
    <panel :title="$t('developer_updplugins.title')" :busy="isBusy">
    
        <error-box :error="error"></error-box>
        <pagination v-model="apps" search="search"></pagination>

        <table class="table" v-if="apps.filtered.length">
			<thead>
            <tr>
                <Sorter sortby="name" v-model="apps" v-t="'developer_yourapps.name'">Name</Sorter>
                <!-- <Sorter sortby="filename" v-model="apps" v-t="'developer_yourapps.filename'">Internal</Sorter> -->
                <Sorter sortby="type" v-model="apps" v-t="'developer_yourapps.type'">Type</Sorter>
                <Sorter sortby="deployStatus" v-model="apps" v-t="'developer_updplugins.deployStatus'"></Sorter>
                <Sorter sortby="repositoryDate" v-model="apps" v-t="'developer_updplugins.repositoryDate'"></Sorter>
                <Sorter sortby="repositoryAuditDate" v-model="apps" v-t="'developer_updplugins.repositoryAuditDate'"></Sorter>
                <Sorter sortby="repositoryRisks" v-model="apps" v-t="'developer_updplugins.repositoryRisks'"></Sorter>
                <Sorter sortby="repositoryUrl" v-model="apps" v-t="'developer_updplugins.repositoryUrl'"></Sorter>
                <th class="d-none d-lg-table-cell"></th>
            </tr>
			</thead>
			<tbody>
            <tr v-for="app in apps.filtered" :key="app._id" :class="{ 'table-success' : app.deployStatus=='DONE', 'table-warning' : app.deployStatus=='RUNNING', 'table-danger' : app.deployStatus=='FAILED' }">
                <td><router-link :to="{ path : './manageapp', query : { appId : app._id } }">{{  app.name }}</router-link></td>
                <!-- <td>{{ app.filename }}</td> -->
                <td>{{ $t('enum.plugintype.'+app.type) }}</td>
                <td>{{ $t('enum.deploymentstatus.'+(app.deployStatus || 'NONE')) }}</td>
                <td>{{ $filters.dateTime(app.repositoryDate) }}</td>
                <td>{{ $filters.dateTime(app.repositoryAuditDate) }}</td>
                <td>{{ app.repositoryRisks }}</td>
                <td>
                 <span class="icon fas fa-check-circle text-success" v-if="app.repositoryUrl != null"></span>                                   
                 <span class="icon fas fa-times-circle" v-else></span>                                                
                </td>
                <td class="d-none d-lg-table-cell">
                    <router-link class="btn btn-default btn-sm me-1" :to="{ path : './repository', query : { appId : app._id }}" v-t="'developer_updplugins.repo_btn'"></router-link> 
                </td>
            </tr>
			</tbody>
        </table>
        <p v-if="apps.filtered.length == 0" v-t="'developer_updplugins.empty'"></p>
        <button class="btn btn-default" type="button" @click="execute('deploy-ready')">{{ $t("developer_updplugins.deploy_ready_btn") }}</button>
        <button class="ms-1 btn btn-default" type="button" @click="execute('deploy-all')">{{ $t("developer_updplugins.deploy_all_btn") }}</button>
        <button class="ms-1 btn btn-default" type="button" @click="execute('audit-all')">{{ $t("developer_updplugins.audit_btn") }}</button>      
    </panel>
    
</template>
<script>

import Panel from "components/Panel.vue"
import session from "services/session.js"
import server from "services/server.js"
import apps from "services/apps.js"
import { rl, status, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {

    data: () => ({	        
        apps : null
    }),

    components: {  Panel, ErrorBox, FormGroup },

    mixins : [ status, rl ],

    methods : {
        init(userId) {	
            const { $data }	= this, me = this;
		    me.doBusy(apps.getApps({ type : ["visualization", "service", "oauth1", "oauth2"] }, [ "creator", "filename", "name", "repositoryDate", "repositoryUrl", "repositoryAuditDate", "repositoryRisks", "targetUserRole", "spotlighted", "type", "deployStatus"])
		    .then(function(data) { 
                for (let app of data.data) { app.search = app.name.toLowerCase()+" "+app.filename.toLowerCase(); }
                $data.apps = me.process(data.data, { filter : { search : "" }, ignoreCase : true, sort : "name" }); 
            }));		  		  
	    },
	    
	    execute(action) {
	      const me = this;
	      this.doBusy(server.post(jsRoutes.controllers.Market.globalRepoAction().url, { action : action })
	      .then(function() { me.init(); }));
	    }
    },

    created() {
        const me = this;        
	    session.currentUser.then(function(userId) { me.init(userId); });        
    }
}
</script>