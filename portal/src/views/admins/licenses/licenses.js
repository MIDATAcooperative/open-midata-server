angular.module('portal')
.controller('LicensesCtrl', ['$scope', '$state', 'views', 'status', 'server', function($scope, $state, views, status, server) {

	$scope.status = new status(true);
		
	$scope.init = function(userId) {	
		$scope.status.doBusy(server.post(jsRoutes.controllers.Market.searchLicenses().url,JSON.stringify({ properties : {} })))
    	.then(function(results) {
		  $scope.licenses = results.data;
    	});
	};
	
			
	$scope.init();
	
}]);