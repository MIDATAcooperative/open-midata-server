angular.module('portal')
.controller('SetPasswordCtrl', ['$scope', 'server', '$location', 'crypto', function($scope, server, $location, crypto) {
	
	// init
	$scope.setpw = {
			token : $location.search().token,
			password : "",
			passwordRepeat : ""
	};
	$scope.error = null;
    $scope.secure = $location.search().ns != 1;		
	// submit
	$scope.submit = function() {
		$scope.error = null;
		
		// check user input
		if (!$scope.setpw.password) {
			$scope.error = { code : "error.missing.newpassword" };
			return;
		}
		if (!$scope.setpw.passwordRepeat || $scope.setpw.passwordRepeat !== $scope.setpw.password) {
			$scope.error = { code : "error.invalid.password_repetition" };
			return;
		}
		$scope.submitted = true;
		
		crypto.generateKeys($scope.setpw.password).then(function(keys) {
			var data = { "token": $scope.setpw.token };
			
			if ($scope.secure) {
				data.password = keys.pw_hash;
				data.pub = keys.pub;
				data.priv_pw = keys.priv_pw;
				data.recovery = keys.recovery;
				data.recoveryKey = keys.recoveryKey;		
			} else {
				data.password = $scope.setpw.password;
			}
			return server.post(jsRoutes.controllers.Application.setPasswordWithToken().url, JSON.stringify(data));
		}).then(function() { $scope.setpw.success = true;$scope.submitted=false; }, function(err) { $scope.error = err.data;$scope.submitted=false; });
	};
			
}]);