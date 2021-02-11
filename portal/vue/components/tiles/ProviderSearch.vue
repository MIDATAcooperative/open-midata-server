<template>
    <panel :title="$t('providersearch.title')" :busy="isBusy">

        <form name="myform" ref="myform" class="css-form form-horizontal" @submit.prevent="search()">
		    <form-group name="name" label="providersearch.name" :path="errors.name">
				<input type="text" class="form-control" :placeholder="$t('providersearch.name')" v-model="criteria.name" v-validate> 
			</form-group>
			<form-group name="city" label="providersearch.city_or_zip" :path="errors.city"> 
				<input type="text" class="form-control" :placeholder="$t('providersearch.city_or_zip')" v-model="criteria.city" v-validate> 
			</form-group>
			<form-group name="restrict" label="common.empty" v-if="role=='MEMBER'">
                <check-box v-model="criteria.onlymine" name="onlymine">
				    <span v-t="'providersearch.only_with_contact'"></span> 
                </check-box>
			</form-group>
			<form-group name="" label="common.empty">
				<button type="submit" v-submit class="btn btn-default" v-t="'common.search_btn'"></button>
			</form-group>
		</form>
		<div v-if="providers.filtered">
			<pagination v-model="providers"></pagination>

			<table class="table" v-if="providers.filtered.length">
				<tr>
					<th v-t="'common.user.lastname'">Surname</th>
					<th v-t="'common.user.firstname'">Firstname</th>
					<th v-t="'common.user.zip'">ZIP</th>
					<th v-t="'common.user.city'">City</th>
					<th v-t="'common.user.street'">Street</th>
					<th>-</th>
				</tr>
				<tr v-for="provider in providers.filtered" :key="provider._id">
					<td>{{ provider.lastname }}</td>
					<td>{{ provider.firstname }}</td>
					<td>{{ provider.zip }}</td>
					<td>{{ provider.city }}</td>
					<td>{{ provider.address1 }}</td>
					<td><a href="javascript:" @click="addConsent(provider)" v-t="'providersearch.add_consent_btn'"></a></td>
				</tr>
			</table>
			
			<p v-if="providers.filtered.length == 0" v-t="'providersearch.empty_result'"></p>
		</div>
    </panel>

</template>
<script>
import CheckBox from "components/CheckBox.vue"
import FormGroup from "components/FormGroup.vue"
import Panel from "components/Panel.vue"
import status from "mixins/status"
import rl from "mixins/resultlist"
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
    			$data.providers = me.process(data.data);
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
    				angular.forEach(data.data, function(x) {     			
    					angular.forEach(x.authorized, function(a) { ids.push(a); });
    				});    			
    				crit._id = ids;
    				me.dosearch(crit);
    			});
    		} else {
    			me.dosearch(crit);	
    		}    	    	
    	},
    
    	addConsent(prov) {
			this.$emit("add", prov);
			/*
			const me = this;
    		if (me.setup && me.setup.studyId) {
    	   		studies.updateParticipation(me.setup.studyId, { add : { providers : [ prov._id ]}})
    	   		.then(function() {    	     
    	     		//views.disableView($scope.view.id);
    	   		});
    		} else if ( $scope.view.setup && $scope.view.setup.callback ) {
    		$scope.view.setup.callback(prov);
    		views.disableView($scope.view.id);
    	} else {
    	   $state.go("^.newconsent", { authorize : prov._id });
    	}*/
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
  