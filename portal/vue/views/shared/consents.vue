<template>
<div>
    <panel :title="$t('consents.title')" :busy="isBusy">
        
		<form class="form" v-if="role!='RESEARCH'">            
		    <div class="form-check">
		      <label class="form-check-label">
                <input class="form-check-input" type="radio" name="consenttype" value="option1" checked> <span class="margin-left" v-t="'consents.where_owner'"></span> 
              </label>
            </div>
            <div class="form-check">
              <label class="form-check-label">
                <input class="form-check-input" type="radio" name="consenttype" value="option2" @click="changeView()"> <span class="margin-left" v-t="'consents.where_authorized'"></span>
              </label>
            </div>
        </form>
		
		
        <p v-t="'consents.description'"></p>
            
		<p>{{ $t('consents.count', { count : consents.all.length }) }}</p>
        <input type="text" v-model="consents.filter.name" class="form-control">

        <pagination v-model="consents"></pagination>

		<table class="table table-striped" v-if="consents.filtered && consents.filtered.length">

			<tr>
				<Sorter sortby="name" v-model="consents" v-t="'consents.name'"></Sorter>
				<Sorter sortby="dateOfCreation" v-model="consents" v-t="'consents.date_of_creation'"></Sorter>
				<Sorter sortby="type" v-model="consents" v-t="'consents.type'"></Sorter>
				<Sorter sortby="status" v-model="consents" v-t="'consents.status'"></Sorter>
				<th v-t="'consents.number_of_people'"></th>
				<th v-t="'consents.number_of_records'"></th>
			</tr>
			<tr v-for="consent in consents.filtered" :key="consent._id" :class="{ 'table-warning' : consent.status == 'UNCONFIRMED' }">
				<td><a @click="editConsent(consent);" href="javascript:">{{ consent.name }}</a></td>
				<td>{{ $filters.date(consent.dateOfCreation) }}</td> 
				<td>{{ $t('enum.consenttype.'+consent.type) }}</td>
				<td>{{ $t('enum.consentstatus.'+consent.status) }}</td>
				<td>{{ consent.authorized.length }}</td>
				<td>{{ consent.records }}</td>
			</tr>
		</table>

		<button class="btn btn-primary" @click="addConsent();" v-if="role!='RESEARCH'" v-t="'consents.add_new_btn'"></button>
		
    </panel>	
</div>
</template>
<script>

import ErrorBox from "components/ErrorBox.vue"
import Panel from "components/Panel.vue"
import Sorter from "components/Sorter.vue"
import Pagination from "components/Pagination.vue"
import session from "services/session.js"
import circles from "services/circles.js"
import status from 'mixins/status.js'
import rl from 'mixins/resultlist.js'
import ENV from 'config';


export default {
  
    data: () => ({
        consents : []
	}),	
		

    components: {  Panel, ErrorBox, Sorter, Pagination },

    mixins : [ status, rl ],
  
    methods : {

        loadConsents(userId) {	
            const { $data, $route } = this,me=this;
		    let prop = {};
		    if ($route.meta.types) prop = { type : $route.meta.types };
		    me.doBusy(circles.listConsents(prop, [ "name", "authorized", "type", "status", "records", "dateOfCreation" ])
		    .then(function(data) {
                $data.consents = me.process(data.data, { filter : { name : "a" } });						
                console.log($data.consents.all.length);
		    }));
	    },
	
	    addConsent() {
		    this.$router.push({ path : "./newconsent" });
	    },
	
	    editConsent(consent) {
		    this.$router.push({ path : "./editconsent", query : { consentId : consent._id } });
	    },
	
	    changeView() {
		    this.$router.push({ path : "./revconsents" });
	    },
		    		
        init() {
            const me = this;
            me.doBusy(session.currentUser.then(function(userId) { me.loadConsents(userId); }));
        }
    },

    created() {
        this.init();
    }
   
}
</script>