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
.controller('OrganizationCtrl', ['$scope', '$state', 'server', 'status', 'session', 'users', function($scope, $state, server, status, session, users) {
		
	$scope.error = null;
	$scope.submitted = false;
	$scope.success = false;
	$scope.status = new status(true, $scope);
	$scope.sortby="lastname";   
		
	$scope.reload = function() {
		
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Researchers.getOrganization(session.org).url))
		.then(function(data) { 				
		  $scope.org = data.data;												
		});
		
		$scope.status.doBusy(users.getMembers({ role : "RESEARCH", organization : session.org }, users.MINIMAL ))
		.then(function(data) {
			$scope.persons = data.data;
		});
		
		
	};
	
	$scope.editorg = function() {
	
		$scope.submitted = true;
		$scope.success = false;
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
						
		
	   $scope.status.doAction("update", server.post(jsRoutes.controllers.research.Researchers.updateOrganization(session.org).url, JSON.stringify($scope.org)))
	   .then(function(result) { $scope.saveOk = true; });
		
	
	};
	
	$scope.isMasterUser = function() {
		return session.hasSubRole('MASTER');
	};
	
	$scope.formChange = function() {
		$scope.saveOk = false;
	};
	
	$scope.setSort = function(key) {
		console.log(key);
		if ($scope.sortby==key) $scope.sortby = "-"+key;
		else { $scope.sortby = key; }
	};
	
	session.currentUser.then(function() { $scope.reload(); });
	
	
}]);