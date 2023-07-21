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
    <panel :title="$t('databrokersearch.title')" :busy="isBusy">

        <form name="myform" ref="myform" class="css-form form-horizontal" @submit.prevent="dosearch()">
		    <form-group name="name" label="databrokersearch.name" :path="errors.name">
				<input type="text" class="form-control" v-model="criteria.name" v-validate> 
			</form-group>	
			<form-group name="" label="common.empty">
				<button type="submit" :disabled="action!=null" v-submit class="btn btn-default" v-t="'common.search_btn'"></button>
			</form-group>		
		</form>
		<div v-if="databrokers">
			<pagination v-model="databrokers"></pagination>
           
			<div v-if="databrokers.filtered.length">
				
				<div v-for="databroker in databrokers.filtered" :key="databroker._id">
				    <div class="row">
					<div class="col-md-6 col-12 main-col">{{ databroker.name }}</div>					
					<div class="col-md-6 col-12">{{ databroker.orgName }}</div>
					<div class="col-12">{{ databroker.description }}</div>									
					<div class="col-lg-1 col-md-2 col-12"><a class="btn btn-primary btn-sm" href="javascript:" @click="addConsent(databroker)" v-t="'common.add_btn'"></a></div>
					</div>
					<div style="border-bottom: 1px solid #e0e0e0; margin-top:10px; margin-bottom:5px"></div>
				</div>
			</div>
			
			<p v-if="databrokers && databrokers.filtered.length == 0" v-t="'databrokersearch.empty_result'"></p>
		</div>
    </panel>

</template>
<script>
import Panel from "components/Panel.vue"
import { status, rl, CheckBox, FormGroup } from 'basic-vue3-components'
import hc from "services/hc"
import apps from "services/apps"

export default {

	props: [ "setup" ],
	emits : [ "add" ],

    data : ()=>({      
		criteria : { 
			  name : "", 
			  city : ""
		},

		databrokers : null
    }),
	
	components: { CheckBox, FormGroup, Panel },

	mixins : [ status, rl ],

	methods : {
		dosearch() {
			const { $data } = this, me = this;
			let crit = { type : "broker" };
			if ($data.criteria.name) crit.name = $data.criteria.name;
			
			$data.databrokers = null;		
    		me.doBusy(apps.getApps(crit, ["filename", "name","type", "orgName", "description"])
			.then(function(data) {
				$data.databrokers = me.process(data.data, { sort : "name" });

			}));
    	},
            
    	addConsent(prov) {
			this.$emit("add", prov);			
		},
		
		init() {
			const { $data, $route } = this;			
			this.ready();
		}
	},

	created() {
		this.init();
	}
    
}
</script>
  