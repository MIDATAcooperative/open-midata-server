angular.module('portal')
.controller('AppsCtrl', ['$scope', '$state', 'circles', 'session', 'views', 'status', 'ENV', 'server', 'spaces','apps', function($scope, $state, circles, session, views, status, ENV, server, spaces, apps) {

	$scope.status = new status(true);
	$scope.targetRole = $state.current.data.role;
	$scope.pluginToSpace = {};
	$scope.greeting.text = "apps.greeting";
		
	loadConsents = function(userId) {	
		$scope.status.doBusy(circles.listConsents({type : "EXTERNALSERVICE" }, [ "name", "authorized", "type", "status"]))
		.then(function(data) {
			$scope.apps = data.data;						
		});
	};
		
	
	$scope.editConsent = function(consent) {
		$state.go("^.editconsent", { consentId : consent._id });
	};
		
	
	session.currentUser.then(function(userId) {
		$scope.userId = userId;
		
		loadConsents(userId);
		
		var stati = ENV.beta ? [ "ACTIVE", "BETA" ] : "ACTIVE";
		var properties = {"spotlighted": true, "targetUserRole" : [ $scope.targetRole, "ANY"], "status" : stati, "tags" : ["Import"] };
		var fields = ["name", "type", "description", "tags"];
		var data = {"properties": properties, "fields": fields};
		
		$scope.status.doBusy(server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data))).
		then(function(apps) { 
			$scope.services = apps.data;
			
			if (session.user.developer) {
				properties = { "type" : ["visualization", "oauth1", "oauth2"], "creator" : session.user.developer, "status" : ["ACTIVE", "BETA", "DEVELOPMENT"]  };
				data = { "properties": properties, "fields": fields};
				
				$scope.status.doBusy(server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data))).
				then(function(apps) {
					$scope.services = $scope.services.concat(apps.data); 
				});
			}
		});
		
		spaces.getSpacesOfUserContext($scope.userId, "config")
    	.then(function(results) {
    		for (var i=0;i<results.data.length;i++) {
    			var space = results.data[i];
    			$scope.pluginToSpace[space.visualization] = space;
    		}
    	});
	});
	
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
					//session.login();
					if (result.data && result.data._id) {
					  if (app.type === "oauth1" || app.type === "oauth2") {
						 $state.go("^.importrecords", { "spaceId" : result.data._id });
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