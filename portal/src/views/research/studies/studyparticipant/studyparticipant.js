angular.module('portal')
.controller('ParticipantCtrl', ['$scope', '$state', 'server', 'views', function($scope, $state, server, views) {
	
	$scope.studyid = $state.params.studyId;
	$scope.memberid = $state.params.participantId;
	$scope.member = {};
	$scope.participation = {};
	$scope.loading = true;
	
	views.reset();
	views.disableView("study_records");
	
	$scope.reload = function() {
					
		
		server.get(jsRoutes.controllers.research.Studies.getParticipant($scope.studyid, $scope.memberid).url).
			success(function(data) { 								
				$scope.participation = data.participation;
				$scope.member = data.member;
				$scope.loading = false;
				if (data.participation && data.participation.status == "ACTIVE") {
					views.link("study_records", "record", "record");
					views.setView("study_records", { aps : $scope.memberid, properties : { } , fields : [ "ownerName", "created", "id", "name" ]});				
				} else {
					views.disableView("study_records");
				}
											
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
		
	$scope.reload();
	
}]);