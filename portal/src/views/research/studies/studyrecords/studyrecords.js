angular.module('portal')
.controller('StudyRecordsCtrl', ['$scope', '$state', 'server', 'ENV', 'session', 'records', 'status', function($scope, $state, server, ENV, session, records, status) {
	
	$scope.studyId = $state.params.studyId;
	$scope.status = new status(true);
	
	$scope.reload = function() {
	
		session.currentUser.then(function(userId) {
			var properties = { study : $scope.studyId };		
			return $scope.status.doBusy(records.getInfos(userId, properties)).
			then(function(results) {
				$scope.infos = results.data;	
				console.log($scope.infos);
			});
		});
		
		$scope.downloadUrl = ENV.apiurl + jsRoutes.controllers.research.Studies.download($scope.studyId).url;
	};
	
	
	
	$scope.reload();
	
}]);