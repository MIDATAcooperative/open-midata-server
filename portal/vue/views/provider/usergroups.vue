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
    <panel :title="$t('provider_usergroups.title')" :busy="isBusy">    
        <pagination v-model="usergroups" search="name"></pagination>

        <table class="table" v-if="usergroups.filtered.length > 0">
        <tr>
          <Sorter sortby="name" v-model="usergroups" v-t="'provider_usergroups.name'"></Sorter>
          <Sorter sortby="type" v-model="usergroups" v-t="'provider_usergroups.type'"></Sorter>
          <th v-t="'provider_usergroups.searchable'"></th>
          <Sorter sortby="status" v-model="usergroups" v-t="'provider_usergroups.status'"></Sorter>  
          <th></th>        
        </tr>
        <tr v-for="item in usergroups.filtered" :key="item._id" >
          <td><router-link :to="{ path : './editusergroup', query :  { groupId : item._id }}">{{ item.name }}</router-link></td>
          <td>{{ $t('enum.usergrouptype.'+item.type) }}</td>
          <td><input type="checkbox" disabled v-model="item.searchable"></td>
          <td>{{ $t('enum.userstatus.'+item.status) }}</td>          
          <td><button class="btn btn-danger btn-sm" v-if="item.status != 'DELETED'" :disabled="action!=null" v-t="'common.delete_btn'" @click="deleteGroup(item)"></button></td>
        </tr>
      </table>
      <p v-if="usergroups.filtered.length == 0" v-t="'provider_usergroups.empty'"></p>
      
      <router-link class="btn btn-primary" v-t="'common.add_btn'" :to="{ path : './newusergroup' }"></router-link> 
            
    </panel>
	
</template>
<script>

import Panel from "components/Panel.vue"
import usergroups from "services/usergroups.js"
import { rl, status, ErrorBox } from 'basic-vue3-components'


export default {
    data: () => ({	
        usergroups : null
    }),

    components: {  Panel, ErrorBox },

    mixins : [ status, rl ],

    methods : {
        init() {	
            const { $data } = this, me = this;
		    me.doBusy(usergroups.search({ "member" : true }, usergroups.ALLPUBLIC )
    	    .then(function(results) {
		        $data.usergroups = me.process(results.data, { sort : "name", filter : { name : "" } });
    	    }));
	    },
	
	    deleteGroup(grp) {
            const me = this;
		    me.doAction("delete", usergroups.deleteUserGroup(grp._id)
		    .then(function() {
			    me.init();
		    }));
	    }
	
    },

    created() {        
        this.init();      
    }
}
</script>