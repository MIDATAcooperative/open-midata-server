var search = angular.module('search', []);
search.controller('SearchCtrl', ['$scope', '$http', '$sce', function($scope, $http, $sce) {
	
	// init
	$scope.error = null;
	$scope.loading = false;
	$scope.results = {};
	$scope.types = [];
	$scope.active = null;
	
	// get search query (format: /search/:query)
	$scope.query = decodeURI(window.location.pathname.split("/")[3]);
	
	// start search
	$scope.loading = true;
	$http(jsRoutes.controllers.GlobalSearch.search($scope.query)).
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
	}
	
	// capitalize a word
	$scope.capitalize = function(string) {
		return string.charAt(0).toUpperCase() + string.slice(1);
	}
	
	// display as html
	$scope.toHtml = function(html) {
		return $sce.trustAsHtml(html);
	}
	
}]);