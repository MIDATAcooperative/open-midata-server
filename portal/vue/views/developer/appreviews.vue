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
     <panel :title="$t('appreviews.title')" :busy="isBusy">		  
	
        <error-box :error="error"></error-box>
          
		  <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="submit()">
		   
		    <form-group name="name" label="appreviews.name">
		      <p class="form-control-plaintext">{{ app.name }}</p>
		    </form-group>
		
		    <table v-if="reviews.length" class="table">
		      <tr>
		        <th v-t="'appreviews.date'"></th>
		        <th v-t="'appreviews.check'"></th>
		        <th v-t="'appreviews.status'"></th>
		        <th v-t="'appreviews.userLogin'"></th>
		        <th v-t="'appreviews.comment'"></th>
		      </tr>
		      <tr v-for="review in reviews" :key="review.timestamp">
		        <td>{{ $filters.date(review.timestamp) }}</td>
		        <td>{{ $t('appreviews.'+review.check) }}</td>
		        <td>{{ $t('appreviews.'+review.status) }}</td> 
		        <td>{{ review.userLogin }}</td>
		        <td>{{ review.comment }}</td>
		      </tr>
		    </table>
		    
		    <p v-else v-t="'appreviews.noreviews'"></p>
		
		    <div v-if="allowReview">
                <form-group name="check" label="appreviews.check" :path="errors.check">
                <select class="form-control" v-validate v-model="newreview.check" required>
                    <option v-for="check in checks" :key="check" :value="check">{{ $t('appreviews.'+check) }}</option>
                </select>
                </form-group>
                <form-group name="status" label="appreviews.status" :path="errors.status">
                    <select class="form-control" v-validate v-model="newreview.status" required>
                        <option v-for="status in stati" :key="status" :value="status">{{ $t('appreviews.'+status) }}</option>
                    </select>
                </form-group>
                <form-group name="comment" label="appreviews.comment" :path="errors.comment">
                <input type="text" class="form-control" v-validate v-model="newreview.comment">
                </form-group>
		    </div>
		    <form-group  label="common.empty">
		      <router-link :to="{ path : './manageapp', query :  {appId:appId} }" class="btn btn-default" v-t="'common.back_btn'"></router-link>
		      <button v-if="allowReview" class="btn btn-primary" :disabled="action!=null" type="submit" v-t="'common.submit_btn'"></button>
		    </form-group>
		   
		  </form>
		 					
    </panel>
			
</div>

</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import apps from "services/apps.js"
import { status, ErrorBox, Success, FormGroup } from 'basic-vue3-components'

export default {
    data: () => ({	
      
        appId : null,
        app : null,

        newreview : null, 
        checks : [ "CONCEPT", "DATA_MODEL", "ACCESS_FILTER", "QUERIES", "DESCRIPTION", "ICONS", "MAILS", "PROJECTS", "CODE_REVIEW", "TEST_CONCEPT", "TEST_PROTOKOLL", "CONTRACT" ],
	    stati : ["ACCEPTED","NEEDS_FIXING"],
	    allowReview : false
    }),

    components: {  Panel, ErrorBox, FormGroup, Success },

    mixins : [ status ],

    methods : {
        loadApp(appId) {
		$scope.appId=appId;
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["creator", "filename", "name", "description", "icons" ]))
		.then(function(data) { 
			$scope.app = data.data[0];			
		});
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.Market.getReviews(appId).url))
		.then(function(reviews) {
			$scope.reviews = reviews.data;
		});
		
		$scope.submitted = false;
	    },
	
	    submit() {
		
            me.doAction("upload", server.post(jsRoutes.controllers.Market.addReview().url, $data.newreview))
            .then(function() {
                $data.newreview = { pluginId : $route.query.appId };
                me.loadApp($route.query.appId);
            });
	    }
    
    },

    created() {
        const { $route, $data } = this, me = this;
        $data.newreview = { pluginId : $route.query.appId };
        $data.allowReview = $route.meta.allowReview;
	
        if ($route.query.check) {
            $scope.newreview.check = $route.query.check;
            $scope.newreview.status = "ACCEPTED";
        }

        me.loadApp($route.query.appId);        
    }
}
</script>