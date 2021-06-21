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
    <ul class="list-group">
        <li v-for="(block,idx) in blocks" :key="idx" class="list-group-item">
	        <span>{{ block.display }} <span v-if="block.code && details">( {{ block.code }} )</span></span>		    
		    <div v-if="details">		    
		        <span v-if="isapp && block.owner && block.owner != 'all' && (!block.public || block.public=='no')">{{ $t('queryeditor.short_owner_'+block.owner) }}</span>		      
		        <span v-if="isapp && block.public == 'no' && (!block.owner || block.owner == 'all')" v-t="'queryeditor.short_owner_all'"></span>
		        <span v-if="block.public == 'only'" v-t="'queryeditor.short_public_only'"></span>
		        <span v-if="block.public == 'also' && (!block.owner || block.owner == 'all')" v-t="'queryeditor.short_public_also'"></span> 
		        <span v-if="block.public == 'also' && block.owner != 'all'" v-t="'queryeditor.short_public_also_self'"></span>				    
		        <span v-if="block.app && block.app != 'all'"><span v-t="'queryeditor.short_app_other'"></span> {{ block.appName }}</span>		      
		    </div>		   
		    
		    <div v-if="block.timeRestrictionMode">
		      <span>{{ $t('queryeditor.'+block.timeRestrictionMode) }}</span>: {{ $filters.date(block.timeRestrictionDate) }}
		    </div>
		    <div v-if="block.dataPeriodRestrictionMode">
		      <span>{{ $t('queryeditor.'+block.dataPeriodRestrictionMode) }}</span>: {{ $filters.date(block.dataPeriodRestrictionStart) }} - {{ $filters.date(block.dataPeriodRestrictionEnd) }} 
		    </div>	
        </li>
        <li v-if="blocks.length==0" class="list-group-item" v-t="'error.missing.access_query'">
        </li>
    </ul>	
</template>
<script>
import labels from "services/labels"
import { getLocale } from "services/lang"

export default {
    props : ['query', 'details', 'isapp'],

    data : ()=>({      
		blocks : []
    }),

    methods : {        
        update() {
            const { $data } = this;           
            if (this.query) {
                labels.parseAccessQuery(getLocale(), this.query).then(function(result) {            
                    $data.blocks = result; 
                });
            }
        }
    },

    watch : {
        query() {
            this.update();
        }
    },

    created() {
        this.update();
    }
        
}
</script>