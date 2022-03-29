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
        			
		<form name="myform" ref="myform" novalidate role="form" class="form-horizontal" @submit.prevent="updateLicence()">
            <error-box :error="error"></error-box>
	      
		    <form-group name="appId" label="admin_addlicence.app" :path="errors.appId">		     
	            <typeahead name="appId" class="form-control" :suggestions="apps" field="filename" @selection="appselection(licence.appName, 'appId');" v-model="licence.appName" required />	          	          
	            <p v-if="app" class="form-text text-muted">{{ app.name }} {{ app.orgName }}</p>
		    </form-group>

		    <form-group name="si" label="admin_addlicence.service" :path="errors.si">			  
			    <check-box :disabled="!app || (app.type!='external' && app.type!='endpoint')" v-model="licence.service" name="si">
		            <span v-t="'admin_addlicence.service2'"></span>
                </check-box>
		    </form-group>
		  
		    <form-group name="licenseeType" label="admin_addlicence.licenseeType" :path="errors.licenseeType">
		        <select class="form-control" v-validate v-model="licence.licenseeType" required name="licenseeType">
                    <option v-for="entity in entities" :key="entity" :value="entity">{{ $t('enum.entitytype.'+entity) }}</option>
                </select>		 
		    </form-group>
		  
		    <form-group name="role" label="admin_addlicence.role" v-if="licence.licenseeType=='USER'" :path="errors.role">
		        <select class="form-control" v-validate v-model="licence.role" name="role">
                    <option v-for="role in roles" :key="role" :value="role">{{ $t('enum.userrole.'+role) }}</option>
                </select>		 
		    </form-group>
		  
		    <form-group name="licenseeId" label="admin_addlicence.licensee" :path="errors.licenseeId">
		        <input type="text" class="form-control" name="licenseeId" v-validate v-model="licence.licenseeName"  @change="licenseeChange()" required>
		        <p class="form-text text-muted" v-if="user">{{ user.firstname }} {{ user.lastname }}</p>
		        <p class="form-text text-muted" v-if="usergroup">{{ usergroup.name }}</p>
		    </form-group>
		  
		    <form-group name="expireDate" label="admin_addlicence.expireDate" :path="expireDate">		        
				<input type="date" name="expireDate" class="form-control" v-validate v-model="licence.expireDate">				              
		    </form-group>
		  		  
		    <form-group label="common.empty">
		        <button type="submit" v-submit :disabled="action!=null" class="btn btn-primary mr-1" v-t="'common.submit_btn'"></button>		    
		        <button type="button" class="btn btn-danger" v-if="allowDelete" @click="doDelete()" :disabled="action!=null" v-t="'common.delete_btn'"></button>
		    </form-group>
        </form>	  
	
    </panel>
</template>
<script>

import Panel from "components/Panel.vue"
import users from "services/users.js"
import server from "services/server.js"
import apps from "services/apps.js"
import usergroups from "services/usergroups.js"

import { status, ErrorBox, FormGroup, CheckBox, Typeahead } from 'basic-vue3-components'

export default {
    data: () => ({	
        licence : { service : null, appName : "", role : null, expireDate : null, licenseeName : null },
        app : null,
        allowDelete : false,
        entities : ["USER","USERGROUP","ORGANIZATION"],
        roles : ["MEMBER", "PROVIDER", "RESEARCH", "DEVELOPER"],
        user : null,
        usergroup : null,
        apps : []
    }),

    components: {  Panel, ErrorBox, FormGroup, CheckBox, Typeahead },

    mixins : [ status ],

    methods : {
        getTitle() {
            const { $data, $t } = this;
            if ($data.licence && $data.licence._id) return $t('admin_addlicence.title2');
            return $t('admin_addlicence.title1');            
        },

        loadLicence(licenceId) {
            const { $data } = this, me = this;
            me.doBusy(server.post(jsRoutes.controllers.Market.searchLicenses().url, { properties : { _id : licenceId } })
            .then(function(data) { 
                $data.licence = data.data[0];						
            }));
	    },
		
	    updateLicence() {
			const { $data, $router } = this, me = this;
            if ($data.licence._id == null) {
                if ($data.licence.expireDate == "") $data.licence.expireDate = "";
                me.doAction('submit', server.post(jsRoutes.controllers.Market.addLicence().url, $data.licence))
                .then(function(data) { $router.push({ path : "./licenses" }); });
            } 
	    },
	
        appselection(app, field) {
            const { $data } = this, me = this;
		    me.doSilent(apps.getApps({ filename : app }, ["_id", "filename", "name", "orgName", "type", "targetUserRole"]))
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				    $data.licence[field] = data.data[0]._id;
				    $data.app = data.data[0];				  
				}
			});
	    },
	
	    licenseeChange() {
            const { $data } = this, me = this;
            if ($data.licence.licenseeType == "USER") {
                users.getMembers({ email : $data.licence.licenseeName, role : $data.licence.role }, ["_id","email","firstname","lastname"])
                .then(function(result) {
                    if (result.data.length==1) {
                        var user = result.data[0];				
                        $data.licence.licenseeId = user._id;
                        $data.licence.licenseeName = user.email;
                        $data.user = user;
                    } else {
                        $data.licence.licenseeId = null;
                        $data.user = null;
                    }
                    $data.usergroup = null;
                });
            } else if ($data.licence.licenseeType == "USERGROUP") {
                usergroups.search({ name : $data.licence.licenseeName, status : "ACTIVE" }, ["_id", "name"])
                .then(function(result) {
                    if (result.data.length == 1) {
                        $data.usergroup = result.data[0];
                        $data.licence.licenseeId = $data.usergroup._id;
                        $data.licence.licenseeName = $data.usergroup.name;
                    }
                    $data.user = null;
                });
            } else if ($data.licence.licenseeType == "ORGANIZATION") {
                $data.user = null;
                $data.usergroup = null;
                users.getMembers({ email : $data.licence.licenseeName, role : "PROVIDER" }, ["_id","email","firstname","lastname","provider"])
                .then(function(result) {
                    if (result.data.length==1) {
                        var user = result.data[0];				
                        $data.licence.licenseeId = user.provider;
                        //$data.licence.licenseeName = user.email;					
                    } else {
                        $data.licence.licenseeId = null;				
                    }				
                });
            }
	    }
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.allowDelete = $route.meta.allowDelete;
        me.doBusy(apps.getApps({  }, ["creator", "developerTeam", "filename", "name", "description", "type", "targetUserRole" ]))
	    .then(function(data) { 
		    $data.apps = data.data;			
	    });
    }
    
}
</script>