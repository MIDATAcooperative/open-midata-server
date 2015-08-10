angular.module('portal')
.controller('VisualizationCtrl', ['$scope', 'server', '$state', function($scope, server, $state) {
	// init
	$scope.error = null;
	$scope.success = false;
	$scope.visualization = {};
	$scope.options = {};
	$scope.params = $state.params;
	console.log($scope.params);
	
	// parse visualization id (format: /visualizations/:id) and load the visualization
	var visualizationId = $state.params.visualizationId;
	var data = {"properties": {"_id": {"$oid": visualizationId}}, "fields": ["name", "creator", "description", "defaultSpaceName", "defaultQuery"]};
	server.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
		success(function(visualizations) {
			$scope.error = null;
			$scope.visualization = visualizations[0];
			if ($scope.visualization.defaultSpaceName!=null) {
			  $scope.options.createSpace = true;
			  $scope.options.spaceName = $scope.params.name || $scope.visualization.defaultSpaceName;
			  if ($scope.visualization.defaultQuery) {
				  $scope.options.query = $scope.visualization.defaultQuery;
				  $scope.options.applyRules = true;
			  }
			}
			if ($scope.params && $scope.params.context) {
				$scope.options.context = $scope.params.context;
			} else { 
				$scope.options.context = "mydata"; 
			}
			if ($scope.params && $scope.params.name) {
				$scope.options.spaceName = $scope.params.name;
			}
			if ($scope.params && $scope.params.query) {
				$scope.options.query = JSON.parse(decodeURIComponent($scope.params.query));
				$scope.options.applyRules = true;
			}
			isInstalled();
			getCreatorName();
		}).
		error(function(err) { $scope.error = "Failed to load visualization details: " + err; });
	
	isInstalled = function() {
		server.get(jsRoutes.controllers.Visualizations.isInstalled($scope.visualization._id.$oid).url).
			success(function(installed) { $scope.visualization.installed = installed; }).
			error(function(err) { $scope.error = "Failed to check whether this visualization is installed: " + err; });
	};
	
	getCreatorName = function() {
		var data = {"properties": {"_id": $scope.visualization.creator}, "fields": ["name"]};
		server.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data)).
			success(function(users) { $scope.visualization.creator = users[0].name; }).
			error(function(err) { $scope.error = "Failed to load the name of the creator: " + err; });
	};
	
	$scope.install = function() {
		server.put(jsRoutes.controllers.Visualizations.install($scope.visualization._id.$oid).url, JSON.stringify($scope.options)).
			success(function() {
				$scope.visualization.installed = true;
				$scope.success = true;
				$state.go('^.dashboard', { dashId : $scope.options.context }); 
			}).
			error(function(err) { $scope.error = "Failed to install the visualization: " + err; });
	};
	
	$scope.uninstall = function() {
		server.delete(jsRoutes.controllers.Visualizations.uninstall($scope.visualization._id.$oid).url).
		success(function() {
			$scope.visualization.installed = false;
			$scope.success = false;
		}).
		error(function(err) { $scope.error = "Failed to uninstall the visualization: " + err; });
	};
	
}]);