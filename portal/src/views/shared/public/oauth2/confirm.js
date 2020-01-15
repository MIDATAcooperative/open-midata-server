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
		
	$scope.view = views.getView("terms");
		
	$scope.prepare = function() {
		
		$scope.app = oauth.app;
		if (!$scope.app || !$scope.app.targetUserRole) $scope.error ="error.unknown.app";
					
		$scope.device = oauth.getDeviceShort();
		$scope.consent = "App: "+$scope.app.name+" (Device: "+$scope.device+")";
		$scope.showApp = true;
		$scope.inlineTerms = false;
		$scope.view.active = false;		
		$scope.links = [];
		var waitFor = [];
		if (!$state.params.nostudies) {
			waitFor.push($scope.status.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("app-use", $scope.app._id).url))
			.then(function(data) {		    	
				$scope.links = [];
				var r = [];
				
				for (var l=0;l<data.data.length;l++) {
					var link = data.data[l];	
					
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
									link.formatted  = [ link.study.description ];									
								}
							}
							
							$scope.pages.push(link);

					    } else {
							$scope.extra.push(link);
						}
						
						r.push($scope.prepareQuery(link.study.recordQuery, null, link.labels, link.study.requiredInformation));	
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
		var sq = labels.simplifyQuery(defaultQuery, appName);
		var result = [];		
		if (sq) {
			if (sq.content) {
				angular.forEach(sq.content, function(r) {
				  if (r === "Patient" || r === "Group" || r === "Person" || r === "Practitioner" || r === "ValueSet") return;
				  result.push(labels.getContentLabel($translate.use(), r).then(function(lab) {
					genLabels.push(lab); 
				  }));
				});
			}
			if (sq.group) {
				angular.forEach(sq.group, function(r) {
					  result.push(labels.getGroupLabel($translate.use(), sq["group-system"], r).then(function(lab) {
						genLabels.push(lab); 
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
			if ($scope.showApp && $scope.labels.length == 0 && !$scope.app.terms && $scope.extra.length==0) $scope.confirm();
		});
		
	};
	
	$scope.nextPage = function() {
		for (var i=0;i<$scope.extra.length;i++) {
			
			if ($scope.extra[i].type.indexOf("OFFER_P") >=0 && $scope.extra[i].type.indexOf("REQUIRE_P")>=0 && $scope.login.confirmStudy.indexOf($scope.extra[i].studyId || $scope.extra[i].userId) < 0) {
				if ($scope.extra[i].linkTargetType == "ORGANIZATION") {
				  $scope.error = { code : "error.missing.consent_accept" };
				} else {
				  $scope.error = { code : "error.missing.study_accept" };
				}
				return true;
			}
		}
	   
	   if ($scope.project >= $scope.pages.length) return false;

	   $scope.showApp = false;
	   $scope.extra = [ $scope.pages[$scope.project] ];
	   $scope.inlineTerms = $scope.extra[0].inlineTerms;
	   if ($scope.inlineTerms) $scope.terms({ which : $scope.extra[0].termsOfUse });

	   $scope.project++;
	   return true;
	};

	$scope.confirm = function() {
		$scope.error = null;
		
		if ($scope.login.unlockCode) oauth.setUnlockCode($scope.login.unlockCode);
		
		if ($scope.nextPage()) return;				  
		for (var i=0;i<$scope.links.length;i++) {
			
			if ($scope.links[i].type.indexOf("OFFER_P") >=0 && $scope.links[i].type.indexOf("REQUIRE_P")>=0 && $scope.login.confirmStudy.indexOf($scope.links[i].studyId || $scope.links[i].userId) < 0) {
				if ($scope.links[i].linkTargetType == "ORGANIZATION") {
				  $scope.error = { code : "error.missing.consent_accept" };
				} else {
				  $scope.error = { code : "error.missing.study_accept" };
				}
				return;
			}
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
		.catch(function(err) { $scope.error = err.data; });
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
		
    if (oauth.app) {		
		$scope.prepare(); 
	} else {		
		$state.go("public.oauth2", $state.params);
	}
}]);
