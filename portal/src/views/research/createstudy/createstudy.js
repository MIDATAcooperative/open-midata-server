angular.module('portal')
.controller('CreateStudyCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
	
	$scope.study = {};
	$scope.error = null;
	$scope.submitted = false;
	$scope.status = new status(true, $scope);
	
	// register new user
	$scope.createstudy = function() {
	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
						
		// send the request
		var data = $scope.study;		
		
		$scope.status.doAction("submit", server.post(jsRoutes.controllers.research.Studies.create().url, JSON.stringify(data)))
		.then(function(result) { $state.go('research.study.overview', { studyId : result.data._id.$oid }); });		
	};
	
}]);