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

angular.module('portal')
.controller('YourAppsCtrl', ['$scope', '$state', 'views', 'session', 'apps', 'status', function($scope, $state, views, session, apps, status) {

	$scope.status = new status(true);
	
	$scope.init = function(userId) {		
		  $scope.status.doBusy(apps.getApps({ developerTeam : userId }, [ "creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type"]))
		  .then(function(data) { $scope.apps = data.data; });		  		  
	};
	
	session.currentUser.then(function(userId) { $scope.init(userId); });
}]);