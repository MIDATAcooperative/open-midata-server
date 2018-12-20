angular.module('portal')
.controller('ResearchListStudiesCtrl', ['$scope', 'server', 'status', function($scope, server, status) {
	
	$scope.results =[];
	$scope.status = new status(true);
	$scope.sortby="-createdAt";   
	
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.list().url))
		.then(function(data) { 				
				$scope.results = data.data;	
		});
	};
	
	$scope.setSort = function(key) {		
		if ($scope.sortby==key) $scope.sortby = "-"+key;
		else { $scope.sortby = key; }
	};
	
	$scope.reload();
	
}]);