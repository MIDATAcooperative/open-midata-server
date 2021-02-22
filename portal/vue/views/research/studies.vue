<template>
    <panel :titel="$t('studies.title')" :busy="isBusy">
		<error-box :error="error"></error-box>
        <pagination v-model="results"></pagination>
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
import ErrorBox from "components/ErrorBox.vue"
import Panel from "components/Panel.vue"
import FormGroup from "components/FormGroup.vue"
import server from "services/server.js"
import status from 'mixins/status.js'
import rl from 'mixins/resultlist.js'

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
				$data.results = me.process(data.data);	
		    }));
	    }
    },

    created() {
        console.log("VIEW REACHED");
        this.reload();       
    }
    
}
</script>