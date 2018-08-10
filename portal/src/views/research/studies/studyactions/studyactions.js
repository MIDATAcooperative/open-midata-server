angular.module('portal')
.controller('StudyActionsCtrl', ['$scope', '$state', 'server', 'views', 'apps', 'status', 'circles', 'spaces', function($scope, $state, server, views, apps, status, circles, spaces) {
	
	$scope.studyId = $state.params.studyId;
	$scope.crit = { group : "" };
	$scope.status = new status(true);
	views.reset();
	
	$scope.error = null;
	$scope.submitted = false;
	
	$scope.reload = function() {
	
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
	    .then(function(data) { 				
			$scope.study = data.data;												
		});			
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("study", $scope.studyId).url))
	    .then(function(data) { 				
			$scope.links = data.data;												
		});		
	};
	
	
	$scope.reload();
	
}]);