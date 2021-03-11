<template>
    <panel :title="$t('provider_usergroups.title')" :busy="isBusy">    
        <pagination v-model="usergroups" search="name"></pagination>

        <table class="table" v-if="usergroups.filtered.length > 0">
        <tr>
          <Sorter sortby="name" v-model="usergroups" v-t="'provider_usergroups.name'"></Sorter>
          <th v-t="'provider_usergroups.searchable'"></th>
          <Sorter sortby="status" v-model="usergroups" v-t="'provider_usergroups.status'"></Sorter>  
          <th></th>        
        </tr>
        <tr v-for="item in usergroups.filtered" :key="item._id" >
          <td><router-link :to="{ path : './editusergroup', query :  { groupId : item._id }}">{{ item.name }}</router-link></td>
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
import ErrorBox from "components/ErrorBox.vue"
import Panel from "components/Panel.vue"

import usergroups from "services/usergroups.js"

import status from 'mixins/status.js'
import rl from 'mixins/resultlist.js'


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