angular.module('portal')
.controller('VisualizationCtrl', ['$scope', '$state', 'server', '$sce', function($scope, $state, server, $sce) {
	
	// init
	$scope.error = null;
	$scope.userId = null;
	$scope.memberId = $state.params.memberId;
	$scope.loading = true;
	$scope.space = { "visualization" : { "$oid" : $state.params.visualizationId }};
	//$scope.memberUrl = portalRoutes.controllers.ProviderFrontend.member($scope.memberId).url;
	
	// get current user
	server.get(jsRoutes.controllers.Users.getCurrentUser().url).
		success(function(userId) {
			$scope.userId = userId;
			loadBaseUrl($scope.space);
		});
		
	// load visualization url for given space
	loadBaseUrl = function(space) {
		server.get(jsRoutes.controllers.Visualizations.getUrl(space.visualization.$oid).url).
			success(function(url) {
				space.baseUrl = url;
				getAuthToken(space);
			}).
			error(function(err) { $scope.error = "Failed to load space '" + space.name + "': " + err; });
	};
	
	// get the authorization token for the current space
	getAuthToken = function(space) {
		var body = { "member" : $scope.memberId };		
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

		// have to detach and append again to force reload; just setting src didn't do the trick
		var iframe = $("#iframe").detach();
		// set src attribute of iframe to avoid creating an entry in the browser history
		iframe.attr("src", space.trustedUrl);
		$("#iframe-placeholder").append(iframe);
	};
		
}]);
