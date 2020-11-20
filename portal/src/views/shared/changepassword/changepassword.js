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
.controller('ChangePasswordCtrl', ['$scope', '$state', 'status', 'server', 'crypto', 'session', function($scope, $state, status, server, crypto, session) {
	// init
	$scope.error = null;
	$scope.status = new status(false, $scope);
	$scope.pw = { oldPassword:"", password:"", password2:"" };
	
    $scope.changePassword = function() {		
		
    	var pwvalid = crypto.isValidPassword($scope.pw.password); 
        $scope.myform.password.$setValidity('tooshort', pwvalid);
        if (!pwvalid) {
        	$scope.myform.password.$invalid = true;
        	$scope.myform.password.$error = { 'tooshort' : true };
        }
    	
        $scope.myform.password.$setValidity('compare', $scope.pw.password ==  $scope.pw.password2);
		
		$scope.submitted = true;
		$scope.success = false;
		
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
				
		var data = $scope.pw;
		
		$scope.status.doAction("changePassword", crypto.generateKeys($scope.pw.password).then(function(keys) {
			
			if ($scope.pw.secure) {
				var data = { oldPassword : $scope.pw.oldPassword, oldPasswordHash : crypto.getHash($scope.pw.oldPassword) };
				data.password = keys.pw_hash;
				data.pub = keys.pub;
				data.priv_pw = keys.priv_pw;
				data.recovery = keys.recovery;
				data.recoverKey = keys.recoverKey;
			} else {
				var data = { oldPassword : $scope.pw.oldPassword, oldPasswordHash : crypto.getHash($scope.pw.oldPassword) };
				data.password = $scope.pw.password;
			}
			return server.post(jsRoutes.controllers.PWRecovery.changePassword().url, JSON.stringify(data));			
		})).then(function() { $scope.success = true;session.login(); });
						 
	};
	
	session.currentUser.then(function() {
		$scope.pw.secure = session.user.security == "KEY_EXT_PASSWORD";
	});
	$scope.status.isBusy = false;
	
}]);