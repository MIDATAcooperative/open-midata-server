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
    <panel :title="$t('admin_studies.title')" :busy="isBusy">
    
        <error-box :error="error"></error-box>
        <form class="css-form form-horizontal">		   
		    <form-group name="criteria" label="admin_studies.criteria">
		        <select class="form-control" name="criteria" @change="doreload();" v-model="selection.criteria">
                    <option v-for="(crit,idx) in searches" :key="idx" :value="idx">{{ $t(crit.id) }}</option>
                </select>
		   </form-group>		      
	    </form>	
			
        <pagination v-model="results" search="codeName"></pagination>
	    <table v-if="results.filtered.length" class="table table-hover">
	        <thead>
	            <tr>
                    <Sorter sortby="code" v-model="results" v-t="'admin_studies.code'"></Sorter>
	                <Sorter sortby="name" v-model="results" v-t="'admin_studies.name'"></Sorter>
	                <Sorter sortby="type" v-model="results" v-t="'admin_studies.type'"></Sorter>
	                <Sorter sortby="createdAt" v-model="results" v-t="'admin_studies.created'"></Sorter>
	            </tr>
	        </thead>
	        <tbody>
	            <tr v-for="study in results.filtered" :key="study._id">
                    <td><router-link :to="{ path : './astudy' ,query : { studyId : study._id}}">{{ study.code }}</router-link></td>
	                <td><router-link :to="{ path : './astudy' ,query : { studyId : study._id}}">{{ study.name }}</router-link></td>
	                <td>{{ $t('enum.studytype.' + study.type) }}</td>
	                <td>{{ $filters.date(study.createdAt) }}</td>	         
	            </tr>
	        </tbody>
	    </table>
	    <p v-else v-t="'admin_studies.empty'"></p>
	
	    <router-link :to="{ path : './definestudy' }" class="btn btn-default" v-t="'admin_studies.add_definition_btn'"></router-link>
			      
    </panel>
  	   
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"

import { status, rl, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {

    data: () => ({	
        results : null,
        selection : { criteria : 0 },
        searches : [
            {
                id : "enum.studyvalidationstatus.VALIDATION",
                properties : {
                "validationStatus" : "VALIDATION"
                }
            },
            {
                id :"enum.participantsearchstatus.SEARCHING",
                properties : {
                    "participantSearchStatus" : "SEARCHING"
                }
            },
            {
                id : "enum.studyexecutionstatus.RUNNING",
                properties : {
                    "executionStatus" : "RUNNING"
                }
            },
            {
                id : "enum.studyvalidationstatus.DRAFT",
                properties : {
                    "validationStatus" : "DRAFT"
                }
            },
            {
                id : "enum.studyvalidationstatus.REJECTED",
                properties : {
                    "validationStatus" : "REJECTED"
                }
            },
            {
                id : "enum.studyvalidationstatus.VALIDATED",
                properties : {
                    "validationStatus" : "VALIDATED"
                }
            },
            {
                id : "admin_studies.ALL",
                properties : {
                    
                }
            }
                
	    ]
    }),

    components: {  Panel, ErrorBox, FormGroup },

    mixins : [ status, rl ],

    methods : {
        doreload() {
			const { $data } = this, me = this;
            me.doBusy(server.post(jsRoutes.controllers.research.Studies.listAdmin().url, $data.searches[$data.selection.criteria])
            .then(function(data) {
                    for (let st of data.data) st.codeName = st.name+" "+st.code;
                    $data.results = me.process(data.data, { filter : { codeName : "" }});	
            }));
	    }
    },
    
    created() {
        this.doreload();       
    }
}
</script>