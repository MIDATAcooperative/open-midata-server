angular.module('portal')
.controller('RegisterAppCtrl', ['$scope', 'server', function($scope, server) {
	
	// init
	$scope.error = null;
	$scope.app = {};
	
	// register app
	$scope.registerApp = function(type) {
		// check required fields
		if (!$scope.app.filename || !$scope.app.name || !$scope.app.description) {
			$scope.error = "Please fill in all required fields";
			return;
		} else if (type === "oauth1" && (!$scope.app.authorizationUrl || !$scope.app.url || !$scope.app.accessTokenUrl || !$scope.app.consumerKey || !$scope.app.consumerSecret || !$scope.app.requestTokenUrl)) {
			$scope.error = "Please fill in all required fields";
			return;
		} else if (type === "oauth2" && (!$scope.app.authorizationUrl || !$scope.app.url || !$scope.app.accessTokenUrl || !$scope.app.consumerKey || !$scope.app.consumerSecret || !$scope.app.scopeParameters)) {
			$scope.error = "Please fill in all required fields";
			return;
		} else if (type === "mobile" && (!$scope.app.secret)) {
			$scope.error = "Please fill in all required fields";
			return;
		} else if (type === "create" && (!$scope.app.url)) {
			$scope.error = "Please fill in all required fields";
			return;
		}
		
		// check whether url contains ":authToken"
		if (type !== "mobile" && $scope.app.url.indexOf(":authToken") < 0) {
			$scope.error = "Url must contain ':authToken' to receive the authorization token required to create records.";
			return;
		}
		
		// piece together data object
		var data = {
				"filename": $scope.app.filename,
				"name": $scope.app.name,
				"description": $scope.app.description,
				"url": $scope.app.url
		};
		if (type === "oauth1" || type === "oauth2") {
			data.authorizationUrl = $scope.app.authorizationUrl;
			data.accessTokenUrl = $scope.app.accessTokenUrl;
			data.consumerKey = $scope.app.consumerKey;
			data.consumerSecret = $scope.app.consumerSecret;
			if (type === "oauth1") {
				data.requestTokenUrl = $scope.app.requestTokenUrl;
			} else if (type === "oauth2") {
				data.scopeParameters = $scope.app.scopeParameters;
			}
		}
		if (type === "mobile") data.secret = $scope.app.secret;
		
		// send the request
		server.post(jsRoutes.controllers.Market.registerApp(type).url, data).
			success(function(redirectUrl) { window.location.replace(portalRoutes.controllers.Market.index().url); }).
			error(function(err) { $scope.error = "Failed to register app: " + err; });
	};
	
}]);