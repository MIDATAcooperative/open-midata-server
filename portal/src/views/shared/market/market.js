angular.module('portal')
.controller('MarketCtrl', ['$scope', '$translate', 'server', '$state', 'ENV', '$window', 'status', 'apps', 'session', 'spaces', function($scope, $translate, server, $state, ENV, $window, status, apps, session, spaces) {
	
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
	$scope.status = new status(true);
	if ($state.params && $state.params.tag) $scope.tag = $state.params.tag;
	
	// get apps and visualizations
	var stati = ENV.beta ? [ "ACTIVE", "BETA" ] : "ACTIVE";
	var properties = {"spotlighted": true, "targetUserRole" : [ $scope.targetRole, "ANY"], "status" : stati };
	var fields = ["name", "type", "description", "tags"];
	var data = {"properties": properties, "fields": fields};
	
	session.currentUser
	.then(function(userId) {
			$scope.userId = userId;
				
			$scope.status.doBusy(server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data))).
			then(function(apps) { 
				$scope.visualizations.spotlighted = apps.data;			
			});
			
	});

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
	
	$scope.install = function(app) {
		
		
	  spaces.get({ "owner": $scope.userId, "visualization" : app._id }, ["_id", "type"])
	  .then(function(spaceresult) {
		 if (spaceresult.data.length > 0) {
			 var target = spaceresult.data[0];
			 if (target.type === "oauth1" || target.type === "oauth2") {
				 $state.go("^.importrecords", { "spaceId" : target._id, params : $state.params.params });
			 } else { 
			     $state.go("^.spaces", { spaceId : target._id, params : $state.params.params });
			 }
		 } else {	  				
			$scope.status.doAction("install", apps.installPlugin(app._id, { applyRules : true }))
			.then(function(result) {				
				session.login();
				if (result.data && result.data._id) {
				  if (app.type === "oauth1" || app.type === "oauth2") {
					 $state.go("^.importrecords", { "spaceId" : result.data._id, params : JSON.stringify(data.params) });
				  } else { 
				     $state.go('^.spaces', { spaceId : result.data._id });
				  }
				} else {
				  $state.go('^.dashboard', { dashId : $scope.options.context });
				}
			});
		 }
	  });
	};
	
}]);