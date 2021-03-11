<template>
    <panel :title="$t('provider_patients.title')" :busy="isBusy">    
        <pagination v-model="patients"></pagination>
	    <table class="table" v-if="patients.filtered.length > 0">
	        <tr>
	            <Sorter sortby="family" v-model="patients">Surname</Sorter>
	            <Sorter sortby="firstname" v-model="patients">Firstname</Sorter>
	            <Sorter sortby="birthDate" v-model="patients">Birthday</Sorter>	        
	        </tr>
	        <tr v-for="patient in patients.filtered" :key="patient._id">
	            <td><a href="javascript:" @click="selectPatient(patient)">{{ patient.name[0].family }}</a></td>
	            <td>{{ patient.name[0].given[0] }}</td>
	            <td>{{ $filters.date(patient.birthDate) }}</td>	        
	        </tr>
	    </table>
	    
	    <p v-if="patients.filtered.length == 0" v-t="'provider_patients.empty'">No patients found.</p>
	    
    </panel>
	
</template>
<script>
import ErrorBox from "components/ErrorBox.vue"
import Panel from "components/Panel.vue"

import fhir from "services/fhir.js"

import status from 'mixins/status.js'
import rl from 'mixins/resultlist.js'


export default {
    data: () => ({	
        patients : null
    }),

    components: {  Panel, ErrorBox },

    mixins : [ status, rl ],

    methods : {
        dosearch() {
            const { $data } = this, me = this;
		    me.doBusy(fhir.search("Patient", {}).
		    then(function(result) { 	
                for (let p of result) {
                    p.family = p.name[0].family;
                    p.given = p.name[0].given[0];
                }
		        $data.patients = me.process(result);
		    }));
	    },
	
	    selectPatient(patient) {
            const { $data, $router } = this;
		    $router.push({ path : './memberdetails', query : { user : patient.id } });
	    }
	
    },

    created() {        
        this.dosearch();      
    }
}
</script>