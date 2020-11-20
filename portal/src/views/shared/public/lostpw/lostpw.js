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
.controller('LostPasswordCtrl', ['$scope', '$state', 'server', '$window', 'status', function($scope, $state, server, $window, status) {
	
	// init
	$scope.lostpw = {};
	$scope.error = null;
	$scope.status = new status(false, $scope);
		
	// submit
	$scope.submit = function() {
		// check user input
		if (!$scope.lostpw.email) {
			$scope.error = { code : "error.missing.email" };
			return;
		}
		
		$scope.error = null;
		
		// send the request
		var data = { "email": $scope.lostpw.email, "role" : $state.current.data.role };
		$scope.status.doAction("pw",server.post(jsRoutes.controllers.Application.requestPasswordResetToken().url, JSON.stringify(data))).
			then(function() { 
				$scope.lostpw.success = true; 
			}, function(err) { $scope.error = err.data; });
	};
	
	$scope.back = function() {
		$window.history.back();
	};
			
}]);

