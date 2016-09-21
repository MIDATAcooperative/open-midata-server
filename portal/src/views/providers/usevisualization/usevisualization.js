angular.module('portal')
.controller('UseVisualizationCtrl', ['$scope', '$state', 'server', 'session', '$sce', function($scope, $state, server, session, $sce) {
	
	// init
	$scope.error = null;
	$scope.userId = null;
	$scope.memberId = $state.params.memberId;
	$scope.consentId = $state.params.consentId;
	$scope.loading = true;
	$scope.space = { "visualization" : $state.params.visualizationId};
	//$scope.memberUrl = portalRoutes.controllers.ProviderFrontend.member($scope.memberId).url;
	
	// get current user
	session.currentUser
	.then(function(userId) {
		$scope.userId = userId;
		loadBaseUrl($scope.space);
	});
		
	// load visualization url for given space
	loadBaseUrl = function(space) {
		server.get(jsRoutes.controllers.Plugins.getUrl(space.visualization).url).
			success(function(url) {
				space.baseUrl = url;
				getAuthToken(space);
			}).
			error(function(err) { $scope.error = "Failed to load space '" + space.name + "': " + err; });
	};
	
	// get the authorization token for the current space
	getAuthToken = function(space) {
		var body = { "consent" : $scope.consentId };		
		server.post(jsRoutes.controllers.providers.Providers.getVisualizationToken().url, body).
			success(function(authToken) {
				space.completedUrl = space.baseUrl.replace(":authToken", authToken);
				reloadIframe(space);
			}).
			error(function(err) { $scope.error = "Failed to get the authorization token: " + err; });
	};
	
	// reload the iframe displaying the visualization
	reloadIframe = function(space) {
		space.trustedUrl = $sce.trustAsResourceUrl(space.completedUrl);
	
	};
		
}]);
