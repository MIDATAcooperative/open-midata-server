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
    <study-nav page="study.records"></study-nav>
    <tab-panel :busy="isBusy">
        <error-box :error="error"></error-box>

        <p class="alert alert-info" v-t="'studyrecords.empty'" v-if="study.myRole.export">Once data is available it can be downloaded here.</p>      
        <p class="alert alert-warning" v-t="'studyrecords.wrongrole'" v-else></p>
                
        <form name="myform" ref="myform" novalidate class="form form-horizontal">
            <form-group name="startDate" label="studyrecords.startDate" :path="errors.startDate">	    
                <input id="startDate" name="startDate" type="date" class="form-control" v-model="filter.startDate" />                              
            </form-group>
            
            <form-group name="file" label="studyrecords.file" :path="errors.file">
                <select id="file" name="file" v-model="filter.selectedFile" class="form-control">
                   <option value="*fhir" v-t="'studyrecords.file_fhir'"></option>
                   <option v-for="file in files" :key="file" :value="file">CSV: {{file}}</option>
                </select>
            </form-group>
    
            <table class="table table-striped" v-if="infos.length">	
                <tr>
                    <th v-t="'studyrecords.group'">Data Group</th>
                    <th v-t="'studyrecords.number_of_records'"># Records</th>
                    <th colspan="2">&nbsp;</th>
                </tr>
                <tr v-for="info in infos" :key="info.group">        
                    <td>{{ info.group }}</td>      
                    <td><span v-if="info.count==-1" v-t="'-1'"></span><span v-else>{{ info.count }}</span></td>
                    <td><button type="button" :disabled="action != null || !study.myRole.export" @click="fhirDownload(info, 'pseudonymized')" href="javascript:" class="btn btn-sm btn-primary" v-t="'studyrecords.fhir_download_btn'"></button></td>
                    <td><button type="button" v-if="!study.myRole.pseudo" :disabled="action != null || !study.myRole.export" @click="fhirDownload(info, 'original')" href="javascript:" class="btn btn-sm btn-primary" v-t="'studyrecords.fhir_download_original_btn'"></button></td>
                </tr>
            </table>
            
            <textarea class="form-control mt-1" v-model="jsonDefinition" rows="10" v-if="csvEdit">
            </textarea>
            <error-box class="mt-1" :error="error"></error-box>
            <button type="button" class="mt-3 btn btn-default" @click="addCSVDefinition()" v-t="'studyrecords.add_csvdef_btn'"></button>
        </form>     
    </tab-panel>
</div>

</template>
<script>

import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import server from "services/server.js"
import records from "services/records.js"
import session from "services/session.js"
import { status, ErrorBox, Success, FormGroup } from 'basic-vue3-components'
import ENV from 'config';
import _ from "lodash";

export default {
    data: () => ({	
        studyid : null,
        filter : { startDate : "", selectedFile : "*fhir" },
        files : [],
        jsonDefinition : "",
        csvEdit : false,
        downloadUrl : "",
        infos : [],
        lockChanges : false,
        study : null
    }),

    components: {  Panel, ErrorBox, FormGroup, StudyNav, Success, TabPanel },

    mixins : [ status ],

    methods : {
        reload() {
            const { $data } = this, me = this;   
            session.currentUser.then(function(userId) {
            
                me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
                .then(function(data) { 				
                    $data.study = data.data;
                    $data.lockChanges = !$data.study.myRole.export;
                    $data.infos = [];
                    for (let group of $data.study.groups) {
                        let inf = { group : group.name, count:"-1" };
                        $data.infos.push(inf);
                        let properties = { study : $data.studyid, "study-group" : group.name };
                        me.doBusy(records.getInfos(userId, properties, "ALL")
                        .then(function(results) {						
                            if (results.data && results.data.length == 1) {	inf.count = results.data[0].count; }
                            else if (results.data && results.data.length === 0) { inf.count = 0; }
                        }));
                    }
                }));
                
                me.doBusy(server.get(jsRoutes.controllers.research.CSVDownload.getCSVDef($data.studyid).url)
                .then(function(data) { 		
                    if (data.data && data.data.names) $data.files = data.data.names;
                    if (data.data && data.data.jsonDefinition) $data.jsonDefinition = data.data.jsonDefinition;                      
                }));
            });
							
		    // $data.downloadUrl = ENV.apiurl + jsRoutes.controllers.research.Studies.download($data.studyid).url;
	    },
	
	/*
	    download() {
            const { $data } = this, me = this;   
            me.doAction("download", server.token())
            .then(function(response) {
                document.location.href = ENV.apiurl + jsRoutes.controllers.research.Studies.download($data.studyid).url + "?token=" + encodeURIComponent(response.data.token);
            });
	    },
	*/
	
	    fhirDownload(what, mode) {
		    const { $data, $filters } = this, me = this;   
            me.doAction("download", server.token())
            .then(function(response) {
                var urlParams = "";
                if ($data.filter.startDate) urlParams += "&startDate="+encodeURIComponent(new Date($data.filter.startDate).getTime());
                if ($data.filter.selectedFile == "*fhir") {                           
                   document.location.href = ENV.apiurl + jsRoutes.controllers.research.Studies.downloadFHIR($data.studyid, what.group, mode).url + "?token=" + encodeURIComponent(response.data.token)+urlParams;
                } else {
                   console.log(jsRoutes.controllers.research.CSVDownload.downloadCSV($data.studyid, what.group, mode, $data.filter.selectedFile));
                   document.location.href = ENV.apiurl + jsRoutes.controllers.research.CSVDownload.downloadCSV($data.studyid, what.group, mode, $data.filter.selectedFile).url + "?token=" + encodeURIComponent(response.data.token)+urlParams;
                }
            });
	    },
	    
	    addCSVDefinition() {
	       const { $data, $filters } = this, me = this;
	       if (!$data.csvEdit) {
	         $data.csvEdit = true;
	         return;
	       }
	       let data = { jsonDefinition : $data.jsonDefinition };
	       me.doAction("download", server.post(jsRoutes.controllers.research.CSVDownload.updateCSVDef($data.studyid).url, data))
	       .then(me.reload);	       
	    }
	    
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.studyid = $route.query.studyId;
        me.reload();
    }
}
</script>