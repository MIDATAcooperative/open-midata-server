angular.module('portal')
.controller('ProviderLoginCtrl', ['$scope', '$state', 'server', 'session', 'crypto', function($scope, $state, server, session, crypto) {
	
	// init
	$scope.login = {};
	$scope.error = null;
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	
	// login
	$scope.dologin = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = { code : "error.missing.credentials" };
			return;
		}
		
		$scope.error = null;
		
		var data = {"email": $scope.login.email, "password": crypto.getHash($scope.login.password) };
		var func = function(data) {
			return $scope.status.doAction("login", server.post(jsRoutes.controllers.providers.Providers.login().url, JSON.stringify(data)));
		};
		
		session.performLogin(func, data, $scope.login.password)
		.then(function(result) {
		   session.postLogin(result, $state);
		}).catch(function(err) { $scope.error = err.data; });
		
		
	};
	
}]);