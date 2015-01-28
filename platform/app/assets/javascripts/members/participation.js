var participation = angular.module('participation', []);
participation.controller('EnterCodeCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.code = {};
	$scope.submitted = false;
	
	// register new user
	$scope.submitcode = function() {
	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.code.error = null;
		
		// send the request
		var data = $scope.code;		
		
		$http.post(jsRoutes.controllers.members.Studies.enterCode().url, JSON.stringify(data)).
			success(function(url) { window.location.replace(url); }).
			error(function(err) {
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);
				else $scope.code.error = err; 
			});
	}
}]);
participation.controller('ListStudiesCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.results =[];
	$scope.loading = true;
	
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.members.Studies.list().url).
			success(function(data) { 				
				$scope.results = data;
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
	
	$scope.reload();
	
}]);