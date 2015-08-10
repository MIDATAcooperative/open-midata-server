angular.module('views')
.controller('EnterCodeCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
	$scope.code = {};
	$scope.error = null;
	$scope.submitted = false;
	
	// register new user
	$scope.submitcode = function() {
	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.code.error = null;
		
		// send the request
		var data = $scope.code;		
		
		server.post(jsRoutes.controllers.members.Studies.enterCode().url, JSON.stringify(data)).
			success(function(data) { 
				$state.go('member.studydetails', { studyId : data.study });				
			}).
			error(function(err) {
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);				
			});
	};
}]);