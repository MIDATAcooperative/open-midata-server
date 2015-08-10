angular.module('portal')
.controller('MemberSearchCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
	$scope.criteria = {};
	$scope.member = null;
	$scope.error = null;
	$scope.loading = false;
	
	$scope.dosearch = function() {
		$scope.loading = true;
		
		server.post(jsRoutes.controllers.providers.Providers.search().url, $scope.criteria).
		success(function(data) { 				
		    $scope.member = data;
		    $scope.error = null;
		    $scope.loading = false;
		    
		    $state.go('^.memberdetails', { memberId : $scope.member._id.$oid });		    
		}).
		error(function(err) {
			$scope.error = err;	
			$scope.results = null;
			$scope.loading = false;
		});
	};
	
}]);