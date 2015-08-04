angular.module('portal')
.controller('ImportRecordsCtrl', ['$scope', '$http', '$sce', function($scope, $http, $sce) {
	
	// init
	$scope.error = null;
	$scope.message = null;
	$scope.loading = true;
	$scope.authorizing = false;
	$scope.authorizingOAuth1 = false;
	$scope.authorized = false;
	$scope.finished = false;
	var app = {};
	var authorizationUrl = null;
	var authWindow = null;
	var userId = null;
	
	// get app id (format: /records/import/:appId)
	var appId = window.location.pathname.split("/")[4];
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(uId) {
			userId = uId.$oid;
			checkAuthorized();
		});
	
	// check whether we have been authorized already
	checkAuthorized = function() {
		var properties = {"_id": {"$oid": userId}};
		var fields = ["tokens." + appId];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				var tokens = users[0].tokens[appId];
				if(!_.isEmpty(tokens)) {
					$scope.authorized = true;
					$scope.message = "Loading app...";
					$scope.loading = false;
					loadApp();
				} else {
					loadAppDetails();
				}
			}).
			error(function(err) {
				$scope.error = "Failed to load user tokens: " + err;
				$scope.loading = false;
			});
	};
	
	// get the app information
	loadAppDetails = function() {
		var properties = {"_id": {"$oid": appId}};
		var fields = ["filename", "name", "type", "authorizationUrl", "consumerKey", "scopeParameters"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
			success(function(apps) {
				app = apps[0];
				$scope.appName = app.name;
				$scope.message = "The app is not authorized to import data on your behalf yet.";
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load apps: " + err;
				$scope.loading = false;
			});
	};

	// start authorization procedure
	$scope.authorize = function() {
		$scope.authorizing = true;
		$scope.message = "Authorization in progress...";
		var redirectUri = "https://" + window.location.hostname + ":9000/records/redirect/" + app._id.$oid;
		if (app.type === "oauth2") {
			var parameters = "?response_type=code" + "&client_id=" + app.consumerKey + "&scope=" + app.scopeParameters +
				"&redirect_uri=" + redirectUri;
			authWindow = window.open(app.authorizationUrl + encodeURI(parameters));
			window.addEventListener("message", onAuthorized);
		} else if (app.type === "oauth1") {
			$http(jsRoutes.controllers.Apps.getRequestTokenOAuth1(appId)).
				success(function(authUrl) {
					authorizationUrl = authUrl;
					$scope.authorizingOAuth1 = true;
				}).
				error(function(err) {
					$scope.error = "Failed to get the request token.";
					$scope.authorizing = false;
				});
		} else {
			$scope.error = "App type not supported yet.";
			$scope.authorizing = false;
		}
	};
	
	// need to factor this out, so that the pop-up doesn't get blocked
	$scope.authorizeOAuth1 = function() {
		authWindow = window.open(authorizationUrl);
		window.addEventListener("message", onAuthorized);
	};
	
	// authorization granted
	onAuthorized = function(event) {
		$scope.authorizingOAuth1 = false;
		var message = null;
		var error = null;
		if (event.origin === "https://" + window.location.hostname + ":9000" && event.source === authWindow) {
			var arguments1 = event.data.split("&");
			var keys = _.map(arguments1, function(argument) { return argument.split("=")[0]; });
			var values = _.map(arguments1, function(argument) { return argument.split("=")[1]; });
			var params = _.object(keys, values);
			if (_.has(params, "error")) {
				error = "The following error occurred: " + params.error + ". Please try again.";
			} else if (_.has(params, "code")) {
				message = "User authorization granted. Requesting access token...";
				requestAccessToken(params.code);
			} else if (_.has(params, "oauth_verifier")) {
				message = "User authorization granted. Requesting access token...";
				requestAccessToken(params.oauth_verifier);
			} else {
				error = "An unknown error occured while requesting authorization. Please try again.";
			}
		} else {
			error = "User authorization failed. Please try again.";
		}
		
		// update message with scope.apply because angular doesn't recognize the change 
		$scope.$apply(function() {
			$scope.message = message;
			$scope.error = error;
			if (error) {
				$scope.authorizing = false;
			}
		});
		authWindow.close();
	};
	
	// request access token
	requestAccessToken = function(code) {
		var data = {"code": code};
		var requestTokensUrl = null; 
		if (app.type === "oauth2") {
			requestTokensUrl = jsRoutes.controllers.Apps.requestAccessTokenOAuth2(appId).url;
		} else if (app.type === "oauth1") {
			requestTokensUrl = jsRoutes.controllers.Apps.requestAccessTokenOAuth1(appId).url;
		}
		$http.post(requestTokensUrl, JSON.stringify(data)).
			success(function() {
				$scope.authorized = true;
				$scope.authorizing = false;
				$scope.message = "Loading app...";
				loadApp();
			}).
			error(function(err) {
				$scope.error = "Requesting access token failed: " + err;
				$scope.authorizing = false;
			});
	};
	
	// load the app into the iframe
	loadApp = function() {
		// get app url
		$http(jsRoutes.controllers.Apps.getUrl(appId)).
			success(function(url) {
				$scope.error = null;
				$scope.message = null;
				$scope.url = $sce.trustAsResourceUrl(url);
				$scope.loaded = true;
			}).
			error(function(err) { $scope.error = "Failed to load app: " + err; });
	};
}]);
