angular.module('portal')
.controller('ImportRecordsCtrl', ['$scope', '$state', '$translate', 'server', '$sce', 'session', 'status', 'spaces', function($scope, $state, $translate, server, $sce, session, status, spaces) {
	
	// init
	$scope.error = null;
	$scope.message = null;	
	$scope.authorizing = false;
	//$scope.authorizingOAuth1 = false;
	$scope.authorized = false;
	$scope.finished = false;
	$scope.status = new status(true);
	$scope.spaceId = $state.params.spaceId;
	$scope.space = { "_id" : $scope.spaceId };
	
	
	// get current user
	session.currentUser
	.then(function(userId) {
			$scope.userId = userId;
			getAuthToken($scope.space);
	});
		
	
	
	var app = {};
	var authorizationUrl = null;
	var authWindow = null;
	
	var spaceId = $state.params.spaceId;
		
	// get the authorization token for the current space
	getAuthToken = function(space, again) {
		var func = again ? $scope.status.doBusy(spaces.regetUrl(space._id)) : $scope.status.doBusy(spaces.getUrl(space._id));
		func.then(function(result) {
			if (result.data && result.data.authorizationUrl) {
		      app = result.data;
			  $scope.appName = result.data.name;
			  $scope.authorized = false;
			  $scope.message = null;	
			  
			  if (sessionStorage.authString) {	
				  $scope.authorizing = true;
				  onAuthorized(sessionStorage.authString);
				  sessionStorage.removeItem("authString");
				  sessionStorage.removeItem("returnTo");
			  }
			  
			} else {
			  var url = spaces.mainUrl(result.data, $translate.use());			  
			  space.trustedUrl = $sce.trustAsResourceUrl(url);
			  
			  $scope.error = null;
			  $scope.message = null;
			  $scope.url = $sce.trustAsResourceUrl(url);
			  $scope.loaded = true;
			  $scope.authorized = true;
			 
			}
		});
	};
	
	// start authorization procedure
	$scope.authorize = function() {
		$scope.authorizing = true;
		$scope.message = "Authorization in progress...";
		var redirectUri = window.location.protocol + "//" + window.location.hostname + /*":" + window.location.port + */ "/authorized.html";
		if (app.type === "oauth2") {
			var parameters = "?response_type=code" + "&client_id=" + app.consumerKey + "&scope=" + app.scopeParameters +
				"&redirect_uri=" + redirectUri;
			sessionStorage.returnTo=document.location.href;
			document.location.href = app.authorizationUrl + encodeURI(parameters);
			//authWindow = window.open(app.authorizationUrl + encodeURI(parameters));
			//window.addEventListener("message", onAuthorized);
		} else if (app.type === "oauth1") {
			$scope.status.doBusy(server.get(jsRoutes.controllers.Plugins.getRequestTokenOAuth1($scope.spaceId).url))
			.then(function(results) {
					authorizationUrl = results.data;
					//$scope.authorizingOAuth1 = true;
					$scope.authorizeOAuth1();
				});
		} else {
			$scope.error = "App type not supported yet.";
			$scope.authorizing = false;
		}
	};
	
	// need to factor this out, so that the pop-up doesn't get blocked
	$scope.authorizeOAuth1 = function() {
		sessionStorage.returnTo=document.location.href;
		document.location.href = authorizationUrl;
		
		//authWindow = window.open(authorizationUrl);
		//window.addEventListener("message", onAuthorized);
	};
	
	// authorization granted
	onAuthorized = function(url) {
		//$scope.authorizingOAuth1 = false;
		var message = null;
		var error = null;

		var arguments1 = url.split("&");
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
			requestAccessToken(params.oauth_verifier, params);
		} else {
			error = "An unknown error occured while requesting authorization. Please try again.";
		}
 
		
		
		$scope.message = message;
		$scope.error = error;
		if (error) {
			$scope.authorizing = false;
		}
	
		//authWindow.close();
	};
	
	// request access token
	requestAccessToken = function(code, additional) {
		var data = {"code": code};
		if (additional) data.params = additional;
		var requestTokensUrl = null; 
		if (app.type === "oauth2") {
			requestTokensUrl = jsRoutes.controllers.Plugins.requestAccessTokenOAuth2($scope.spaceId).url;
		} else if (app.type === "oauth1") {
			requestTokensUrl = jsRoutes.controllers.Plugins.requestAccessTokenOAuth1($scope.spaceId).url;
		}
		server.post(requestTokensUrl, JSON.stringify(data)).
			success(function() {
				$scope.authorized = true;
				$scope.authorizing = false;
				$scope.message = "Loading app...";
				getAuthToken($scope.space, true);
			}).
			error(function(err) {
				$scope.error = "Requesting access token failed: " + err;
				$scope.authorizing = false;
			});
	};
	
	$scope.goBack = function() {
		   spaces.get({ "_id" :  $scope.spaceId }, ["context"]).
		   then(function(result) { $state.go('^.dashboard', { dashId : result.data[0].context }); });
	};
		
}]);
