angular.module('portal')
.controller('ProviderLoginCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
	// init
	$scope.login = {};
	$scope.error = null;
	
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
		server.post(jsRoutes.controllers.providers.Providers.login().url, JSON.stringify(data)).
			success(function(url) { $state.go('provider.patientsearch'); }).
			error(function(err) { $scope.error = err; });
	};
	
}]);