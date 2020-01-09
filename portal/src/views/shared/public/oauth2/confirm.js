angular.module('portal')
.controller('OAuth2ConfirmLoginCtrl', ['$scope', '$location', '$translate', 'server', '$state', 'status', 'session', 'apps', 'studies', 'oauth', 'views', 'labels', 'ENV', function($scope, $location, $translate, server, $state, status, session, apps, studies, oauth, views,labels, ENV) {
	
	// init
	$scope.login = { role : "MEMBER", confirmStudy:[] };	
	$scope.error = null;
	$scope.status = new status(false);
	$scope.params = $location.search();
	$scope.translate = $translate;
	$scope.labels = [];
		
	$scope.view = views.getView("terms");
		
	$scope.prepare = function() {
		
		$scope.app = oauth.app;
		if (!$scope.app || !$scope.app.targetUserRole) $scope.error ="error.unknown.app";
					
		$scope.device = oauth.getDeviceShort();
		$scope.consent = "App: "+$scope.app.name+" (Device: "+$scope.device+")";
		
		$scope.links = [];
		if (!$state.params.nostudies) {
			$scope.status.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("app-use", $scope.app._id).url))
			.then(function(data) {		    	
				$scope.links = [];
				for (var l=0;l<data.data.length;l++) {
					var link = data.data[l];
					if (link.type.indexOf("OFFER_P")>=0) $scope.links.push(link);
				}
								
				oauth.links = $scope.links;
			});							
		}

		$scope.prepareConfirm();

	};
		
	$scope.prepareConfirm = function() {
		var sq = labels.simplifyQuery($scope.app.defaultQuery);
		
		$scope.labels = [];
		if (sq) {
			if (sq.content) {
				angular.forEach(sq.content, function(r) {
				  if (r === "Patient" || r === "Group" || r === "Person" || r === "Practitioner") return;
				  labels.getContentLabel($translate.use(), r).then(function(lab) {
					 $scope.labels.push(lab); 
				  });
				});
			}
			if (sq.group) {
				angular.forEach(sq.group, function(r) {
					  labels.getGroupLabel($translate.use(), sq["group-system"], r).then(function(lab) {
						 $scope.labels.push(lab); 
					  });
				});
			}
		}
		
	};
	
	$scope.confirm = function() {
		$scope.error = null;
		
		if ($scope.login.unlockCode) oauth.setUnlockCode($scope.login.unlockCode);
		
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
			if (link.type.indexOf("REQUIRE_P") >= 0) return "oauth2.confirm_study";
			return "oauth2.confirm_study_opt";
		}
		if (link.study.type == "CITIZENSCIENCE") return "oauth2.confirm_citizen_science";		
		if (link.study.type == "COMMUNITY") {
			if (link.type.indexOf("REQUIRE_P") >= 0) return "oauth2.confirm_community";
			return "oauth2.confirm_community_opt";
		}
		
	};
	
	console.log(oauth.app);
    if (oauth.app) {		
		$scope.prepare(); 
	} else {		
		$state.go("public.oauth2", $state.params);
	}
}]);
