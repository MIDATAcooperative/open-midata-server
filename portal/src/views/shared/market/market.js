angular.module('portal')
.controller('MarketCtrl', ['$scope', 'server', '$state', function($scope, server, $state) {
	
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
	server.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
		success(function(apps) { $scope.apps.spotlighted = apps; }).
		error(function(err) { $scope.error = "Failed to load apps: " + err; });
	server.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
		success(function(visualizations) { $scope.visualizations.spotlighted = visualizations; }).
		error(function(err) { $scope.error = "Failed to load visualizations: " + err; });
	
	// show app details
	$scope.showAppDetails = function(app) {		
		$state.go("^.app", { appId : app._id.$oid, context : $state.params.context, next : $state.params.next });
	};
	
	$scope.getAppImage = function(app) {
	  return "/images/icons/"+app.type.toLowerCase()+".png";
	};
	
	$scope.getVisualizationImage = function(visulization) {
		  return "/images/icons/eye.png";
	};
	
	// show visualization details
	$scope.showVisualizationDetails = function(visualization) {
		$state.go("^.visualization", { visualizationId : visualization._id.$oid, context : $state.params.context, next : $state.params.next });		
	};
	
}]);