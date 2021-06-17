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
    <panel :title="$t('visualization.title')" :busy="isBusy" @close="goBack()">
        <error-box :error="error"></error-box>
			
		<p class="lead">{{ getName(visualization) }}</p>
        <form-group label="visualization.created_by">
			<p class="form-control-plaintext">{{visualization.creatorLogin}}</p>
        </form-group>
		<form-group v-if="visualization.orgName" label="visualization.orgName">
			<p class="form-control-plaintext">{{visualization.orgName}}</p>
		</form-group>
		<form-group v-if="visualization.publisher" label="visualization.publisher">
			<p class="form-control-plaintext">{{visualization.publisher}}</p>
		</form-group>
		<form-group label="visualization.description">
			<p class="form-control-plaintext">{{ getDescription(visualization) }}</p>
		</form-group>
							
		<form-group v-if="labels.length || visualization.allowsUserSearch" label="visualization.grant_access">
            <div class="form-control-plaintext">
                <ul>
					<li v-for="label in labels" :key="label"><span>{{ label }}</span></li>						
                </ul>
				<p v-t="'oauth2.reshares_data'" v-if="visualization.resharesData"></p>
				<p v-t="'oauth2.allows_user_search'" v-if="visualization.allowsUserSearch"></p>                			      			      
			</div>
        </form-group>
		<form-group v-if="visualization.termsOfUse" label="visualization.terms">
            <check-box name="appAgb" v-model="options.appAgb" :path="errors.appAgb">
			    <span v-t="'registration.app_agb2'"></span>
                <a @click="terms({which : visualization.termsOfUse })" href="javascript:" v-t="'registration.app_agb3'"></a>
            </check-box>
        </form-group>
                
		<div class="extraspace"></div>
					
		<button type="button" :disabled="action!=null || (visualization.termsOfUse && !options.appAgb)" class="btn btn-primary" @click="install(visualization)" v-t="'visualization.install_btn'"></button>			
    </panel>

	
    <div v-if="terms.active">        
      <terms-modal :which="terms.which" @close="terms.active=false"></terms-modal>
    </div>
</div>
</template>
<script>


import Panel from "components/Panel.vue"
import server from "services/server.js"
import { getLocale } from "services/lang.js"
import apps from "services/apps.js"
import spaces from "services/spaces.js"
import labels from "services/labels.js"
import { status, ErrorBox, FormGroup, CheckBox } from 'basic-vue3-components'


export default {
  
    data: () => ({
        terms : { which : "", active : false },
        visualization : {},
        options : { appAgb : false, termsOfUse : null },
        labels : []
	}),	
		

    components: {  Panel, ErrorBox, FormGroup, CheckBox },

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

        showTerms(def) {		
            const { $data } = this;            
            $data.terms = { which : def, active : true };		
	    },
                
        install(app) {
            const { $data, $route, $router } = this, me = this;
	                    
            me.doAction("install", spaces.openAppLink($router, $route, $route.query.user, { plugin : app, context : $data.options.context, study : $route.query.study, user : $route.query.user }));
		
	    },
        
        init() {
            const { $data, $route, $t } = this, me = this;             
	        var visualizationId = $route.query.visualizationId;	
	        me.doBusy(apps.getApps({"_id":  visualizationId}, ["name", "creator", "creatorLogin", "developerTeam", "description", "defaultSpaceContext", "defaultSpaceName", "defaultQuery", "type", "orgName", "publisher", "requirements", "termsOfUse"])
	        .then(function(results) {
                let app = results.data[0];
		   
			    if (!app.termsOfUse) $data.options.appAgb = true;
			
			    if ($route.query.context) {
				    $data.options.context = $route.query.context;
			    } else { 
				    $data.options.context = app.defaultSpaceContext; 
			    }
			    if (app.defaultSpaceContext == "config" && $data.options.context !== "sandbox") $data.options.context = "config"; 
			   
                $data.visualization = app;
									
			    return labels.prepareQuery($t, app.defaultQuery, app.filename, $data.labels);
		    }));
        }
    },

    created() {
        this.init();
    }
   
}
</script>