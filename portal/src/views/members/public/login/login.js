angular.module('portal')
.controller('LoginCtrl', ['$scope', 'server', '$state', 'status', 'session', 'ENV', 'crypto', function($scope, server, $state, status, session, ENV, crypto) {
	
	// init		
	$scope.login = { role : "MEMBER" };	
	$scope.error = null;
	$scope.status = new status(false);
	$scope.action = $state.params.action;
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);	
	$scope.notPublic = ENV.instanceType == "prod";
	$scope.roles = [
		{ value : "MEMBER", name : "enum.userrole.MEMBER" },
		{ value : "PROVIDER" , name : "enum.userrole.PROVIDER"},
		{ value : "RESEARCH" , name : "enum.userrole.RESEARCH"},
		{ value : "DEVELOPER" , name : "enum.userrole.DEVELOPER"},
    ];
	
	if ($state.params.login) {
		$scope.login.email = $state.params.login;
	}
	if ($state.params.role) {
		$scope.login.role = $state.params.role;
	}
	
	// login
	$scope.dologin = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = { code : "error.missing.credentials" };
			return;
		}
		
		// send the request
		var data = {"email": $scope.login.email, "password": crypto.getHash($scope.login.password), "role" : $scope.login.role  };
		var func = function(data) {
			return $scope.status.doAction("login", server.post(jsRoutes.controllers.Application.authenticate().url, JSON.stringify(data)));
		};
		
		session.performLogin(func, data, $scope.login.password)
		.then(function(result) {
		   session.postLogin(result, $state);
		}).catch(function(err) { $scope.error = err.data; });
				
	};
}]);
