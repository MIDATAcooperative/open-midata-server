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
             <panel :busy="!allLoaded" style="padding-top:20px; margin:0 auto;" :title="$t('oauth2.title')" v-if="!terms.active">
                <form ref="myform" name="myform" @submit.prevent="" novalidate>  
				<div id="x">
				<div v-if="pleaseConfirm">			
					
				   <div class="mb-3">{{ $t('oauth2.please_confirm', { consent }) }}</div>			
							
				</div>
             						   	
                <div v-if="showAppSection() && loginTemplatePage == 0">
					
				  <strong>{{ appname() }}</strong> <span v-t="'oauth2.requesting_app'"></span>

				  <div class="mt-3 mb-3">
					{{ appdescription() }}
				  </div>										
											
				</div>
				
				<div v-if="showTermsSection()">								
					<div v-if="app.termsOfUse && loginTemplatePage == 0" class="mb-3">	
						<terms :which="app.termsOfUse"></terms>						
					</div>
					<div v-for="link in extra" :key="link._id">
						<div v-if="link.termsOfUse" class="mb-3">	
							<terms :which="link.termsOfUse"></terms>						
						</div>
					</div>
				</div>

				<div v-if="showLinkSection()">								
					<div v-for="link in extra" :key="link._id">
						<div class="mb-1">							
							<div>{{ getLinkHeading(link) }}</div>
							<strong>{{ getLinkName(link) }}</strong>					
						</div>
						<div v-if="link.formatted.length" class="mb-3">						
							<div v-for="line in link.formatted" :key="line">
								<span>{{ line }}</span>
							</div>																	
						</div>
						<div v-else-if="link.serviceApp" class="mb-3">
							{{ description(link.serviceApp) }}
						</div>												
					</div>
				</div>
							
			    <section v-if="showSummary()">
				
					<div v-if="showSimpleSummary()">
						<div class="mb-3">{{ $t("oauth2.request_access") }}</div>
						<ul>
						  <li v-for="line in summary" :key="line.label">
							{{ line.label }}							
						  </li>
						</ul>
					</div>
					<div v-else class="summary">
					
				  	  <p><strong v-t="'oauth2.sharing_summary'"></strong></p>
					  <div v-for="inp of input" :key="inp.letter">
						  <b>{{ inp.letter }}</b> : <span v-if="inp.mode">{{ $t(inp.mode) }} <i class="fas fa-arrow-right"></i></span> {{ inp.system }} ({{ inp.short}}) {{ inp.target }}
					  </div>
					  <table class="table table-sm mt-2">
						  <thead>
						  <tr>
							  <th v-t="'oauth2.requests_access_short'"></th>
							  <th class="d-none d-sm-table-cell" v-for="sh in short" :key="sh">{{ sh }}</th>
							<!-- <td></td> -->
					  	  </tr>
						  </thead>
						  <tbody>
						  <tr v-for="line in summary" :key="line.label">
							  <td>{{ line.label }}
								  <div class="d-inline-block d-sm-none float-end text-muted">{{ line.letters }}</div>
							  </td>
							  <td class="d-none d-sm-table-cell" v-for="(sh,idx) in short" :key="idx"><i class="fas fa-check" v-if="line.checks[idx]"></i></td>
							<!-- <td>{{ line.summary }}</td> -->
						  </tr>
						  </tbody>
					  </table>
					
					</div>
					<p v-t="'oauth2.reshares_data'" v-if="app.resharesData"></p>
					<p v-t="'oauth2.allows_user_search'" v-if="app.allowsUserSearch"></p>                    				
				</section>
				
				<div v-if="app.loginButtonsTemplate == 'ONE_CONFIRM_AND_OPTIONAL_CHECKBOXES'">
					<section v-for="link in extra" :key="link._id">
						<div class="form-check" v-if="link.extraCheckbox">
							<input type="checkbox" class="form-check-input" :id="link._id" :name="link._id" value="" :checked="login.confirmStudy.indexOf(link.studyId || link.userId || link.serviceAppId)>=0" @click="toggle(login.confirmStudy, link.studyId || link.userId || link.serviceAppId)" /> 
							<label :for="link._id" class="form-check-label">
						  		<span>{{ $t(getLinkLabel(link)) }}</span>: <span>{{ getLinkName(link) }}</span>						  
						 	</label>
						</div>					
					</section>

					<error-box :error="error" />
												
				    <button class="btn btn-primary" :disabled="action!=null || doneLock" type="submit" v-submit @click="singleConfirm()">
						<span v-t="'oauth2.confirm_btn'"></span>					
					</button>
				</div>

				<div v-if="app.loginButtonsTemplate == 'ONE_CONFIRM_PER_PAGE'">
					<error-box :error="error" />

					<button class="btn btn-primary space" :disabled="action!=null || doneLock" type="submit" v-submit @click="confirm(true)">
						<span v-t="'oauth2.confirm_btn'"></span>					
					</button>
					<button class="btn btn-default" :disabled="action!=null || doneLock" type="submit" v-submit @click="reject()">
						<span v-t="'oauth2.reject_btn'"></span>					
					</button>
				</div>
				
				<div v-if="app.loginButtonsTemplate == 'CHECKBOXES_WITH_LINKED_TERMS'">
					<section v-if="app.termsOfUse">
						<div class="form-check">
							<input id="appAgb" name="appAgb" class="form-check-input" type="checkbox" v-model="login.appAgb" />
							
							<label for="appAgb" class="form-check-label">
						   		<span v-t="'registration.app_agb2'"></span>&nbsp;
						   		<a @click="showTerms(app.termsOfUse)" href="javascript:" v-t="'registration.app_agb3'"></a>
						 	</label>							 					
						 
						</div>					
					</section>
					<section v-for="link in extra" :key="link._id">
						<div class="form-check" v-if="link.extraCheckbox">
							<input type="checkbox" class="form-check-input" :id="link._id" :name="link._id" value="" :checked="login.confirmStudy.indexOf(link.studyId || link.userId || link.serviceAppId)>=0" @click="toggle(login.confirmStudy, link.studyId || link.userId || link.serviceAppId)" /> 
							<label :for="link._id" class="form-check-label">
							<span>{{ $t(getLinkLabel(link)) }}</span>:
							<a v-if="link.termsOfUse" @click="showTerms(link.termsOfUse)" href="javascript:">{{ (link.study || {}).name }} {{ (link.provider || {}).name }} {{ (link.serviceApp || {}).name }}</a>
							<span v-if="!(link.termsOfUse)">{{ getLinkName(link) }}</span>
							</label>
						</div>					
					</section>

					<error-box :error="error" />	

					<button class="btn btn-primary" :disabled="action!=null || doneLock" @click="confirm()" type="submit" v-submit>
						<span v-if="showApp" v-t="'oauth2.confirm_btn'"></span>
						<span v-if="!showApp" v-t="'oauth2.continue_btn'"></span>
					</button>
				</div>
																																				
			</div>
			</form>
		</panel>

	<div class="mi-or-signup" v-if="terms.active">	
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
import Terms from 'components/Terms.vue';
import Panel from 'components/Panel.vue';
import { getLocale } from 'services/lang';
//import { $ts } from 'vue-i18n';

export default {
  data: () => ({
    pleaseConfirm : false,
    app : null,
    login : { unlockCode : "", role : "MEMBER", confirmStudy:[] },    
    labels : [],
    extra : [],
    pages : [],
	summary : [],
	short: [],
	input : [],
	termsLabel: "",   
    project : 0,
    device : "",
	consent : "",
	terms : { which : null, active : false },
	allLoaded : false,    	
	loginTemplatePage : 0
  }),
  
  props: ['preview','previewpage'],

  components : {
     FormGroup, ErrorBox, TermsModal, Panel, Terms
  },

  mixins : [ status ],
 
  methods : {

    // GENERATED, TERMS_OF_USE_AND_GENERATED, TERMS_OF_USE, TERMS_WITH_VARIABLES, REDUCED
	showAppSection() {
       return this.$data.app.loginTemplate == "GENERATED";
	},

	showLinkSection() {
       return this.$data.app.loginTemplate == "GENERATED";
	},

	showTermsSection() {
		return this.$data.app.loginTemplate == "TERMS_OF_USE_AND_GENERATED" || this.$data.app.loginTemplate == "TERMS_OF_USE";
	},

	showSummary() {
       return this.$data.app.loginTemplate == "GENERATED" || this.$data.app.loginTemplate == "TERMS_OF_USE_AND_GENERATED";
	},
	
	showSimpleSummary() {
	   return this.$data.input.length == 1;
	},

	getMultiPage() {
	   return this.$data.app.loginButtonsTemplate == "ONE_CONFIRM_PER_PAGE";
	},

	showInlineTerms() {
		return this.$data.app.loginButtonsTemplate == "CHECKBOXES_WITH_LINKED_TERMS";
	},

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
		 if ($data.app && $data.app.i18n && $data.app.i18n[getLocale()] && $data.app.i18n[getLocale()].name) return $data.app.i18n[getLocale()].name;
		 return $data.app.name;
	},

	appdescription() {
		 const { $data } = this;
		 if ($data.app && $data.app.i18n && $data.app.i18n[getLocale()] && $data.app.i18n[getLocale()].description) return $data.app.i18n[getLocale()].description;
		 return $data.app.description;
	},

	description(app) {
		 const { $data } = this;
		 if (app && app.i18n && app.i18n[getLocale()] && app.i18n[getLocale()].description) return app.i18n[getLocale()].description;
		 return app.description;
	},
	
	toggle(array,itm) {	
		let pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
    },

	getLinkHeading(link) {
		let t = (link.study && link.study.type) ? link.study.type : (link.linkTargetType || "STUDY");
		
		return this.$t('oauth2.link_'+t+"_"+((link.type.indexOf("CHECK_P") >= 0) ? "required" : "optional"));
	},
   
    getLinkLabel(link) {
	    if (link.linkTargetType == "ORGANIZATION") {
			if (link.type.indexOf("CHECK_P") >= 0) return "oauth2.confirm_provider";
			return "oauth2.confirm_provider_opt";
		} 
		if (link.linkTargetType == "SERVICE") {
			if (link.type.indexOf("CHECK_P") >= 0) return "oauth2.confirm_service";
			return "oauth2.confirm_service_opt";
		} 
		if (link.study.type == "CLINICAL" || link.study.type == "REGISTRY") {
			if (link.type.indexOf("CHECK_P") >= 0 /*&& !(link.type.indexOf("OFFER_EXTRA_PAGE") >=0)*/) return "oauth2.confirm_study";
			return "oauth2.confirm_study_opt";
		}
		if (link.study.type == "CITIZENSCIENCE") return "oauth2.confirm_citizen_science";		
		if (link.study.type == "COMMUNITY") {
			if (link.type.indexOf("CHECK_P") >= 0 /*&& !(link.type.indexOf("OFFER_EXTRA_PAGE") >=0)*/) return "oauth2.confirm_community";
			return "oauth2.confirm_community_opt";
		}		
	},

	getLinkName(link) {
		if (link.study) return link.study.name;
		if (link.provider) return link.provider.name;
		if (link.userLogin) return link.userLogin;
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
    
	singleConfirm() {
		const { $data } = this;
		$data.login.appAgb = true;
		this.confirm(false);
	},

    confirm(autoconfirm) {
        const { $data, $router, $route } = this;
		$data.error = null;
		
		if (autoconfirm) {
			if ($data.loginTemplatePage==0) {
				$data.login.appAgb = true;				
			}
			for (let link of $data.extra) {
				let conf = link.studyId || link.userId || link.serviceAppId;
				if ($data.login.confirmStudy.indexOf(conf)<0) $data.login.confirmStudy.push(conf);
			}
		}

		//if ($data.login.unlockCode) oauth.setUnlockCode($data.login.unlockCode);
		
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

	reject() {
		const { $data, $router, $route } = this;
		$data.error = null;		
		
		for (let link of $data.extra) {
			let conf = link.studyId || link.userId || link.serviceAppId;
			if ($data.login.confirmStudy.indexOf(conf)>=0) this.toggle($data.login.confirmStudy, conf);		
		}

		this.confirm(false);
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

	   if ($data.loginTemplatePage >= $data.pages.length) return false;
	   
	   $data.extra = [ $data.pages[$data.loginTemplatePage] ];
	   //$data.inlineTerms = $data.extra[0].inlineTerms;
	   //if ($data.inlineTerms) me.showTerms({ which : $data.extra[0].termsOfUse });
	   $data.allLoaded = true;
	   me.prepareQuerySummary();	   
	   $data.loginTemplatePage++;
	   return true;
    },
    
    prepareConfirm() {				
        const { $data } = this;
        const me = this;
		$data.labels = [];
		this.prepareQuery($data.app.defaultQuery, null/*$data.app.filename*/, $data.labels).then(function() {
			if ($data.app.loginTemplate == 'REDUCED' && !$data.app.termsOfUse && $data.extra.length == 0) {
            //if ($data.showApp && !$data.app.terms && $data.extra.length==0 && $data.pages.length > 0) {
				me.confirm(true);
			} else  {
				$data.allLoaded = true;

				if (me.previewpage>0) {
				   $data.extra = [ $data.pages[me.previewpage-1] ];
				} else if (me.getMultiPage()) {
					$data.extra = [];
				}
				me.prepareQuerySummary();
			}
		});		
	},

	checkConfirmed(link) {
        const { $data } = this;
		if (link.type.indexOf("OFFER_P") >=0 && link.type.indexOf("CHECK_P")>=0 && $data.login.confirmStudy.indexOf(link.studyId || link.userId || link.serviceAppId) < 0) {
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

	showTerms(def) {
		
		const { $data } = this;
		$data.terms = { which : def, active : true };
	},

	prepareQuerySummary() {
		const { $data, $t } = this, me = this;
		let short = [];
		let letters = ["", "A","B","C","D","E","F","G","H","I"];
		let idx = 1;		
		let input = [];
		let req = [];			

		$data.termsLabel = null;	
		if ($data.loginTemplatePage == 0) {
			input.push({ system : me.appname(), letter : letters[idx], short : $t('oauth2.short_app'), target : ($data.app.resharesData ? null : $t("oauth2.target_device")), labels:$data.labels, mode : "oauth2.mode_all" });
			short.push(letters[idx]);
			req.push(me.appname());
			idx++;
		}
		
		for (let link of $data.extra) {			
			if (link.type.indexOf("AUTOADD_P")>=0 && link.type.indexOf("OFFER_P") <0) req.push(me.getLinkName(link));
			let sname = "";
			let mode = "oauth2.mode_all";
			let target = null;
			if (!link.linkTargetType || link.linkTargetType=="STUDY") {
				sname = $t('oauth2.short_'+(link.study.type.toLowerCase()));
				if (link.study.anonymous) mode = "oauth2.mode_anonymized";
				else if (link.study.requiredInformation=="RESTRICTED" || link.study.requiredInformation=="NONE") mode = "oauth2.mode_pseudonymized";
				if (link.study.type!="COMMUNITY") {
					if (link.study.ownerName) target = $t("oauth2.target_performed")+" "+link.study.ownerName;
				} else if (link.study.type="COMMUNITY") target = $t("oauth2.target_community");
			} else if (link.linkTargetType=="SERVICE") {
				sname = $t('oauth2.short_service');
				if (link.serviceApp && link.serviceApp.publisher) target =  $t("oauth2.target_operated")+" "+link.serviceApp.publisher;
			} else if (link.linkTargetType=="ORGANIZATION") {
				sname = $t('oauth2.short_party');
				target = link.provider.name;
			}
			short.push(letters[idx]);
			input.push({ system : me.getLinkName(link), letter : letters[idx], short : sname, labels : link.labels, mode : mode, target : target });
			idx++;
		}
		if (req.length>1) {
			let last = req.pop();
			$data.termsLabel = $t('oauth2.confirm_app')+" "+req.join(", ")+" "+$t('oauth2.and')+" "+last+".";
		}
		
		$data.summary = labels.joinQueries(this.$t, input);
		$data.short = short;
		$data.input = input;
	}
		    
  },

  created() {    
     const { $data, $route, $router } = this;
     const me = this;
     if (this.preview) {
         $data.app = this.preview;
         $data.loginTemplatePage = this.previewpage;
         $data.device = "Tst";       
     } else if (!oauth.app || !oauth.app.targetUserRole) {		
        $router.push({ name : "oauth.oauth2", query : $route.query });
        return;		
	 } else {		
        $data.app = oauth.app;
        $data.device = oauth.getDeviceShort();
     }
	if (!$data.app.loginTemplate) $data.app.loginTemplate = "GENERATED";
	if (!$data.app.loginButtonsTemplate) $data.app.loginButtonsTemplate = "ONE_CONFIRM_AND_OPTIONAL_CHECKBOXES";

	$data.consent = "App: "+$data.app.name+" (Device: "+$data.device+")";
	
	$data.links = [];
	var waitFor = [];
	if (!$route.query.nostudies) {
		var project = oauth.getProject();
		var addToUrl = "";
		if (project) addToUrl = "?project="+encodeURIComponent(project);
		waitFor.push(me.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("app-use", $data.app._id).url+addToUrl)
		.then(function(data) {	
			//console.log(data.data);
			let links = [];
			var r = [];
			
			for (var l=0;l<data.data.length;l++) {
				var link = data.data[l];	
				
				if (link.type.indexOf("OFFER_P")>=0 || link.type.indexOf("AUTOADD_P")>=0) {
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
											
					if (link.formatted.length==0) {	
						if (link.study)	{
							link.formatted  = [ link.study.description ];
							} else if (link.serviceApp) {
								if (link.serviceApp.i18n[getLocale()]) {
								link.formatted  = [ link.serviceApp.i18n[getLocale()].description ];
								} else {
								link.formatted  = [ link.serviceApp.description ];
								}
							} 
					}
			
					if (me.getMultiPage()) {					
					  $data.pages.push(link);
				    } else {						
					  $data.extra.push(link);
					}

					let recordQuery = link.study ? link.study.recordQuery : (link.serviceApp ? link.serviceApp.defaultQuery : {});
					r.push(me.prepareQuery(recordQuery, null, link.labels, link.study ? link.study.requiredInformation : null));	
				}
			}
							
			$data.links = oauth.links = links;
			return Promise.all(r);
		})));							


		Promise.all(waitFor).then(() => me.prepareConfirm());
        

	 }
  }
}
</script>
<style scoped>
.summary { 
	border-top: 1px solid #c0c0c0;
	margin-left: -15px;
	margin-right: -15px;
	padding-top: 30px;
	padding-bottom: 30px;
	padding-left: 15px;
	padding-right: 15px;
}
</style>