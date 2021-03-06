/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
		
		var pwvalid = crypto.isValidPassword($scope.setpw.password);         
        if (!pwvalid) {
        	$scope.error = { code : "error.tooshort.password" };
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