var participation = angular.module('healthprovider', [ 'services', 'views' ]);

participation.controller('ListHealthProviderCtrl', ['$scope', '$http', 'views', function($scope, $http, views) {
	
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
	
	$scope.showRecords = function(mk) {
		views.setView("records", { aps : mk.aps.$oid, properties: {}, fields : [ "ownerName", "created", "id", "name" ]});
	};
	
	$scope.reload();
	
}]);