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
    <panel :title="$t('developer_yourapps.title')" :busy="isBusy">
    
        <error-box :error="error"></error-box>
        <pagination v-model="apps" search="search"></pagination>

        <table class="table" v-if="apps.filtered.length">
            <tr>
                <Sorter sortby="name" v-model="apps" v-t="'developer_yourapps.name'">Name</Sorter>
                <Sorter sortby="filename" v-model="apps" v-t="'developer_yourapps.filename'">Internal</Sorter>
                <Sorter sortby="type" v-model="apps" v-t="'developer_yourapps.type'">Type</Sorter>
                <Sorter sortby="targetUserRole" v-model="apps" v-t="'developer_yourapps.role'">Target Role</Sorter>
                <th></th>
            </tr>
            <tr v-for="app in apps.filtered" :key="app._id">
                <td><router-link :to="{ path : './manageapp', query : { appId : app._id } }">{{  app.name }}</router-link></td>
                <td>{{ app.filename }}</td>
                <td>{{ $t('enum.plugintype.'+app.type) }}</td>
                <td>{{ $t('enum.userrole.'+app.targetUserRole) }}</td>
                <td>
                    <router-link class="btn btn-default btn-sm mr-1" :to="{ path : './manageapp', query : { appId : app._id }}" v-t="'developer_yourapps.manage_btn'">manage</router-link> 
                    <router-link class="btn btn-default btn-sm" :to="{ path : './appstats' ,query :  { appId : app._id }}" v-t="'developer_yourapps.debug_info_btn'">debug info</router-link></td>
            </tr>
        </table>
        <p v-if="apps.filtered.length == 0" v-t="'developer_yourapps.empty'"></p>
      
        <router-link class="btn btn-default" :to="{ path : './registerapp' }" v-t="'developer_yourapps.register'">Register a new plugin</router-link>
    </panel>

    <change-log></change-log>	  	   
</template>
<script>

import ChangeLog from "components/tiles/ChangeLog.vue"
import Panel from "components/Panel.vue"
import session from "services/session.js"
import apps from "services/apps.js"
import { rl, status, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {

    data: () => ({	        
        apps : null
    }),

    components: {  Panel, ErrorBox, FormGroup, ChangeLog },

    mixins : [ status, rl ],

    methods : {
        init(userId) {	
            const { $data }	= this, me = this;
		    me.doBusy(apps.getApps({ developerTeam : userId }, [ "creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type"])
		    .then(function(data) { 
                for (let app of data.data) { app.search = app.name.toLowerCase()+" "+app.filename.toLowerCase(); }
                $data.apps = me.process(data.data, { filter : { search : "" }, ignoreCase : true, sort : "name" }); 
            }));		  		  
	    }
    },

    created() {
        const me = this;        
	    session.currentUser.then(function(userId) { me.init(userId); });        
    }
}
</script>