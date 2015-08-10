angular.module('portal')
.controller('ResearchRegistrationCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
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
		
		server.post(jsRoutes.controllers.research.Researchers.register().url, JSON.stringify(data)).
			success(function(url) { $state.go('research.studies'); }).
			error(function(err) {
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);		
			});
	};
	
}]);