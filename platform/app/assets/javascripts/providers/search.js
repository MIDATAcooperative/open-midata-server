var search = angular.module('search', []);
search.controller('MemberSearchCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.criteria = {};
	$scope.results = null;
	$scope.error = null;
	$scope.loading = false;
	
	$scope.dosearch = function() {
		$scope.loading = true;
		
		$http.post(jsRoutes.controllers.providers.Providers.search().url, $scope.criteria).
		success(function(data) { 				
		    $scope.results = data;
		    $scope.error = null;
		    $scope.loading = false;
		}).
		error(function(err) {
			$scope.error = err;	
			$scope.results = null;
			$scope.loading = false;
		});
	};
}]);
search.controller('MemberDetailsCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.memberid = window.location.pathname.split("/")[3];
	$scope.member = {};	
	$scope.loading = true;
		
	$scope.reload = function() {
			
		$http.get(jsRoutes.controllers.providers.Providers.getMember($scope.memberid).url).
			success(function(data) { 												
				$scope.member = data;
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
		
	$scope.reload();
	
}]);