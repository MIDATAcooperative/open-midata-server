var login = angular.module('login', []);
login.controller('LoginCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.login = {};	
	
	// login
	$scope.login = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.login.error = "Please provide an email address and a password.";
			return;
		}
		
		// send the request
		var data = {"email": $scope.login.email, "password": $scope.login.password};
		$http.post(jsRoutes.controllers.Application.authenticate().url, JSON.stringify(data)).
			success(function(url) { window.location.replace(url); }).
			error(function(err) { $scope.login.error = err; });
	};
}]);
login.controller('RegistrationCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.registration = {};
	
	// register new user
	$scope.register = function() {
		// check user input
		/*if (!$scope.registration.email || !$scope.registration.firstName || 
				!$scope.registration.lastName || !$scope.registration.password) {
			$scope.registration.error = "Please fill in all required fields.";
			return;
		}*/
		
        $scope.myform.password.$setValidity('compare', $scope.registration.password ==  $scope.registration.password2);
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		if ($scope.registration.password !=  $scope.registration.password2) {
			$scope.registration.error = "Password and password repetition do not match!";
			return;
		}
		
        $scope.registration.error = null;
        $scope.registration.birthday = $scope.registration.birthdayYear + "-" + 
                                       $scope.registration.birthdayMonth + "-" +
                                       $scope.registration.birthdayDay;
		
		// send the request
		var data = $scope.registration;		
		$http.post(jsRoutes.controllers.Application.register().url, JSON.stringify(data)).
			success(function(url) { window.location.replace(url); }).
			error(function(err) { 
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);
				else $scope.registration.error = err; 
			});
	};
	
	$scope.days = [];
	$scope.months = [];
	$scope.years = [];
	for (var i=1;i <= 9; i++ ) { $scope.days.push("0"+i); $scope.months.push("0"+i); }
	for (var i=10;i <= 31; i++ ) $scope.days.push(""+i);	
	for (var i=10;i <= 12; i++ ) $scope.months.push(""+i);
	for (var i=2015;i > 1900; i-- ) $scope.years.push(""+i);	
	
}]);