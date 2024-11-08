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
    <panel :title="$t('provider_patients.title')" :busy="isBusy">    
        <pagination v-model="patients"></pagination>
	    <table class="table" v-if="patients.filtered.length > 0">
			<thead>
	        <tr>
	            <Sorter sortby="family" v-model="patients">Surname</Sorter>
	            <Sorter sortby="firstname" v-model="patients">Firstname</Sorter>
	            <Sorter sortby="birthDate" v-model="patients">Birthday</Sorter>	        
	        </tr>
			</thead>
			<tbody>
	        <tr v-for="patient in patients.filtered" :key="patient._id">
	            <td><a href="javascript:" @click="selectPatient(patient)">{{ patient.name[0].family }}</a></td>
	            <td>{{ patient.name[0].given[0] }}</td>
	            <td>{{ $filters.date(patient.birthDate) }}</td>	        
	        </tr>
			</tbody>
	    </table>
	    
	    <p v-if="patients.filtered.length == 0" v-t="'provider_patients.empty'">No patients found.</p>
	    
    </panel>
	
</template>
<script>

import Panel from "components/Panel.vue"
import fhir from "services/fhir.js"
import { rl, status, ErrorBox } from 'basic-vue3-components'


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