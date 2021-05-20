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
    <panel :title="$t('admin_pwrecover.title')" :busy="isBusy">
		<error-box :error="error"></error-box>
        <form class="css-form form-horizontal">
            <form-group name="me" label="admin_pwrecover.myid" :path="errors.me">            
                <input class="form-control" type="text" v-model="criteria.me" v-validate>
            </form-group>
            <p v-t="'admin_pwrecover.instr1'"></p>
            <p v-t="'admin_pwrecover.instr2'"></p>
            <input class="form-control" readonly @click="copyToClip($event)" value='read -p "Enter Share:" x;printf $x| base64 -d |openssl rsautl -decrypt -inkey recoverykey.pem;echo'>
            <p v-t="'admin_pwrecover.instr3'"></p>
            <input class="form-control" readonly @click="copyToClip($event)" value='read -p "Enter Share:" x;printf $x| base64 -D |openssl rsautl -decrypt -inkey recoverykey.pem;echo'>
        
            <pagination v-model="members" search="name"></pagination>
                        
            <table class="table table-striped" v-if="members.filtered.length">

                <tr>
                    <Sorter sortby="name" v-model="members" v-t="'admin_pwrecover.name'"></Sorter>
                    <Sorter sortby="started" v-model="members" v-t="'admin_pwrecover.started'"></Sorter>
                    <th v-t="'admin_pwrecover.share'"></th>
                    <th v-t="'admin_pwrecover.decrypted'"></th>						
                    <th>&nbsp;</th>
                </tr>
                            
                <tr v-for="member in members.filtered" :key="member._id">
                    <td><router-link :to="{ path : './address', query : { userId : member._id } }">{{ member.name || 'none' }}</router-link></td>
                    <td>{{ $filters.date(member.started) }}</td>
                    <td>
                        <input type="text" style="width:70px" readonly @click="copyToClip($event)" class="form-control" v-model="member.encShares[criteria.me]">
                    </td><td>
                        <input type="text" class="form-control" v-model="member.shares[criteria.me]">
                    </td>
                    <td>
                        <button type="button" @click="commit(member)" :disabled="member.success || action!=null" class="btn btn-sm btn-default">ok</button>
                        <span class="fas fa-check text-success" v-if="member.success"></span>
                        <span class="fas fa-times text-danger" v-if="member.fail"></span>
                        {{ member.success }}{{ member.fail }}
                    </td>					
                </tr>
            </table>

            <p v-if="members.filtered.length === 0" v-t="'admin_pwrecover.empty'"></p>
        
        </form>
    </panel>
	
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import crypto from "services/crypto.js"
import { rl, status, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {

    data: () => ({	
        criteria : { me : "" },
        members : null
    }),

    components: {  Panel, ErrorBox, FormGroup },

    mixins : [ status, rl ],

    methods : {
        reload() {	
            const { $data } = this, me = this;
            me.doBusy(server.get(jsRoutes.controllers.PWRecovery.getUnfinished().url)
            .then(function(result) {		
                $data.members = me.process(result.data, { filter : { name : "" }});
            }));
        },
        
        copyToClip(elem) {
            elem = elem.currentTarget;
            elem.focus();
            elem.select();		
            window.document.execCommand("copy");
        },
            
        commit(user) {
            const me = this;
            user.success = "[...]";
            if (Object.keys(user.shares).length == crypto.keysNeeded()) {
                var rec = JSON.parse(JSON.stringify(user.shares));
                rec.encrypted = user.encShares.encrypted;
                rec.iv = user.encShares.iv;
                try {
                    var response = crypto.dorecover(rec, user.challenge);
                    server.post(jsRoutes.controllers.PWRecovery.finishRecovery().url, { _id : user._id, session : response })
                    .then(function() {
                        user.success = "["+Object.keys(user.shares).length+"/"+crypto.keysNeeded()+"]";
                    });
                } catch (e) {
                    console.log(e);
                    user.success = null;
                    user.fail = e.message;
                }
            } else {
                try {
                server.post(jsRoutes.controllers.PWRecovery.storeRecoveryShare().url, user)
                .then(function() {
                    user.success = "["+Object.keys(user.shares).length+"/"+crypto.keysNeeded()+"]";
                });
                } catch (e) {
                    console.log(e);
                    if (e.response) e = e.response;
                    user.success = null;
                    user.fail = e.message;
                }
            
            }
        }
            
    },

    created() {
        this.reload();
    }
}
</script>