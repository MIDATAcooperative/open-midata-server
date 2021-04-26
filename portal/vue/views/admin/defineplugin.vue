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
    <panel :title="$t('admin_defineplugin.title')" :busy="isBusy">
        
        <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="submit()">
            <error-box :error="error"></error-box>
		    <form-group name="definition" label="admin_defineplugin.definition" :path="errors.definition">
		        <textarea class="form-control" name="definition" id="definition" v-validate v-model="appdef.value" rows="20" required>	
		        </textarea>	      		      
		    </form-group>
		    <button type="submit" v-submit class="btn btn-primary" v-t="'common.submit_btn'"></button>
	    </form>        		    
	
    </panel>
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"

import { status, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {
    data: () => ({	
        appdef : { value : "" }
    }),

    components: {  Panel, ErrorBox, FormGroup },

    mixins : [ status ],

    methods : {
        submit() {		
            const { $data, $router } = this;
	        this.doAction("import", server.post(jsRoutes.controllers.Market.importPlugin().url, { "base64" : $data.appdef.value }))
	        .then(function(result) {
		        $router.push({ path : './manageapp', query : { appId : result.data._id } }); 
	        });	
	    }
    },

    created() {
        const me = this;
        me.ready();        
    }
    
}
</script>