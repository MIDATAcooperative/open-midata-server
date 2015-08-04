angular.module('portal')
.controller('MarketCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {
	
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
		if (addToURL!=="") addToURL = "#?"+addToURL.substr(1);
		
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
		if (addToURL!== "") addToURL = "#?"+addToURL.substr(1);
		
		if ($scope.targetRole == "PROVIDER") {
			window.location.href = portalRoutes.controllers.ProviderFrontend.visualizationDetails(visualization._id.$oid).url;
		} else window.location.href = portalRoutes.controllers.Visualizations.details(visualization._id.$oid).url+addToURL;
	};
	
}]);