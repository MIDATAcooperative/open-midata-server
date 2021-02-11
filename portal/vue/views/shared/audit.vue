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
                    <input type="date" id="date" @input="recalc()" class="form-control" autofocus v-model="criteria.to">				  
                </div>
            
             </form>            
		        
		    <auditlog api="auditlog" :from="criteria.from" :to="criteria.to"></auditlog>		                
      
    </panel>
</div>
</template>
<script>

import ErrorBox from "components/ErrorBox.vue"
import Panel from "components/Panel.vue"
import Auditlog from "components/AuditLog.vue"
import status from 'mixins/status.js'


export default {
  
    data: () => ({
        criteria : { from: null, to : null, days:2 }
	}),	
		

    components: {  Panel, ErrorBox, Auditlog },

    mixins : [ status ],
  
    methods : {
        recalc() {
            const { $data } = this;
            $data.criteria.from = new Date($data.criteria.to);
            console.log($data.criteria.from);
            $data.criteria.from.setDate(new Date($data.criteria.to).getDate() - $data.criteria.days);
            console.log($data.criteria);
        }

        
    },

    created() {
        this.$data.criteria.to = this.$filters.usDate(new Date());
        this.recalc();
    }
   
}
</script>