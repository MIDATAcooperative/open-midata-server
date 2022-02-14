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
    <panel :title="$t('enum.studytype.'+study.type)" :busy="isBusy" @close="goBack">		
		<error-box :error="error"></error-box>
	
				
		<div class="row" v-if="study && study.infos">
			<div class="col-sm-4 infopanel">
          		<div class="panel panel-primary">
					<div class="panel-heading" v-t="'studydetails.duration'"></div>
					<div class="panel-body">
						<div class="lead">{{ $filters.date(study.startDate) }} - {{ $filters.date(study.endDate) }}</div>					
					</div>
				</div>

				<div class="panel panel-primary">
					<div class="panel-heading" v-t="'studydetails.information_that_needs_sharing'"></div>
					<div class="panel-body">
                    	<span v-for="label in labels" :key="label">
					   		{{ label }}, 
						</span>
						<span v-if="needs('RESTRICTED')" v-t="'studydetails.information_restricted'"></span>
						<span v-if="needs('DEMOGRAPHIC')" v-t="'studydetails.information_demographic'"></span>
						<span v-if="needs('NONE')" v-t="'studydetails.information_none'"></span>
					</div>
				</div>

				<div class="panel panel-primary">
					<div class="panel-heading" v-t="'studydetails.status'"></div>
					<div class="panel-body">                    
                    	<div class="" v-if="!(!participation || participation.pstatus == 'MATCH')">{{ $t('enum.participationstatus.'+participation.pstatus) }}</div>
						<div class="" v-if="!participation || participation.pstatus == 'MATCH'">{{ $t('enum.participantsearchstatus.'+study.participantSearchStatus) }}</div>
						<div class="">{{ $t('enum.studyexecutionstatus.'+study.executionStatus) }}</div>
						<div class="extraspace">&nbsp;</div>
						<div v-if="mayRequestParticipation()">
																
							<p v-t="'studydetails.request_participation_help'"></p>
							<button @click="requestParticipation()" :disabled="action!=null" class="btn btn-primary"
									v-t="'studydetails.request_participation_btn'"></button>		                		              
						</div>
						<div v-if="mayDeclineParticipation()">		            		             		            
							<p v-t="'studydetails.do_not_participate_help'"></p>
							<button @click="noParticipation()" :disabled="action!=null" class="btn btn-danger"
								v-t="'studydetails.do_not_participate_btn'"></button>		                		              
						</div>
						<div v-if="mayRetreatParticipation()">
														
							<p v-t="'studydetails.retreat_help'"></p>
							<button @click="retreatParticipation()" :disbled="action!=null" class="btn btn-danger"
								v-t="'studydetails.retreat_btn'"></button>		                		             
						</div>
						<div v-if="maySkip()" class="margin-top">		            		              		            		              
							<button @click="skip()" :disabled="action!=null" class="btn btn-default"
								v-t="'common.skip_btn'"></button>		                		             
						</div>
					</div>
				</div>
			</div>
		
			<div class="col-sm-8">
			
				<div class="alert alert-info" v-if="pleaseReview">
					<h4 class="alert-heading" v-t="'studydetails.please_review1'"></h4>
					<p v-t="'studydetails.please_review2'"></p>
				</div>
				
				<div class="panel panel-primary" v-if="study.infosPart.length">
					<div class="panel-heading" v-t="'studydetails.for_participants'"></div>
					<div class="panel-body">					
						<div v-for="(info,index) in study.infosPart" :key="index">
							<h4 v-if="info.type!='DESCRIPTION'">{{ $t('enum.infos.'+info.type) }}</h4>
							<p>{{ info.value[lang] || info.value.int }}</p>
						</div> 					
					</div>
				</div>
				
				<p class="lead">{{ study.name }}</p>
				<div class="line"></div>	
						
				<p v-if="!study.infos.length">{{ study.description }}</p>
														
				<div v-for="(info,index) in study.infos" :key="index">
					<h4 v-if="info.type!='DESCRIPTION'">{{ $t('enum.infos.'+info.type) }}</h4>
					<p>{{ info.value[lang] || info.value.int }}</p>
				</div>
																		
				<p v-if="study.termsOfUse">
					<router-link :to="{ path : './terms', query : { which : study.termsOfUse } }" v-t="'registration.study_agb'"></router-link>
				</p>
				
				<div class="extraspace">&nbsp;</div><div class="extraspace">&nbsp;</div>
			
				<div v-if="links.length">
					<div class="extraspace">&nbsp;</div><div class="extraspace">&nbsp;</div>
					<p class="lead" v-t="'studydetails.recommended_apps'"></p>
					<div v-for="link in links" :key="link._id">
						<div><b>{{ getAppName(link.app) }}</b></div>
						<div>{{ getAppDescription(link.app) }}</div>
						<div v-if="link.app.type != 'mobile'">
							<button class="btn btn-default" @click="installApp(link.app)" :disabled="action!=null" v-t="'studydetails.use_app'"></button>
						</div>
						<div class="extraspace"></div>
					</div>
				</div>           
			</div>
		</div>
	</panel>
	
	<panel :title="$t('studydetails.providers_monitoring')" :busy="isBusy" v-if="participation && study.assistance=='HCPROFESSIONAL'">
		<p v-if="providers.length == 0" v-t="'studydetails.no_providers'"></p>
		<div v-for="prov in providers" :key="prov._id">
			<address>
				<strong>{{ prov.firstname }} {{ prov.lastname }}</strong><br> {{ prov.address1 }}<br> {{ prov.address2 }}<br> {{
				prov.zip }} {{ prov.city }}<br> {{ $t('enum.country'+prov.country) }}<br> <span v-if="prov.phone"><span
						v-t="'common.user.phone'"></span>: {{ prov.phone }}</span><br> <span v-if="prov.mobile"><span
						v-t="'common.user.mobile_phone'"></span>: {{ prov.mobile }}</span><br> <span v-if="prov.email"><span
						v-t="'common.user.email'"></span>: {{ prov.email }}</span><br>
				</address>
				<button class="btn btn-danger btn-sm" @click="removeProvider(prov);" :disabled="action!=null" v-t="'studydetails.provider_remove_btn'"></button>
		</div>
		<div class="extraspace"></div>
		<button class="btn btn-default" @click="addProvider()" v-t="'studydetails.provider_add_btn'"></button>
	</panel>

	<panel :title="$t('studydetails.participation_history')" v-if="participation" :busy="isBusy">
		<auditlog :entity="participation._id"></auditlog>
	</panel>

	<panel :title="$t('dashboard.shared_with_study')" v-if="recordsSetup">
		<records :setup="recordsSetup"></records>
	</panel>

	<modal id="providerSearch" @close="providerSearchSetup=null" :open="providerSearchSetup" :title="$t('providersearch.title')" full-width="true">
	   <provider-search :setup="providerSearchSetup" @add="addPerson"></provider-search>
	</modal>

</template>
<script>
import Auditlog from "components/AuditLog.vue"
import Panel from 'components/Panel.vue';
import Records from 'components/tiles/Records.vue';
import { getLocale } from 'services/lang.js';
import server from 'services/server.js';
import users from 'services/users.js';
import actions from 'services/actions.js';
import studies from 'services/studies.js';
import labels from 'services/labels.js';
import session from 'services/session.js';
import { status, ErrorBox } from 'basic-vue3-components';
import _ from 'lodash';

export default {
    data: () => ({
        studyid : null,
	    study : {},
        participation : null,
        research : null,
	    providers : [],
        labels : [],
        links : [],
        lang : getLocale(),
		pleaseReview : false,
		userId : null,
		recordsSetup : null,
		providerSearchSetup : null
	}),				

	components : { ErrorBox, Panel, Auditlog, Records },

    mixins : [ status ],

    methods : {
        reload () {	
            const { $data, $route, $router, $t } = this, me = this;
		    me.doBusy(server.get(jsRoutes.controllers.members.Studies.get($data.studyid).url).
			then(function(data1) {
				var data = data1.data;
				$data.studyid = data.study._id;

				if (data.study.infosPart) data.study.infosPart = _.filter(data.study.infosPart, (x) => x.type != "SUMMARY");
				else data.study.infosPart = [];
				if (data.study.infos) data.study.infos = _.filter(data.study.infos, (x) => x.type != "SUMMARY" && x.type != "ONBOARDING");								
				else data.study.infos = [];
				delete data.study.recordQuery["group-system"];

				$data.study = data.study;
				$data.participation = data.participation;
				$data.research = data.research;							
				
				me.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("study-use", $data.study._id).url)
			    .then(function(data) {		    	
			        let links =  [];
			    	for (var l=0;l<data.data.length;l++) {
			    		var link = data.data[l];
			    		if (link.type.indexOf("RECOMMEND_A")>=0) {
			    			if (link.type.indexOf("REQUIRE_P")<0 || ($data.participation && $data.participation.pstatus=="ACCEPTED")) {
			    			  links.push(link);
			    			}
			    		}
			    	}	
					$data.links = links;
			    	
				}));	
				
				let providers = [];
				if (data.participation && data.participation.providers) {
					for (let p of data.participation.providers) {					
						providers.push(session.resolve(p, function() { return users.getMembers({ "_id" : p },users.ALLPUBLIC ); }));
					}
				}
				$data.providers = providers;
				
				if ($data.participation && !($data.participation.status == "CODE" || $data.participation.status == "MATCH" )) {
				  $data.recordsSetup = { aps : $data.participation._id, properties : { } , type:"participations", allowAdd : true, allowRemove : false, fields : [ "ownerName", "created", "id", "name" ]};
				} else {
				  $data.recordsSetup = null;
				}
                
                let genLabels = [];
                labels.prepareQuery($t, data.study.recordQuery, null, genLabels, data.study.requiredInformation);
                $data.labels = genLabels;
				
			}));
	    },
	
	    addProvider() {
            const { $data, $route, $router } = this, me = this;		   
		
		    $data.providerSearchSetup = {};
	    },

		addPerson(prov) {
			const { $data, $route, $router } = this, me = this;		   
		    return me.doAction("update", studies.updateParticipation($data.study._id, { add : { providers : [ prov._id ]}})
		    .then(function() {
				$data.providerSearchSetup = null;
		      	me.reload();
		    }));
		   
		},
	
	    removeProvider(prov) {
            const { $data, $route, $router } = this, me = this;
            me.doAction("remove", studies.updateParticipation($data.study._id, { remove : { providers : [ prov._id ]}})
            .then(function() {
                me.reload();
            }));
	    },
		
        needs(what) {
            const { $data, $route, $router } = this, me = this;
		    return $data.study.requiredInformation && $data.study.requiredInformation == what;
	    },
	
	    mayRequestParticipation() {
            const { $data, $route, $router } = this, me = this;
		    return ($data.participation != null && ( $data.participation.pstatus == "MATCH" || $data.participation.pstatus == "CODE" || $data.participation.status == "INVALID" )) ||
			       ($data.participation != null && ( $data.participation.pstatus == "MEMBER_RETREATED" || $data.participation.pstatus == "MEMBER_REJECTED" ) && $data.study.rejoinPolicy == "DELETE_LAST" && $data.study.participantSearchStatus == 'SEARCHING' && $route.meta.role.toUpperCase() == 'MEMBER' && (!$data.study.joinMethods || $data.study.joinMethods.indexOf("PORTAL")>=0 )) ||
		           ($data.participation == null && $data.study.participantSearchStatus == 'SEARCHING' && $route.meta.role.toUpperCase() == 'MEMBER' && (!$data.study.joinMethods || $data.study.joinMethods.indexOf("PORTAL")>=0 ));
	    },
	
	    mayDeclineParticipation() {
            const { $data, $route, $router } = this, me = this;
		    return $data.participation != null && ( $data.participation.pstatus == "MATCH" || $data.participation.pstatus == "CODE" || $data.participation.pstatus == "REQUEST" );
	    },
	
	    mayRetreatParticipation() {
            const { $data, $route, $router } = this, me = this;
		    return $data.participation != null && $data.participation.pstatus == "ACCEPTED";
	    },
	
	    maySkip() {
            const { $data, $route, $router } = this, me = this;
		    return $route.query.action != null;
	    },
	
	    skip() {
            const { $data, $route, $router } = this, me = this;
		    if (!actions.showAction($router, $route)) {
		        me.reload();
		    }
	    },
	
	    requestParticipation() {
            const { $data, $route, $router } = this, me = this;
		
            me.doAction("request", server.post(jsRoutes.controllers.members.Studies.requestParticipation($data.studyid).url).
            then(function(data) { 	
                if (!actions.showAction($router, $route)) {
                me.reload();
                }
            }));
	    },
	
	    noParticipation() {
			const { $data, $route, $router } = this, me = this;	
            me.doAction("no",server.post(jsRoutes.controllers.members.Studies.noParticipation($data.studyid).url).
            then(function(data) { 				
                if (!actions.showAction($router, $route)) {
                    me.reload();
                }
            }));
	    },
	
	    retreatParticipation() {
			const { $data, $route, $router } = this, me = this;
            me.doAction("retreat", server.post(jsRoutes.controllers.members.Studies.retreatParticipation($data.studyid).url).
            then(function(data) { 				
                if (!actions.showAction($router, $route)) {
                    me.reload();
                }
            }));
	    },
	
        goBack() {
            const { $data, $route, $router } = this, me = this;
		    $router.go(-1);
	    },
	

	    terms(def) {
            const { $data, $route, $router } = this, me = this;
           
            views.setView("terms", { which : def }, "Terms");
		},
		
		installApp(app) {
			const { $data, $route, $router } = this, me = this;
			spaces.openAppLink($router, $route, $data.userId , { app : app.filename });
		}
    },

    created() {
		const { $data, $route, $router } = this, me = this;
        $data.studyid = $route.query.studyId;
        $data.pleaseReview = ($route.query.action != null);
		session.currentUser.then(function(userId) {	 
			$data.userId = userId;
	        me.reload();
	    });
    }
}
</script>