angular.module('portal')
.controller('AdminStatsCtrl', ['$scope', '$state', 'status', 'server',  '$filter', 'crypto', function($scope, $state, status, server, $filter, crypto) {

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
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.admin.Administration.getSystemHealth().url))
		.then(function(result) {
			$scope.health = result.data;						
		});
	};
	
	$scope.requestKey = function() {
		crypto.generateKeys("12345").then(function(keys) {
			var data = {  };					
			data.password = keys.pw_hash;
			data.pub = keys.pub;
			data.priv_pw = keys.priv_pw;
			data.recovery = keys.recovery;
			data.recoveryKey = keys.recoveryKey;
		
			return $scope.status.doAction("key", server.post(jsRoutes.controllers.PWRecovery.requestServiceKeyRecovery().url, JSON.stringify(data)));
		}).then(function() {
			$state.go("^.pwrecover");
		});			
	};
	
	
    $scope.refresh();

}]);