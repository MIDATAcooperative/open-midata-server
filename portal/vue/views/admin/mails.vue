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
    <panel :title="$t('admin_mails.title')" :busy="isBusy">
    
        <error-box :error="error"></error-box>
        <pagination v-model="mails"></pagination>
        <table class="table" v-if="mails.filtered.length">
            <thead>
                <tr>
                    <Sorter v-model="mails" sortby="created" v-t="'admin_mails.created'"></Sorter>
                    <Sorter v-model="mails" sortby="name" v-t="'admin_mails.name'"></Sorter>
                    <Sorter v-model="mails" sortby="status" v-t="'admin_mails.status'"></Sorter>
                    <Sorter v-model="mails" sortby="progressCount" v-t="'admin_mails.progressCount'"></Sorter>
                    <Sorter v-model="mails" sortby="studyName" v-t="'admin_mails.studyName'"></Sorter>
                    <Sorter v-model="mails" sortby="creatorName" v-t="'admin_mails.creatorName'"></Sorter>
                </tr>
            </thead>
            <tbody>
                <tr v-for="item in mails.filtered" :key="item._id">
                    <td>{{ $filters.date(item.created) }}</td>
                    <td><router-link :to="{ path : './managemails', query :  { mailId : item._id } }">{{ item.name }}</router-link></td>
                    <td>{{ $t('admin_mails.stati.'+item.status ) }}</td>
                    <td>{{ item.progressCount }}</td>
                    <td>{{ item.studyName }}{{ item.appName }}</td>
                    <td>{{ item.creatorName }}</td>                   
                </tr>
            </tbody>
      </table>
      <p v-else v-t="'admin_mails.empty'"></p>
      
      <router-link class="btn btn-primary" v-t="'common.add_btn'" :to="{ path : './newmail' }"></router-link> 
            
    </panel>
  	   
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"

import { status, rl, ErrorBox } from 'basic-vue3-components'

export default {

    data: () => ({	
        mails : []
    }),

    components: {  Panel, ErrorBox },

    mixins : [ status, rl ],
    
    created() {
        const { $data } = this, me = this;
        me.doBusy(server.post(jsRoutes.controllers.BulkMails.get().url, { properties:{}, fields:["creator", "creatorName", "developerTeam", "created", "started", "finished", "name", "status", "title", "content", "studyId", "studyName", "studyCode", "studyGroup", "progressId", "progressCount"] })
    	.then(function(results) {
		    $data.mails = me.process(results.data, { sort : "-created" });
    	}));
    }
}
</script>