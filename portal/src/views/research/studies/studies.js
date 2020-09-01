angular.module('portal')
.controller('ResearchListStudiesCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
	
	$scope.results =[];
	$scope.status = new status(true);
	$scope.sortby="-createdAt";  
	$scope.nav = $state.$current.name.split(".")[0];
	console.log($scope.nav);
	
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