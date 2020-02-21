angular.module('portal')
.controller('AdminMailsCtrl', ['$scope', '$state', 'views', 'status', 'server', function($scope, $state, views, status, server) {

	$scope.status = new status(true);
		
	$scope.init = function(userId) {	
		$scope.status.doBusy(server.post(jsRoutes.controllers.BulkMails.get().url, JSON.stringify({ properties:{}, fields:["creator", "creatorName", "created", "started", "finished", "name", "status", "title", "content", "studyId", "studyName", "studyCode", "studyGroup", "progressId", "progressCount"] })))
    	.then(function(results) {
		  $scope.mails = results.data;
    	});
	};
		
	$scope.init();
	
}]);