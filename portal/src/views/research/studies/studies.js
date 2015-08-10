angular.module('portal')
.controller('ResearchListStudiesCtrl', ['$scope', 'server', function($scope, server) {
	
	$scope.results =[];
	$scope.error = null;
	$scope.loading = true;
	
	$scope.reload = function() {
			
		server.get(jsRoutes.controllers.research.Studies.list().url).
			success(function(data) { 				
				$scope.results = data;
				$scope.loading = false;
				$scope.error = null;
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
	
	$scope.reload();
	
}]);