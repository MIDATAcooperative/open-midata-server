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
          
          <form-group name="host" label="Host" :path="errors.host">
             <input type="text" id="host" name="host" class="form-control" v-validate v-model="smtp.host" required>
          </form-group>
          
          <form-group name="port" label="Port" :path="errors.port">
             <input type="text" id="port" name="port" class="form-control" v-validate v-model="smtp.port" required>
          </form-group>
                    
          <form-group name="ssl" label="appsmtp.ssl" class="midata-checkbox-row">
            <check-box name="required" v-model="smtp.ssl">              
              
            </check-box>
          </form-group>  
          
          <form-group name="tls" label="appsmtp.tls" class="midata-checkbox-row">
             <check-box name="required" v-model="smtp.tls">              
                
             </check-box>
          </form-group>  
          
          <form-group name="username" label="Username" :path="errors.username">
             <input type="text" id="username" name="username" class="form-control" v-validate v-model="smtp.username" required>
          </form-group>
           
          <form-group name="password" label="password" :path="errors.password">
             <input type="text" id="password" name="password" class="form-control" v-validate v-model="smtp.password" required>
          </form-group>
                                                          
          <form-group label="common.empty">
            <router-link :to="{ path : './manageapp' , query :  {appId:appId} }" class="btn btn-default me-1" v-t="'common.back_btn'"></router-link>              
            <button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'common.submit_btn'"></button>
            <button type="button" class="btn btn-danger" v-t="'common.delete_btn'" @click="removeSMTP"></button>
            <success :finished="finished" msg="appsmtp.success" action="submit"></success>
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
        smtp : null,
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
            return p+$t("appsmtp.title");                       
        },

        loadApp(appId) {
            const { $data} = this, me = this;
            $data.appId = appId;
            me.doBusy(apps.getApps({ "_id" : appId }, ["_id", "version", "filename", "name", "smtp"])
            .then(function(data) { 
                $data.app = data.data[0];
                if ($data.app.smtp) {
                    $data.smtp = $data.app.smtp;
                } else {
                    $data.smtp = { host : "", port :_0, tls: false, ssl: false, username: "", password: "" };
                }
            }));
        },
    
        updateApp() {       
          const { $data } = this, me = this;                   
          me.doAction('submit', server.post(jsRoutes.controllers.Market.updateSMTP($data.app._id).url, $data.smtp)
          .then(function() { 
            me.loadApp($data.app._id);
          }));
        }      ,
          
        removeSMTP() {       
          const { $data } = this, me = this;                   
          me.doAction('submit', server.post(jsRoutes.controllers.Market.updateSMTP($data.app._id).url, {})
          .then(function() { 
            me.loadApp($data.app._id);
          }));
      }
    },

    created() {
        const { $route } = this, me = this;
        me.loadApp($route.query.appId);
        
    }
}
