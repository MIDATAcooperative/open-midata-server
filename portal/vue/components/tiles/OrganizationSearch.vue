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
    <panel :title="$t('organizationsearch.title')" :busy="isBusy">

        <form name="myform" ref="myform" class="css-form form-horizontal" @submit.prevent="dosearch()">
		    <form-group name="name" label="organizationsearch.name" :path="errors.name">
				<input type="text" class="form-control" v-model="criteria.name" v-validate> 
			</form-group>
			<form-group name="city" label="organizationsearch.city_or_zip" :path="errors.city"> 
				<input type="text" class="form-control" v-model="criteria.city" v-validate> 
			</form-group>			
			<form-group name="" label="common.empty">
				<button type="submit" :disabled="action!=null" v-submit class="btn btn-default" v-t="'common.search_btn'"></button>
			</form-group>
		</form>
		<div v-if="organizations">
			<pagination v-model="organizations"></pagination>
           
			<div v-if="organizations.filtered.length">
				
				<div v-for="organization in organizations.filtered" :key="organization._id">
				    <div class="row">
					<div class="col-md-6 col-12 main-col">{{ organization.name }}</div>					
					<div class="col-md-6 col-12 main-col">{{ organization.description }}</div>									
					<div class="col-lg-1 col-md-2 col-12"><a class="btn btn-primary btn-sm" href="javascript:" @click="addConsent(organization)" v-t="'common.add_btn'"></a></div>
					</div>
					<div style="border-bottom: 1px solid #e0e0e0; margin-top:10px; margin-bottom:5px"></div>
				</div>
			</div>
			
			<p v-if="organizations && organizations.filtered.length == 0" v-t="'organizationsearch.empty_result'"></p>
		</div>
    </panel>

</template>
<script>
import Panel from "components/Panel.vue"
import { status, rl, CheckBox, FormGroup } from 'basic-vue3-components'
import hc from "services/hc"

export default {

	props: [ "setup" ],
	emits : [ "add" ],

    data : ()=>({      
		criteria : { 
			  name : "", 
			  city : ""
		},

		organizations : null
    }),
	
	components: { CheckBox, FormGroup, Panel },

	mixins : [ status, rl ],

	methods : {
		dosearch() {
			const { $data } = this, me = this;
			let crit = {};
			if ($data.criteria.name) crit.name = $data.criteria.name;
			if ($data.criteria.city) crit.city = $data.criteria.city;
			if (me.setup && me.setup.serviceId) crit.serviceId = me.setup.serviceId;	
			$data.organizations = null;		
    		me.doBusy(hc.searchOrganization(crit))
    		.then(function(data) {
    			$data.organizations = me.process(data.data, { sort : "name" });
    		});
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
  