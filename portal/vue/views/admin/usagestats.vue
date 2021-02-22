<template>
    <panel :title="$t('usagestats.title')" :busy="isBusy">
		 		  
		<p v-if="app" class="lead">{{ app.name }}</p>
		  
		<form class="row">
            <div class="form-group col-4">
                <label for="from">From:</label>
                <div class="form-control-static">{{ $filters.date(criteria.from) }}</div>
            </div>
            
            <div class="form-group col-4">
                <label for="days">Days:</label>
                <input type="number" class="form-control" @input="recalc()" v-model="criteria.days">
            </div>
            
            <div class="form-group col-4">
                <label for="until">Until:</label>            
                <input type="date" id="date" @input="recalc()" class="form-control" autofocus v-model="criteria.to">				  
            </div>
              	    	    
        </form>           
		  
		  
		<table class="table table-striped table-sm">
		    <tr>
		        <Sorter sortby="date" v-model="result" v-t="'usagestats.date'"></Sorter>
		        <Sorter sortby="objectName" v-model="result" v-t="'usagestats.object'"></Sorter>
		        <Sorter sortby="actions.REGISTRATION.count" v-model="result" v-t="'usagestats.REGISTRATION'"></Sorter>
		        <Sorter sortby="actions.LOGIN.count" v-model="result" v-t="'usagestats.LOGIN'"></Sorter>
		        <Sorter sortby="actions.REFRESH.count" v-model="result" v-t="'usagestats.REFRESH'"></Sorter>
		        <Sorter sortby="actions.INSTALL.count" v-model="result" v-t="'usagestats.INSTALL'"></Sorter>
		        <Sorter sortby="actions.GET.count" v-model="result" v-t="'usagestats.GET'"></Sorter>
		        <Sorter sortby="actions.POST.count" v-model="result" v-t="'usagestats.POST'"></Sorter>
		        <Sorter sortby="actions.PUT.count" v-model="result" v-t="'usagestats.PUT'"></Sorter>
		        <Sorter sortby="actions.DELETE.count" v-model="result" v-t="'usagestats.DELETE'"></Sorter>		      
		    </tr>
		    <tr v-for="(entry,idx) in result.filtered" :key="idx">
		        <td>{{ entry.date }}</td>
		        <td><router-link :to="{ path : './manageapp', query :  { appId : entry.object } }">{{ entry.objectName }}</router-link></td>
		        <td v-for="ac in actions" :key="ac">
		            <span v-if="entry.actions[ac]">{{ entry.actions[ac].count }}</span>
		        </td>
		    </tr>
		</table>
    </panel>
</template>
<script>

import ErrorBox from "components/ErrorBox.vue"
import Panel from "components/Panel.vue"
import server from "services/server.js"
import apps from "services/apps.js"
import status from 'mixins/status.js'
import rl from 'mixins/resultlist.js'

export default {

    data: () => ({	
        now : new Date(),
        criteria : { from: null, to : null, days:7  },
        actions : ["REGISTRATION","LOGIN","REFRESH","INSTALL","GET","POST","PUT","DELETE"],
        result : null
    }),

    components: {  Panel, ErrorBox },

    mixins : [ status, rl ],

    methods : {
        recalc() {
            const { $data } = this, me = this;
             
            $data.criteria.from = new Date($data.criteria.to);
            $data.criteria.from.setDate(new Date($data.criteria.to).getDate() - $data.criteria.days);

    	    me.refresh();
        },
		
	    loadApp(appId) {
            const { $data } = this, me = this;
		    me.doBusy(apps.getApps({ "_id" : appId }, ["creator", "creatorLogin", "filename", "name", "description", "tags", "targetUserRole","i18n", "orgName", "publisher"])
		    .then(function(data) { 
			    $data.app = data.data[0];			
		    }));
	    },
	
	    refresh() {
            const { $data, $route } = this, me = this;
            //var limit = new Date();
            //limit.setDate(limit.getDate()-30);
            var data = { "properties" : { "date" : { "$gte" : $data.criteria.from, "$lte" : $data.criteria.to }}};
            if ($route.query.appId) data.properties.object = $route.query.appId;
            me.doBusy(server.post(jsRoutes.controllers.admin.Administration.getUsageStats().url, data)
            .then(function(result) {
                //$scope.result = result.data;
                
                var bykey = {};                
                var list = [];
                for (let r of result.data) {
                    var k = r.date+r.object;
                    var grp = bykey[k];
                    if (!grp) {
                        bykey[k] = grp = { object : r.object, date : r.date, objectName : r.objectName, actions : {} };
                        list.push(grp);
                    }
                    grp.actions[r.action] = r;
                }
                
                $data.result = me.process(list);
            }));				
	    }
	       
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.criteria.to = this.$filters.usDate(new Date());
        if ($route.query.appId) {
            $data.criteria.days = 30;
            me.loadApp($route.query.appId);
        }    
        this.recalc();
    }
}
</script>