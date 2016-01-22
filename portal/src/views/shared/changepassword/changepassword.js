angular.module('portal')
.controller('ChangePasswordCtrl', ['$scope', '$state', 'status', 'server', function($scope, $state, status, server) {
	// init
	$scope.error = null;
	$scope.status = new status(false, $scope);
	$scope.pw = { oldPassword:"", password:"", password2:"" };
	
    $scope.changePassword = function() {		
		
        $scope.myform.password.$setValidity('compare', $scope.pw.password ==  $scope.pw.password2);
		
		$scope.submitted = true;
		$scope.success = false;
		
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
				
		var data = $scope.pw;
		
		$scope.status.doAction("changePassword", server.post(jsRoutes.controllers.Application.changePassword().url, JSON.stringify(data))).
		then(function() { $scope.success = true; });
	};
	
	$scope.status.isBusy = false;
	
}]);