angular.module('portal')
.controller('CreateStudyCtrl', ['$scope', '$state', 'server', 'status', 'studies', function($scope, $state, server, status, studies) {
	
	$scope.study = {};
	$scope.studyId = $state.params.studyId;
	$scope.error = null;
	$scope.submitted = false;
	$scope.status = new status(true, $scope);
	$scope.studytypes = studies.studytypes;
	
	
	$scope.reload = function() {
		
		if ($scope.studyId) {
			$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
		    .then(function(data) { 				
				$scope.study = data.data;												
			});			
		}
		
	};
	
	// register new user
	$scope.createstudy = function() {
	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
						
		// send the request
	    var data;
		
		if ($scope.studyId) {
		
			   data = { name : $scope.study.name, description : $scope.study.description, type : $scope.study.type };
			   $scope.status.doAction("update", server.put(jsRoutes.controllers.research.Studies.update($scope.studyId).url, JSON.stringify(data)))
			   .then(function(result) { $state.go('research.study.overview', { studyId : $scope.studyId }); });
		
		} else {
			data = $scope.study;		
			$scope.status.doAction("submit", server.post(jsRoutes.controllers.research.Studies.create().url, JSON.stringify(data)))
			.then(function(result) { $state.go('research.study.overview', { studyId : result.data._id }); });
			
		}
	};
	
	$scope.reload();
	
}]);