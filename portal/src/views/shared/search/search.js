angular.module('portal')
.controller('SearchCtrl', ['$scope', '$state', 'server', '$sce', function($scope, $state, server, $sce) {
	
	// init
	$scope.error = null;
	$scope.loading = false;
	$scope.results = {};
	$scope.types = [];
	$scope.active = null;
	
	$scope.query = $state.params.query;
	
	// start search
	$scope.loading = true;
	server.get(jsRoutes.controllers.GlobalSearch.search($scope.query).url).
		success(function(results) {
			$scope.error = null;
			$scope.results = results;
			$scope.types = Object.keys($scope.results);
			if ($scope.types.length > 0) {
				$scope.makeActive($scope.types[0]);
			}
			$scope.loading = false;
		}).
		error(function(err) {
			$scope.error = "Search failed: " + err;
			$scope.loading = false;
		});
	
	// show results of one type
	$scope.makeActive = function(type) {
		$scope.active = type;
	};
	
	// capitalize a word
	$scope.capitalize = function(string) {
		return string.charAt(0).toUpperCase() + string.slice(1);
	};
	
	// display as html
	$scope.toHtml = function(html) {
		return $sce.trustAsHtml(html);
	};
	
	$scope.goTo = function(type, id) {		
		switch (type) {
		case "user" : $state.go('^.user', { userId : id });break;
		case "app" : $state.go('^.app', { appId : id });break;
		case "visualization" : $state.go('^.visualization', { visualizationId : id });break;
		case "record" : $state.go('^.recorddetail', { recordId : id });break;
		case "space" : $state.go('^.space', { spaceId : id });break;
		case "circle" : $state.go('^.circles', { circleId : id });break;
		}
	};
	
}]);