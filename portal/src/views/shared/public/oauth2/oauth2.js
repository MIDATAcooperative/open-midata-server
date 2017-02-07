angular.module('portal')
.controller('OAuth2LoginCtrl', ['$scope', '$location', '$translate', 'server', '$state', 'status', 'session', 'apps', 'oauth', function($scope, $location, $translate, server, $state, status, session, apps, oauth) {
	
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
	
	$scope.prepare = function() {
		$scope.status.doBusy(apps.getAppInfo($scope.params.client_id))
		.then(function(results) {
			$scope.app = results.data;
			oauth.init($scope.params.client_id, $scope.params.redirect_uri, $scope.params.state);
		});
	};
	
	// login
	$scope.dologin = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = { code : "error.missing.credentials" };
			return;
		}
		
		oauth.setUser($scope.login.email, $scope.login.password);
				
		$scope.status.doAction("login", oauth.login()).		
		catch(function(err) { $scope.error = err.data; });
	};	
	
	$scope.showRegister = function() {
		$state.go("public.registration");
	};
	
	$scope.prepare();
}]);
