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
.controller('GuideCtrl', ['$scope', '$state', 'server', 'ENV', function($scope, $state, server, ENV) {

	$scope.formats = [];
	$scope.groups = [];
	$scope.contents = [];
	$scope.ENV = ENV;
	
	var nameMap = {};
	
	server.get(jsRoutes.controllers.FormatAPI.listFormats().url)
	.then(function(data) { $scope.formats = data.data; });
	
	server.get(jsRoutes.controllers.FormatAPI.listContents().url)
	.then(function(data) { 
		$scope.contents = data.data;		
	});
	
	server.get(jsRoutes.controllers.FormatAPI.listCodes().url)
	.then(function(data) { 
		$scope.codes = data.data;		
	});
	
	server.get(jsRoutes.controllers.FormatAPI.listGroups().url)
	.then(function(data) { 
		$scope.groups = data.data;
		angular.forEach($scope.groups, function(group) { nameMap[group.name] = group.label; });
	});			
	
	$scope.getGroupLabel = function(group) {
		return nameMap[group] || "?";
	};
}]);