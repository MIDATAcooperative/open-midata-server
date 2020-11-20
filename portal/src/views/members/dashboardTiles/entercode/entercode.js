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

angular.module('views')
.controller('EnterCodeCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
	$scope.code = {};
	$scope.error = null;
	$scope.submitted = false;
	
	// register new user
	$scope.submitcode = function() {
	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.code.error = null;
		
		// send the request
		var data = $scope.code;		
		
		server.post(jsRoutes.controllers.members.Studies.enterCode().url, JSON.stringify(data)).
			then(function(data) { 
				$state.go('member.studydetails', { studyId : data.data.study });				
			}, function(err) {
				$scope.error = err.data;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);				
			});
	};
}]);