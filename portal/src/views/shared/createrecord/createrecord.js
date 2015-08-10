angular.module('portal')
.controller('CreateRecordsCtrl', ['$scope', '$state', 'server', '$sce', function($scope, $state, server, $sce) {
	
	// init
	$scope.error = null;
	
	// get app id (format: /records/create/:appId)
	var appId = $state.params.appId;
	
	// get app url
	server.get(jsRoutes.controllers.Apps.getUrl(appId).url).
		success(function(url) {
			$scope.error = null;
			$scope.url = $sce.trustAsResourceUrl(url);
		}).
		error(function(err) { $scope.error = "Failed to load app: " + err; });
	
}]);