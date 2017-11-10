angular.module('portal')
.controller('ParticipantCtrl', ['$scope', '$state', 'server', 'views', 'status', 'spaces', function($scope, $state, server, views, status, spaces) {
	
	$scope.studyid = $state.params.studyId;
	$scope.memberid = $state.params.participantId;
	$scope.member = {};
	$scope.participation = {};
	$scope.status = new status(true);
	$scope.loading = true;
	
	views.reset();
	views.disableView("study_records");
	
	$scope.reload = function() {
					
		
		server.get(jsRoutes.controllers.research.Studies.getParticipant($scope.studyid, $scope.memberid).url).
			success(function(data) { 								
				$scope.participation = data.participation;
				console.log($scope.participation);
				$scope.member = data.member;
				$scope.loading = false;
				if (data.participation && data.participation.status == "ACTIVE") {
					views.link("study_records", "record", "record");
					views.setView("study_records", { aps : $scope.memberid, properties : { } , fields : [ "ownerName", "created", "id", "name" ]});				
				} else {
					views.disableView("study_records");
				}
				
				$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
				.then(function(data) { 				
					$scope.study = data.data;
					
					if ($scope.study.myRole.readData) {
						spaces.getSpacesOfUserContext($scope.userId, $scope.study.code)
				    	.then(function(results) {
				    		$scope.me_menu = results.data;
				    	});						
					}
				});
				
				
											
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
		
	
	$scope.showSpace = function(space) {
		$state.go('^.spaces', { spaceId : space._id, user : $scope.memberid, study : $scope.study._id });
	};
	
	$scope.showApp = function(app) {
		spaces.openAppLink($state, $scope.userId, { app : app, user : $scope.memberid, context : $scope.study.code, study : $scope.study._id });
	};
	
	$scope.addSpace = function() {
		$state.go("research.market", { next : document.location.href , context : $scope.study.code, study : $scope.study._id, user : $scope.memberid });
	};
	
	    $scope.mayApproveParticipation = function(participation) {
		   return $scope.study && $scope.study.myRole.participants && participation.pstatus == "REQUEST";
		
		};
		
		$scope.mayAddParticipants = function() {
			   return $scope.study && $scope.study.myRole.participants && $scope.study.participantSearchStatus == "SEARCHING";
			
		};
		
	    $scope.mayRejectParticipation = function(participation) {
	      return $scope.study && $scope.study.myRole.participants && participation.pstatus == "REQUEST";
		};
		
		
		$scope.rejectParticipation = function(participation) {
			$scope.error = null;
			var params = { member : participation._id };
			
			$scope.status.doAction("reject", server.post(jsRoutes.controllers.research.Studies.rejectParticipation($scope.studyid).url, params))
			.then(function(data) { 				
			    $scope.reload();
			});
		};
		
		$scope.approveParticipation = function(participation) {
			$scope.error = null;
		
			var params = { member : participation._id };
			
			$scope.status.doAction("approve", server.post(jsRoutes.controllers.research.Studies.approveParticipation($scope.studyid).url, params))
			.then(function(data) { 				
			    $scope.reload();
			});
		};
		
		$scope.changeGroup = function(participation) {
			var params = { member : participation._id, group : participation.group };
			$scope.status.doAction("change", server.post(jsRoutes.controllers.research.Studies.updateParticipation($scope.studyid).url, JSON.stringify(params)))
			.then(function(data) { 				
			    //$scope.reload();
			});
		};
	
	$scope.reload();
	
}]);