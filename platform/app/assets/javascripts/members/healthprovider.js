var participation = angular.module('healthprovider', []);

participation.controller('ListHealthProviderCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.results =[];
	$scope.error = null;
	$scope.loading = true;
	
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.members.HealthProvider.list().url).
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