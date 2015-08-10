angular.module('portal')
.controller('ParticipantCtrl', ['$scope', '$state', 'server', 'views', function($scope, $state, server, views) {
	
	$scope.studyid = $state.params.studyId;
	$scope.memberid = $state.params.participantId;
	$scope.member = {};
	$scope.participation = {};
	$scope.loading = true;
		
	views.link("1", "record", "record");
	
	$scope.reload = function() {
			
		views.setView("1", { aps : $scope.memberid, properties : { } , fields : [ "ownerName", "created", "id", "name" ]});
		
		server.get(jsRoutes.controllers.research.Studies.getParticipant($scope.studyid, $scope.memberid).url).
			success(function(data) { 								
				$scope.participation = data.participation;
				$scope.member = data.member;
				$scope.loading = false;
								
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
		
	$scope.reload();
	
}]);