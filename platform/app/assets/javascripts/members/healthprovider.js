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
				$scope.showNewHCRecords();
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
	
	$scope.showNewHCRecords = function() {
		var creators = [];
		var aps = null;
		_.each($scope.results, function(hc) {
			console.log(hc);
			if (hc.provider) {
				creators.push(hc.provider.$oid);
				aps = hc.member.$oid;
			}
		});
		
		if (aps != null) {
		  views.setView("hcrecords", { aps : aps, properties: { "max-age" : 60*60*24*31, "creator" : creators }, fields : [ "ownerName", "created", "id", "name" ]});
		} else {
		  views.disableView("hcrecords");
		}
	};
	
	$scope.showRecords = function(mk) {
		views.setView("records", { aps : mk.aps.$oid, properties: {}, fields : [ "ownerName", "created", "id", "name" ]});
	};
	
	$scope.reload();
	
}]);