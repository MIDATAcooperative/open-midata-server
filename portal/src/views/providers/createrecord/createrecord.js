angular.module('portal')
.controller('ProviderCreateRecordsCtrl', ['$scope', '$state', 'server', '$sce', function($scope, $state, server, $sce) {
	
	// init
	$scope.error = null;
	
	// get app id (format: /records/create/:appId)
	var appId = $state.params.appId;
	var consentId = $state.params.consentId;
	$scope.memberId = $state.params.memberId;
	
	//$scope.memberUrl = portalRoutes.controllers.ProviderFrontend.member(userId).url;
	//console.log($scope.memberUrl);
	
	// get app url
	server.get(jsRoutes.controllers.Apps.getUrlForConsent(appId, consentId).url).
		success(function(url) {
			$scope.error = null;
			$scope.url = $sce.trustAsResourceUrl(url);
		}).
		error(function(err) { $scope.error = "Failed to load app: " + err; });
	
}]);