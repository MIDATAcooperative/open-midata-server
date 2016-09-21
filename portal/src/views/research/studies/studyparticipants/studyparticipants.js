angular.module('portal')
.controller('ListParticipantsCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
	
	$scope.studyid = $state.params.studyId;
	$scope.results =[];
    $scope.status = new status(true);
	
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
		.then(function(data) { 				
			$scope.study = data.data;	
		});
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.listParticipants($scope.studyid).url))
		.then(function(data) { 				
			$scope.results = data.data;		
		});
	};
	
	
	$scope.mayApproveParticipation = function(participation) {
	   return participation.pstatus == "REQUEST";
	
	};
	
    $scope.mayRejectParticipation = function(participation) {
      return participation.pstatus == "REQUEST";
	};
	
	
	$scope.rejectParticipation = function(participation) {
		$scope.error = null;
		var params = { member : participation._id };
		
		server.post(jsRoutes.controllers.research.Studies.rejectParticipation($scope.studyid).url, params).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.approveParticipation = function(participation) {
		$scope.error = null;
		console.log(participation);
		var params = { member : participation._id };
		
		server.post(jsRoutes.controllers.research.Studies.approveParticipation($scope.studyid).url, params).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.changeGroup = function(participation) {
		var params = { member : participation._id, group : participation.group };
		server.post(jsRoutes.controllers.research.Studies.updateParticipation($scope.studyid).url, JSON.stringify(params))
		.then(function(data) { 				
		    //$scope.reload();
		});
	};
	
	$scope.reload();
	
}]);