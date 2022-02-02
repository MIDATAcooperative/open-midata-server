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
    <div class="midata-overlay borderless">
	  <div class="overlay-body">
        <panel :title="$t('recorddetail.meta')" :busy="isBusy" @close="goBack()">        					
			<div class="row">
				<div class="col-md-4 col-lg-2" v-t="'recorddetail.name'"></div>
				<div class="col-md-8 col-lg-10 main-col">{{record.name}}</div>
			</div>
			<div class="row mt-1">
				<div class="col-md-4 col-lg-2" v-t="'recorddetail.owned_by'"></div>
				<div class="col-md-8 col-lg-10 main-col">{{record.ownerName}}</div>
			</div>
			<div class="row mt-1">
				<div class="col-md-4 col-lg-2" v-t="'recorddetail.created_by'"></div>
				<div class="col-md-8 col-lg-10 main-col">{{record.creatorName}}</div>
			</div>
			<div class="row mt-1">
				<div class="col-md-4 col-lg-2" v-t="'recorddetail.created_with'"></div>
				<div class="col-md-8 col-lg-10 main-col">{{record.app}}</div>
			</div>
			<div class="row mt-1">
				<div class="col-md-4 col-lg-2" v-t="'recorddetail.created_on'"></div>
				<div class="col-md-8 col-lg-10 main-col">{{ $filters.date(record.created) }}</div>
			</div>
			<div class="row mt-1">
				<div class="col-md-4 col-lg-2" v-t="'recorddetail.id'"></div>
				<div class="col-md-8 col-lg-10 main-col">{{record._id}}</div>
			</div>		
			<div v-if="record.format && record.format.startsWith('fhir/')" class="row mt-1">
				<div class="col-md-4 col-lg-2" v-t="'enum.codesystems.fhir'"></div>
				<div class="col-md-8 col-lg-10 main-col">{{record.format.substr(5)}}</div>
			</div>
			<div v-else class="row mt-1">
				<div class="col-md-4 col-lg-2" v-t="'recorddetail.format'"></div>
				<div class="col-md-8 col-lg-10 main-col">{{record.format}}</div>
			</div>
		    <div class="row mt-1">
				<div class="col-md-4 col-lg-2" v-t="'recorddetail.code'"></div>
				<div class="col-md-8 col-lg-10 main-col">
				  <div v-for="code in record.code" :key="code">{{code}}</div>
				</div>
			</div>
			<div class="row mt-1">
				<div class="col-md-4 col-lg-2" v-t="'recorddetail.content'"></div>
				<div class="col-md-8 col-lg-10 main-col">{{record.content}}</div>
			</div>		
			<button href="javascript:" @click="download()" class="btn btn-primary" v-if="isFile()" v-t="'recorddetail.download_btn'">Download</button>
        </panel>
		<panel :title="$t('recorddetail.content_visualization')" :busy="isBusy">
            <div v-if="!url" class="body" v-t="'recorddetail.no_visualization'"></div>
			<div v-if="url" class="body">
                <div id="iframe" style="min-height:200px;width:100%;" v-pluginframe="url"></div>                			  
			</div>
        </panel>		
	  </div>	
	</div>
</template>
<script>

import Panel from 'components/Panel.vue';
import spaces from 'services/spaces.js';
import records from 'services/records.js';
import session from 'services/session.js';
import server from 'services/server.js';
import { status, ErrorBox } from 'basic-vue3-components';
import { getLocale } from 'services/lang.js';

export default {
    data: () => ({       
        record: {},
        url : null,
        userId : null
		
	}),				

	components : { ErrorBox, Panel },

    mixins : [ status ],

    methods : {
        init() {
            const { $data, $route } = this, me = this;
            let recordId = $route.query.recordId;
        	me.doBusy(records.getUrl(recordId)).
	        then(function(results) {
		        if (results.data) {
                    var url = spaces.mainUrl(results.data, getLocale());          
		            $data.url = url;
		        }
	        });
		
	        me.doBusy(server.post(jsRoutes.controllers.Records.get().url, {"_id": recordId }))
	        .then(function(records) {
			    let record = records.data;
			    let data = {"properties": {"_id": record.app}, "fields": ["name"]};
		        me.doBusy(server.post(jsRoutes.controllers.Plugins.get().url, data).
			    then(function(apps) { 
                    record.app = apps.data[0].name; 
                    $data.record = record;
                }));
		    });
        },
			
        goBack() {
            const { $router } = this, me = this;
            $router.go(-1);
        },
	
        download() {
            const { $data } = this, me = this;
            me.doAction("download", server.token())
            .then(function(response) {
                document.location.href = ENV.apiurl + jsRoutes.controllers.Records.getFile($data.record._id).url + "?token=" + encodeURIComponent(response.data.token);
            });
        },
	
	    isFile() {
            const { $data } = this;
		    return $data.record && $data.record.data && ($data.record.data.resourceType === "Binary" || 
		       ($data.record.data.resourceType === "DocumentReference" && $data.record.data.content) ||
		       $data.record.data.resourceType === "Media" ||
		       ($data.record.data.resourceType === "DiagnosticReport" && $data.record.data.presentedForm));
	    },
	
	    openAppLink(data) {	
            const { $data, $route, $router } = this;	
		    spaces.openAppLink($router, $route, $data.userId, data);	 
	    }
    },

    created() {
        const { $data } = this, me = this;
        this.doBusy(session.currentUser.then(function(userId) {
            $data.userId = userId;
            me.init();
        }));
    }
}
</script>