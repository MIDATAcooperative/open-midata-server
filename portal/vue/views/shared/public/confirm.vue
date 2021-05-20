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
    <div class="container">
       <div class="row">
		  <div class="col-sm-12">
             <panel :busy="!allLoaded">  
				<div id="x" v-if="(!(terms.active && !inlineTerms))">
				<div v-if="pleaseConfirm">			
					<section>
						<p>{{ $t('oauth2.please_confirm', { consent }) }}</p>			
					</section>			
				</div>
			
                <div v-if="showApp">
				<section>
					<strong>{{ appname() }}</strong> <span v-t="'oauth2.requesting_app'"></span>

					<div>
					  <span>{{ appdescription() }}</span>
					</div>
										
				</section>
				
				<section v-if="app.unlockCode">						
					<label for="unlockCode" v-t="'registration.unlock_code'"></label> 
                    <input type="text" class="form-control" id="unlockCode"
						   name="unlockCode" :placeholder="$t('registration.unlock_code')" v-model="login.unlockCode" v-validate required>									
			    </section>
				</div>
				
								
				<div v-for="link in extra" :key="link._id">
					<hr>
					<div>{{ getLinkHeading(link) }}</div>
					<strong>{{ getLinkName(link) }}</strong>					
					<section v-if="link.formatted.length && !(link.inlineTerms)">						
						<div v-for="line in link.formatted" :key="line">
							<span>{{ line }}</span>
						</div>																	
					</section>
					<section v-if="link.serviceApp">
						{{ description(link.serviceApp) }}
					</section>

					<section v-if="link.inlineTerms">	
						<terms :which="link.termsOfUse"></terms>						
					</section>
					
				</div>
			
			    <section class="summary">
					
					<p><strong v-t="'oauth2.sharing_summary'"></strong></p>
					<table class="table table-sm">
						<tr>
							<th v-t="'oauth2.requests_access_short'"></th>
							<th v-for="sh in short" :key="sh">{{ sh }}</th>
							<!-- <td></td> -->
						</tr>
						<tr v-for="line in summary" :key="line.label">
							<td>{{ line.label }}</td>
							<td v-for="(sh,idx) in short" :key="idx"><i class="fas fa-check" v-if="line.checks[idx]"></i></td>
							<!-- <td>{{ line.summary }}</td> -->
						</tr>
					</table>
					<p v-t="'oauth2.reshares_data'" v-if="app.resharesData"></p>
					<p v-t="'oauth2.allows_user_search'" v-if="app.allowsUserSearch"></p>                    				
				</section>
				
				<section v-if="app.termsOfUse && showApp">
					<div class="form-check">
						<input id="appAgb" name="appAgb" class="form-check-input" type="checkbox" v-model="login.appAgb" />
							
						<label for="appAgb" class="form-check-label">
						   <span v-t="'registration.app_agb2'"></span>
						   <a @click="terms({which : app.termsOfUse })" href="javascript:" v-t="'registration.app_agb3'"></a>
						 </label>							 					
						 
					</div>
					
				</section>
				
				<section v-if="!app.termsOfUse && showApp && termsLabel">
					<div class="form-check">
						<input id="appAgb" name="appAgb" class="form-check-input" type="checkbox" v-model="login.appAgb" />							
						<label class="form-check-label" for="appAgb">{{ termsLabel }}</label>	
					</div>						 											 
					
				</section>
				
				<section v-for="link in extra" :key="link._id">
					<div class="form-check" v-if="link.extraCheckbox">
						<input type="checkbox" class="form-check-input" :id="link._id" :name="link._id" value="" :checked="login.confirmStudy.indexOf(link.studyId || link.userId || link.serviceAppId)>=0" @click="toggle(login.confirmStudy, link.studyId || link.userId || link.serviceAppId)" /> 
						<label :for="link._id" class="form-check-label">
						  <span>{{ $t(getLinkLabel(link)) }}</span>:
						  <a v-if="link.termsOfUse && !(link.inlineTerms)" @click="terms({which : link.termsOfUse })" href="javascript:">{{ (link.study || {}).name }} {{ (link.provider || {}).name }} {{ (link.serviceApp || {}).name }}</a>
						  <span v-if="!(link.termsOfUse && !(link.inlineTerms))">{{ getLinkName(link) }}</span>
						 </label>
					</div>					
				</section>
								
				<error-box :error="error" />
												
				<button class="btn btn-primary" :disabled="action!=null || doneLock" type="button" @click="confirm()">
					<span v-if="showApp" v-t="'oauth2.confirm_btn'"></span>
					<span v-if="!showApp" v-t="'oauth2.continue_btn'"></span>
				</button>
			

	</div>
			 </panel>

	<div class="mi-or-signup" v-if="!inlineTerms && terms.active">	
		<terms-modal :which="terms.which" @close="terms.active=false"></terms-modal>		
	</div>
		  </div></div>

</div>

</template>
<style scoped>
 section { margin-top:10px; margin-bottom:10px }
</style>
<script>
import server from "services/server.js";
import session from "services/session.js";
import labels from "services/labels.js";
import oauth from "services/oauth.js";
import { status, FormGroup, ErrorBox } from 'basic-vue3-components';
import ENV from "config";
import TermsModal from 'components/TermsModal.vue';
import Panel from 'components/Panel.vue';
import { getLocale } from 'services/lang';
//import { $ts } from 'vue-i18n';

export default {
  data: () => ({
    pleaseConfirm : false,
    showApp : false,
    app : null,
    login : { unlockCode : "", role : "MEMBER", confirmStudy:[] },    
    labels : [],
    extra : [],
    pages : [],
	summary : [],
	short: [],
	termsLabel: "",
    inlineTerms : false,
    project : 0,
    device : "",
	consent : "",
	terms : { which : null, active : false },
	allLoaded : false
  }),

  components : {
     FormGroup, ErrorBox, TermsModal, Panel
  },

  mixins : [ status ],
 
  methods : {
     hasIcon() {
        const { $data } = this;
		if (!$data.app || !$data.app.icons) return false;
		return $data.app.icons.indexOf("LOGINPAGE") >= 0;
	},
	
	getIconUrl() {
        const { $data } = this;
		if (!$data.app) return null;
		return ENV.apiurl + "/api/shared/icon/LOGINPAGE/" + $data.app.filename;
	},
	
	getIconUrlBG() {
        const { $data } = this;
		if (!$data.app) return null;
		return { "background-image" : "url('"+ENV.apiurl + "/api/shared/icon/LOGINPAGE/" + $data.app.filename+"')" };
	},

	appname() {
		 const { $data } = this;
		 if ($data.app && $data.app.i18n && $data.app.i18n[$data.lang] && $data.app.i18n[$data.lang].name) return $data.app.i18n[$data.lang].name;
		 return $data.app.name;
	},

	appdescription() {
		 const { $data } = this;
		 if ($data.app && $data.app.i18n && $data.app.i18n[$data.lang] && $data.app.i18n[$data.lang].description) return $data.app.i18n[$data.lang].description;
		 return $data.app.description;
	},

	description(app) {
		 const { $data } = this;
		 if (app && app.i18n && app.i18n[$data.lang] && app.i18n[$data.lang].description) return app.i18n[$data.lang].description;
		 return app.description;
	},
	
	toggle(array,itm) {	
		let pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
    },

	getLinkHeading(link) {
		let t = (link.study && link.study.type) ? link.study.type : (link.linkTargetType || "STUDY");
		
		return this.$t('oauth2.link_'+t+"_"+((link.type.indexOf("REQUIRE_P") >= 0) ? "required" : "optional"));
	},
   
    getLinkLabel(link) {
	    if (link.linkTargetType == "ORGANIZATION") {
			if (link.type.indexOf("REQUIRE_P") >= 0) return "oauth2.confirm_provider";
			return "oauth2.confirm_provider_opt";
		} 
		if (link.linkTargetType == "SERVICE") {
			if (link.type.indexOf("REQUIRE_P") >= 0) return "oauth2.confirm_service";
			return "oauth2.confirm_service_opt";
		} 
		if (link.study.type == "CLINICAL") {
			if (link.type.indexOf("REQUIRE_P") >= 0 && !(link.type.indexOf("OFFER_EXTRA_PAGE") >=0)) return "oauth2.confirm_study";
			return "oauth2.confirm_study_opt";
		}
		if (link.study.type == "CITIZENSCIENCE") return "oauth2.confirm_citizen_science";		
		if (link.study.type == "COMMUNITY") {
			if (link.type.indexOf("REQUIRE_P") >= 0 && !(link.type.indexOf("OFFER_EXTRA_PAGE") >=0)) return "oauth2.confirm_community";
			return "oauth2.confirm_community_opt";
		}		
	},

	getLinkName(link) {
		if (link.study) return link.study.name;
		if (link.provider) return link.provider.name;
		if (link.serviceApp) 
			return (link.serviceApp.i18n[getLocale()] && link.serviceApp.i18n[getLocale()].name) ? link.serviceApp.i18n[getLocale()].name : link.serviceApp.name;
		return "???";
    },

    needs(what) {
        const { $data } = this;
		return $data.study.requiredInformation && $data.study.requiredInformation == what;
	},
    
    showRegister() {
        const { $router, $route } = this;
		let params = JSON.parse(JSON.stringify($route.query));
		params.login = params.email;
		$router.push({ path : "./registration", query : params }); 	
    },
    
    confirm() {
        const { $data, $router, $route } = this;
		$data.error = null;
		
		if ($data.login.unlockCode) oauth.setUnlockCode($data.login.unlockCode);
		
		if (this.nextPage()) return;				  
		for (let i=0;i<$data.links.length;i++) {
			if (!this.checkConfirmed($data.links[i])) return;			
		}
		
		this.doAction("login", oauth.login(true, $data.login.confirmStudy))
		.then(function(result) {
		  if (result !== "ACTIVE") {
			  if (result.istatus) { $data.pleaseConfirm = true; }	
			  else {
				  session.postLogin({ data : result}, $router, $route);
			  }
		  }
		})
		.catch(function(err) { 
			$data.allLoaded = true;
			$data.error = err.response.data;
			session.failurePage($router, $route, err.response.data);
		});
    },
    
    nextPage() {
        const { $data } = this, me = this;
		if (($data.app.termsOfUse || $data.termsLabel) && !($data.login.appAgb)) {
			$data.error = { code : "error.missing.agb" };
			return true;
		}

		for (var i=0;i<$data.extra.length;i++) {
			if (!this.checkConfirmed($data.extra[i])) return true;			
		}

	   //views.disableView("terms");

	   if ($data.project >= $data.pages.length) return false;

	   $data.showApp = false;
	   $data.extra = [ $data.pages[$data.project] ];
	   $data.inlineTerms = $data.extra[0].inlineTerms;
	   if ($data.inlineTerms) this.terms({ which : $data.extra[0].termsOfUse });
	   $data.allLoaded = true;
	   me.prepareQuerySummary();
	   $data.project++;
	   return true;
    },
    
    prepareConfirm() {				
        const { $data } = this;
        const me = this;
		$data.labels = [];
		this.prepareQuery($data.app.defaultQuery, null/*$data.app.filename*/, $data.labels).then(function() {
			if ($data.showApp && !$data.app.terms && $data.extra.length==0 && $data.pages.length > 0) {
				me.confirm();
			} else  {
				$data.allLoaded = true;
				me.prepareQuerySummary();
			}
		});		
	},

	checkConfirmed(link) {
        const { $data } = this;
		if (link.type.indexOf("OFFER_P") >=0 && link.type.indexOf("REQUIRE_P")>=0 && $data.login.confirmStudy.indexOf(link.studyId || link.userId || link.serviceAppId) < 0) {
			if (link.linkTargetType == "ORGANIZATION") {
			  $data.error = { code : "error.missing.consent_accept" };
			} else if (link.linkTargetType == "SERVICE") {
				$data.error = { code : "error.missing.service_accept" };
			} else {
			  $data.error = { code : "error.missing.study_accept" };
			}
			return false;
		}
		return true;
    },
    
    prepareQuery(defaultQuery, appName, genLabels, reqInf) {
		return labels.prepareQuery(this.$t, defaultQuery, appName, genLabels, reqInf);
	},

	terms(def) {
		const { $data } = this;
		$data.terms = { which : def, active : false };
	},

	prepareQuerySummary() {
		const { $data, $t } = this, me = this;
		let short = [];
		let letters = ["", " A"," B"," C"," D"," E"," F"," G"," H"," I"];
		let idx = 0;
		let projectIdx = 0;
		let input = [];
		let req = [];	
		$data.termsLabel = null;	
		//if ($data.showApp) {
			input.push({ system : me.appname(), labels:$data.labels });
			short.push($t('oauth2.short_app'));
			req.push(me.appname());
		//}
		if ($data.extra.length > 1) idx++;
		for (let link of $data.extra) {
			input.push({ system : me.getLinkName(link), labels : link.labels });
			if (link.type.indexOf("REQUIRE_P")>=0 && link.type.indexOf("OFFER_P") <0) req.push(me.getLinkName(link));
			if (!link.linkTargetType || link.linkTargetType=="STUDY") {
				short.push($t('oauth2.short_'+(link.study.type.toLowerCase()))+letters[idx]);
				idx++;				
			} else if (link.linkTargetType=="SERVICE") {
				short.push($t('oauth2.short_service')+letters[idx]);
				idx++
			} else if (link.linkTargetType=="ORGANIZATION") {
				short.push($t('oauth2.short_party')+letters[idx]);
				idx++;
			}
		}
		if (req.length>1) {
			let last = req.pop();
			$data.termsLabel = $t('oauth2.confirm_app')+" "+req.join(", ")+" "+$t('oauth2.and')+" "+last+".";
		}
		console.log(input);
		$data.summary = labels.joinQueries(this.$t, input);
		$data.short = short;
	}
		    
  },

  created() {    
     const { $data, $route, $router } = this;
     const me = this;
     if (!oauth.app || !oauth.app.targetUserRole) {		
        $router.push({ name : "oauth.oauth2", query : $route.query });		
	 } else {		
        $data.app = oauth.app;
							
		$data.device = oauth.getDeviceShort();
		$data.consent = "App: "+$data.app.name+" (Device: "+$data.device+")";
		$data.showApp = true;
		$data.inlineTerms = false;
		//views.disableView("terms");
		$data.links = [];
		var waitFor = [];
		if (!$route.query.nostudies) {
			var project = oauth.getProject();
			var addToUrl = "";
			if (project) addToUrl = "?project="+encodeURIComponent(project);
			waitFor.push(me.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("app-use", $data.app._id).url+addToUrl)
			.then(function(data) {		    	
				let links = [];
				var r = [];
				
				for (var l=0;l<data.data.length;l++) {
					var link = data.data[l];	
					
					if (link.type.indexOf("OFFER_P")>=0 || link.type.indexOf("REQUIRE_P")>=0) {
						link.labels = [];							
						link.formatted = [];
						link.extraCheckbox = (link.type.indexOf("OFFER_P")>=0);					
						if (link.study && link.study.infos) {		
							
							for(let info of link.study.infos) {
								if (info.type=="ONBOARDING") {
									var v = info.value[getLocale()] || info.value.int || "";
									link.formatted = v.split(/\s\s/);
									//$scope.extra.push(link);
								}
							}
						}
						links.push(link);
						
						if (link.type.indexOf("OFFER_EXTRA_PAGE")>=0) {

							if (link.termsOfUse && link.type.indexOf("OFFER_INLINE_AGB")>=0) {
								link.inlineTerms = true;								
								link.formatted = [];
							} else {
								link.inlineTerms = false;
								if (link.formatted.length==0) {	
									if (link.study)	{
									  link.formatted  = [ link.study.description ];
									 } else {
										 if (link.serviceApp.i18n[getLocale()]) {
											link.formatted  = [ link.serviceApp.i18n[getLocale()].description ];
										 } else {
											link.formatted  = [ link.serviceApp.description ];
										 }
									 }
								}
							}
							
							$data.pages.push(link);

					    } else {
							$data.extra.push(link);
						}
						let recordQuery = link.study ? link.study.recordQuery : link.serviceApp ? link.serviceApp.defaultQuery : {};
						r.push(me.prepareQuery(recordQuery, null, link.labels, link.study ? link.study.requiredInformation : null));	
					}
				}
								
				$data.links = oauth.links = links;
				return Promise.all(r);
			})));							
		}

		Promise.all(waitFor).then(() => me.prepareConfirm());
        

	 }
  }
}
</script>
<style scoped>
.summary { 
	background-color: #c0c0c0;
	margin-left: -15px;
	margin-right: -15px;
	padding-top: 30px;
	padding-bottom: 30px;
	padding-left: 15px;
	padding-right: 15px;
}
</style>