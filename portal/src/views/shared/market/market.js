angular.module('portal')
.controller('MarketCtrl', ['$scope', 'server', '$state', 'ENV', function($scope, server, $state, ENV) {
	
	var pathsegment = window.location.pathname.split("/")[1];
	// init
	$scope.error = null;
	$scope.beta = ENV.beta;
	$scope.targetRole = $state.params.context == "PROVIDER" ? "PROVIDER" : "MEMBER"; 
	/*$scope.apps = {};
	$scope.apps.spotlighted = [];
	$scope.apps.suggested = [];*/
	$scope.visualizations = {};
	$scope.visualizations.spotlighted = [];
	$scope.visualizations.suggested = [];
	$scope.tags = [ "Analysis", "Import", "Planning", "Tools", "Fitbit", "Jawbone" ];
	
	// get apps and visualizations
	var stati = ENV.beta ? [ "ACTIVE", "BETA" ] : "ACTIVE";
	var properties = {"spotlighted": true, "targetUserRole" : [ $scope.targetRole, "ANY"], "status" : stati };
	var fields = ["name", "type", "description", "tags"];
	var data = {"properties": properties, "fields": fields};
	server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data)).
		success(function(apps) { 
			$scope.visualizations.spotlighted = apps;			
		}).
		error(function(err) { $scope.error = "Failed to load apps: " + err; });
	/*server.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
		success(function(visualizations) { $scope.visualizations.spotlighted = visualizations; }).
		error(function(err) { $scope.error = "Failed to load visualizations: " + err; });
	*/
	// show app details
	$scope.showAppDetails = function(app) {		
		$state.go("^.app", { appId : app._id.$oid, context : $state.params.context, next : $state.params.next });
	};
	
	$scope.getVisualizationImage = function(app) {
	  return "/images/icons/"+app.type.toLowerCase()+".png";
	};
	
	/*$scope.getVisualizationImage = function(visulization) {
		  return "/images/icons/eye.png";
	};*/
	
	// show visualization details
	$scope.showVisualizationDetails = function(visualization) {
		$state.go("^.visualization", { visualizationId : visualization._id.$oid, context : $state.params.context, next : $state.params.next });		
	};
	
	$scope.setTag = function(tag) {
		$scope.tag = tag;
	};
	
	$scope.hasTag = function(visualization) {
		if ($scope.tag == null) return true;
		return visualization.tags.indexOf($scope.tag) >= 0;
	};
	
}]);