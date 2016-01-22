angular.module('portal')
.controller('ProviderRegistrationCtrl', ['$scope', '$state', 'server', 'status' , function($scope, $state, server, status) {
	
	$scope.registration = {};
	$scope.error = null;
	$scope.submitted = false;
	$scope.status = new status(true, $scope);
	
	// register new user
	$scope.register = function() {

		$scope.myform.password.$setValidity('compare', $scope.registration.password ==  $scope.registration.password2);
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
						
		// send the request
		var data = $scope.registration;		
		
		$scope.status.doAction("register", server.post(jsRoutes.controllers.providers.Providers.register().url, JSON.stringify(data)))
		.then(function(data) { $state.go("public.postregister", { progress : data.data }, { location : false }); });
			
	};
	
}]);