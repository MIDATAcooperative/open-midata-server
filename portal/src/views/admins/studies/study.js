angular.module('portal')
.controller('AdminStudyCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
	
	$scope.studyid = $state.params.studyId;
	$scope.study = {};
	$scope.status = new status(true);
		
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.getAdmin($scope.studyid).url))
		.then(function(data) { 				
			$scope.study = data.data.study;	
		});
	};
	
	
	
	$scope.finishValidation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.endValidation($scope.studyid).url).
		success(function(data) { 				
			$state.go("admin.studies");
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
		
	$scope.delete = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.delete($scope.studyid).url).
		success(function(data) { 				
		    $state.go("admin.studies");
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.readyForDelete = function() {
		return true;
	};
	
	$scope.reload();
	
}]);