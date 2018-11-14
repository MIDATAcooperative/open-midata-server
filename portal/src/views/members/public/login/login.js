angular.module('portal')
.controller('LoginCtrl', ['$scope', 'server', '$state', 'status', 'session', 'ENV', 'crypto', function($scope, server, $state, status, session, ENV, crypto) {
	
	// init		
	$scope.login = {};	
	$scope.error = null;
	$scope.status = new status(false);
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);	
	$scope.notPublic = ENV.instanceType == "prod";
	
	// login
	$scope.login = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = { code : "error.missing.credentials" };
			return;
		}
		
		// send the request
		var data = {"email": $scope.login.email, "password": crypto.getHash($scope.login.password) };
		var func = function(data) {
			return $scope.status.doAction("login", server.post(jsRoutes.controllers.Application.authenticate().url, JSON.stringify(data)));
		};
		
		session.performLogin(func, data, $scope.login.password)
		.then(function(result) {
		   session.postLogin(result, $state);
		}).catch(function(err) { $scope.error = err.data; });
				
	};
}]);
