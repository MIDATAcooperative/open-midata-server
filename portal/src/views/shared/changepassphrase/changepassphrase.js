angular.module('portal')
.controller('ChangePassphraseCtrl', ['$scope', '$state', 'status', 'server', function($scope, $state, status, server) {
	// init
	$scope.error = null;
	$scope.status = new status(false, $scope);
	$scope.pw = { oldPassphrase:"", passphrase:"", passphrase2:"" };
	$scope.phase = 0;
	
    $scope.preparePassphrase = function() {				        				
		$scope.success = false;
		
		$scope.myform.passphrase.$setValidity('compare', true);
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.phase = 1;		
	};
	
    $scope.changePassphrase = function() {		
		
        $scope.myform.passphrase.$setValidity('compare', $scope.pw.passphrase ==  $scope.pw.passphrase2);
		
		$scope.submitted = true;
		$scope.success = false;
		
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) {
			$scope.phase = 0;
			$scope.pw.passphrase2 = "";
			return;
		}
				
		var data = $scope.pw;
		
		$scope.status.doAction("changePassphrase", server.post(jsRoutes.controllers.Application.changePassphrase().url, JSON.stringify(data))).
		then(function() { $scope.success = true; $scope.phase = 2; }, function() { $scope.phase = 0; $scope.pw.passphrase2 = "";  $scope.pw.oldPassphrase = ""; });
	};
	
	$scope.status.isBusy = false;
	
}]);