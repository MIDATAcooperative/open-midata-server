angular.module('portal')
.controller('MemberSearchCtrl', ['$scope', '$http', function($scope, $http) {
	
	$scope.criteria = {};
	$scope.member = null;
	$scope.error = null;
	$scope.loading = false;
	
	$scope.dosearch = function() {
		$scope.loading = true;
		
		$http.post(jsRoutes.controllers.providers.Providers.search().url, $scope.criteria).
		success(function(data) { 				
		    $scope.member = data;
		    $scope.error = null;
		    $scope.loading = false;
		    
		    document.location.href = portalRoutes.controllers.ProviderFrontend.member($scope.member._id.$oid).url;
		}).
		error(function(err) {
			$scope.error = err;	
			$scope.results = null;
			$scope.loading = false;
		});
	};
	
}]);