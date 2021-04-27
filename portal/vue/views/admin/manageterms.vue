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
    <panel :title="$t('admin_manageterms.pagetitle')" :busy="isBusy">
    
        <error-box :error="error"></error-box>
      		        
		<form class="form-horizontal" name="myform" ref="myform" @submit.prevent="addTerms()" novalidate role="form">
		  <form-group name="name" label="admin_manageterms.name" :path="errors.name">
		    <input type="text" id="name" name="name" class="form-control" v-validate v-model="newTerms.name" autofocus required>
		  </form-group>
		
		  <form-group name="version" label="admin_manageterms.version" :path="errors.version">
		    <input type="text" id="version" name="version" class="form-control" v-validate v-model="newTerms.version" required>
		  </form-group>		  
		
		  <form-group name="language" label="admin_manageterms.language" :path="errors.language">
		    <select id="language" class="form-control" v-validate v-model="newTerms.language">
                 <option v-for="lang in languages" :key="lang.value" :value="lang.value">{{ $t(lang.name) }}</option>
            </select>		    
		  </form-group>
		
		  <form-group name="title" label="admin_manageterms.title" :path="errors.title">
		    <input type="text" id="title" name="title" class="form-control" v-validate v-model="newTerms.title" required>
		  </form-group>
		
		  <form-group name="text" label="admin_manageterms.text" :path="errors.text">
		    <textarea rows="10" id="text" class="form-control" v-validate v-model="newTerms.text"></textarea>
		  </form-group>		  		  

		  <form-group name="replace" label="admin_manageterms.replace" :path="errors.replace">
			   <check-box name="replace" v-model="newTerms.replace"></check-box>			  
		  </form-group>
		  		
		  <form-group label="common.empty">
		    <button type="submit" v-submit :disabled="action!=null" class="btn btn-primary" v-t="'common.submit_btn'"></button>		    		  
		  </form-group>
	     </form>	  
	</panel>
    <panel :title="newTerms.title" :busy="isBusy">	
        <div class="terms" v-html="newTerms.text"></div>			
    </panel>
  	   
</template>
<script>

import Panel from "components/Panel.vue"
import languages from "services/languages.js"
import terms from "services/terms.js"

import { status, ErrorBox, CheckBox, FormGroup } from 'basic-vue3-components'

export default {

    data: () => ({	
        newTerms : {},
        languages : languages.all
    }),

    components: {  Panel, ErrorBox, CheckBox, FormGroup },

    mixins : [ status ],

    methods : {
        addTerms() {								
            const { $data, $router } = this;						
            this.doAction('submit', terms.add($data.newTerms))
            .then(function() { 
                $router.push({ path : './viewterms' }); 
            });		
	    }

    },
    
    created() {
        this.ready();        
    }
}
</script>