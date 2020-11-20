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
.controller('LicensesCtrl', ['$scope', '$state', 'views', 'status', 'server', function($scope, $state, views, status, server) {

	$scope.status = new status(true);
		
	$scope.init = function(userId) {	
		$scope.status.doBusy(server.post(jsRoutes.controllers.Market.searchLicenses().url,JSON.stringify({ properties : {} })))
    	.then(function(results) {
		  $scope.licenses = results.data;
    	});
	};
	
			
	$scope.init();
	
}]);