angular.module('portal')
.controller('AppCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	// init
	$scope.error = null;
	$scope.success = false;
	$scope.app = {};
	$scope.visualizations = [];
	
	// parse app id (format: /apps/:id) and load the app
	var appId = $state.params.appId;
	var data = {"properties": {"_id": {"$oid": appId}}, "fields": ["name", "creator", "description", "recommendedVisualizations"]};
	server.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
		success(function(apps) {
			$scope.error = null;
			$scope.app = apps[0];
			isInstalled();
			getCreatorName();
			if ($scope.app.recommendedVisualizations && $scope.app.recommendedVisualizations.length > 0) {
				$scope.loadRecommendations($scope.app.recommendedVisualizations);
			}
		}).
		error(function(err) { $scope.error = "Failed to load app details: " + err; });
	
	$scope.loadRecommendations = function(ids) {
		var data = { "properties": {"_id": ids }, "fields": ["name", "creator", "description"]};
		server.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
			success(function(visualizations) {				
				$scope.visualizations = visualizations;				
			}).
			error(function(err) { $scope.error = "Failed to load visualization details: " + err; });
	};
	
	// show visualization details
	$scope.showVisualizationDetails = function(visualization) {
		$state.go('^.visualization', { visualizationId : visualization._id.$oid });		
	};
	
	isInstalled = function() {
		server.get(jsRoutes.controllers.Apps.isInstalled($scope.app._id.$oid).url).
			success(function(installed) { $scope.app.installed = installed; }).
			error(function(err) { $scope.error = "Failed to check whether this app is installed: " + err; });
	};
	
	getCreatorName = function() {
		var data = {"properties": {"_id": $scope.app.creator}, "fields": ["name"]};
		server.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data)).
			success(function(users) { $scope.app.creator = users[0].name; }).
			error(function(err) { $scope.error = "Failed to load the name of the creator: " + err; });
	};
	
	$scope.install = function() {
		server.put(jsRoutes.controllers.Apps.install($scope.app._id.$oid).url).
			success(function() {
				$scope.app.installed = true;
				$scope.success = true;
				if ($state.params.next) $state.go($state.params.next); 
			}).
			error(function(err) { $scope.error = "Failed to install the app: " + err; });
	};
	
	$scope.uninstall = function() {
		server.delete(jsRoutes.controllers.Apps.uninstall($scope.app._id.$oid).url).
		success(function() {
			$scope.app.installed = false;
			$scope.success = false;
		}).
		error(function(err) { $scope.error = "Failed to uninstall the app: " + err; });
	};
	
}]);