/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('views')
.controller('ListStudiesCtrl', ['$scope', 'server', '$attrs', 'views', 'status', function($scope, server, $attrs, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.results =[];
	
	$scope.reload = function() {
		if (!$scope.view.active) return;
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.members.Studies.list().url)).
		then(function(results) { 				
		   $scope.results = results.data;			
		});
	};
	
	$scope.reload();
	
}]);
