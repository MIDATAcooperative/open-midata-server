var login = angular.module('login', [ 'services' ]);
login.controller('LoginCtrl', ['$scope', '$http', 'status', function($scope, $http, status) {
	
	// init
	$scope.login = {};	
	$scope.error = null;
	$scope.status = new status(false);
	
	// login
	$scope.login = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = "Please provide an email address and a password.";
			return;
		}
		
		// send the request
		var data = {"email": $scope.login.email, "password": $scope.login.password};
		$scope.status.doAction("login", $http.post(jsRoutes.controllers.Application.authenticate().url, JSON.stringify(data))).
		then(function() { window.location.replace(portalRoutes.controllers.News.index().url); },
			 function(err) { $scope.error = err; });
	};
}]);
login.controller('RegistrationCtrl', ['$scope', '$http', 'status', function($scope, $http, status) {
	
	$scope.registration = {};
	$scope.error = null;
	$scope.status = new status(false);
	
	// register new user
	$scope.register = function() {		
		
        $scope.myform.password.$setValidity('compare', $scope.registration.password ==  $scope.registration.password2);
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		if ($scope.registration.password !=  $scope.registration.password2) {
			$scope.error = "Password and password repetition do not match!";
			return;
		}
		        
        $scope.registration.birthday = $scope.registration.birthdayYear + "-" + 
                                       $scope.registration.birthdayMonth + "-" +
                                       $scope.registration.birthdayDay;
		
		// send the request
		var data = $scope.registration;		
		$scope.status.doAction("register", $http.post(jsRoutes.controllers.Application.register().url, JSON.stringify(data))).
		then(function() { window.location.replace(portalRoutes.controllers.News.index().url); },
			 function(err) { 
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);				
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