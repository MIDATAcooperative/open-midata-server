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
.controller('ChangeEmailCtrl', ['$scope', '$state', 'status', 'server', 'session', 'users', function($scope, $state, status, server, session, users) {
	// init
	$scope.error = null;
	$scope.status = new status(false, $scope);
	$scope.pw = { email:"", email2:"" };
	
    $scope.changeEmail = function() {		
		
        //$scope.myform.email.$setValidity('compare', $scope.pw.email ==  $scope.pw.email2);
		
		$scope.submitted = true;
		$scope.success = false;
		
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
				
		var data = { user : $scope.userId , email : $scope.pw.email };
		
		$scope.status.doAction("changeEmail", server.post(jsRoutes.controllers.admin.Administration.changeUserEmail().url, JSON.stringify(data))).
		then(function() { $scope.success = true; });
	};
	
	session.currentUser.then(function(userId) {
		$scope.userId = $state.params.userId || userId;
		$scope.status.doBusy(users.getMembers({ _id : $scope.userId }, [ "midataID", "firstname", "lastname", "email", "role" ]))
		.then(function(data) {
			$scope.member = data.data[0];
		});
	});
	
	
}]);