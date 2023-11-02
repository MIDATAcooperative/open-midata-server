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
    <panel :title="$t('admin_address.title')" :busy="isBusy">
		
        <address>
            {{ member.firstname }} {{ member.lastname }}<br>            
		    {{ member.address1 }}<br>
			{{ member.address2 }}<br>
			{{ member.zip }} {{ member.city }}<br>
			{{ member.country }}<br><br>
			<span v-t="'common.user.email'"></span>: {{ member.email }}<br>
			<span v-t="'common.user.phone'"></span>: {{ member.phone }}
		</address>
		<div class="extraspace">
			<router-link class="btn btn-danger btn-sm space" v-t="'admin_address.change_email_btn'" :to="{ path : './changeemail', query : { userId : member._id }}"></router-link>
			<router-link class="btn btn-danger btn-sm space" v-t="'admin_address.change_address_btn'" :to="{ path : './changeaddress', query : { userId : member._id }}"></router-link>
		</div>
		<div class="extraspace" v-if="member.role=='DEVELOPER' || member.role=='ADMIN'">
			<router-link :to="{ path : './yourapps', query : {creator:member.email}}" v-t="'admin_address.show_apps_created'"></router-link>
		</div>
		<table class="table table-striped table-bordered">
		    <tr>
			    <td v-t="'admin_address.midata_id'"></td><td>{{ member.midataID }}</td>
			</tr><tr>
			    <td v-t="'admin_address.role'"></td><td>{{ $t('enum.userrole.'+ member.role) }}</td>
			</tr><tr>
			    <td v-t="'admin_address.subroles'"></td>
                <td>
			        <div v-for="subrole in member.subroles" :key="subrole">{{ $t('enum.subuserrole.'+subrole) }}</div>
			        <button v-if="(member.role=='PROVIDER' || member.role=='RESEARCH') && member.subroles.indexOf('MASTER') < 0" @click="addSubRole('MASTER')" :disabled="action!=null" class="btn btn-default btn-sm space" v-t="'admin_address.make_master_btn'"></button>
			        <button v-if="(member.role=='PROVIDER' || member.role=='RESEARCH') && !(member.subroles.indexOf('MASTER') < 0)" @click="removeSubRole('MASTER')" :disabled="action!=null" class="btn btn-default btn-sm space" v-t="'admin_address.remove_master_btn'"></button>
			    </td>
			</tr><tr v-if="member.coach">
			    <td v-t="'registration.coach'"></td><td>{{ member.coach }}</td>
			</tr><tr v-if="member.reason">
			    <td v-t="'registration.reason'"></td><td>{{ member.reason }}</td>
			</tr><tr v-if="member.developer">
			    <td v-t="'admin_address.developer'"></td><td>{{ member.developerName }}</td>
			</tr><tr>
			    <td v-t="'admin_address.security'"></td><td>{{ $t('admin_address.security_type.'+member.security) }}</td>			  
			</tr><tr>
			    <td v-t="'admin_address.auth_type'"></td><td>
			        <select @change="changeUser(member);" v-model="member.authType" v-validate class="form-control">
                        <option v-for="authType in authTypes" :key="authType" :value="authType">{{ $t('enum.secondaryauthtype.'+authType) }}</option>
                    </select>			  
                </td>			  
			</tr><tr>
			    <td v-t="'admin_address.status'"></td><td>
                    <select @change="changeUser(member);" v-model="member.status" v-validate class="form-control">
                        <option v-for="status in stati" :key="status" :value="status">{{ $t('enum.userstatus.'+status) }}</option>
                    </select>
                </td>
			</tr><tr>
			    <td v-t="'admin_address.agb_status'"></td><td>
                    <select @change="changeUser(member);" v-model="member.agbStatus" v-validate class="form-control">
                        <option v-for="status in contractStati" :key="status" :value="status">{{ $t('enum.contractstatus.'+status) }}</option>
                    </select>
                </td>
			</tr><tr>
			    <td v-t="'admin_address.contract_status'"></td><td>
                    <select @change="changeUser(member);" v-model="member.contractStatus" v-validate class="form-control">
                        <option v-for="status in contractStati" :key="status" :value="status">{{ $t('enum.contractstatus.'+status) }}</option>
                    </select>
                </td>
			</tr><tr>
			    <td v-t="'admin_address.email_status'"></td><td>{{ $t('enum.emailstatus.'+member.emailStatus) }}  <button v-if="member.emailStatus != 'VALIDATED'" @click="confirmEmail(member);" :disabled="action!=null" class="btn btn-sm btn-danger" v-t="'admin_address.email_confirm_btn'"></button></td>
			</tr><tr>
			    <td v-t="'admin_address.mobile_status'"></td><td>{{ $t('enum.emailstatus.'+(member.mobileStatus || 'UNVALIDATED')) }}</td>
			</tr><tr>
			    <td v-t="'admin_address.marketing_email'"></td><td>{{ $t('enum.channeluse.'+(member.marketingEmail || 'NULL')) }}</td>
			</tr><tr>
			    <td v-t="'admin_address.confirmation_code'"></td><td><b>{{ member.confirmationCode }}</b></td>
			</tr><tr>
			    <td v-t="'admin_address.confirmation_date'"></td><td>{{ $filters.date(member.confirmedAt) }}</td>
			</tr><tr>
			    <td v-t="'admin_address.registration_date'"></td><td>{{ $filters.date(member.registeredAt) }}</td>
			</tr><tr>
			    <td v-t="'admin_address.last_login'"></td><td>{{ $filters.date(member.login) }}</td>
			</tr>
		</table>
		<router-link :to="{ path : './members' }" class="btn btn-default mr-1" v-t="'common.back_btn'"></router-link>
		<button v-if="member.status == 'DELETED' || member.status == 'FAKE'" class="btn btn-danger mr-1" :disabled="action!=null" @click="wipe()" v-t="'admin_address.wipe_btn'"></button>
    </panel>
    <panel :title="$t('admin_address.history')" :busy="isBusy">
	
        <auditlog :patient="member._id"></auditlog>   
        <form class="form" name="myform" ref="myform" @submit.prevent="addComment()">
            <div class="form-group">
                <label for="comment" v-t="'admin_address.comment'"></label>
                <textarea class="form-control" rows="3" id="comment" v-model="comment"></textarea>
            </div>  
            <button type="submit" class="btn btn-default" v-submit v-t="'admin_address.add_comment_btn'"></button>
        </form>    
    </panel>
</template>
<script>


import Panel from "components/Panel.vue"
import Auditlog from "components/AuditLog.vue"
import users from "services/users.js"
import administration from "services/administration.js"
import { status, rl, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {
    data: () => ({	
        criteria : null,
	    stati : [ "NEW", "ACTIVE", "BLOCKED", "FAKE", "DELETED" ],
	    contractStati : [ "NEW", "REQUESTED", "PRINTED", "SIGNED" ],
	    authTypes : ["NONE", "SMS"],
        comment : ""
    }),

    components: {  Panel, ErrorBox, FormGroup, Auditlog },

    mixins : [ status, rl ],

    methods : {
        reload() {
            const { $data, $route } = this, me = this;
		    me.doBusy(users.getMembers($data.criteria, [ "midataID", "firstname", "lastname", "email", "role", "subroles", "status", "address1", "address2", "city", "confirmationCode", "agbStatus", "contractStatus", "emailStatus", "mobileStatus", "country", "email", "gender", "phone", "zip", "registeredAt", "login", "confirmedAt", "developer", "security", "authType", "marketingEmail" ])
		    .then(function(data) {
			    $data.member = data.data[0];
			    if ($data.member.role == "DEVELOPER") {
				    me.doBusy(users.getMembers({ _id : $route.query.userId, role : "DEVELOPER" }, [ "midataID", "firstname", "lastname", "email", "role", "subroles", "status", "address1", "address2", "city", "confirmationCode", "agbStatus", "contractStatus", "emailStatus", "mobileStatus", "country", "email", "gender", "phone", "zip", "registeredAt", "login", "confirmedAt", "coach", "reason", "security", "authType", "marketingEmail" ]))
				    .then(function(data2) { $data.member = data2.data[0]; });
			    }
			    if ($data.member.developer) {				
				    me.doBusy(users.getMembers({ _id : $data.member.developer }, ["email", "firstname", "lastname"])
				    .then(function(data3) {
					    $data.member.developerName = data3.data[0].firstname + " " + data3.data[0].lastname;						
				    }));
		    	}
		    }));
	    },
	
	    changeUser(user) {		
            const me = this;
		    me.doAction("changeUser", administration.changeStatus(user._id, user.status, user.contractStatus, user.agbStatus, undefined, user.authType).then(function() { me.reload(); }));
	    },
	
	    confirmEmail(user) {
            const me = this;
		    me.doAction("confirmEmail", administration.changeStatus(user._id, user.status, null, null, "VALIDATED")
		    .then(function() {
		        user.emailStatus = "VALIDATED";
		    }));
	    },
	
	    addComment() {
            const { $data } = this, me = this;
		    me.doAction("addComment", administration.addComment($data.member._id, $data.comment)
            .then(function() {
                $data.comment = "";
                $data.member._id = null;
                me.reload();
            }));		
	    },
	
	    wipe() {
            const { $data, $router } = this, me = this;
		    me.doAction("wipe", administration.wipe($data.member._id)
		    .then(function() {
			    $router.push({ path : './members' } );
		    }));
	    },
	
	    addSubRole(subrole) {
            const { $data } = this, me = this;
		    if (!$data.member.subroles) $data.member.subroles = [];
		    if (!$data.member.subroles.indexOf(subrole)>=0) $data.member.subroles.push(subrole);
		    me.doAction("addRole", administration.changeStatus($data.member._id, $data.member.status, null, null, null, null, $data.member.subroles));
	    },
	
	    removeSubRole(subrole) {
            const { $data } = this, me = this;
		    if (!$data.member.subroles) return;
		    if ($data.member.subroles.indexOf(subrole)>=0) $data.member.subroles.splice($data.member.subroles.indexOf(subrole), 1);
		    me.doAction("removeRole", administration.changeStatus($data.member._id, $data.member.status, null, null, null, null, $data.member.subroles));
	    }
    },

    created() {
        const { $data, $route } = this, me = this;
        $data.criteria = {_id : $route.query.userId };
        me.reload();
    }
    
}
</script>