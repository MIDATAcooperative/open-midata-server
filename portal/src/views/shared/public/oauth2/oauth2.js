angular.module('portal')
.controller('OAuth2LoginCtrl', ['$scope', '$location', '$translate', 'server', '$state', 'status', 'session', 'apps', 'studies', 'oauth', 'views', 'labels', 'ENV', function($scope, $location, $translate, server, $state, status, session, apps, studies, oauth, views,labels, ENV) {
	
	// init
	$scope.login = { role : "MEMBER", confirmStudy:[] };	
	$scope.error = null;
	$scope.status = new status(false);
	$scope.params = $location.search();
	$scope.translate = $translate;
	$scope.labels = [];
	$scope.roles = [
		{ value : "MEMBER", name : "enum.userrole.MEMBER" },
		{ value : "PROVIDER" , name : "enum.userrole.PROVIDER"},
		{ value : "RESEARCH" , name : "enum.userrole.RESEARCH"}
    ];
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);

	$scope.view = views.getView("terms");
	
	if ($scope.params.language) {
		$translate.use($scope.params.language);
	}
	
	if ($scope.params.email) {
		$scope.login.email = $scope.params.email;
	}
	
	$scope.prepare = function() {
		$scope.status.doBusy(apps.getAppInfo($scope.params.client_id))
		.then(function(results) {
			$scope.app = results.data;
			$scope.login.role = $scope.app.targetUserRole === 'ANY'? "MEMBER" : $scope.app.targetUserRole;
			oauth.init($scope.params.client_id, $scope.params.redirect_uri, $scope.params.state, $scope.params.code_challenge, $scope.params.code_challenge_method, $scope.params.device_id);
			$scope.device = oauth.getDeviceShort();
			$scope.consent = "App: "+$scope.app.name+" (Device: "+$scope.device+")";
			
			$scope.status.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("app-use", $scope.app._id).url))
		    .then(function(data) {		    	
		        $scope.links = [];
		    	for (var l=0;l<data.data.length;l++) {
		    		var link = data.data[l];
		    		if (link.type.indexOf("OFFER_P")>=0) $scope.links.push(link);
		    	}
		    	console.log("Studies");
				console.log($scope.links);
				
				oauth.app = $scope.app;
				oauth.links = $scope.links;
			});	
			
			/*
			if ($scope.app.linkedStudy) {
				$scope.status.doBusy(studies.search({ _id : $scope.app.linkedStudy }, ["code", "name", "description", "termsOfUse"]))
				.then(function(studyresult) {
					if (studyresult.data && studyresult.data.length) {
					  oauth.app = $scope.app;
					  $scope.app.linkedStudyCode = studyresult.data[0].code;
				 	  $scope.app.linkedStudyName = studyresult.data[0].name;
				 	  $scope.app.linkedStudyDescription = studyresult.data[0].description;
				 	  $scope.app.linkedStudyTermsOfUse = studyresult.data[0].termsOfUse;
					}
				});
			}*/
		});
	};
	
	// login
	$scope.dologin = function() {
		$scope.error = null;
		
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = { code : "error.missing.credentials" };
			return;
		}
		
		oauth.setUser($scope.login.email, $scope.login.password, $scope.login.role, $scope.login.studyLink);
				
		$scope.status.doAction("login", oauth.login(false))
		.then(function(result) {
		  if (result === "CONFIRM" || result === "CONFIRM-STUDYOK") {
			  if (result === "CONFIRM-STUDYOK") { $scope.links = []; }
			  $scope.acceptConsent = true;
			  $scope.prepareConfirm();
		  } else if (result !== "ACTIVE") {
			  if (result.studies) {
				  $scope.studies = result.studies;
			  } else if (result.istatus) { $scope.pleaseConfirm = true; }
			  else {
				  session.postLogin({ data : result}, $state);  
			  }
		  }
		})
		.catch(function(err) { $scope.error = err.data; });
	};	
	
	$scope.prepareConfirm = function() {
		var sq = labels.simplifyQuery($scope.app.defaultQuery);
		console.log(sq);
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
					  labels.getGroupLabel($translate.use(), r).then(function(lab) {
						 $scope.labels.push(lab); 
					  });
				});
			}
		}
		
	};
	
	$scope.confirm = function() {
		$scope.error = null;
		
		if ($scope.login.unlockCode) oauth.setUnlockCode($scope.login.unlockCode);
		console.log($scope.login);
		for (var i=0;i<$scope.links.length;i++) {
			console.log($scope.links[i]);
			if ($scope.links[i].type.indexOf("OFFER_P") >=0 && $scope.links[i].type.indexOf("REQUIRE_P")>=0 && $scope.login.confirmStudy.indexOf($scope.links[i].studyId) < 0) {
				$scope.error = { code : "error.missing.study_accept" };
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
		$state.go("public.registration_new");
	};
	
	$scope.lostpw = function() {
		$state.go("public.lostpw");
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
	
	$scope.prepare();
}]);
