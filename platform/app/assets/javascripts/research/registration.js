var registration = angular.module('registration', []);
registration.controller('RegistrationCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.registration = {};
	$scope.error = null;
	$scope.submitted = false;
	
	// register new user
	$scope.register = function() {

		$scope.myform.password.$setValidity('compare', $scope.registration.password ==  $scope.registration.password2);
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
				
		
		// send the request
		var data = $scope.registration;		
		
		$http.post(jsRoutes.controllers.research.Researchers.register().url, JSON.stringify(data)).
			success(function(url) { window.location.replace(portalRoutes.controllers.ResearchFrontend.messages().url); }).
			error(function(err) {
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);		
			});
	}
	
}]);