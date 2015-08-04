angular.module('portal')
.controller('ParticipantCtrl', ['$scope', '$http', 'views', function($scope, $http, views) {
	
	$scope.studyid = window.location.pathname.split("/")[3];
	$scope.memberid = window.location.pathname.split("/")[5];
	$scope.member = {};
	$scope.participation = {};
	$scope.loading = true;
		
	views.link("1", "record", "record");
	
	$scope.reload = function() {
			
		views.setView("1", { aps : $scope.memberid, properties : { } , fields : [ "ownerName", "created", "id", "name" ]});
		
		$http.get(jsRoutes.controllers.research.Studies.getParticipant($scope.studyid, $scope.memberid).url).
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