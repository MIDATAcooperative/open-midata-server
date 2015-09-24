angular.module('portal')
.controller('VisualizationCtrl', ['$scope', 'apps', '$state', 'status', 'users', 'session', function($scope, apps, $state, status, users, session) {
	// init
	$scope.error = null;
	$scope.success = false;
	$scope.visualization = {};
	$scope.options = {};
	$scope.params = $state.params;
	$scope.status = new status(true);
	
	
	// parse visualization id (format: /visualizations/:id) and load the visualization
	var visualizationId = $state.params.visualizationId;	
	$scope.status.doBusy(apps.getApps({"_id": {"$oid": visualizationId}}, ["name", "creator", "description", "defaultSpaceName", "defaultQuery"]))
	.then(function(results) {
		   var visualizations = results.data;
			$scope.error = null;
			$scope.visualization = visualizations[0];
			if ($scope.visualization.defaultSpaceName!=null) {
			  $scope.options.createSpace = true;
			  $scope.options.spaceName = $scope.params.name || $scope.visualization.defaultSpaceName;
			  if ($scope.visualization.defaultQuery != null) {
				  $scope.options.query = $scope.visualization.defaultQuery;
				  $scope.options.applyRules = true;
				  console.log($scope.options.query);
			  } else {
				  console.log("XXX");
				  console.log($scope.visualization);
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
		});
	
	isInstalled = function() {
		 apps.isVisualizationInstalled($scope.visualization._id.$oid)
		 .then(function(installed) { $scope.visualization.installed = installed.data; });			
	};
	
	getCreatorName = function() {
		users.getMembers({"_id": $scope.visualization.creator}, ["name"])
		.then(function(users) { if (users.data && users.data[0]) { $scope.visualization.creator = users.data[0].name; } });			
	};
	
	$scope.install = function() {
		$scope.status.doAction("install", apps.installPlugin($scope.visualization._id.$oid, $scope.options))
		.then(function() {
				$scope.visualization.installed = true;
				$scope.success = true;
				session.login();
				$state.go('^.dashboard', { dashId : $scope.options.context }); 
		});			
	};
	
	$scope.uninstall = function() {	  
		$scope.status.doAction("uninstall", apps.uninstallPlugin($scope.visualization._id.$oid))
		.then(function() {
			$scope.visualization.installed = false;
			session.login();
			$scope.success = false;
		});		
	};
	
}]);