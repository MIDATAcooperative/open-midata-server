angular.module('portal')
.controller('AdminStatsCtrl', ['$scope', '$state', 'status', 'server',  '$filter', function($scope, $state, status, server, $filter) {

	$scope.status = new status(true);    

		
	$scope.refresh = function() {
		var limit = new Date();
		limit.setDate(limit.getDate()-7);
		var data = { "properties" : { "date" : { "$gt" : limit }}};
		$scope.status.doBusy(server.post(jsRoutes.controllers.admin.Administration.getStats().url, JSON.stringify(data)))
		.then(function(result) {
			$scope.result = result.data;
			
			
			var ordered = $filter('orderBy')($scope.result, "date", true);
						
			$scope.today = ordered[0];
			$scope.yesterday = ordered.length > 1 ? ordered[1] : ordered[0];
			$scope.week = ordered[ordered.length-1];
			
		});
	};
	
    $scope.refresh();

}]);