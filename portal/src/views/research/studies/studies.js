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
.controller('ResearchListStudiesCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
	
	$scope.results =[];
	$scope.status = new status(true);
	$scope.sortby="-createdAt";  
	$scope.nav = $state.$current.name.split(".")[0];
	console.log($scope.nav);
	
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.list().url))
		.then(function(data) { 				
				$scope.results = data.data;	
		});
	};
	
	$scope.setSort = function(key) {		
		if ($scope.sortby==key) $scope.sortby = "-"+key;
		else { $scope.sortby = key; }
	};
	
	$scope.reload();
	
}]);