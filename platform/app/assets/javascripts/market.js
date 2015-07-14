var market = angular.module('market', []);
market.controller('MarketCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {
	
	var pathsegment = window.location.pathname.split("/")[1];
	// init
	$scope.error = null;
	$scope.targetRole = (pathsegment == "providers") ? "PROVIDER" : "MEMBER"; 
	$scope.apps = {};
	$scope.apps.spotlighted = [];
	$scope.apps.suggested = [];
	$scope.visualizations = {};
	$scope.visualizations.spotlighted = [];
	$scope.visualizations.suggested = [];
	
	// get apps and visualizations
	var properties = {"spotlighted": true, "targetUserRole" : [ $scope.targetRole, "ANY"] };
	var fields = ["name", "type", "description"];
	var data = {"properties": properties, "fields": fields};
	$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
		success(function(apps) { $scope.apps.spotlighted = apps; }).
		error(function(err) { $scope.error = "Failed to load apps: " + err; });
	$http.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
		success(function(visualizations) { $scope.visualizations.spotlighted = visualizations; }).
		error(function(err) { $scope.error = "Failed to load visualizations: " + err; });
	
	// show app details
	$scope.showAppDetails = function(app) {
		var addToURL = "";
		var q = $location.search();
		if (q.context != null) addToURL += "&context="+encodeURIComponent(q.context);
		if (q.next != null) addToURL += "&next="+encodeURIComponent(q.next);
		if (addToURL!="") addToURL = "#?"+addToURL.substr(1);
		
		if ($scope.targetRole == "PROVIDER") {
			window.location.href = portalRoutes.controllers.ProviderFrontend.appDetails(app._id.$oid).url;
		} else window.location.href = portalRoutes.controllers.Apps.details(app._id.$oid).url+addToURL;
	};
	
	$scope.getAppImage = function(app) {
	  return "/assets/images/icons/"+app.type.toLowerCase()+".png";
	};
	
	$scope.getVisualizationImage = function(visulization) {
		  return "/assets/images/icons/eye.png";
	};
	
	// show visualization details
	$scope.showVisualizationDetails = function(visualization) {
		var addToURL = "";
		var q = $location.search();
		if (q.context != null) addToURL += "&context="+encodeURIComponent(q.context);
		if (q.next != null) addToURL += "&next="+encodeURIComponent(q.next);
		if (addToURL!="") addToURL = "#?"+addToURL.substr(1);
		
		if ($scope.targetRole == "PROVIDER") {
			window.location.href = portalRoutes.controllers.ProviderFrontend.visualizationDetails(visualization._id.$oid).url;
		} else window.location.href = portalRoutes.controllers.Visualizations.details(visualization._id.$oid).url+addToURL;
	}
	
}]);
market.controller('RegisterAppCtrl', ['$scope', '$http', function($scope, $http) {
	
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
		$http.post(jsRoutes.controllers.Market.registerApp(type).url, data).
			success(function(redirectUrl) { window.location.replace(portalRoutes.controllers.Market.index().url); }).
			error(function(err) { $scope.error = "Failed to register app: " + err; });
	}
	
}]);
market.controller('RegisterVisualizationCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.visualization = {};
	
	// register visualization
	$scope.registerVisualization = function() {
		if (!$scope.visualization.filename || !$scope.visualization.name || !$scope.visualization.description || !$scope.visualization.url) {
			$scope.error = "Please fill in all required fields";
			return;
		}
		
		// check whether url contains ":authToken"
		if ($scope.visualization.url.indexOf(":authToken") < 0) {
			$scope.error = "Url must contain ':authToken' to receive the authorization token required to create records.";
			return;
		}
		
		// send the request
		var data = {
				"filename": $scope.visualization.filename,
				"name": $scope.visualization.name,
				"description": $scope.visualization.description,
				"url": $scope.visualization.url
		};
		$http.post(jsRoutes.controllers.Market.registerVisualization().url, data).
			success(function(redirectUrl) { window.location.replace(portalRoutes.controllers.Market.index().url); }).
			error(function(err) { $scope.error = "Failed to register visualization: " + err; });
	}
	
}]);