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

		<form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="submit()">		    
		    <form-group label="appicon.name">
		        <p class="form-control-plaintext">{{ app.name }}</p>
		    </form-group>
		    <form-group label="appicon.internalname">
		        <p class="form-control-plaintext">{{ app.filename }}</p>
		    </form-group>
		    <form-group name="use" label="appicon.use.title" :path="errors.use">
		        <select name="use" class="form-control" required v-validate v-model="meta.use">
                    <option v-for="use in uses" :key="use" :value="use">{{ $t('appicon.use.'+use) }}</option>
                </select>
		    </form-group>		    
		    <form-group name="file" label="appicon.file" :path="errors.file">
		        <input type="file" name="file" id="iconfile" required>
		        <p class="help-text" v-t="'appicon.file_help'"></p>
		    </form-group>
		    <form-group label="common.empty">
		        <router-link :to="{ path : './manageapp', query : {appId:appId} }" class="btn btn-default me-1" v-t="'common.back_btn'"></router-link>  
		        <button class="btn btn-primary" type="submit" v-submit v-t="'common.submit_btn'"></button>
		    </form-group>
		</form>
		
    </panel>
    <panel v-if="app && app.icons && app.icons.length" :title="$t('appicon.existing')" :busy="isBusy">
    
		<div v-for="use in app.icons" :key="use">
		    <hr>
		    <p>{{ $t('appicon.use.'+use) }}</p>
		    <div class="extraspace">
		        <img style="max-width:100%" :src="getUrl(use)">
		    </div>		    
		    <button class="btn btn-danger" v-t="'common.delete_btn'" @click="doDelete(use);"></button>
		</div>
	      		
    </panel>
			
</div>

</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import apps from "services/apps.js"
import { status, ErrorBox, Success, FormGroup } from 'basic-vue3-components'
import ENV from "config";
import Axios from 'axios';

export default {
    data: () => ({	
        uses : ["LOGINPAGE", "APPICON"],
	    meta : { },
        appId : null,
        app : null
    }),

    components: {  Panel, ErrorBox, FormGroup, Success },

    mixins : [ status ],

    methods : {
        getTitle() {
            const { $route, $t, $data } = this;
            let p = this.$data.app ? this.$data.app.name+" - " : "";
            return p+$t("manageapp.icon_btn");                       
        },
    
        loadApp(appId) {
            const { $data } = this, me = this;
            $data.appId=appId;
            me.doBusy(apps.getApps({ "_id" : appId }, ["creator", "filename", "name", "description", "icons" ])
            .then(function(data) { 
                $data.app = data.data[0];			
            }));
	    },
	
	    submit() {
            const { $data, $route } = this, me = this;
            var fileelem = window.document.getElementById("iconfile");
            
            if (! (fileelem && fileelem.files && fileelem.files.length == 1)) {
                $data.error = "error.missing.file";
                return;
            }
                        
            
            var fd = new FormData();
                    
            fd.append('use', $data.meta.use );
            fd.append('file', fileelem.files[0]);
            me.doAction("upload", Axios.post(ENV.apiurl + "/api/developers/plugins/"+$data.app._id+"/icon", fd, {
            //transformRequest: angular.identity,
            headers: {'Content-Type': undefined, "X-Session-Token" : sessionStorage.token }
            })).then(function() {
                me.loadApp($route.query.appId);
            });

	    },
	
	    doDelete(use) {
            const { $data, $route } = this, me = this;
            me.doAction("delete", server.delete(jsRoutes.controllers.Market.deleteIcon($data.app._id, use).url)
            .then(function() { me.loadApp($route.query.appId); }));
	    },
	
	    getUrl(use) {
            const { $data } = this, me = this;
            if (!$data.app) return null;
            return ENV.apiurl + "/api/shared/icon/" + use + "/" + $data.app.filename;
	    }
    },

    created() {
        const { $route } = this, me = this;
        me.loadApp($route.query.appId);
        
    }
}
</script>