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
    <panel :title="$t('admin_plugins.title')" :busy="isBusy">
    
        <error-box :error="error"></error-box>
        <form class="css-form form-horizontal">		   
		    <form-group name="developer" label="admin_plugins.developer">
		        <typeahead class="form-control" id="developer" @selection="reload();" v-model="search.criteria.creatorLogin" :suggestions="developers" field="email"></typeahead>
		    </form-group>
		   <form-group name="name" label="admin_plugins.name">
		        <restrict v-model="apps" name="search" label="common.empty"></restrict>
		   </form-group>			      
		   <form-group name="organisation" label="admin_plugins.organisation">
		        <restrict v-model="apps" name="orgName" label="common.empty"></restrict>
		   </form-group>
	    </form>

        <pagination v-model="apps"></pagination>
        <table class="table" v-if="apps.filtered.length">
            <tr>
            <Sorter sortby="name" v-model="apps" v-t="'admin_plugins.plugin_name'">Name</Sorter>
            <Sorter sortby="creatorLogin" v-model="apps" v-t="'admin_plugins.plugin_developer'">Developer</Sorter>
            <Sorter sortby="type" v-model="apps" v-t="'admin_plugins.plugin_type'">Type</Sorter>
            <Sorter sortby="targetUserRole" v-model="apps" v-t="'admin_plugins.plugin_role'">Target Role</Sorter>
            <th v-t="'admin_plugins.plugin_market'">Market</th>
            <th v-t="'admin_plugins.plugin_stats'">Stats</th>
            <Sorter sortby="status" v-model="apps" v-t="'admin_plugins.plugin_status'">Status</Sorter>
            </tr>
            <tr v-for="app in apps.filtered" :key="app._id">
                <td><router-link :to="{ path : './manageapp', query : { appId : app._id }}">{{  app.name }}</router-link><div class="text-muted">{{ app.filename }}</div></td>
                <td><router-link :to="{ path : './address', query : { userId : app.creator }}">{{ app.creatorLogin }}</router-link><div class="text-muted">{{ app.orgName }}</div></td>
                <td>{{ $t('enum.plugintype.'+app.type) }}</td>
                <td>{{ $t('enum.userrole.'+app.targetUserRole) }}</td>
                <td><input class="form-check-input" type="checkbox" @change="changePlugin(app);" v-model="app.spotlighted"></td>
                <td><router-link :to="{ path : './appstats', query : { appId : app._id }}" v-t="'common.show'"></router-link></td>
                <td>
                    <select class="form-control" v-model="app.status" @change="changePlugin(app);">
                        <option v-for="status in pluginStati" :key="status" :value="status">{{ status }}</option>
                    </select>
                </td>
            </tr>
        </table>
        <p v-if="apps.filtered.length == 0" v-t="'admin_plugins.empty'"></p>
                  
        <router-link :to="{ path : './defineplugin' }" class="btn btn-default" v-t="'admin_plugins.add_definition_btn'"></router-link>
    </panel>
  	   
</template>
<script>

import Panel from "components/Panel.vue"
import session from "services/session.js"
import apps from "services/apps.js"
import users from "services/users.js"
import { status, rl, ErrorBox, FormGroup, Typeahead } from 'basic-vue3-components'

export default {

    data: () => ({	
        pluginStati : ["DEVELOPMENT", "BETA", "ACTIVE", "DEPRECATED"],
        apps : { filtered : [], filter : {}, sort : {} },
        search : { criteria : {} },
        developers : []
    }),

    components: {  Panel, ErrorBox, FormGroup, Typeahead },

    mixins : [ status, rl ],

    methods : {
        init(userId) {		
            const { $data } = this, me = this;
		    me.reload();
		    me.doBusy(users.getMembers({ role : "DEVELOPER", status : ["NEW", "ACTIVE", "BLOCKED"] }, [ "firstname", "lastname", "email" ])
		    .then(function(data) {                
			    $data.developers = data.data;
			    //$data.developers.push({});
		    }));
	    },
	
	    reload() {
            const { $data } = this, me = this;
	        if ($data.search.criteria.creatorLogin === "") $data.search.criteria.creatorLogin = undefined;
	        me.doSilent(apps.getApps( $data.search.criteria, [ "creator", "creatorLogin", "developerTeam", "filename", "version", "name", "description", "tags", "targetUserRole", "spotlighted", "type", "status", "orgName", "publisher"]))
	        .then(function(data) {
                for (let app of data.data) { app.search = app.name.toLowerCase()+" "+app.filename.toLowerCase() }
                $data.apps = me.process(data.data, { filter : { search:"", orgName:"" }}); 
            });
	    },
	
	    changePlugin(plugin) {				
            const me = this;
		    me.doAction(apps.updatePluginStatus(plugin))
	        .then(function() { me.init(); });				
	    }
    },

    created() {
        const { $data, $route } = this, me = this;
        if ($route.query.creator) $data.search.criteria.creatorLogin = $route.query.creator;
	    session.currentUser.then(function(userId) { me.init(userId); });        
    }
}
</script>