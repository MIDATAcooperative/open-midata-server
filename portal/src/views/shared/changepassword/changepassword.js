angular.module('portal')
.controller('ChangePasswordCtrl', ['$scope', '$state', 'status', 'server', 'crypto', function($scope, $state, status, server, crypto) {
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
		
		$scope.status.doAction("changePassword", crypto.generateKeys($scope.pw.password).then(function(keys) {
			var data = { oldPassword : $scope.pw.oldPassword, oldPasswordHash : crypto.getHash($scope.pw.oldPassword) };
			data.password = keys.pw_hash;
			data.pub = keys.pub;
			data.priv_pw = keys.priv_pw;
			data.recovery = keys.recovery;
			data.recoverKey = keys.recoverKey;
			return server.post(jsRoutes.controllers.PWRecovery.changePassword().url, JSON.stringify(data));
		})).then(function() { $scope.success = true; });
						 
	};
	
	$scope.status.isBusy = false;
	
}]);