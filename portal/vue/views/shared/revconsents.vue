<template>
<div>
    <panel :title="$t('consents.title')" :busy="isBusy">
        
		<form class="form" v-if="role!='RESEARCH'">            
		    <div class="form-check">
		      <label class="form-check-label">
                <input class="form-check-input" type="radio" name="consenttype" value="option1" @click="changeView()"> <span class="margin-left" v-t="'consents.where_owner'"></span> 
              </label>
            </div>
            <div class="form-check">
              <label class="form-check-label">
                <input class="form-check-input" type="radio" name="consenttype" value="option2" checked> <span class="margin-left" v-t="'consents.where_authorized'"></span>
              </label>
            </div>
        </form>
		
		
        <p v-t="'consents.description'"></p>
            
		<p>{{ $t('revconsents.count', { count : consents.all.length }) }}</p>
        
        
        <pagination v-model="consents" search="name"></pagination>

		<table class="table table-striped" v-if="consents.filtered && consents.filtered.length">

			<tr>
                <Sorter sortby="ownerName" v-model="consents" v-t="'consents.ownerName'"></Sorter>
				<Sorter sortby="name" v-model="consents" v-t="'consents.name'"></Sorter>				
				<Sorter sortby="type" v-model="consents" v-t="'consents.type'"></Sorter>
				<Sorter sortby="status" v-model="consents" v-t="'consents.status'"></Sorter>				
				<th v-t="'consents.number_of_records'"></th>
			</tr>
			<tr v-for="consent in consents.filtered" :key="consent._id" :class="{ 'table-warning' : consent.status == 'UNCONFIRMED' }">
				<td><a @click="editConsent(consent);" href="javascript:">{{ consent.ownerName || consent.externalOwner }}</a></td>
                <td>{{ consent.name }}</td>				
				<td>{{ $t('enum.consenttype.'+consent.type) }}</td>
				<td>{{ $t('enum.consentstatus.'+consent.status) }}</td>				
				<td>{{ consent.records }}</td>
			</tr>
		</table>

		<button class="btn btn-primary" @click="addConsent();" v-t="'revconsents.add_new_btn'"></button>
		
    </panel>	
</div>
</template>
<script>

import ErrorBox from "components/ErrorBox.vue"
import Panel from "components/Panel.vue"

import session from "services/session.js"
import circles from "services/circles.js"
import status from 'mixins/status.js'
import rl from 'mixins/resultlist.js'
import ENV from 'config';

export default {
  
    data: () => ({
        consents : []
	}),	
		

    components: {  Panel, ErrorBox },

    mixins : [ status, rl ],
  
    methods : {

        loadConsents(userId) {	
            const { $data, $route } = this,me=this;		    
		    me.doBusy(circles.listConsents({ member : true }, [ "name", "authorized", "type", "status", "records", "owner", "ownerName", "externalOwner" ])
		    .then(function(data) {
                $data.consents = me.process(data.data, { filter : { name : "" } });						
                console.log($data.consents.all.length);
		    }));
	    },
	
	    addConsent() {
		    this.$router.push({ path : "./newconsent", query : { request : true } });
	    },
	
	    editConsent(consent) {
		    this.$router.push({ path : "./editconsent", query : { consentId : consent._id } });
	    },
	
	    changeView() {
		    this.$router.push({ path : "./consents" });
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