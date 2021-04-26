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
    <panel :title="$t('admin_licenses.title')" :busy="isBusy">
    
        <error-box :error="error"></error-box>       
		<pagination v-model="licenses"></pagination>
 	    <table class="table" v-if="licenses.filtered.length">
             <thead>
                <tr>
                    <Sorter sortby="appName" v-model="licenses" v-t="'admin_licenses.appName'"></Sorter>
                    <Sorter sortby="licenseeType" v-model="licenses" v-t="'admin_licenses.licenseeType'"></Sorter>
                    <Sorter sortby="licenseeName" v-model="licenses" v-t="'admin_licenses.licenseeName'"></Sorter>
                    <Sorter sortby="expireDate" v-model="licenses" v-t="'admin_licenses.expireDate'"></Sorter>
                    <th></th>        
                </tr>
            </thead>
            <tbody>
                <tr v-for="licence in licenses.filtered" :key="licence._id">
                    <td>{{ licence.appName }}</td>
                    <td>{{ licence.licenseeType }}</td>
                    <td>{{ licence.licenseeName }}</td>
                    <td>{{ $filters.date(licence.expireDate) }}</td>
                </tr>
            </tbody>
        </table>
        <p v-if="licenses.filtered.length == 0" v-t="'admin_licenses.empty'"></p>
      
        <router-link class="btn btn-primary" v-t="'common.add_btn'" :to="{ path : './newlicence' }"></router-link> 
                  
    </panel>
  	   
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"

import { status, rl, ErrorBox } from 'basic-vue3-components'

export default {

    data: () => ({	
        licenses : null
    }),

    components: {  Panel, ErrorBox },

    mixins : [ status, rl ],
    
    created() {
        const { $data, $route } = this, me = this;
        me.doBusy(server.post(jsRoutes.controllers.Market.searchLicenses().url, { properties : {} })
    	.then(function(results) {
		    $data.licenses = me.process(results.data);
    	}));
    }
}
</script>