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
    <panel :title="$t('apps.title')" :busy="isBusy">
        <error-box :error="error"></error-box>
                            
        <p>{{ $t('apps.count', {count : apps.length }) }}</p>

		<table class="table table-hover clickable" v-if="apps.length">
				
			<tr v-for="app in apps" :key="app._id" :class="{ 'table-warning' : app.status == 'UNCONFIRMED' }" @click="editConsent(app);">
				<td>{{ app.name }}</td>				
				<td class="status-column">
					<span class="icon fas fa-check-circle" v-show="app.status == 'ACTIVE'"></span>
	                <span class="icon fas fa-question-circle" v-show="app.status == 'UNCONFIRMED'"></span>
	                <span class="icon fas fa-times-circle" v-show="app.status == 'REJECTED'"></span>
	    
				</td>
					
					
			</tr>
		</table>
            
    </panel>

    <panel :title="$t('apps.services')" :busy="isBusy">
        <table class="table table-hover clickable" ng-show="filtered.length">
		    <tr v-for="visualization in filtered" :key="visualization._id" @click="install(visualization)">
		        <td>
		            {{ getName(visualization) }}
		        </td>
		        <td class="status-column">
		            <span class="icon fas fa-check-circle" v-show="pluginToSpace[visualization._id]"></span>
		            <button class="btn btn-sm btn-default" v-show="!pluginToSpace[visualization._id]">{{ $t(visualization.type=='visualization'?'apps.use_btn':'apps.connect_btn') }}</button>
		        </td>
		    </tr>
		</table>	
		<p v-if="filtered.length===0" v-t="'apps.empty'"></p>	      
    </panel>
</div>
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import session from "services/session.js"
import { getLocale } from "services/lang.js"
import apps from "services/apps.js"
import spaces from "services/spaces.js"
import { status, ErrorBox } from 'basic-vue3-components'
import ENV from 'config';


export default {
  
    data: () => ({
        pluginToSpace : {},
        apps : [],
        filtered : []
	}),	
		

    components: {  Panel, ErrorBox },

    mixins : [ status ],
  
    methods : {
        getName(app) {            
		    if (app.i18n && app.i18n[getLocale()] && app.i18n[getLocale()].name) return app.i18n[getLocale()].name;
		    return app.name;
        },

        loadConsents(userId) {	
            const { $data } = this, me = this;
		    me.doBusy(apps.listUserApps([ "name", "authorized", "type", "status", "applicationId"])
		    .then(function(data) {
                $data.apps = data.data;
                let pluginToSpace = $data.pluginToSpace;
                for (let app of $data.apps) pluginToSpace[app.applicationId] = true;                
                $data.pluginToSpace = Object.assign({}, $data.pluginToSpace);
			}));
		},
	
        editConsent(consent) {
		    this.$router.push({ path : "./editconsent", query : { consentId : consent._id } });
	    },

        
        install(app) {
            const { $data, $route, $router } = this, me = this;
	        if (app.type == "external" || app.termsOfUse || app.type == "service") {
		        $router.push({ path : "./visualization", query : { visualizationId : app._id, context : $route.query.context, next : $route.query.next, study : $route.query.study, user : $route.query.user } }); 
		        return;
            }
            
            me.doAction("install", spaces.openAppLink($router, $route, $data.userId, { plugin : app, context : $route.query.context, study : $route.query.study }))
            .then(function() { me.loadConsents() });
		
	    },
        
        init() {
            const { $data, $route } = this, me = this;             
	        if ($route.query.tag) $data.tag = $route.query.tag;
            let stati = ENV.beta ? [ "ACTIVE", "BETA" ] : "ACTIVE";
	        let properties = {"spotlighted": true, "targetUserRole" : [ $route.meta.role.toUpperCase(), "ANY"], "status" : stati, "tags" : ["Import"] };
	        let fields = ["name", "type", "description", "tags"];
	        let data = {"properties": properties, "fields": fields};
			
			me.loadConsents();

	        me.doBusy(session.currentUser.then(function(userId) {
			    $data.userId = userId;
			
			    if ($data.tag == "developer") {				    				
				    $data.tag = undefined;
			    }
				
			    me.doBusy(server.post(jsRoutes.controllers.Plugins.get().url, data)).
			    then(function(apps) { 
				    let allApps = apps.data;	
				
				    if (session.user.developer) {
					    properties = { "type" : ["visualization", "oauth1", "oauth2"], "developerTeam" : session.user.developer, "status" : ["ACTIVE", "BETA", "DEVELOPMENT"], "tags" : ["Import"]  };
					    data = { "properties": properties, "fields": fields};
					
					    return me.doBusy(server.post(jsRoutes.controllers.Plugins.get().url, data)).
					    then(function(apps) {
                            allApps = allApps.concat(apps.data); 
                            return allApps;
					    });
				    } else return allApps;
				
			    }).then(function(allApps) {                    
                    $data.filtered = allApps;
                });
			
	        }));	    
		
		    me.doBusy(spaces.getSpacesOfUserContext($data.userId, "config"))
    	    .then(function(results) {
    		    for (var i=0;i<results.data.length;i++) {
    			    var space = results.data[i];
    			    $data.pluginToSpace[space.visualization] = space;
                }
                $data.pluginToSpace = Object.assign({}, $data.pluginToSpace);
    	    });	
        }
    },

    created() {
        this.init();
    }
   
}
</script>