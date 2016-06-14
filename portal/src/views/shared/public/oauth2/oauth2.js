angular.module('portal')
.controller('OAuth2LoginCtrl', ['$scope', '$location', '$translate', 'server', '$state', 'status', 'session', 'apps', function($scope, $location, $translate, server, $state, status, session, apps) {
	
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
		});
	};
	
	// login
	$scope.dologin = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = { code : "error.missing.credentials" };
			return;
		}
		
		// send the request
		
		var data = { 
		      "appname" : $scope.params.client_id,
		      "redirectUri" : $scope.params.redirect_uri,
		      "username" : $scope.login.email, 
		      "password" : $scope.login.password,
		      "state" : $scope.params.state || "none",
		      "role" : $scope.login.role
		};
				
		$scope.status.doAction("login", server.post("/v1/authorize", JSON.stringify(data))).
		then(function(result) {			
			document.location.href = $scope.params.redirect_uri + "?state=" + encodeURIComponent($scope.params.state) + "&code=" + result.data.code;			
		}).
		catch(function(err) { $scope.error = err.data; });
	};
	
	$scope.prepare();
}]);
