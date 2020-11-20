/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.controller('MarketCtrl', ['$scope', '$translate', 'server', '$state', 'ENV', '$window', 'status', 'apps', 'session', 'spaces', function($scope, $translate, server, $state, ENV, $window, status, apps, session, spaces) {
		
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
			
			if ($scope.tag == "developer") {
				console.log(session);
				
				$scope.tag = undefined;
			}
				
			$scope.status.doBusy(server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data))).
			then(function(apps) { 
				$scope.visualizations.spotlighted = apps.data;	
				
				if (session.user.developer) {
					properties = { "type" : ["visualization", "oauth1", "oauth2"], "developerTeam" : session.user.developer, "status" : ["ACTIVE", "BETA", "DEVELOPMENT"]  };
					data = { "properties": properties, "fields": fields};
					
					$scope.status.doBusy(server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data))).
					then(function(apps) {
						$scope.visualizations.spotlighted = $scope.visualizations.spotlighted.concat(apps.data); 
					});
				}
				
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
		$state.go("^.visualization", { visualizationId : visualization._id, context : $state.params.context, next : $state.params.next, study : $state.params.study, user : $state.params.user });		
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
	  if (app.type == "external" || app.termsOfUse || app.type == "service") {
		$state.go("^.visualization", { visualizationId : app._id, context : $state.params.context, next : $state.params.next, study : $state.params.study, user : $state.params.user }); 
		return;
	  }
		
	  spaces.get({ "owner": $scope.userId, "visualization" : app._id, "context" : $state.params.context }, ["_id", "type"])
	  .then(function(spaceresult) {
		 if (spaceresult.data.length > 0) {
			 var target = spaceresult.data[0];
			 if (target.type === "oauth1" || target.type === "oauth2") {
				 $state.go("^.importrecords", { "spaceId" : target._id, params : $state.params.params });
			 } else { 
			     $state.go("^.spaces", { spaceId : target._id, params : $state.params.params, user : $state.params.user });
			 }
		 } else {	  				
			$scope.status.doAction("install", apps.installPlugin(app._id, { applyRules : true, context : $state.params.context, study : $state.params.study }))
			.then(function(result) {				
				session.login();
				console.log(result);
				if (result.data && result.data._id) {
					console.log("NAV");
				  if (app.type === "oauth1" || app.type === "oauth2") {
					 $state.go("^.importrecords", { "spaceId" : result.data._id, params : JSON.stringify(data.params) });
				  } else { 
				     $state.go('^.spaces', { spaceId : result.data._id, user : $state.params.user, study : $state.params.study });
				  }
				} else {
				  if (app.type === "external") {
					$state.go('^.apps');
				  } else {					
					$state.go('^.timeline'); //, { dashId : $state.params.context || "me" });
				  }
				}
			});
		 }
	  });
	};
	
}]);