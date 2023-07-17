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
<div v-if="log">
    <pagination v-model="log"></pagination>
			

	<div v-for="entry in log.filtered" :key="entry.id">
		<div class="row">
		    <div class="col-sm-6 col-md-2" style="color:#707070">{{ $filters.dateTime(entry.recorded)  }}</div>
		    <div class="col-sm-6 col-md-3">
   			    
			    <span class="fas" :class="{ 'fa-wrench text-danger' : entry.outcome == 12, 'fa-exclamation-triangle text-danger' : entry.outcome == 8, 'fa-times text-warning' : entry.outcome == 4, 'fa-check text-success' : entry.outcome == 0 }"></span> <b v-if="entry.subtype && entry.subtype.length"><span>{{ $t('enum.eventtype.'+entry.subtype[0].code) }}</span></b>			    
			    <div v-if="entry.extension && entry.extension.length && entry.extension[0].valueString">{{ $t(entry.extension[0].valueString) }}</div>	      			    			   
                <div v-else>{{ entry.outcomeDesc }}</div>		
			</div>
			<div class="col-sm-6 col-md-4" v-if="entry.agent">
			    <div>
                    <span v-if="entry.agent[0].name!='?'">{{ entry.agent[0].name }}</span>
                    <span v-else v-t="'auditlog.anonymous'"></span>
                    <span v-if="entry.agent[0].role && entry.agent[0].role[0].coding[0].code == '110150'" class="badge badge-info ml-1">{{ $t('enum.plugintype.external') }}</span> 
                    <span v-else-if="entry.agent[0].role && entry.agent[0].role[0].coding[0].code != 'MEMBER' && entry.agent[0].role[0].coding[0].code != '110150'" class="badge badge-info ml-1">{{ $t('enum.userrole.'+entry.agent[0].role[0].coding[0].code) }}</span>
                </div>
			    <div class="text-primary">{{ entry.agent[0].altId }}</div>
			    <div v-if="entry.agent.length>1 && entry.agent[1].name"><i><span v-t="'auditlog.via'"></span> {{ entry.agent[1].name }}</i></div>
			    <div v-if="entry.agent.length>1 && entry.agent[1].location"><i><span v-t="'auditlog.via'"></span> {{ entry.agent[1].location.display }}</i></div>
			    <div v-if="entry.agent.length>2 && entry.agent[2].name"><i><span v-t="'auditlog.via'"></span> {{ entry.agent[2].name }}</i></div>  
			    <div v-if="entry.agent.length>2 && entry.agent[2].location"><i><span v-t="'auditlog.via'"></span> {{ entry.agent[2].location.display }}</i></div>
			</div><div v-else class="col-sm-6 col-md-4"></div>		  
			  
			<div class="col-sm-6 col-md-3">
			    <div v-for="entity in entry.entity" :key="entity._id">
			        <div v-if="entity.name">
			            <b v-if="entity.type && entity.type.code"><span>{{ $t('auditlog.'+entity.type.code) }}</span>:</b>
			            <div>{{ entity.name }}</div>
			            <div v-if="entity.what" class="text-primary">{{ entity.what.display }}</div>
			       </div>
			    </div>
			</div>
		</div>			  			  
		<div style="border-bottom: 1px solid #e0e0e0; margin-top:10px; margin-bottom:5px"></div>
	</div>
            
    <p v-if="log.filtered.length==0" v-t="'auditlog.empty'"></p>
</div>		
		   
</template>
<script>


import { rl } from 'basic-vue3-components'
import fhir from "services/fhir";

export default {
    props: ['patient', 'entity', 'altentity', 'from', 'to'],

    data : ()=>({      
        title : "",
        log : null
    }),

    components : { },

    mixins : [ rl ],

     computed: {
        range() {
          return `${this.from}|${this.to}`;
        },
      },

    methods : {
        reload() {
            const { $data }	= this, me = this;
            let crit = {};
    		if (me.patient) crit.patient = me.patient;
    		if (me.entity && me.altentity) {
    			crit.entity = [me.entity, me.altentity];
    		} else if (me.entity) crit.entity = me.entity;
    		if (me.from && me.to) crit.date = ["sa"+new Date(me.from).toISOString(), "eb"+new Date(me.to).toISOString()];
    			    			    		
    		crit._count=1001;
    		$data.log = null;
    		fhir.search("AuditEvent", crit)
    		.then(function(log) {    		    
    			$data.log = me.process(log);    				
    		});
    			
    			
    	}
    },

    watch : {
        range() { this.reload(); }
    },

    mounted() {
        this.reload();
    }
}
</script>