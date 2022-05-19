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
    <panel :title="getTitle()" :busy="isBusy">
	    <error-box :error="error"></error-box>
	    <address>	
	        <strong>{{ member.firstname }} {{ member.lastname }}</strong><br>
	        <span v-if="member.address1">{{ member.address1 }}<br></span>
	        <span v-if="member.address2">{{ member.address2 }}<br></span>
	        <span v-if="member.zip||member.city">{{ member.zip }} {{ member.city }}<br></span>
	        {{ member.country }}<br>
	        <span v-if="member.phone"><span v-t="'common.user.phone'"></span>: {{ member.phone }}<br></span>
	        <span v-if="member.mobile"><span v-t="'common.user.mobile'"></span>: {{ member.mobile }}<br></span>	  
	        <span v-if="member.email"><span v-t="'common.user.email'"></span>: {{ member.email }}<br></span>
        </address>
        <p v-t="'provider_memberdetails.pick_consent'">Pick consent:</p>
        <div class="row">
            <div class="col-sm-7">
                <select class="form-control" @change="selectConsent(data.consent)" v-model="data.consent">
                    <option v-for="consent in consents" :key="consent._id" :value="consent._id">{{ consent.name }} - {{ $t('enum.consentstatus.'+consent.status) }}</option>
                </select>
            </div>
            <div class="col-sm-5">
                <button class="btn btn-default mr-1" @click="addData();" v-if="!hideAdd" v-t="'provider_memberdetails.share_btn'">Share Data With Patient</button>      
                <router-link class="btn btn-default" :to="{ path : './newconsent', query : { owner : member._id } }" v-t="'provider_memberdetails.propose_consent'"></router-link>            
            </div>           
        </div>
        <div v-if="setup">
            <records :setup="setup"></records>        
        </div>
    </panel>
      
    <div v-if="activeFound">
        <timeline></timeline>    
    </div>       

    <div v-if="member">
        <router-link :to="{ path : './patientsearch', query :  { email : member.email }}" class="btn btn-default" v-t="'common.back_btn'">Back</router-link>    
    </div>

</template>
<script>

import Panel from "components/Panel.vue"
import Records from "components/tiles/Records.vue"
import Timeline from "views/shared/timeline.vue"

import server from "services/server.js"
import circles from "services/circles.js"
import session from "services/session.js"
import _ from "lodash"

import { status, ErrorBox } from 'basic-vue3-components'


export default {
    data: () => ({	
        member : null,
        memberid : null,
        hideAdd : false,
        activeFound : false,
        consents : [],
        data : { consent : null },
        setup : null	
    }),

    components: {  Panel, ErrorBox, Timeline, Records },

    mixins : [ status ],

    methods : {
        getTitle() {
            const { $data } = this, me = this;
            if ($data.member) return $data.member.firstname+" "+$data.member.lastname;
            return " ";
        },

        reload() {
			const { $data } = this, me = this;
		    me.doBusy(server.get(jsRoutes.controllers.providers.Providers.getMember($data.memberid).url)
            .then(function(results) {
                var data = results.data;
                $data.member = data.member;
                $data.consents = data.consents;
                $data.backwards = data.backwards;
                
                $data.activeFound = false;
                for (let consent of $data.consents) { if (consent.status=="ACTIVE" || consent.status=="FROZEN") $data.activeFound = true; }
                
                
                if (data.memberkey) {
                    $data.setup = { aps : $data.memberkey._id, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd: false, type : "memberkeys"};
                } else {
                    $data.setup = null;
                }

            }));
	    },
	
	    selectConsent(consent) {
            const { $data } = this, me = this;
            $data.hideAdd = false;
            $data.consent = _.filter($data.consents, (x) => x._id == consent)[0];
            
            if ($data.consent != null && ($data.consent.status=="ACTIVE" || $data.consent.status=="FROZEN")) {
                $data.setup = { aps : $data.consent._id, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : false, type : "memberkeys" };
            } else {
                $data.setup = null;
            }
	    },
	
	    addDataConsent(backConsent) {
            const { $data } = this, me = this;
		    $data.data.consent = $data.consent = null;
		    $data.hideAdd = true;
            $data.setup = { aps : backConsent._id, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type : "hcrelated" };
	    },
	
	    addData() {
            const { $data } = this, me = this;
            if ($data.backwards.length > 0) {
                var consent = $data.backwards[0];
                me.addDataConsent(consent);
            } else {
                me.doAction("add", circles.createNew({ type : "HCRELATED", name : $data.member.firstname+" "+$data.member.lastname })
                .then(function(data) {
                    me.doAction("add", circles.addUsers(data.data._id, [ $data.memberid ])
                    .then(function(xdata) {
                        $data.backwards.push(data.data);
                        me.addDataConsent(data.data);
                    }));
                }));
            }
	    }
    },

    created() {  
        const { $data, $route } = this, me = this;      
        $data.memberid = $route.query.user;
        this.reload();
    }
}
</script>