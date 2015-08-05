angular.module('portal')
.controller('RegisterVisualizationCtrl', ['$scope', 'server', function($scope, server) {
	
	// init
	$scope.error = null;
	$scope.visualization = {};
	
	// register visualization
	$scope.registerVisualization = function() {
		if (!$scope.visualization.filename || !$scope.visualization.name || !$scope.visualization.description || !$scope.visualization.url) {
			$scope.error = "Please fill in all required fields";
			return;
		}
		
		// check whether url contains ":authToken"
		if ($scope.visualization.url.indexOf(":authToken") < 0) {
			$scope.error = "Url must contain ':authToken' to receive the authorization token required to create records.";
			return;
		}
		
		// send the request
		var data = {
				"filename": $scope.visualization.filename,
				"name": $scope.visualization.name,
				"description": $scope.visualization.description,
				"url": $scope.visualization.url
		};
		server.post(jsRoutes.controllers.Market.registerVisualization().url, data).
			success(function(redirectUrl) { window.location.replace(portalRoutes.controllers.Market.index().url); }).
			error(function(err) { $scope.error = "Failed to register visualization: " + err; });
	};
	
}]);