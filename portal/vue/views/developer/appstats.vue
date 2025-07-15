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
     <panel :title="getTitle()" :busy="isBusy">		  
	
        <error-box :error="error"></error-box>
          
        <p v-if="!calls.length" v-t="'appstats.empty'"></p>
        <div v-if="calls.length">
            <p><span v-t="'appstats.first'"></span>: <b>{{ $filters.dateTime(firstrun) }}</b></p>
            <table class="table table-striped" >
              <thead>
                <tr>
                    <th v-t="'appstats.action'"></th>
                    <th v-t="'appstats.lastrun'"></th>              
                    <th colspan="2" v-t="'appstats.count'"></th>
                    <th colspan="2" v-t="'appstats.time'"></th>
                    <th><a href="javascript:" @click="showdb=true;">db</a></th>               
                    <th v-t="'appstats.results'"></th>               
                </tr>
              
                <tr>
                    <th></th>
                    <th></th>              
                    <th>run</th>               
                    <th>retry</th>
                    <th v-t="'appstats.avgtime'"></th>
                    <th v-t="'appstats.lasttime'"></th>
                    <th></th>
                    <th></th>
                </tr>
				</thead>
				<tbody>
                <tr v-for="(call,idx) in calls" :key="idx">
                    <td>{{ call.action }}<span v-if="call.params">?{{ call.params }}</span></td>               
                    <td>{{ $filters.dateTime(call.lastrun) }}</td>               
                    <td>{{ call.count }}</td>              
                    <td>{{ call.conflicts }}</td>
                    <td>{{ Math.round(call.totalExecTime / call.count) }} ms</td>
                    <td>{{ call.lastExecTime }} ms</td>
                    <td>
                        <div v-if="showdb">
                            <div v-for="(e,idx2) in call.queries" :key="idx2">{{ e.k }} : {{ Math.round(e.v / call.count) }}</div>
                        </div>
                    </td>
                    <td>
                        <div v-for="(v,k) in call.resultCount" :key="v" :class="{ 'text-danger' : (k>=400) }">status {{ k }} : {{ v }}</div>
                        <div class="text-info" v-for="(comment,idx3) in call.comments" :key="idx3">{{ comment }}</div>
                    </td>
               
                </tr>
				</tbody>
            </table>
           
           
 		</div>
 		<router-link class="btn btn-default me-1" :to="{ path : './manageapp', query : { appId : appId } }" v-t="'common.back_btn'"></router-link>
		
		<button type="button" class="btn btn-default me-1" v-t="'appstats.reload_btn'" @click="reload();"></button>
		<button type="button" class="btn btn-default me-1" v-t="'appstats.reset_btn'" @click="reset();"></button>
		   
		<div v-if="calls.length">
			<hr>
			<div><code>run</code>: <span v-t="'appstats.run'"></span></div>
	        <div><code>db</code>: <span v-t="'appstats.db'"></span></div>           
	        <div><code>retry</code>: <span v-t="'appstats.retry'"></span></div>
        </div>
                     
    </panel>
			
</div>

</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import session from "services/session.js"
import apps from "services/apps.js"
import { status, ErrorBox, Success, FormGroup } from 'basic-vue3-components'
import _ from "lodash";


export default {
    data: () => ({	
        calls : [],
        app : null,
        userId : null,
        appId : null,
        firstrun : false
    }),

    components: {  Panel, ErrorBox, FormGroup, Success },

    mixins : [ status ],

    methods : {
          getTitle() {
            const { $route, $t, $data } = this;
            let p = this.$data.app ? this.$data.app.name+" - " : "";
            return p+$t("manageapp.stats_btn");                       
        },

        init(userId, appId) {
            const { $data } = this, me = this;
            $data.userId = userId;
            $data.appId = appId;
            //var properties = {"owner": userId, "visualization" : appId, "context" : "sandbox" };
            //var fields = ["name", "type", "order", "autoImport", "context", "visualization"];
            
            me.doBusy(server.get(jsRoutes.controllers.Market.getPluginStats(appId).url)
            .then(function(results) {	    	
                $data.calls = _.orderBy(results.data, ["lastrun"], [ "desc" ]);
                var firstrun;
                for (let c of $data.calls) {
                    if (!firstrun || c.firstrun < firstrun) firstrun = c.firstrun;
                
                    c.queries = [];
                    for (let k in c.queryCount) {
                        let v = c.queryCount[k];
                        c.queries.push({ k : k, v : v});
                    }
                    c.queries = _.orderBy(c.queries, ["k"], ["asc"]);
                }
                                
                $data.firstrun = firstrun;
            }));		
            
            me.doBusy(apps.getApps({ "_id" : appId }, ["filename", "name"])
            .then(function(data) { 
                $data.app = data.data[0];						
            }));
	    },
	
	    reload() {
            const { $data } = this, me = this;
		    this.init($data.userId, $data.appId);
	    },
	
	    reset() {
            const { $data } = this, me = this;
            this.doBusy(server.delete(jsRoutes.controllers.Market.deletePluginStats($data.appId).url))
            .then(function() {	    	
                $data.calls = [];
            });		
	    }
    },

    created() {
        const { $route } = this, me = this;
        session.currentUser.then(function(userId) { me.init(userId, $route.query.appId); });	        
    }
}
</script>