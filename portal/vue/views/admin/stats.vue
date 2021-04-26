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
    <panel :title="$t('admin_stats.health.title')" :busy="isBusy">
		<error-box :error="error"></error-box>
		<table class="table table-striped table-sm">
		    <tr>
		        <td v-t="'admin_stats.health.servicekey'"></td>
		        <td>
		            <span class="fas fa-check text-success" v-if="health.servicekey"></span>
		            <span class="fas fa-times text-danger" v-else></span>
		            {{ health.servicekey }}
		            <button class="btn btn-primary btn-sm" v-t="'admin_stats.health.requestkey_btn'" :disabled="action=='key'" @click="requestKey()" v-if="!health.servicekey"></button>
		        </td>
		    </tr>
		    <tr>
		        <td v-t="'admin_stats.health.cluster'"></td>
		        <td>
		            <div v-for="member in health.cluster" :key="member.address">
		                <span class="fas fa-check text-success" v-if="member.status=='Up'"></span>
		                <span class="fas fa-times text-danger" v-else></span>
		                {{ member.status }} :
		                {{ member.address }}
		            </div>
                </td>
		    </tr>
		</table>
    </panel>
    <panel :title="$t('admin_stats.title')" :busy="isBusy">
		
		<p class="lead">{{ $filters.dateTime(today.date) }}</p>
		<table class="table table-striped table-sm">
		    <tr>
		        <th></th>
		        <th class="text-right" v-t="'admin_stats.value'"></th>
		        <th class="text-right" v-t="'admin_stats.diff_yesterday'"></th>
		        <th class="text-right" v-t="'admin_stats.diff_week'"></th>
		    </tr>
		  
		    <tr class="info">
		        <td v-t="'admin_stats.recordCount'"></td>
		        <td class="text-right">{{ today.recordCount  }}</td>
		        <td class="text-right">{{ today.recordCount - yesterday.recordCount }}</td>
		        <td class="text-right">{{ today.recordCount - week.recordCount }}</td>
		    </tr>
		     		     
		    <tr>
		        <td v-t="'admin_stats.appCount'"></td>
		        <td class="text-right">{{ today.appCount }}</td>
		        <td class="text-right">{{ today.appCount - yesterday.appCount }}</td>
		        <td class="text-right">{{ today.appCount - week.appCount }}</td>
		    </tr>
		     
		    <tr>
		        <td v-t="'admin_stats.runningStudyCount'"></td>
		        <td class="text-right">{{ today.runningStudyCount }}</td>
		        <td class="text-right">{{ today.runningStudyCount - yesterday.runningStudyCount }}</td>
		        <td class="text-right">{{ today.runningStudyCount - week.runningStudyCount }}</td>
		    </tr>
		     
		    <tr>
		        <td v-t="'admin_stats.groupCount'"></td>
		        <td class="text-right">{{ today.groupCount }}</td>
		        <td class="text-right">{{ today.groupCount - yesterday.groupCount }}</td>
		        <td class="text-right">{{ today.groupCount - week.groupCount }}</td>
		    </tr>
		     
		    <tr>
		        <td v-t="'admin_stats.auditEventCount'"></td>
		        <td class="text-right">{{ today.auditEventCount }}</td>
		        <td class="text-right">{{ today.auditEventCount - yesterday.auditEventCount }}</td>
		        <td class="text-right">{{ today.auditEventCount - week.auditEventCount }}</td>
		    </tr>
		     		     
		    <tr>
		        <td v-t="'admin_stats.vRecordCount'"></td>
		        <td class="text-right">{{ today.vRecordCount  }}</td>
		        <td class="text-right">{{ today.vRecordCount - yesterday.vRecordCount }}</td>
		        <td class="text-right">{{ today.vRecordCount - week.vRecordCount }}</td>
		    </tr>
		     
		    <tr>
		        <td v-t="'admin_stats.indexPageCount'"></td>
		        <td class="text-right">{{ today.indexPageCount  }}</td>
		        <td class="text-right">{{ today.indexPageCount - yesterday.indexPageCount }}</td>
		        <td class="text-right">{{ today.indexPageCount - week.indexPageCount }}</td>
		    </tr>
		     
		    <tr>
		        <th v-t="'admin_stats.users'"></th>
		        <th class="text-right" v-t="'admin_stats.value'"></th>
		        <th class="text-right" v-t="'admin_stats.diff_yesterday'"></th>
		        <th class="text-right" v-t="'admin_stats.diff_week'"></th>
		    </tr>
		     
		    <tr v-for="role in today.userCount" :key="role" :class="{ 'info' : role == 'MEMBER' }" v-show="!(role=='ANY')">
		        <td>{{ $t('enum.userrole.'+role) }}</td>
		        <td class="text-right">{{ today.userCount[role] }}</td>
		        <td class="text-right">{{ today.userCount[role] - yesterday.userCount[role] }}</td>
		        <td class="text-right">{{ today.userCount[role] - week.userCount[role] }}</td>
		    </tr>
		     
		    <tr>
		        <th v-t="'admin_stats.languages'"></th>
		        <th class="text-right" v-t="'admin_stats.value'"></th>
		        <th class="text-right" v-t="'admin_stats.diff_yesterday'"></th>
		        <th class="text-right" v-t="'admin_stats.diff_week'"></th>
		    </tr>
		     
		    <tr v-for="lang in today.languages" :key="lang">
		        <td>{{ $t('enum.language.'+lang.toUpperCase()) }}</td>
		        <td class="text-right">{{ today.languages[lang] }}</td>
		        <td class="text-right">{{ today.languages[lang] - (yesterday.languages[lang] || 0) }}</td>
		        <td class="text-right">{{ today.languages[lang] - (week.languages[lang] || 0) }}</td>
		    </tr>
		     
		    <tr>
		        <th v-t="'admin_stats.consents'"></th>
		        <th class="text-right" v-t="'admin_stats.value'"></th>
		        <th class="text-right" v-t="'admin_stats.diff_yesterday'"></th>
		        <th class="text-right" v-t="'admin_stats.diff_week'"></th>
		    </tr>
		     
		    <tr v-for="type in today.consentCount" :key="type">
		        <td>{{$t('enum.consenttype.'+type) }}</td>
		        <td class="text-right">{{ today.consentCount[type] }}</td>
		        <td class="text-right">{{ today.consentCount[type] - yesterday.consentCount[type] }}</td>
		        <td class="text-right">{{ today.consentCount[type] - week.consentCount[type] }}</td>
		    </tr>
		     
		  </table>
		  <div class="margin-top">
		    <router-link :to="{ path : './usagestats' }" v-t="'admin_stats.showusage'"></router-link>
	      </div>
    </panel>
	<change-log></change-log>
</template>
<script>

import ChangeLog from "components/tiles/ChangeLog.vue"
import Panel from "components/Panel.vue"
import server from "services/server.js"
import crypto from "services/crypto.js"
import { status, ErrorBox } from 'basic-vue3-components'

export default {

    data: () => ({	
        result : null,
        today : null,
        yesterday : null,
        week : null,
        health : null
    }),

    components: {  Panel, ErrorBox, ChangeLog },

    mixins : [ status ],

    methods : {
        refresh() {
            const { $data } = this, me = this;
		    var limit = new Date();
		    limit.setDate(limit.getDate()-7);
		    var data = { "properties" : { "date" : { "$gt" : limit }}};
		    me.doBusy(server.post(jsRoutes.controllers.admin.Administration.getStats().url, data)
		    .then(function(result) {
			    						
			    var ordered = _.orderBy(result.data, [ "date" ], [ true ]);
				$data.result = ordered;	
                if (ordered.length) {
                    $data.today = ordered[0];
                    $data.yesterday = ordered.length > 1 ? ordered[1] : ordered[0];
                    $data.week = ordered[ordered.length-1];
                } else {
                    $data.today = { date : "" };
                    $data.yesterday = {};
                    $data.week = {};
                }
			
		    }));
		
		    me.doBusy(server.get(jsRoutes.controllers.admin.Administration.getSystemHealth().url)
		    .then(function(result) {
			    $data.health = result.data;						
		    }));
	    },
	
	    requestKey() {
            const { $data, $router } = this, me = this;
		    crypto.generateKeys("12345").then(function(keys) {
                var data = {  };					
                data.password = keys.pw_hash;
                data.pub = keys.pub;
                data.priv_pw = keys.priv_pw;
                data.recovery = keys.recovery;
                data.recoveryKey = keys.recoveryKey;
            
                return me.doAction("key", server.post(jsRoutes.controllers.PWRecovery.requestServiceKeyRecovery().url, data));
		    }).then(function() {
			    $router.push({ path : './pwrecover' });
		    });			
	    }		    
    },

    created() {
        this.refresh();
    }
}
</script>