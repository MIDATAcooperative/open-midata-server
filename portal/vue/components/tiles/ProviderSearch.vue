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
    <panel :title="$t('providersearch.title')" :busy="isBusy">

        <form name="myform" ref="myform" class="css-form form-horizontal" @submit.prevent="search()">
		    <form-group name="name" label="providersearch.name" :path="errors.name">
				<input type="text" class="form-control" :placeholder="$t('providersearch.name')" v-model="criteria.name" v-validate> 
			</form-group>
			<form-group name="city" label="providersearch.city_or_zip" :path="errors.city"> 
				<input type="text" class="form-control" :placeholder="$t('providersearch.city_or_zip')" v-model="criteria.city" v-validate> 
			</form-group>
			<form-group name="restrict" label="common.empty" v-if="role=='MEMBER'" class="midata-checkbox-row">
                <check-box v-model="criteria.onlymine" name="onlymine">
				    <span v-t="'providersearch.only_with_contact'"></span> 
                </check-box>
			</form-group>
			<form-group name="" label="common.empty">
				<button type="submit" :disabled="action!=null" v-submit class="btn btn-default" v-t="'common.search_btn'"></button>
			</form-group>
		</form>
		<div v-if="providers.filtered">
			<pagination v-model="providers"></pagination>
           
			<div v-if="providers.filtered.length">
				
				<div v-for="provider in providers.filtered" :key="provider._id">
				    <div class="row">
					<div class="col-lg-6 col-12 main-col">{{ provider.firstname }} {{ provider.lastname }}</div>					
					<div class="col-lg-4 col-12"><span v-if="provider.address1">{{ provider.address1 }}<br/></span>{{ provider.zip }} {{ provider.city }}</div>					
					<div class="col-lg-2 col-12"><a class="btn btn-primary btn-sm" href="javascript:" @click="addConsent(provider)" v-t="'common.add_btn'"></a></div>
					</div>
					<div style="border-bottom: 1px solid #e0e0e0; margin-top:10px; margin-bottom:5px"></div>
				</div>
			</div>
			
			<p v-if="providers.filtered.length == 0" v-t="'providersearch.empty_result'"></p>
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
			  city : "", 
			  onlymine : false 
		},

		providers : {},

		role : null
    }),
	
	components: { CheckBox, FormGroup, Panel },

	mixins : [ status, rl ],

	methods : {
		dosearch(crit) {
			const { $data } = this, me = this;
    		me.doBusy(hc.search(crit, ["firstname", "lastname", "city", "zip", "address1", "role"]))
    		.then(function(data) {
    			$data.providers = me.process(data.data, { sort : "lastname" });
    		});
    	},
    
    	search() {
			const { $data } = this, me = this;
    		var crit = {};
    		if ($data.criteria.city != "") crit.city = $data.criteria.city;
    		if ($data.criteria.name != "") crit.name = $data.criteria.name;
    		if ($data.criteria.onlymine) {
    			me.doBusy(hc.list()).then(function(data) {
    				var ids = [];
    				for (let x of data.data) {     			
    					for (let a of x.authorized) { ids.push(a); }
    				}
    				crit._id = ids;
    				me.dosearch(crit);
    			});
    		} else {
    			me.dosearch(crit);	
    		}    	    	
    	},
    
    	addConsent(prov) {
			this.$emit("add", prov);			
		},
		
		init() {
			const { $data, $route } = this;
			$data.role = $route.meta.role.toUpperCase();
			this.ready();
		}
	},

	created() {
		this.init();
	}
    
}
</script>
  