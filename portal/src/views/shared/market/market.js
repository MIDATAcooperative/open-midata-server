angular.module('portal')
.controller('MarketCtrl', ['$scope', '$translate', 'server', '$state', 'ENV', '$window', function($scope, $translate, server, $state, ENV, $window) {
	
	var pathsegment = window.location.pathname.split("/")[1];
	// init
	$scope.error = null;
	$scope.beta = ENV.beta;
	$scope.targetRole = $state.current.data.role; //context == "PROVIDER" ? "PROVIDER" : "MEMBER"; 
	/*$scope.apps = {};
	$scope.apps.spotlighted = [];
	$scope.apps.suggested = [];*/
	$scope.visualizations = {};
	$scope.visualizations.spotlighted = [];
	$scope.visualizations.suggested = [];
	$scope.translate = $translate;
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
		error(function(err) { $scope.error = { "code" : "error.internal" }; });

	// show app details
	$scope.showAppDetails = function(app) {		
		$state.go("^.app", { appId : app._id, context : $state.params.context, next : $state.params.next });
	};
	
	$scope.getVisualizationImage = function(app) {
	  return "/images/icons/"+app.type.toLowerCase()+".png";
	};
	
	/*$scope.getVisualizationImage = function(visulization) {
		  return "/images/icons/eye.png";
	};*/
	
	// show visualization details
	$scope.showVisualizationDetails = function(visualization) {
		$state.go("^.visualization", { visualizationId : visualization._id, context : $state.params.context, next : $state.params.next });		
	};
	
	$scope.setTag = function(tag) {
		$scope.tag = tag;
	};
	
	$scope.hasTag = function(visualization) {
		if ($scope.tag == null) return true;
		return visualization.tags.indexOf($scope.tag) >= 0;
	};
	
	$scope.goBack = function() {
		$window.history.back();
	};
	
}]);