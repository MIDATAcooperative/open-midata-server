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
	
	if ($scope.params.language) {
		$translate.use($scope.params.language);
	}
	
	if ($scope.params.email) {
		$scope.login.email = $scope.params.email;
	}
	
	if ($scope.params.login) {
		$scope.login.email = $scope.params.login;
	}
	
	$scope.prepare = function() {
		$scope.status.doBusy(apps.getAppInfo($scope.params.client_id))
		.then(function(results) {
			$scope.app = results.data;
			if (!$scope.app || !$scope.app.targetUserRole) $scope.error ="error.unknown.app";
			
			$scope.login.role = $scope.app.targetUserRole === 'ANY'? "MEMBER" : $scope.app.targetUserRole;
			oauth.init($scope.params.client_id, $scope.params.redirect_uri, $scope.params.state, $scope.params.code_challenge, $scope.params.code_challenge_method, $scope.params.device_id);
			$scope.device = oauth.getDeviceShort();
			$scope.consent = "App: "+$scope.app.name+" (Device: "+$scope.device+")";
			
			oauth.app = $scope.app;
			
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
			  if (result === "CONFIRM-STUDYOK") $scope.params.nostudies = true;
			  $state.go("^.oauthconfirm", $scope.params);
			  
		  } else if (result !== "ACTIVE") {
			  if (result.studies) {
				  $scope.studies = result.studies;
			  } else if (result.istatus) { $scope.pleaseConfirm = true; }
			  else {
				  session.postLogin({ data : result}, $state);  
			  }
		  } else { $scope.doneLock = true; }
		})
		.catch(function(err) { $scope.error = err.data; });
	};	
	
	
	
	
	$scope.showRegister = function() {
		$scope.params.login = $scope.params.email;
		console.log($scope.params);
		$state.go("public.registration_new", $state.params);
	};
	
	$scope.lostpw = function() {
		$state.go("public.lostpw");
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
		
	$scope.prepare();
}]);
