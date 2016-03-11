angular.module('portal')
.controller('ResearchRegistrationCtrl', ['$scope', '$state', 'server', 'status', 'session', function($scope, $state, server, status, session) {
	
	$scope.registration = {};
	$scope.error = null;
	$scope.submitted = false;
	$scope.offline = (window.jsRoutes == undefined) || (window.jsRoutes.controllers == undefined);
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
		
		$scope.status.doAction("submit", server.post(jsRoutes.controllers.research.Researchers.register().url, JSON.stringify(data)))
		.then(function(data) { session.postLogin(data, $state); });
	};
	
}]);