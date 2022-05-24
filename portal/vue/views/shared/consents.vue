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
    <panel :title="$t('consents.title')" :busy="isBusy">        
		<form class="form" v-if="role!='research'">            
		    <div class="form-check">
		      <label class="form-check-label">
                <input class="form-check-input" type="radio" name="consenttype" value="option1" v-model="consenttype"> <span class="margin-left" v-t="'consents.where_owner'"></span> 
              </label>
            </div>
            <div class="form-check">
              <label class="form-check-label">
                <input class="form-check-input" type="radio" name="consenttype" value="option2" v-model="consenttype" @click="changeView()"> <span class="margin-left" v-t="'consents.where_authorized'"></span>
              </label>
            </div>
        </form>
		
		
        <p v-t="'consents.description'"></p>
            
		<p>{{ $t('consents.count', { count : consents.all.length }) }}</p>
        
        
        <pagination v-model="consents" search="name"></pagination>

		<table class="table table-striped mt-1" v-if="consents.filtered && consents.filtered.length">

			<tr>
				<Sorter sortby="name" v-model="consents" v-t="'consents.name'"></Sorter>
				<Sorter class="d-none d-sm-table-cell" sortby="dateOfCreation" v-model="consents" v-t="'consents.date_of_creation'"></Sorter>
				<Sorter sortby="type" v-model="consents" v-t="'consents.type'"></Sorter>
				<Sorter class="d-none d-sm-table-cell" sortby="status" v-model="consents" v-t="'consents.status'"></Sorter>
				<th class="d-none d-lg-table-cell" v-t="'consents.number_of_people'"></th>
				<th class="d-none d-lg-table-cell" v-t="'consents.number_of_records'"></th>
			</tr>
			<tr v-for="consent in consents.filtered" :key="consent._id" :class="{ 'table-warning' : consent.status == 'UNCONFIRMED' }">
				<td><a @click="editConsent(consent);" href="javascript:">{{ consent.name }}</a></td>
				<td class="d-none d-sm-table-cell">{{ $filters.date(consent.dateOfCreation) }}</td> 
				<td>{{ $t('enum.consenttype.'+consent.type) }}</td>
				<td class="d-none d-sm-table-cell">{{ $t('enum.consentstatus.'+consent.status) }}</td>
				<td class="d-none d-lg-table-cell">{{ consent.authorized.length }}</td>
				<td class="d-none d-lg-table-cell">{{ consent.records }}</td>
			</tr>
		</table>

		<button class="btn btn-primary" @click="addConsent();" v-if="role!='RESEARCH'" v-t="'consents.add_new_btn'"></button>
		
    </panel>	
</div>
</template>
<script>

import Panel from "components/Panel.vue"

import session from "services/session.js"
import circles from "services/circles.js"
import { rl, status, ErrorBox } from 'basic-vue3-components'
import ENV from 'config';


export default {
  
    data: () => ({
        consents : [],
        role : null,
        consenttype : "option1"
	}),	
		

    components: {  Panel, ErrorBox },

    mixins : [ status, rl ],
  
    methods : {

        loadConsents(userId) {	
            const { $data, $route } = this,me=this;
		    let prop = {};
		    if ($route.meta.types) prop = { type : $route.meta.types };
		    me.doBusy(circles.listConsents(prop, [ "name", "authorized", "type", "status", "records", "dateOfCreation" ])
		    .then(function(data) {
                $data.consents = me.process(data.data, { filter : { name : "" }, ignoreCase : true, sort : "-dateOfCreation" });						
               
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
        this.$data.role = this.$route.meta.role;
        this.$data.consenttype ="option1";
        this.init();
    }
   
}
</script>