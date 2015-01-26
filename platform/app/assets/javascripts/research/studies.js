var studies = angular.module('studies', []);
studies.controller('CreateStudyCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.study = {};
	$scope.submitted = false;
	
	// register new user
	$scope.createstudy = function() {
	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.study.error = null;
		
		// send the request
		var data = $scope.study;		
		
		$http.post(jsRoutes.controllers.research.Studies.create().url, JSON.stringify(data)).
			success(function(url) { window.location.replace(url); }).
			error(function(err) {
				$scope.error = err;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);
				else $scope.study.error = err; 
			});
	}
	
}]);
studies.controller('ListStudiesCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.results =[];
	$scope.loading = true;
	
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.research.Studies.list().url).
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
studies.controller('OverviewCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.studyid = window.location.pathname.split("/")[2];
	$scope.study = null;
	$scope.loading = true;
		
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url).
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