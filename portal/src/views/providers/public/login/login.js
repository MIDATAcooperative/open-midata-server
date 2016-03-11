angular.module('portal')
.controller('ProviderLoginCtrl', ['$scope', '$state', 'server', 'session', function($scope, $state, server, session) {
	
	// init
	$scope.login = {};
	$scope.error = null;
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	
	// login
	$scope.dologin = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = "Please provide an email address and a password.";
			return;
		}
		
		$scope.error = null;
		
		// send the request
		var data = {"email": $scope.login.email, "password": $scope.login.password};
		server.post(jsRoutes.controllers.providers.Providers.login().url, JSON.stringify(data))
		.then(function(result) {
			    session.postLogin(result, $state);
		}).catch(function(err) { $scope.error = err.data; });
	};
	
}]);