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

<div class="midata-overlay borderless">
    <panel :title="$t('enum.tags.'+(tag || 'Available'))" :busy="isBusy" @close="goBack()">
        <error-box :error="error"></error-box>
                            
        <p v-show="!filtered.length" v-t="'market.no_spotlighted_plugins'"></p>
        <table class="table table-hover clickable">
			<tbody>
            <tr @click="install(visualization)" v-for="visualization in filtered" :key="visualization._id">
                <!--  a class="float-start" href="javascript:;" ng-click="showVisualizationDetails(visualization)"> <img class="media-object"
                    ng-src="{{ getVisualizationImage(visualization) }}" alt="Icon">
                </a -->
                <td>
                    <!--div class="float-end">
                    <button ng-click="showVisualizationDetails(visualization)" class="btn btn-sm btn-default" translate="market.install_now_btn"></button>
                    </div-->
                    <b>{{ getName(visualization) }}</b>
                    <p>{{ getDescription(visualization) }}</p>
                                    
                </td>	
            </tr>	
			</tbody>				
        </table>
            
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

function hasTag(visualization, tag) {
	if (tag == null) return true;
	return visualization.tags.indexOf(tag) >= 0;
}

export default {
  
    data: () => ({
        userId : null,
        beta : ENV.beta,
        tag : null,
        filtered : []
	}),	
		

    components: {  Panel, ErrorBox },

    mixins : [ status ],
  
    methods : {
        getName(app) {            
		    if (app.i18n && app.i18n[getLocale()] && app.i18n[getLocale()].name) return app.i18n[getLocale()].name;
		    return app.name;
        },

        getDescription(app) {
            if (app.i18n && app.i18n[getLocale()] && app.i18n[getLocale()].description) return app.i18n[getLocale()].description;
		    return app.description;
        },

        goBack() {
            this.$router.go(-1);
        },

        install(app) {
            const { $data, $route, $router } = this, me = this;
	        if (app.type == "external" || app.termsOfUse || app.type == "service") {
		        $router.push({ path : "./visualization", query : { visualizationId : app._id, context : $route.query.context, next : $route.query.next, study : $route.query.study, user : $route.query.user } }); 
		        return;
	        }
        
            me.doAction("install", spaces.openAppLink($router, $route, $data.userId, { plugin : app, context : $route.query.context, study : $route.query.study, user : $route.query.user }));	       
	    },
        
        init() {
            const { $data, $route } = this, me = this;
	        if ($route.query.tag) $data.tag = $route.query.tag;
            let stati = ENV.beta ? [ "ACTIVE", "BETA" ] : "ACTIVE";
	        let properties = {"spotlighted": true, "targetUserRole" : [ $route.meta.role.toUpperCase(), "ANY"], "status" : stati };
	        let fields = ["name", "type", "description", "tags"];
	        let data = {"properties": properties, "fields": fields};
			
	        me.doBusy(session.currentUser.then(function(userId) {
			    $data.userId = userId;
			
			    if ($data.tag == "developer") {				    				
				    $data.tag = undefined;
			    }
				
			    me.doBusy(server.post(jsRoutes.controllers.Plugins.get().url, data)).
			    then(function(apps) { 
				    let allApps = apps.data;	
				
				    if (session.user.developer) {
					    properties = { "type" : ["visualization", "oauth1", "oauth2"], "developerTeam" : session.user.developer, "status" : ["ACTIVE", "BETA", "DEVELOPMENT"]  };
					    data = { "properties": properties, "fields": fields};
					
					    return me.doBusy(server.post(jsRoutes.controllers.Plugins.get().url, data)).
					    then(function(apps) {
                            allApps = allApps.concat(apps.data); 
                            return allApps;
					    });
				    } else return allApps;
				
			    }).then(function(allApps) {
                    let filtered = [];
                    for (let app of allApps) {
                        if (hasTag(app, $data.tag)) filtered.push(app);
                    }
                    $data.filtered = filtered;
                });
			
	        }));
        }
    },

    created() {
        this.init();
    }
   
}
</script>