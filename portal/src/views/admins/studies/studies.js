angular.module('portal')
.controller('AdminListStudiesCtrl', ['$scope', 'server', 'status', function($scope, server, status) {
	
	$scope.results =[];
	$scope.status = new status(true);
	
	
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.listAdmin().url))
		.then(function(data) { 				
				$scope.results = data.data;	
		});
	};
	
	$scope.reload();
	
}]);