angular.module('portal')
.controller('StudyOverviewCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
	
	$scope.studyid = $state.params.studyId;
	$scope.study = {};
	$scope.status = new status(true);
		
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
		.then(function(data) { 				
				$scope.study = data.data;	
		});
	};
	
	$scope.readyForValidation = function() {
		return $scope.study.validationStatus == "DRAFT" || $scope.study.validationStatus == "REJECTED";
	};
	
	$scope.readyForParticipantSearch = function() {
		return $scope.study.validationStatus == "VALIDATED" &&
		       $scope.study.executionStatus == "PRE" &&
		       (
				 $scope.study.participantSearchStatus == "PRE" ||
				 $scope.study.participantSearchStatus == "CLOSED"
			   );
	};
	
	$scope.readyForEndParticipantSearch = function() {
		return $scope.study.validationStatus == "VALIDATED" && $scope.study.participantSearchStatus == "SEARCHING";
	};
	
	$scope.readyForStartExecution = function() {
		return $scope.study.validationStatus == "VALIDATED" && 
		       $scope.study.participantSearchStatus == "CLOSED" &&
		       $scope.study.executionStatus == "PRE";
	};
	
	$scope.startValidation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.startValidation($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.startParticipantSearch = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.startParticipantSearch($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.endParticipantSearch = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.endParticipantSearch($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.startExecution = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.startExecution($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.reload();
	
}]);