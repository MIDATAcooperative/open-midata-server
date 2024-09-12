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
    <panel :title="getTitle()" :busy="isBusy">		  
	
        <error-box :error="error"></error-box>
		
		<div>
	        <pagination v-model="services" search="name"></pagination>
            <table class="table table-striped" v-if="services.filtered.length">
                <tr>
                    <th v-t="'admin_services.name'"></th>
                    <!-- <th v-t="'admin_services.endpoint'"></th> -->
                    <th v-t="'admin_services.status'"></th>
                    <th>&nbsp;</th>
                </tr>
                <tr v-for="service in services.filtered" :key="service._id">
                    <td>{{ service.name }}</td>
                    <!-- <td>{{ service.endpoint }}</td> -->                    
                    <td>{{ service.status }}</td>
                    <td>
                      <router-link v-if="service.linkedStudy" :to="{ path: './astudy', query : { studyId : service.linkedStudy }}" v-t="'admin_services.study'"></router-link>
                      <router-link v-if="service.managerAccount!=service.linkedStudy" :to="{ path: './address', query : { userId : service.managerAccount }}" v-t="'admin_services.manager'"></router-link>
                    </td>
                    
                </tr>
            </table>
                            
            <p v-if="!services.filtered.length" v-t="'admin_services.empty'"></p>
                        
            
		</div>
					
    </panel>
</div>		

</template>
<script>

import Panel from "components/Panel.vue"
import languages from "services/languages.js"
import apps from "services/apps.js"
import server from "services/server.js"
import { status, rl, ErrorBox } from 'basic-vue3-components'

export default {
    data: () => ({	
       
        appId : null,
        app : null,
        services : null

    }),

    components: {  Panel, ErrorBox },

    mixins : [ status, rl ],
    
    watch : {
		$route(to, from) { 						
			if (to.path.indexOf("services") < 0) return;
			this.prepare(); 
		}
	},

    methods : {
        getTitle() {
            const { $route, $t, $data } = this;
            let p = this.$data.app ? this.$data.app.name+" - " : "";
            if ($route.query.appId) return p+$t("manageapp.services_btn");
            else return $t("admin_services.endpoints");
        },

        loadApp(appId) {
            const { $data } = this, me = this;
            me.doBusy(apps.getApps({ "_id" : appId }, ["creator", "developerTeam", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "predefinedMessages", "requirements", "termsOfUse", "orgName", "publisher", "unlockCode", "codeChallenge", "writes", "loginTemplate", "loginButtonsTemplate", "usePreconfirmed", "accountEmailsValidated", "allowedIPs", "decentral", "organizationKeys", "acceptTestAccounts", "acceptTestAccountsFromApp", "acceptTestAccountsFromAppNames", "testAccountsCurrent", "testAccountsMax"])
            .then(function(data) { 
                let app = data.data[0];                
                $data.app = app;                
            }));
            me.doBusy(server.get(jsRoutes.controllers.Services.listServiceInstancesApp(appId).url)
            .then(function(data) {
               $data.services = me.process(data.data, { filter : { name : "" }});
            }));
	    },
	    
	    loadEndpoints() {
            const { $data } = this, me = this;
            
            me.doBusy(server.get(jsRoutes.controllers.Services.listEndpoints().url)
            .then(function(data) {
               $data.services = me.process(data.data, { filter : { name : "" }});
            }));
	    },
	    
	    prepare() {
	       const { $route } = this, me = this;
           if ($route.query.appId) {
              me.loadApp($route.query.appId);
           } else {
             me.loadEndpoints();
           }
	    }    
	        
	},

    created() {
       this.prepare();    
    }
}
</script>