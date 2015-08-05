angular.module('portal')
.controller('ProviderCreateRecordsCtrl', ['$scope', 'server', '$sce', function($scope, server, $sce) {
	
	// init
	$scope.error = null;
	
	// get app id (format: /records/create/:appId)
	var appId = window.location.pathname.split("/")[4];
	var userId = window.location.pathname.split("/")[5];
	
	$scope.memberUrl = portalRoutes.controllers.ProviderFrontend.member(userId).url;
	console.log($scope.memberUrl);
	
	// get app url
	server.get(jsRoutes.controllers.Apps.getUrlForMember(appId, userId).url).
		success(function(url) {
			$scope.error = null;
			$scope.url = $sce.trustAsResourceUrl(url);
		}).
		error(function(err) { $scope.error = "Failed to load app: " + err; });
	
}]);