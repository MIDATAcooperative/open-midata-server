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
.controller('FailureCtrl', ['$scope', '$state', '$stateParams', 'status', 'views', 'ENV', function($scope, $state, $stateParams, status, views, ENV) {
		
	$scope.status = new status(false, $scope);
	$scope.status.isBusy = false;	
	$scope.view = views.getView("terms");
	$scope.ENV = ENV;
    $scope.reason = $state.params.reason;	
		
    $scope.showLogin = function() {		
		$state.go("^.oauth2", $state.params);
	};
}]);

