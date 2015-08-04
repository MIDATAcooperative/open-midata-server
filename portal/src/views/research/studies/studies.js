angular.module('portal')
.controller('ListStudiesCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.results =[];
	$scope.error = null;
	$scope.loading = true;
	
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.research.Studies.list().url).
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