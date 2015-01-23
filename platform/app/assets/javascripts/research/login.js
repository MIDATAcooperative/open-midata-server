var login = angular.module('login', []);
login.controller('LoginCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.login = {};
	
	// login
	$scope.dologin = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.login.error = "Please provide an email address and a password.";
			return;
		}
		
		// send the request
		var data = {"email": $scope.login.email, "password": $scope.login.password};
		$http.post(jsRoutes.controllers.research.Researchers.login().url, JSON.stringify(data)).
			success(function(url) { window.location.replace(url); }).
			error(function(err) { $scope.login.error = err; });
	}
	
}]);