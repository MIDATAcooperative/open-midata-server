angular.module('portal')
.controller('CreateStudyCtrl', ['$scope', 'server', function($scope, server) {
	
	$scope.study = {};
	$scope.error = null;
	$scope.submitted = false;
	
	// register new user
	$scope.createstudy = function() {
	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
						
		// send the request
		var data = $scope.study;		
		
		server.post(jsRoutes.controllers.research.Studies.create().url, JSON.stringify(data)).
			success(function(result) { window.location.replace(portalRoutes.controllers.ResearchFrontend.studyoverview(result._id.$oid).url); }).
			error(function(err) {
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);				
			});
	};
	
}]);