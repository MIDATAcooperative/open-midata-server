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
    <panel :title="$t('provider_organization.title')" :busy="isBusy">	 
        <error-box :error="error"></error-box>
	    <p v-if="!isMasterUser()" class="alert alert-info" v-t="'provider_organization.master_user'"></p>	
	    
	    <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="editorg()" role="form">	     
	        <form-group name="name" label="provider_organization.name" :path="errors.name"> 
		        <input type="text" class="form-control" id="name" :readonly="!isMasterUser()" name="name" v-validate v-model="org.name" required>		    
            </form-group>
            <form-group name="description" label="provider_organization.description" :path="errors.description">
                <textarea class="form-control" id="description" :readonly="!isMasterUser()" name="description" rows="5" v-validate v-model="org.description" required></textarea>
            </form-group>
            
            	<form-group name="address1" label="registration.address" :path="errors.address1">
            <input type="text" class="form-control" id="address1" :readonly="!isMasterUser()" name="address1" v-model="org.address1" v-validate>
        </form-group>
		<form-group name="address2" label="common.empty" :path="errors.address2">
            <input type="text" class="form-control" id="address2" :readonly="!isMasterUser()" name="address2" v-model="org.address2" v-validate>
        </form-group>
		<form-group name="city" label="registration.city" :path="errors.city">
            <input type="text" class="form-control" id="city" :readonly="!isMasterUser()" name="city" v-model="org.city" v-validate>
        </form-group>
		<form-group name="zip" label="registration.zip" :path="errors.zip">
            <input type="text" class="form-control" id="zip" :readonly="!isMasterUser()" name="zip" v-model="org.zip" v-validate>
        </form-group>
		<form-group name="country" label="registration.country" :path="errors.country">
            <select class="form-control" id="country" name="country" :readonly="!isMasterUser()" v-model="org.country" required v-validate>
                <option value selected disabled hidden>{{ $t('common.fillout') }}</option>
                <option value="CH" v-t="'enum.country.CH'"></option>
            </select>
        </form-group>
                        
        <form-group name="phone" label="registration.phone" :path="errors.phone">
            <input type="text" class="form-control" id="phone" :readonly="!isMasterUser()" name="phone" v-model="org.phone" v-validate>
        </form-group>
        <form-group name="mobile" label="registration.mobile_phone" :path="errors.mobile">
            <input type="text" class="form-control" id="mobile" :readonly="!isMasterUser()" name="mobile" v-model="org.mobile" v-validate>
        </form-group>
            
            <form-group name="status" label="provider_organization.status" :path="errors.status">
                <p class="form-control-plaintext" v-t="'enum.userstatus.'+org.status"></p>
            </form-group>
            <form-group id="status" label="provider_editusergroup.searchable" v-if="usergroup && usergroup._id" class="midata-checkbox-row">
                <div class="form-check">
                    <input class="form-check-input" type="checkbox" v-validate v-model="usergroup.searchable" @change="edit();">
                    <div class="margin-left">
                        <success :finished="finished" action="change" msg="common.save_ok"></success>                        
                    </div>
                </div>                     
            </form-group> 
            <form-group name="parent" label="provider_organization.parent" :path="errors.parent">
                <p class="form-control-plaintext"><span class="me-1" v-if="parentOrg">{{ (parentOrg || {}).name }}</span><button type="button" @click="selectParent()" class="btn btn-sm btn-default" v-t="'provider_organization.select_btn'"></button></p>
                
            </form-group>
            <form-group name="identifier" label="provider_organization.identifiers" :path="errors.identifier">
                <textarea class="form-control" id="identifiers" name="identifiers" rows="5" v-validate v-model="org.identifiersStr"></textarea>
                <p class="form-text text-muted" v-t="'provider_organization.identifiers_info'"></p>               
            </form-group>
            <form-group v-if="!org._id" name="managerType" label="provider_organization.managerType" :path="errors.managerType">
                <select name="managerType" class="form-control" v-validate v-model="org.managerTypeExt" required>
                  <option v-for="m in managers" :key="m" :value="m">{{ $t('provider_organization.managers.'+m) }}</option>
                </select>                
            </form-group>
            <form-group v-if="!org._id && (org.managerTypeExt=='OTHERMEMBER' || org.managerTypeExt=='EXTERNALUSER')" name="manager" label="provider_organization.manager" :path="errors.manager"> 
		        <input type="text" class="form-control" id="manager" name="manager" v-validate v-model="org.manager" required>		    
            </form-group>
            <form-group name="protection" label="provider_organization.protection" :path="errors.protection" class="midata-checkbox-row"> 
		         <check-box v-model="usergroup.protection" name="protection" :disabled="org._id">
                    <span v-t="'provider_organization.protection2'"></span>
                </check-box>
            </form-group>
            <form-group name="x" label="common.empty">
                <button type="submit" v-submit :disabled="!isMasterUser() || action!=null" class="btn btn-primary" v-t="'common.submit_btn'"></button>
                <button v-if="org._id" type="button" :disabled="!isMasterUser() || action!=null" class="btn btn-default ms-1" v-t="'common.delete_btn'" @click="deleteOrg()"></button>
                <button v-if="org._id && usergroup.protection" type="button" :disabled="!isMasterUser() || action!=null || (usergroup.currentUserAccessUntil && usergroup.currentUserAccessUntil > now)" class="btn btn-primary ms-1" v-t="'provider_organization.request_btn'" @click="requestAccess()"></button> 
                <success :finished="finished" action="update" msg="common.save_ok"></success>                
            </form-group>          
        </form>	
    </panel>  
    <div v-if="orgId">
      <editgroups></editgroups>
      <editusergroup></editusergroup>
      <panel :title="$t('admin_address.history')" :busy="isBusy">
         <auditlog :patient="org._id"></auditlog>  
       </panel>
    </div>      
    <modal id="parentOrganizationSearch" :full-width="true" @close="setupOrganizationSearch=null" :open="setupOrganizationSearch!=null" :title="$t('organizationsearch.title')">
	   <organization-search :setup="parentOrganizationSearch" @add="setParent"></organization-search>
	</modal>
    
   <!-- <panel :title="$t('provider_organization.members')" :busy="isBusy">	 
        <pagination v-model="persons" search="search"></pagination>
	    <table class="table table-striped" v-if="persons.filtered.length">
	        <tr>
	            <Sorter v-t="'common.user.firstname'" sortby="firstname" v-model="persons"></Sorter>
	            <Sorter v-t="'common.user.lastname'" sortby="lastname" v-model="persons"></Sorter>
	            <Sorter v-t="'common.user.email'" sortby="email" v-model="persons"></Sorter>
	        </tr>
	        <tr v-for="person in persons.filtered" :key="person._id">
	            <td>{{ person.firstname }}</td>
	            <td>{{ person.lastname }}</td>
	            <td>{{ person.email }}</td>
	        </tr>
	    </table>
	  
	    <button class="btn btn-default" :disabled="!isMasterUser()" @click="add()" v-t="'provider_organization.addprovider'"></button>	  
    </panel> -->
</template>
<script>


import Panel from "components/Panel.vue"
import server from "services/server.js"
import session from "services/session.js"
import Auditlog from "components/AuditLog.vue"
import usergroups from "services/usergroups.js"
import editusergroup from "views/provider/editusergroup.vue"
import editgroups from "views/provider/editgroups.vue"
import OrganizationSearch from "components/tiles/OrganizationSearch.vue"
import { rl, status, ErrorBox, FormGroup, Success, Modal, CheckBox } from 'basic-vue3-components'

export default {
    data: () => ({	
        orgId : null,
        org : null,
        usergroup : null,
        currentUser : null,
        parentOrg : null,
        persons : null,
        now : new Date(),
        managers : ["ME", "ME2", "PARENT", "OTHERMEMBER", "EXTERNALUSER"],
        setupOrganizationSearch : null
    }),

    components: {  Panel, ErrorBox, FormGroup, CheckBox, Success, editgroups, editusergroup, OrganizationSearch, Modal, Auditlog },

    mixins : [ status, rl ],
    
    watch : {
		$route(to, from) { 						
			if (to.path.indexOf("addorganization") < 0 && to.path.indexOf("updateorganization") < 0) return;
		    this.$data.orgId = this.$route.query.orgId;        
            this.reload();	    
		}
	},

    methods : {
        reload() {
            const { $data, $route } = this, me = this;
		
		    if ($data.orgId) {
				me.doBusy(server.get(jsRoutes.controllers.providers.Providers.getOrganization($data.orgId).url)
	    		.then(function(data) { 	               
	
	        		let org = data.data;
	        		if (org.identifiers) org.identifiersStr = org.identifiers.join("\n");

	        		$data.org = org;	        		
	        		if ($data.org.parent) {
	        		   me.doBusy(server.get(jsRoutes.controllers.providers.Providers.getOrganization($data.org.parent).url)
	    		       .then(function(data2) { 	               
	        		     $data.parentOrg = data2.data;
	        		   }));
	        		}											
	    		}));
	    		
	    		me.doBusy(server.get(jsRoutes.controllers.UserGroups.getUserGroup($data.orgId).url)
    		    .then(function(data) { 	               		        		
        		   $data.usergroup = data.data;	        		        		   
    		    }));
	    		
	    		/*me.doBusy(usergroups.search({ "_id" : $data.orgId }, ["name", "status", "searchable" ])
				.then(function(data) {		
				    if (data.data.length) $data.usergroup = data.data[0]; else $data.usergroup = null;								                						
				}));*/	   
    		} else {
    		   $data.org = { parent : $route.query.parentId, status : "ACTIVE" };
    		   $data.usergroup = { protection : false };
    		   if ($route.query.parentId) {
		      	
        		   me.doBusy(server.get(jsRoutes.controllers.providers.Providers.getOrganization($route.query.parentId).url)
    		       .then(function(data2) { 	               
        		     $data.parentOrg = data2.data;
        		   }));
        		
    		   } else {
    		      me.ready();
    		   }
    		}
	    			   
				    				
	    },
	
	    editorg() {									
            const { $data } = this, me = this;
            
            if ($data.org.identifiersStr) {
		      $data.org.identifiers = $data.org.identifiersStr.split(/\s*\n\s*/);
		    } else {
		      $data.org.identifiers = [];
		    }
            
            if ($data.orgId) { 
	       		me.doAction("update", server.put(jsRoutes.controllers.providers.Providers.updateOrganization($data.org._id).url, $data.org)
	       		.then((res) => {	       		  	
	       		  me.reload();      		  
	       		}));
	       	} else {
	       	    switch($data.org.managerTypeExt) {
	       	      case "ME":
	       	        $data.org.managerType = "USER";
	       	        $data.org.manager = undefined;
	       	        $data.org.fullAccess = false;
	       	        break;
	       	      case "ME2":
	       	        $data.org.managerType = "USER";
	       	        $data.org.manager = undefined;
	       	        $data.org.fullAccess = true;
	       	        break;
	       	      case "PARENT":
	       	        $data.org.managerType = "ORGANIZATION";
	       	        $data.org.manager = $data.org.parent;
	       	        $data.org.fullAccess = false;
	       	        break;
	       	      case "OTHERMEMBER":
	       	        $data.org.managerType = "USER";
	       	        $data.org.fullAccess = true;
	       	        break;
	       	      case "EXTERNALUSER":
	       	        $data.org.managerType = "USER";
	       	        $data.org.fullAccess = false;
	       	        break;
	       	    }
	       	    let org = $data.org;
	       	    org.protection = $data.usergroup.protection;
  	       	    me.doAction("update", server.post(jsRoutes.controllers.providers.Providers.createOrganization().url, org))
  	       	    .then((res) => {
	       		  me.$router.back();		  
	       		});
	       	}	       		
	    },
	
		deleteOrg() {									
            const { $data } = this, me = this;
            if ($data.orgId) { 
                let org = $data.org;
                org.status = "DELETED";
	       		me.doAction("update", server.put(jsRoutes.controllers.providers.Providers.updateOrganization($data.org._id).url, org)
	       		.then((res) => {
	       		   this.$router.push({ path : './organization' });	       		  
	       		}));
	       	} 
	    },
	
	
	    isMasterUser() {
		    return session.hasSubRole('MASTER') || session.user.role == "ADMIN";
	    },

        add() {
            this.$router.push({ path : './addprovider' });
        },
        
        requestAccess() {
            this.$router.push({ path : './requestaccess', query : { orgId : this.$data.orgId } });
        },
        
        edit() {	
            const { $data, $router } = this, me = this;	
		    me.doAction("change", usergroups.editUserGroup($data.usergroup));
	    },
        
        selectParent() {
            const { $data, $route, $router } = this, me = this;
		
		    $data.setupOrganizationSearch = {}; 
	    },
	    
	    setParent(parentOrg) {	
		    const { $data, $route, $router } = this, me = this;
		    console.log("TRIGGER");
		    
		    $data.setupOrganizationSearch = null;

            if (parentOrg.length) parentOrg = parentOrg[0];					
		    $data.org.parent = parentOrg._id || parentOrg.id;
		    $data.parentOrg = parentOrg;		    		
        }
	
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.orgId = $route.query.orgId;
        
        session.currentUser.then(function(user) { $data.currentUser = user; me.reload(); });	    
    }
    
}
</script>