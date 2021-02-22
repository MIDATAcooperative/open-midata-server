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
                    <router-link class="btn btn-default btn-sm" :to="{ path : './manageapp', query : { appId : app._id }}" v-t="'developer_yourapps.manage_btn'">manage</router-link> 
                    <router-link class="btn btn-default btn-sm" :to="{ path : './appstats' ,query :  { appId : app._id }}" v-t="'developer_yourapps.debug_info_btn'">debug info</router-link></td>
            </tr>
        </table>
        <p v-if="apps.filtered.length == 0" v-t="'developer_yourapps.empty'"></p>
      
        <router-link class="btn btn-default" :to="{ path : './registerapp' }" v-t="'developer_yourapps.register'">Register a new plugin</router-link>
    </panel>

    <change-log></change-log>	  	   
</template>
<script>

import ErrorBox from "components/ErrorBox.vue"
import FormGroup from "components/FormGroup.vue"
import ChangeLog from "components/tiles/ChangeLog.vue"
import Panel from "components/Panel.vue"
import session from "services/session.js"
import apps from "services/apps.js"
import status from 'mixins/status.js'
import rl from 'mixins/resultlist.js'

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
                $data.apps = me.process(data.data, { filter : { search : "" }}); 
            }));		  		  
	    }
    },

    created() {
        const me = this;        
	    session.currentUser.then(function(userId) { me.init(userId); });        
    }
}
</script>