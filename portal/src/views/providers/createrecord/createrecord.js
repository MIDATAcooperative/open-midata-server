angular.module('portal')
.controller('ProviderCreateRecordsCtrl', ['$scope', '$http', '$sce', function($scope, $http, $sce) {
	
	// init
	$scope.error = null;
	
	// get app id (format: /records/create/:appId)
	var appId = window.location.pathname.split("/")[4];
	var userId = window.location.pathname.split("/")[5];
	
	$scope.memberUrl = portalRoutes.controllers.ProviderFrontend.member(userId).url;
	console.log($scope.memberUrl);
	
	// get app url
	$http(jsRoutes.controllers.Apps.getUrlForMember(appId, userId)).
		success(function(url) {
			$scope.error = null;
			$scope.url = $sce.trustAsResourceUrl(url);
		}).
		error(function(err) { $scope.error = "Failed to load app: " + err; });
	
}]);