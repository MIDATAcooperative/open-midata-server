var login = angular.module('lostpw', []);
login.controller('LostPasswordCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.lostpw = {};
	$scope.error = null;
		
	// submit
	$scope.submit = function() {
		// check user input
		if (!$scope.lostpw.email) {
			$scope.error = "Please provide your email address.";
			return;
		}
		
		$scope.error = null;
		
		// send the request
		var data = { "email": $scope.lostpw.email, "role" : $scope.role };
		$http.post(jsRoutes.controllers.Application.requestPasswordResetToken().url, JSON.stringify(data)).
			success(function() { $scope.lostpw.success = true; }).
			error(function(err) { $scope.error = err; });
	}
			
}]);

login.controller('SetPasswordCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {
	
	// init
	$scope.setpw = {
			token : $location.search().token,
			password : "",
			passwordRepeat : ""
	};
	$scope.error = null;
		
	// submit
	$scope.submit = function() {
		// check user input
		if (!$scope.setpw.password) {
			$scope.error = "Please set a new password.";
			return;
		}
		if (!$scope.setpw.passwordRepeat || $scope.setpw.passwordRepeat !== $scope.setpw.password) {
			$scope.error = "Password and its repetition do not match.";
			return;
		}
		
		// send the request
		var data = { "token": $scope.setpw.token, "password" : $scope.setpw.password };
		$http.post(jsRoutes.controllers.Application.setPasswordWithToken().url, JSON.stringify(data)).
			success(function() { $scope.setpw.success = true; }).
			error(function(err) { $scope.error = err; });
	}
			
}]);