angular.module('portal')
.controller('StudyRecordsCtrl', ['$scope', '$state', 'server', 'ENV', function($scope, $state, server, ENV) {
	
	$scope.studyId = $state.params.studyId;
	
	$scope.reload = function() {
	
		$scope.downloadUrl = ENV.apiurl + jsRoutes.controllers.research.Studies.download($scope.studyId).url;
	};
	
	
	
	$scope.reload();
	
}]);