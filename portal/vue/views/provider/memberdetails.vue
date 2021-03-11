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
                    <option v-for="consent in consents" :key="consent._id">{{ consent.name }}</option>
                </select>
            </div>
            <div class="col-sm-5">
                <button class="btn btn-default" @click="addData();" v-if="!hideAdd" v-t="'provider_memberdetails.share_btn'">Share Data With Patient</button>      
                <router-link class="btn btn-default" :to="{ path : './newconsent', query : { owner : member._id } }" v-t="'provider_memberdetails.propose_consent'"></router-link>            
            </div>
        </div>
    </panel>
      
    <div v-if="activeFound">
    <div ng-include="'/views/shared/dashboard/dashboard.html'"></div>
    <div ng-include="'/views/shared/timeline/timeline.html'"></div>
    </div>       
                     
    <router-link :to="{ path : './patientsearch', query :  { email : member.email }}" class="btn btn-default" v-t="'common.back_btn'">Back</router-link>    

</template>
<script>
import ErrorBox from "components/ErrorBox.vue"
import Panel from "components/Panel.vue"

import fhir from "services/fhir.js"

import status from 'mixins/status.js'
import rl from 'mixins/resultlist.js'

/*
angular.module('portal')
.controller('MemberDetailsCtrl', ['$scope', '$state', 'server', 'views', 'circles', 'session', 'status', function($scope, $state, server, views, circles, session, status) {
	
		
	views.reset();
	views.link("patient_records", "record", "record");
	
		
		
	
	
					
}]);*/

export default {
    data: () => ({	
        member : null,
        memberid : null,
        data : { consent : null }	
    }),

    components: {  Panel, ErrorBox },

    mixins : [ status ],

    methods : {
        getTitle() {
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
                    views.setView("patient_records", { aps : $data.memberkey._id, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd: false, type : "memberkeys"});
                } else {
                    views.disableView("patient_records");
                }
            }));
	    },
	
	    selectConsent(consent) {
            const { $data } = this, me = this;
            $data.hideAdd = false;
            $data.consent = consent;
            
            if ($data.consent != null && ($data.consent.status=="ACTIVE" || $data.consent.status=="FROZEN")) {
                views.setView("patient_records", { aps : $data.consent._id, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : false, type : "memberkeys" });			
            } else {
                views.disableView("patient_records");
            }
	    },
	
	    addDataConsent(backConsent) {
            const { $data } = this, me = this;
		    $data.data.consent = $data.consent = null;
		    $data.hideAdd = true;
		    views.setView("patient_records", { aps : backConsent._id, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type : "hcrelated" });
	    },
	
	    addData() {
            const { $data } = this, me = this;
            if ($data.backwards.length > 0) {
                var consent = $data.backwards[0];
                addDataConsent(consent);
            } else {
                circles.createNew({ type : "HCRELATED", name : $data.member.firstname+" "+$data.member.lastname })
                .then(function(data) {
                    circles.addUsers(data.data._id, [ $data.memberid ])
                    .then(function(xdata) {
                        $data.backwards.push(data.data);
                        addDataConsent(data.data);
                    });
                });
            }
	    }
    },

    created() {        
        $data.memberid = $route.query.user;
        this.reload();
    }
}
</script>