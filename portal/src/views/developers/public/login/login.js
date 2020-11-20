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
.controller('DeveloperLoginCtrl', ['$scope', 'server', '$state', 'status', 'session', 'crypto', function($scope, server, $state, status, session, crypto) {
	
	// init
	$scope.login = {};	
	$scope.error = null;
	$scope.status = new status(false);
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	
	// login
	$scope.login = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = { code : "error.missing.credentials" };
			return;
		}
		
		// send the request		
		var data = {"email": $scope.login.email, "password": crypto.getHash($scope.login.password)};
		
		var func = function(data) {
			return $scope.status.doAction("login", server.post(jsRoutes.controllers.Developers.login().url, JSON.stringify(data)));
		};
		
		session.performLogin(func, data, $scope.login.password)
		.then(function(result) {
		   session.postLogin(result, $state);
		}).catch(function(err) { $scope.error = err.data; });
				
	};
}]);
