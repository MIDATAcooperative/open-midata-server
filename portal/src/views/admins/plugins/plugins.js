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
.controller('PluginsCtrl', ['$scope', '$state', 'views', 'session', 'apps', 'users', 'status', function($scope, $state, views, session, apps, users, status) {

	$scope.status = new status(true);
	$scope.pluginStati = ["DEVELOPMENT", "BETA", "ACTIVE", "DEPRECATED"];
	$scope.search = { criteria : {} };
	$scope.page = { nr : 1 };
	
	$scope.init = function(userId) {		
		$scope.reload();
		$scope.status.doBusy(users.getMembers({ role : "DEVELOPER" }, [ "firstname", "lastname", "email" ]))
		.then(function(data) {
			$scope.developers = data.data;
			$scope.developers.push({});
		});
	};
	
	$scope.reload = function() {
	   if ($scope.search.criteria.creatorLogin === "") $scope.search.criteria.creatorLogin = undefined;
	   $scope.status.doBusy(apps.getApps( $scope.search.criteria, [ "creator", "creatorLogin", "developerTeam", "filename", "version", "name", "description", "tags", "targetUserRole", "spotlighted", "type", "status", "orgName", "publisher"]))
	   .then(function(data) { $scope.apps = data.data; });
	};
	
	$scope.changePlugin = function(plugin) {				
		apps.updatePluginStatus(plugin)
	    .then(function() { $scope.init(); });				
	};	
	
	$scope.emptyIsUndefined = function(x) {
	  return x === "" ? undefined : x;
	};
	
	$scope.filterByName = function(item) {
        return !$scope.search.name || (item && item.name.toLowerCase().indexOf($scope.search.name.toLowerCase())>=0 || item.filename.toLowerCase().indexOf($scope.search.name.toLowerCase())>=0);
    };
	
	session.load("PluginsCtrl", $scope, ["search", "page"]);
	if ($state.params.creator) $scope.search.criteria.creatorLogin = $state.params.creator;
	session.currentUser.then(function(userId) { $scope.init(userId); });
}]);