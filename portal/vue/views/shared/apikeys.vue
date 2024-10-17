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
    <panel :title="$t('apikeys.title')" :busy="isBusy">
        <error-box :error="error"></error-box>

            <p v-if="services.filtered.length===0" v-t="'apikeys.empty'"></p>
            <pagination v-model="services"></pagination>
            <div v-for="service in services.filtered" :key="service._id">
                <div class="row extraspace">
                    <div class="col-8"><b>{{ service.name }}</b> <i v-if="service.managerName">{{ $t('apikeys.managedby') }} {{ service.managerName }}</i></div>
                    <div class="col-4">
                        <button type="button" class="btn btn-default space" v-t="'apikeys.add_btn'" :disabled="action!=null || service.name.startsWith('Midata Project Auto-Approver')" @click="addKey(service)"></button>                            
                        <button type="button" class="btn btn-danger space" v-t="'apikeys.delete_btn'" v-if="service.linkedStudy" :disabled="action!=null" @click="deleteService(service);"></button>
                        <!-- <button type="button" class="btn btn-danger space" v-t="'apikeys.delete_service_btn'" v-if="service.app.decentral" :disabled="action!=null" @click="deleteService(service);"></button> -->
                        <button type="button" class="btn btn-danger space" v-t="'apikeys.delete_endpoint_btn'" v-else-if="service.endpoint" :disabled="action!=null" @click="deleteService(service);"></button>
                    </div>
                </div>
                <div v-if="service.keys.length" class="row">
                    <div class="col-12">
                        <table class="table table-sm table-bordered">
                            <tr>
                                <th v-t="'apikeys.date'"></th>
                                <th v-t="'apikeys.restrictions'"></th>
                                <th v-t="'apikeys.status'"></th>
                                <th></th>
                            </tr>
                            <tr v-for="key in service.keys" :key="key._id" :class="{ 'table-danger' : key.status!='ACTIVE' }">                        
                                <td>
                                    {{ $filters.dateTime(key.dateOfCreation) }}
                                </td>
                                <td>
                                   {{ key.comment }}
                                </td>
                                <td>
                                    <span>{{ $t('apikeys.'+key.status) }}</span>
                                </td>
                                <td>
                                    <button type="button" class="btn btn-danger btn-sm" :disabled="action!=null" v-t="'apikeys.revoke_btn'" @click="deleteKey(service, key);"></button>
                                </td>
                            </tr>
                        </table>
                    </div>
                </div>
                <hr>
            </div>                
			
    </panel>
    
    <modal :title="$t('apikeys.title')" @close="showkey=null" :open="showkey" :full-width="true" id="apikey">
        <div class="body">
            <div v-t="'apikeys.instructions'"></div>
            <hr>
            <div v-if="showkey.access_token">
            <div><b v-t="'apikeys.key'"></b></div>
            <div v-t="'apikeys.instructions_key'"></div>
            <input type="text" class="form-control mt-1" readonly @click="copyToClip($event)" v-model="showkey.access_token">
            <hr>
            </div>
            <div v-if="showkey.refresh_token">
            <div><b v-t="'apikeys.refresh'"></b></div>
            <div v-t="'apikeys.instructions_refresh'"></div>
            <input type="text" class="form-control mt-1" readonly @click="copyToClip($event)" v-model="showkey.refresh_token">
            <hr>
            </div>
            <div><b v-t="'apikeys.common_name'"></b></div>
            <div v-t="'apikeys.instructions_common_name'"></div>
            <div class="row">
            <div class="input-group col-lg-4 col-12 mb-1 mt-1">
               <div class="input-group-prepend">
                  <span class="input-group-text">OU</span>
                </div>            
                <input type="text" class="form-control" readonly @click="copyToClip($event)" v-model="showkey.ou">
            </div>
            
            <div class="input-group col-lg-8 col-12 mb-1 mt-1">
               <div class="input-group-prepend">
                  <span class="input-group-text">CN</span>
               </div>
               <input type="text" class="form-control" readonly @click="copyToClip($event)" v-model="showkey.cn">
            </div>
            </div>
        </div>
    </modal>
    
    <modal id="organizationSearch" :full-width="true" @close="setupOrganizationSearch=null" :open="setupOrganizationSearch!=null" :title="$t('organizationsearch.title')">
	   <organization-search :setup="setupOrganizationSearch" @add="addKeyWithGroup"></organization-search>
	</modal>
</div>
</template>
<script>

import session from "services/session.js"
import Panel from "components/Panel.vue"
import OrganizationSearch from "components/tiles/OrganizationSearch.vue"
import services from "services/services.js"
import { status, rl, ErrorBox, Modal } from 'basic-vue3-components'

export default {
  
    data: () => ({
        appId : null,
        groupId : null,
        serviceId : null,
        services : null,
        showkey : null,
        setupOrganizationSearch : null,
        selectedService : null
	}),	
		

    components: {  Panel, Modal, ErrorBox, OrganizationSearch },

    mixins : [ status, rl ],
  
    methods : {
        loadServices(studyId) {	
            const { $data } = this, me = this;
            if (studyId) {
                me.doBusy(services.listByStudy(studyId)
                .then(function(data) {
                    for (let service of data.data) { service.keys = []; me.showKeys(service) };
                    $data.services = me.process(data.data);
                    
                }));
            } else {
                let crit = {};
                if ($data.groupId) crit.group = $data.groupId;
                if ($data.appId) crit.app = $data.appId;
                if ($data.serviceId) crit.service = $data.serviceId;
                me.doBusy(services.list(crit)
                .then(function(data) {
                    for (let service of data.data) { service.keys = []; me.showKeys(service) };
                    $data.services = me.process(data.data);						                    
                }));
            }
				
        },
    
        addKey(service) {
            const { $data } = this, me = this;
            me.doAction("add", services.addApiKey(service._id)
            .then(function(result) {
                $data.showkey = result.data;                
                me.showKeys(service);
            }, function(result) {
            
              if (result.response && result.response.data && result.response.data.field==="group") {
                $data.setupOrganizationSearch = { serviceId : service._id };
                $data.selectedService = service;
              }              
              
            }));
        },
        
        addKeyWithGroup(group) {
            const { $data } = this, me = this;
            $data.setupOrganizationSearch = null;
            
            me.doAction("add", services.addApiKey($data.selectedService._id, group._id || group.id)
            .then(function(result) {
                $data.showkey = result.data;                
                me.showKeys($data.selectedService);
            }));
        },

        showKeys(service) {
            const me = this;
            me.doAction("add", services.listKeys(service._id)
            .then(function(result) {
                service.keys = result.data;
            }));
        },

        deleteService(service, key) {
            const { $route } = this, me = this;
            me.doAction("add", services.removeService(service._id)
            .then(function() {
                me.loadServices($route.query.studyId);
            }));
        },

        deleteKey(service, key) {
            const { $route } = this, me = this;
            me.doAction("add", services.removeApiKey(service._id, key._id)
            .then(function() {
                me.loadServices($route.query.studyId);
            }));
        },

        copyToClip(elem) {
            elem = elem.currentTarget;
            elem.focus();
            elem.select();
            
            window.document.execCommand("copy");
	    }
    },

    created() {
        const { $route, $data } = this, me = this;
        if ($route.query.groupId) $data.groupId = $route.query.groupId;
        if ($route.query.appId) $data.appId = $route.query.appId;
        if ($route.query.serviceId) $data.serviceId = $route.query.serviceId;  
        session.currentUser.then(function(userId) { me.loadServices($route.query.studyId); });
    }
   
}
</script>
