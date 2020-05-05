angular.module('portal')
.controller('FailureCtrl', ['$scope', '$state', '$stateParams', 'status', 'views', 'ENV', function($scope, $state, $stateParams, status, views, ENV) {
		
	$scope.status = new status(false, $scope);
	$scope.status.isBusy = false;	
	$scope.view = views.getView("terms");
	$scope.ENV = ENV;
    $scope.reason = $state.params.reason;	
		
    $scope.showLogin = function() {		
		$state.go("^.oauth2", $state.params);
	};
}]);

