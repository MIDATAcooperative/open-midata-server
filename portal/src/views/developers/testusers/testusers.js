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
.controller('TestUsersCtrl', ['$scope', '$state', 'views', 'session', 'users', 'server', 'status', '$translatePartialLoader', function($scope, $state, views, session, users, server, status, $translatePartialLoader) {

	$scope.status = new status(true);    
	    
	$scope.reload = function(userId) {		
		
		$scope.status.doBusy(users.getMembers({ "developer" : userId }, [ "firstname", "lastname", "email", "role" ]))
		.then(function(data) {
			$scope.members = data.data;						
		});
	};	
	
	$scope.resetPassword = function(member) {
		
		$scope.status.doBusy(server.post(jsRoutes.controllers.Developers.resetTestAccountPassword().url, JSON.stringify({ user : member._id })))
		.then(function(data) {
			document.location.href=data.data;
		});
	};
	
	session.currentUser.then(function(userId) { $scope.userId = userId; $scope.reload(userId); });
	
	$translatePartialLoader.addPart("providers");
	$translatePartialLoader.addPart("researchers");
}]);