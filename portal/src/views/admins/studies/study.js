angular.module('portal')
.controller('AdminStudyCtrl', ['$scope', '$state', 'server', 'status', 'users', function($scope, $state, server, status, users) {
	
	$scope.studyid = $state.params.studyId;
	$scope.study = {};
	$scope.status = new status(true);
		
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.getAdmin($scope.studyid).url))
		.then(function(data) { 				
			$scope.study = data.data.study;
			
			$scope.status.doBusy(users.getMembers({ _id : $scope.study.createdBy, "role" : "RESEARCH" }, users.MINIMAL))
			.then(function(data2) {
				$scope.creator = data2.data[0];
			});
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
	
	$scope.backToDraft = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.backToDraft($scope.studyid).url).
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