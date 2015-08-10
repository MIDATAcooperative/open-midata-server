angular.module('portal')
.controller('ListParticipantsCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
	$scope.studyid = $state.params.studyId;
	$scope.results =[];
	$scope.error = null;
	$scope.loading = true;
	
	$scope.reload = function() {
			
		server.get(jsRoutes.controllers.research.Studies.listParticipants($scope.studyid).url).
			success(function(data) { 				
				$scope.results = data;
				$scope.loading = false;
				$scope.error = null;
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
	
	
	$scope.mayApproveParticipation = function(participation) {
	   return participation.status == "REQUEST";
	
	};
	
    $scope.mayRejectParticipation = function(participation) {
      return participation.status == "REQUEST";
	};
	
	
	$scope.rejectParticipation = function(participation) {
		$scope.error = null;
		var params = { member : participation._id.$oid };
		
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
		var params = { member : participation._id.$oid };
		
		server.post(jsRoutes.controllers.research.Studies.approveParticipation($scope.studyid).url, params).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.reload();
	
}]);