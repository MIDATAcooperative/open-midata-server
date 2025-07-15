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
    <panel :title="$t('admin_terms.title')" :busy="isBusy">
    
        <error-box :error="error"></error-box>
        
        <!--
        <form class="form-horizontal">
            <form-group name="name" label="admin_terms.restrict_name">
                <input type="text" class="form-control" v-model="filter.name">
            </form-group>
        </form>
        -->
      
      <pagination v-model="terms" search="name"></pagination>
      <table class="table" v-if="terms.filtered.length">
		<thead>
        <tr>
          <Sorter v-model="terms" sortby="name" v-t="'admin_terms.name'"></Sorter>
          <Sorter v-model="terms" sortby="version" v-t="'admin_terms.version'"></Sorter>
          <Sorter v-model="terms" sortby="language" v-t="'admin_terms.language'"></Sorter>
          <Sorter v-model="terms" sortby="createdAt" v-t="'admin_terms.createdAt'"></Sorter>  
          <th></th>        
        </tr>
		</thead>
        <tbody>
         <tr v-for="term in terms.filtered" :key="term._id">
           <td>{{ term.name }}</td>
           <td>{{ term.version }}</td>
           <td>
             
               <router-link :to="{ path : './terms', query :  { which : name(term), lang : term.language } }">{{ term.language }}</router-link>
             
           </td>
           <td>{{ $filters.date(term.createdAt) }}</td>
         </tr>
        </tbody>         
      </table>
      <p v-else v-t="'admin_terms.empty'"></p>
      
      <router-link class="btn btn-primary" v-t="'common.add_btn'" :to="{ path : './newterms' }"></router-link> 
                
    </panel>
  	   
</template>
<script>

import Panel from "components/Panel.vue"
import terms from "services/terms.js"

import { status, rl, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {

    data: () => ({	
        filter : { name : "" },
        terms : null
    }),

    components: {  Panel, ErrorBox, FormGroup },

    mixins : [ status, rl ],

    methods : {
        init() {	
            const { $data } = this, me = this;
            me.doBusy(terms.search({ }, ["name", "version", "language", "title", "createdAt"])
            .then(function(results) {
                $data.terms = me.process(results.data, { filter : { name : "" }, ignoreCase : true, sort : "name" });
                       
            }));
	    },
	
	    byName(t) {
	    	const { $data } = this, me = this;
		    return t[0][0].name.indexOf($data.filter.name) >= 0;
	    },
		
	    name(term) {
		    return term.name+"--"+term.version;
	    }
    },
    
    created() {
        this.init();       
    }
}
</script>