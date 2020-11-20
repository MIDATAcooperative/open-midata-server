/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.controller('OAuth2ConfirmLoginCtrl', ['$scope', '$location', '$translate', 'server', '$state', 'status', 'session', 'apps', 'studies', 'oauth', 'views', 'labels', 'ENV', '$q', function($scope, $location, $translate, server, $state, status, session, apps, studies, oauth, views,labels, ENV, $q) {
	
	// init
	$scope.login = { role : "MEMBER", confirmStudy:[] };	
	$scope.error = null;
	$scope.status = new status(false);
	$scope.params = $location.search();
	$scope.translate = $translate;
	$scope.labels = [];
	$scope.extra = [];
	$scope.showApp = true;
	$scope.inlineTerms = false;
	$scope.pages = [];
	$scope.project = 0;
	$scope.allLoaded = false;
		
	$scope.view = views.getView("terms");
		
	$scope.prepare = function() {
		
		$scope.app = oauth.app;
		if (!$scope.app || !$scope.app.targetUserRole) $scope.error ="error.unknown.app";
					
		$scope.device = oauth.getDeviceShort();
		$scope.consent = "App: "+$scope.app.name+" (Device: "+$scope.device+")";
		$scope.showApp = true;
		$scope.inlineTerms = false;
		views.disableView("terms");
		$scope.links = [];
		var waitFor = [];
		if (!$state.params.nostudies) {
			var project = oauth.getProject();
			var addToUrl = "";
			if (project) addToUrl = "?project="+encodeURIComponent(project);
			waitFor.push($scope.status.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("app-use", $scope.app._id).url+addToUrl))
			.then(function(data) {		    	
				$scope.links = [];
				var r = [];
				
				for (var l=0;l<data.data.length;l++) {
					var link = data.data[l];	
					console.log(link);
					if (link.type.indexOf("OFFER_P")>=0) {
						link.labels = [];							
						link.formatted = [];					
						if (link.study && link.study.infos) {		
							
							angular.forEach(link.study.infos, function(info) {
								if (info.type=="ONBOARDING") {
									var v = info.value[$translate.use()] || info.value.int || "";
									link.formatted = v.split(/\s\s/);
									//$scope.extra.push(link);
								}
							});
						}
						$scope.links.push(link);
						
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
										 if (link.serviceApp.i18n[$translate.use()]) {
											link.formatted  = [ link.serviceApp.i18n[$translate.use()].description ];
										 } else {
											link.formatted  = [ link.serviceApp.description ];
										 }
									 }
								}
							}
							
							$scope.pages.push(link);

					    } else {
							$scope.extra.push(link);
						}
						let recordQuery = link.study ? link.study.recordQuery : link.serviceApp ? link.serviceApp.defaultQuery : {};
						r.push($scope.prepareQuery(recordQuery, null, link.labels, link.study ? link.study.requiredInformation : null));	
					}
				}
								
				oauth.links = $scope.links;
				return $q.all(r);
			}));							
		}

		$q.all(waitFor).then($scope.prepareConfirm);

	};
		
	$scope.needs = function(what) {
		return $scope.study.requiredInformation && $scope.study.requiredInformation == what;
	};	

	$scope.prepareQuery = function(defaultQuery, appName, genLabels, reqInf) {
		var sq = labels.simplifyQuery(defaultQuery, appName, true);
		var result = [];		
		if (sq) {
			if (sq.content) {
				angular.forEach(sq.content, function(r) {
				  if (r === "Patient" || r === "Group" || r === "Person" || r === "Practitioner" || r === "ValueSet") return;
				  result.push(labels.getContentLabel($translate.use(), r).then(function(lab) {
					if (genLabels.indexOf(lab)<0) genLabels.push(lab); 
				  }));
				});
			}
			if (sq.group) {
				angular.forEach(sq.group, function(r) {
					  result.push(labels.getGroupLabel($translate.use(), sq["group-system"], r).then(function(lab) {
						  if (genLabels.indexOf(lab)<0) genLabels.push(lab); 
					  }));
				});
			}			
		}
		if (reqInf == 'RESTRICTED') result.push($translate("studydetails.information_restricted").then(function(l) { genLabels.push(l); }));
		if (reqInf == 'DEMOGRAPHIC') result.push($translate("studydetails.information_demographic").then(function(l) { genLabels.push(l); }));
		return $q.all(result);
				
	};

	$scope.prepareConfirm = function() {				
		$scope.labels = [];
		$scope.prepareQuery($scope.app.defaultQuery, $scope.app.filename, $scope.labels).then(function() {
			if ($scope.showApp && $scope.labels.length == 0 && !$scope.app.terms && $scope.extra.length==0) {
				$scope.confirm();
			} else  {
				$scope.allLoaded = true;
			}
		});
		
	};

	$scope.checkConfirmed = function(link) {
		if (link.type.indexOf("OFFER_P") >=0 && link.type.indexOf("REQUIRE_P")>=0 && $scope.login.confirmStudy.indexOf(link.studyId || link.userId || link.serviceAppId) < 0) {
			if (link.linkTargetType == "ORGANIZATION") {
			  $scope.error = { code : "error.missing.consent_accept" };
			} else if (link.linkTargetType == "SERVICE") {
				$scope.error = { code : "error.missing.service_accept" };
			} else {
			  $scope.error = { code : "error.missing.study_accept" };
			}
			return false;
		}
		return true;
	};
	
	$scope.nextPage = function() {
		if ($scope.app.termsOfUse && !($scope.login.appAgb)) {
			$scope.error = { code : "error.missing.agb" };
			return true;
		}

		for (var i=0;i<$scope.extra.length;i++) {
			if (!$scope.checkConfirmed($scope.extra[i])) return true;			
		}

	   views.disableView("terms");

	   if ($scope.project >= $scope.pages.length) return false;

	   $scope.showApp = false;
	   $scope.extra = [ $scope.pages[$scope.project] ];
	   $scope.inlineTerms = $scope.extra[0].inlineTerms;
	   if ($scope.inlineTerms) $scope.terms({ which : $scope.extra[0].termsOfUse });
	   $scope.allLoaded = true;
	   $scope.project++;
	   return true;
	};

	$scope.confirm = function() {
		$scope.error = null;
		
		if ($scope.login.unlockCode) oauth.setUnlockCode($scope.login.unlockCode);
		
		if ($scope.nextPage()) return;				  
		for (var i=0;i<$scope.links.length;i++) {
			if (!$scope.checkConfirmed($scope.links[i])) return;			
		}
		
		$scope.status.doAction("login", oauth.login(true, $scope.login.confirmStudy))
		.then(function(result) {
		  if (result !== "ACTIVE") {
			  if (result.istatus) { $scope.pleaseConfirm = true; }	
			  else {
				  session.postLogin({ data : result}, $state);
			  }
		  }
		})
		.catch(function(err) { 
			$scope.allLoaded = true;
			$scope.error = err.data;
			session.failurePage($state, err.data);
		});
	};
	
	$scope.showRegister = function() {
		$scope.params.login = $scope.params.email;
		$state.go("public.registration_new", $scope.params);
	};
		
	$scope.terms = function(def) {
		views.setView("terms", def, "Terms");
	};
	
	$scope.hasIcon = function() {
		if (!$scope.app || !$scope.app.icons) return false;
		return $scope.app.icons.indexOf("LOGINPAGE") >= 0;
	};
	
	$scope.getIconUrl = function() {
		if (!$scope.app) return null;
		return ENV.apiurl + "/api/shared/icon/LOGINPAGE/" + $scope.app.filename;
	};
	
	$scope.getIconUrlBG = function() {
		if (!$scope.app) return null;
		return { "background-image" : "url('"+ENV.apiurl + "/api/shared/icon/LOGINPAGE/" + $scope.app.filename+"')" };
	};
	
	$scope.toggle = function(array,itm) {	
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
   };
   
   $scope.getLinkLabel = function(link) {
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
		
	};

	$scope.getLinkName = function(link) {
		if (link.study) return link.study.name;
		if (link.provider) return link.provider.name;
		if (link.serviceApp) 
			return link.serviceApp.i18n[$translate.use()] ? link.serviceApp.i18n[$translate.use()].name : link.serviceApp.name;
		return "???";
	};
		
    if (oauth.app) {		
		$scope.prepare(); 
	} else {		
		$state.go("public.oauth2", $state.params);
	}
}]);
