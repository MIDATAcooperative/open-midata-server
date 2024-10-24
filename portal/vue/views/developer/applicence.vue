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

     <panel :title="getTitle()" :busy="isBusy">		  
	
        <error-box :error="error"></error-box>
     
        <form name="myform" ref="myform" novalidate role="form" class="form-horizontal" @submit.prevent="updateApp()" >
		  	
		  <form-group name="name" label="appicon.name">
		     <p class="form-control-plaintext">{{ app.name }}</p>
		  </form-group>
		  
		  <form-group name="filename" label="appicon.internalname">
		     <p class="form-control-plaintext">{{ app.filename }}</p>
		  </form-group>
		  
		  <form-group name="required" label="applicence.required">
		    <check-box name="required" v-model="licence.required">		        
		      
		    </check-box>
		  </form-group>  
		  
		  <form-group name="allowedEntities" label="applicence.allowedEntities" v-if="licence.required">
		    <check-box v-for="entity in entities" :key="entity" :name="entity" :checked="licence.allowedEntities.indexOf(entity)>=0" @click="toggle(licence.allowedEntities, entity)">		      
		        {{ $t('enum.entitytype.'+entity) }}		      
		    </check-box>		   
		  </form-group>  			  
		  		    		  
		  <form-group label="common.empty">
  		    <router-link :to="{ path : './manageapp' , query :  {appId:appId} }" class="btn btn-default me-1" v-t="'common.back_btn'"></router-link>		      
		      <button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'common.submit_btn'"></button>
          <success :finished="finished" msg="applicence.success" action="submit"></success>
		  </form-group>
	    </form>	  
		
    </panel>
			
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import apps from "services/apps.js"
import { status, ErrorBox, Success, FormGroup, CheckBox } from 'basic-vue3-components'

export default {
    data: () => ({	
       	entities : ["USER","USERGROUP","ORGANIZATION"],
        appId : null,
        app : null,
        licence : null
    }),

    components: {  Panel, ErrorBox, FormGroup, Success, CheckBox },

    mixins : [ status ],

    methods : {
         getTitle() {
            const { $route, $t, $data } = this;
            let p = this.$data.app ? this.$data.app.name+" - " : "";
            return p+$t("manageapp.applicence_btn");                       
        },

        loadApp(appId) {
            const { $data} = this, me = this;
            $data.appId = appId;
            me.doBusy(apps.getApps({ "_id" : appId }, ["_id", "version", "creator", "filename", "name", "description", "licenceDef"])
            .then(function(data) { 
                $data.app = data.data[0];
                if ($data.app.licenceDef) {
                    $data.licence = $data.app.licenceDef;
                    $data.licence.required = true;
                } else {
                    $data.licence = { required : false, allowedEntities : [] };
                }
                $data.licence.version = $data.app.version;
            }));
	    },
	
	
	    updateApp() {		
			  const { $data } = this, me = this;	
        $data.app.msgOnly = true;				
        me.doAction('submit', server.post(jsRoutes.controllers.Market.updateLicence($data.app._id).url, $data.licence)
        .then(function() { 
            me.loadApp($data.app._id);
        }));
            
	    },
	
	    toggle(array,itm) {		
            var pos = array.indexOf(itm);
            if (pos < 0) array.push(itm); else array.splice(pos, 1);
	    }
    },

    created() {
        const { $route } = this, me = this;
        me.loadApp($route.query.appId);
        
    }
}
</script>