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
    <panel :titel="$t('admin_members.title')" :busy="isBusy">
		<error-box :error="error"></error-box>
		<form class="css-form form-horizontal">
		    <form-group name="search" label="admin_members.search_type">
		        <select class="form-control" id="search" @change="reload(searchName);" v-model="searchName" v-validate>
                    <option v-for="search in searches" :key="search.name" :value="search.name">{{ $t(search.name) }}</option>
                </select>
		    </form-group>
		    <form-group name="role" label="admin_members.role" v-if="search.changeable">
		        <select class="form-control" id="role" @change="reload();" v-model="search.criteria.role" v-validate>
                    <option v-for="role in roles" :key="role" :value="role">{{ $t('enum.userrole.'+role) }}</option>
                </select>
		    </form-group>
		    <form-group name="status" label="admin_members.status" v-if="search.changeable">
		        <select  class="form-control" id="status" @change="reload();" v-model="search.criteria.status">
                    <option v-for="status in stati" :key="status" :value="status">{{ $t('enum.userstatus.'+status) }}</option>
                </select>
		    </form-group>
		    <form-group name="lastname" label="common.user.lastname" v-if="search.searchable">
		        <div class="input-group">
		            <input type="text" class="form-control" id="lastname" v-model="search.criteria.lastname" v-validate>
		            <div class="input-group-append">
		                <button class="btn btn-primary" :disabled="action!=null" @click="reload()" v-t="'common.search_btn'"></button>
		            </div>
		        </div>
		    </form-group>
		    <form-group name="email" label="common.user.email" v-if="search.searchable">
		        <div class="input-group">
		            <input type="text" class="form-control" id="email" v-model="search.criteria.email" v-validate>
		            <div class="input-group-append">
		                <button class="btn btn-primary" @click="reload()" :disabled="action!=null" v-t="'common.search_btn'"></button>
		            </div>
		        </div>
		    </form-group>
		</form>
		            
        <pagination v-model="members" search="search"></pagination>
						
		<table class="table table-striped" v-if="members.filtered.length">

				<tr>
					<Sorter sortby="midataID" v-model="members" v-t="'admin_members.midata_id'"></Sorter>
					<Sorter sortby="firstname" v-model="members" v-t="'common.user.firstname'"></Sorter>
					<Sorter sortby="lastname" v-model="members" v-t="'common.user.lastname'"></Sorter>
					<Sorter sortby="email" v-model="members"  v-t="'common.user.email'"></Sorter>
					<th></th>
					<Sorter sortby="role" v-model="members" v-t="'admin_members.role'"></Sorter>
					<Sorter sortby="status" v-model="members" v-t="'admin_members.status'"></Sorter>
				</tr>
								
				<tr v-for="member in members.filtered" :key="member._id" >
					<td><router-link :to="{ path : './address', query :  { userId : member._id } }">{{ member.midataID || 'none' }}</router-link></td>
					<td>{{ member.firstname }}</td>
					<td>{{ member.lastname }}</td>
					<td>{{ member.email }}</td>
					<td>
					  <span v-if="member.developer" class="fas fa-link mr-1" title="tied to developer"></span>
					  <span v-if="member.emailStatus != 'VALIDATED' && member.emailStatus != 'EXTERN_VALIDATED'" class="fas fa-question-sign mr-1" title="email not confirmed"></span>					  
					  <span v-if="!member.login || member.login &lt; dateLimit" class="fas fa-clock mr-1" title="last login older 1 month"></span>
					  <span v-if="member.security != 'KEY_EXT_PASSWORD'" class="fas fa-eye mr-1" title="Non standard key protection"></span>
					</td>
					<td>{{ $t('enum.userrole.'+member.role) }}</td>
					<td><select @change="changeUser(member);" v-model="member.status" class="form-control">
                            <option v-for="status in stati" :key="status" :value="status">{{ $t('enum.userstatus.'+status) }}</option>
                        </select>
                    </td>
				</tr>
		</table>

        <p v-if="members.filtered.length === 0" v-t="'admin_members.empty'"></p>
			
		<router-link :to="{ path : './pwrecover' }" v-t="'admin_pwrecover.menu'"></router-link>
						
    </panel>  
</template>
<script>

import Panel from "components/Panel.vue"
import users from "services/users.js"
import administration from "services/administration.js"
import session from "services/session.js"
import { status, rl, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {
    data: () => ({	
        roles : [ "MEMBER", "PROVIDER", "RESEARCH", "DEVELOPER", "ADMIN"],
	    stati : [ "NEW", "ACTIVE", "BLOCKED", "DELETED" ],
	    searches : [ 
            { 
                name : "admin_members.contract_required",
                criteria : { status : "NEW", agbStatus : "REQUESTED" }
            },
            {
                name : "admin_members.contract_confirm_required",
                criteria : { status : "NEW", agbStatus : "PRINTED" }
            },	 
            {
                name : "admin_members.no_email_confirm",
                criteria : { emailStatus : "UNVALIDATED" }
            },
            {
                name : "admin_members.email_rejected",
                criteria : { emailStatus : "REJECTED" }
            },
            {
                name : "admin_members.not_admin_confirmed",
                criteria : { status : "NEW" }
            },
            {
                name : "admin_members.overview",
                criteria : { role : "MEMBER", status : "NEW" },
                changeable : true
            },
            {
                name : "admin_members.specific_user",
                criteria : { },
                searchable : "lastname"
            }
        ],
        search : null,
        searchName : "admin_members.no_email_confirm",
        setup : { sort : "email", filter : { search : "" }, ignoreCase : true},
        dateLimit: null
    }),

    components: {  Panel, ErrorBox, FormGroup },

    mixins : [ status, rl ],

    methods : {
        reload(searchName, comeback) {		
            const { $data } = this, me = this;
            if (searchName && (!$data.search || !comeback)) $data.search = session.map($data.searches, "name")[searchName];
              
            if ($data.search.searchable && !$data.search.criteria.lastname && !$data.search.criteria.email) return;
            if (!$data.search.criteria.lastname) { delete $data.search.criteria.lastname; }
            if (!$data.search.criteria.email) { delete $data.search.criteria.email; }
            me.doBusy(users.getMembers($data.search.criteria, [ "midataID", "firstname", "lastname", "email", "role", "subroles", "status", "emailStatus", "developer", "login", "security" ])
            .then(function(data) { 
                for (let user of data.data) user.search = user.firstname+" "+user.lastname+" "+user.email;
                $data.members = me.process(data.data, $data.setup);						
            }));
            
            $data.dateLimit = new Date();
            $data.dateLimit.setMonth($data.dateLimit.getMonth() - 1);
	    },
	
	    changeUser(user) {	
            const me = this;
		    me.doAction("change", administration.changeStatus(user._id, user.status));
	    }
    },

    created() {
        const { $data } = this, me = this;
        session.load("MembersListCtrl", me, ["search", "searchName"]);	    
	    me.reload($data.searchName, true);
    },

    unmounted() {
        session.save("MembersListCtrl", this, ["search", "searchName"]);
    }
    
}
</script>