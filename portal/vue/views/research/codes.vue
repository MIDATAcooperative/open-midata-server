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
    <study-nav page="study.codes"></study-nav>
	<tab-panel :busy="isBusy">
	        
	        <p v-t="'codes.intro'"></p>
	
	        <error-box :error="error"></error-box>
			<pagination v-model="codes" search="code"></pagination>
	        
	        <table class="table table-hover" v-if="codes.filtered.length > 0">
	            <thead>
	                <tr>
	                    <Sorter sortby="code" v-model="codes" v-t="'codes.code'">Code</Sorter>
	                    <Sorter sortby="group" v-model="codes" v-t="'codes.group'">Group</Sorter>
	                    <!-- <th v-t="'codes.recruiter">Recruiter</th>  -->
	                    <Sorter sortby="status" v-model="codes" v-t="'codes.status'">Status</Sorter>
	                    <Sorter sortby="createdAt" v-model="codes" v-t="'codes.created_at'">Created At</Sorter>	      
	                </tr>
	            </thead>
	            <tbody>
	                <tr v-for="code in codes.filtered" :key="code.code">
	                    <td>{{ code.code }}</td>	  
	                    <td>{{ code.group }}</td>
	                    <!-- <td>{{ code.recruiterName }}</td>  -->
	                    <td><span>{{ $t('enum.pcodestatus.'+code.status) }}</span></td>
	                    <td>{{ $filters.date(code.createdAt) }}</td>	         
	                </tr>
	            </tbody>
	        </table>
	        
	        <p v-if="codes.filtered.length === 0" v-t="'codes.empty'"></p>
		    <div v-if="!blocked">
		        <button class="btn btn-primary" :disabled="action!=null" @click="showcreatenew()" v-t="'codes.showcreate_btn'">Generate new codes</button>
		    </div>
		
	</tab-panel>
	
    <panel :title="$t('codes.new_title')" :busy="isBusy" v-if="createnew">
	
	    <p v-if="newcodes.error" :class="{ 'alert-warning' : newcodes.error.level == 'warning', 'alert-danger' : !newcodes.error.level }" class="alert">
	        {{ newcodes.error.message || newcodes.error }}
	    </p>
	    <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="generate()" role="form">	
		    <form-group name="count" label="codes.number_of_codes" :path="errors.count"> 
			    <input type="number" class="form-control" id="count" name="count" placeholder="1" @change="updateCodeCount()" v-validate v-model="newcodes.count" required>		    
  	        </form-group>
  	        <form-group name="group" label="codes.group" :path="errors.group">  	            	        
	            <select v-validate v-model="newcodes.group" class="form-control">
                    <option v-for="group in study.groups" :key="group.name" :value="group.name">{{ group.name }}</option>
                </select>
	        </form-group>
  	          	          	 
  	        <form-group name="reuseable" label="codes.reuseable" :path="errors.reuseable">
  	            <label class="radio-inline mr-2">
  	                <input type="radio" id="reuseable" name="reuseable" value="true" v-validate v-model="newcodes.reuseable">
  	                <span v-t="'codes.yes'"></span>
  	            </label>
  	            <label class="radio-inline">
  	                <input type="radio" name="reuseable" value="false" v-validate v-model="newcodes.reuseable">
  	                <span v-t="'codes.no'"></span>
  	            </label>
            </form-group>  	
         	<form-group name="manually" label="codes.type_of_creation" :path="errors.manually">
  	       		<label class="radio-inline mr-2">
  	         		<input type="radio" id="manually" name="manually" value="true" @change="updateCodeCount()" v-validate v-model="newcodes.manually">
  	         		<span v-t="'codes.manually'"></span>
  	       		</label>
  	       		<label class="radio-inline">
  	         		<input type="radio" name="generate" value="false" @change="updateCodeCount()" v-validate v-model="newcodes.manually">
  	         		<span v-t="'codes.generated'"></span>
  	       		</label>
        	</form-group>  	    
        	<form-group name="codes" label="codes.codes" v-if="newcodes.manually=='true'" :path="errors.codes"> 
          		<div v-for="(code,idx) in newcodes.codes" :key="idx">
					<input type="text" class="form-control" v-validate v-model="newcodes.codes[idx]" required>
		  		</div>		    
  	    	</form-group>
        
			<form-group label="common.empty">
          		<button type="submit" v-submit class="btn btn-primary" v-t="'codes.add_btn'">Generate</button>
        	</form-group>
   		</form>
	</panel>
   
</div>

</template>
<script>

import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import server from "services/server.js"
import { rl, status, ErrorBox, Success, FormGroup } from 'basic-vue3-components'

export default {
    data: () => ({	
        studyid : null,
		codes : null,
		newcodes : { count:1, reuseable:"true", manually:"false", group:"" },
		createnew : false,
		blocked : false	
    }),

    components: {  Panel, TabPanel, ErrorBox, FormGroup, StudyNav, Success },

    mixins : [ status, rl ],

    methods : {
		reload() {
			const { $data } = this, me = this;
			me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
			.then(function(data) { 				
				$data.study = data.data;			
			}));
		
			me.doBusy(server.get(jsRoutes.controllers.research.Studies.listCodes($data.studyid).url).
			then(function(data) { 				
				$data.codes = me.process(data.data, { filter : { code : "" }});				
				$data.createnew = false;
				
			}, function(err) {
				//$data.error = err.data.type;
				$data.blocked = true;
				$data.createnew = false;
			}));
		},
	
		generate() {
			const { $data } = this, me = this;
			// send the request
			var data = $data.newcodes;
			data.count = parseInt(data.count);
			
			me.doAction("generate", server.post(jsRoutes.controllers.research.Studies.generateCodes($data.studyid).url, data).
			then(function(url) { me.reload(); }));
		},
	
		showcreatenew() {		
			const { $data } = this;
			$data.createnew = true;
		},
	
		updateCodeCount() {
			const { $data } = this;
			if ($data.newcodes.manually=="true") {
				if ($data.newcodes.codes === undefined) {
					$data.newcodes.codes = [];
					for (var i=0;i<$data.newcodes.count;i++) $data.newcodes.codes.push("");
				} else {
					while ($data.newcodes.codes.length<$data.newcodes.count) $data.newcodes.codes.push("x");
					if ($data.newcodes.codes.length > $data.newcodes.count) {
						let diff = $data.newcodes.codes.length - $data.newcodes.count;
						$data.newcodes.codes.splice($data.newcodes.count, diff);
					}
				}
			} else $data.newcodes.codes = undefined;
		}
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.studyid = $route.query.studyId;
		me.reload();
    }
}
</script>