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
    <panel :title="$t('auditlog.title')" >
        <error-box :error="error"></error-box>
        <form class="form-row">
            
                <div class="form-group col-4">
                    <label for="from">From:</label>
                    <div class="form-control-static">{{ $filters.date(criteria.from) }}</div>
                </div>
                
                <div class="form-group col-4">
                    <label for="days">Days:</label>
                    <input type="number" class="form-control" @input="recalc()" v-model="criteria.days">
                </div>
                
                <div class="form-group col-4">
                    <label for="until">Until:</label>            
                    <input type="date" id="date" @input="recalc()" class="form-control" autofocus v-date="criteria.to" v-model="criteria.to">				  
                </div>
            
             </form>            
		        
		    <auditlog api="auditlog" :from="criteria.from" :to="criteria.to"></auditlog>		                
      
            <button type="button" @click="$router.go(-1);" class="btn btn-default" v-t="'common.back_btn'"></button>
    </panel>
</div>
</template>
<script>

import Panel from "components/Panel.vue"
import Auditlog from "components/AuditLog.vue"
import { status, ErrorBox } from 'basic-vue3-components'


export default {
  
    data: () => ({
        criteria : { from: null, to : null, days:2 },
        actions : null
	}),	
		

    components: {  Panel, ErrorBox, Auditlog },

    mixins : [ status ],
  
    methods : {
        recalc() {
            const { $data } = this;
            $data.criteria.from = new Date($data.criteria.to);
         
            $data.criteria.from.setDate(new Date($data.criteria.to).getDate() - $data.criteria.days);
           
        }

        
    },

    created() {
        this.$data.actions = this.$route.query.actions;
        let today = new Date();
        today.setDate(today.getDate() + 1);
        this.$data.criteria.to = this.$filters.usDate(today);
        this.recalc();
    }
   
}
</script>