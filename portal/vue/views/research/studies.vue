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
    <panel :title="$t('studies.title')" :busy="isBusy">
		<error-box :error="error"></error-box>
        <pagination v-model="results" search="name"></pagination>
	    <table v-if="results.filtered.length" class="table table-hover">
	        <thead>
	            <tr>
	                <Sorter sortby="name" v-model="results" v-t="'studies.name'"></Sorter>
	                <Sorter sortby="validationStatus" v-model="results" v-t="'studies.validation'"></Sorter>
	                <Sorter sortby="participantSearchStatus" v-model="results" v-t="'studies.participants'"></Sorter>
	                <Sorter sortby="executionStatus" v-model="results" v-t="'studies.execution'"></Sorter>
	                <Sorter sortby="createdAt" v-model="results" v-t="'studies.creation_date'"></Sorter>
	            </tr>
	        </thead>
	        <tbody>
	            <tr v-for="study in results.filtered" :key="study._id">
	                <td><router-link :to="{ path : './study.overview', query : { studyId : study._id}}">{{ study.name }}</router-link></td>	  
	                <td>{{ $t('enum.studyvalidationstatus.'+study.validationStatus) }}</td>
	                <td>{{ $t('enum.participantsearchstatus.'+study.participantSearchStatus) }}</td>
	                <td>{{ $t('enum.studyexecutionstatus.'+study.executionStatus) }}</td>
	                <td>{{ $filters.date(study.createdAt) }}</td>	         
	            </tr>
	        </tbody>
	    </table>
	    <p v-if="!results.filtered.length" v-t="'studies.empty'"></p>
	    <router-link class="btn btn-primary" :to="{ path : './createstudy' }" v-t="'studies.create_btn'"></router-link>
	    <div class="extraspace"></div>							
    </panel>  
</template>
<script>
import Panel from "components/Panel.vue"
import server from "services/server.js"
import { rl, status, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {
    data: () => ({	
        results : null
    }),

    components: {  Panel, ErrorBox, FormGroup },

    mixins : [ status, rl ],

    methods : {
        reload() {
			const { $data } = this, me = this;
		    me.doBusy(server.get(jsRoutes.controllers.research.Studies.list().url)
		    .then(function(data) { 				
				$data.results = me.process(data.data, { filter : { name : "" }, ignoreCase : true, sort : "-createdAt" });	
		    }));
	    }
    },

    created() {
       
        this.reload();       
    }
    
}
</script>