angular.module('portal')
.controller('OAuth2LoginCtrl', ['$scope', '$location', '$translate', 'server', '$state', 'status', 'session', 'apps', 'studies', 'oauth', 'views', function($scope, $location, $translate, server, $state, status, session, apps, studies, oauth, views) {
	
	// init
	$scope.login = { role : "MEMBER"};	
	$scope.error = null;
	$scope.status = new status(false);
	$scope.params = $location.search();
	$scope.translate = $translate;
	$scope.roles = [
		{ value : "MEMBER", name : "enum.userrole.MEMBER" },
		{ value : "PROVIDER" , name : "enum.userrole.PROVIDER "}
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
			oauth.init($scope.params.client_id, $scope.params.redirect_uri, $scope.params.state, $scope.params.code_challenge, $scope.params.code_challenge_method);
			$scope.device = oauth.getDeviceShort();
			$scope.consent = "App: "+$scope.app.name+" (Device: "+$scope.device+")";
			
			if ($scope.app.linkedStudy) {
				$scope.status.doBusy(studies.search({ _id : $scope.app.linkedStudy }, ["code", "name", "description"]))
				.then(function(studyresult) {
					if (studyresult.data && studyresult.data.length) {
					  oauth.app = $scope.app;
					  $scope.app.linkedStudyCode = studyresult.data[0].code;
				 	  $scope.app.linkedStudyName = studyresult.data[0].name;
				 	  $scope.app.linkedStudyDescription = studyresult.data[0].description;
					}
				});
			}
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
		
		oauth.setUser($scope.login.email, $scope.login.password);
				
		$scope.status.doAction("login", oauth.login(false))
		.then(function(result) {
		  if (result === "CONFIRM") {
			  $scope.acceptConsent = true;
		  } else if (result !== "ACTIVE") {
			  if (result.istatus) { $scope.pleaseConfirm = true; }
			  else {
				  session.postLogin({ data : result}, $state);  
			  }
		  }
		})
		.catch(function(err) { $scope.error = err.data; });
	};	
	
	$scope.confirm = function() {
		$scope.error = null;
		
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
		$state.go("public.registration");
	};
	
	$scope.terms = function(def) {
		views.setView("terms", def, "Terms");
	};
	
	$scope.prepare();
}]);
