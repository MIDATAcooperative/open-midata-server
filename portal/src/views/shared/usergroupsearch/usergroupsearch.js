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
.controller('UserGroupSearchResultsCtrl', ['$scope', 'server', 'usergroups', '$state', 'status', 'views', function($scope, server, usergroups, $state, status, views) {
		
    $scope.status = new status(true);
    $scope.criteria = { name : $state.params.name };
    $scope.view = $scope.def ? views.getView($scope.def.id) : views.getView("usergroupsearch");
    
    var dosearch = function(crit) {
    	$scope.status.doBusy(usergroups.search(crit, ["name"]))
    	.then(function(data) {
    		$scope.usergroups = data.data;
    		angular.forEach($scope.usergroups, function(usergroup) {
    			
    			usergroups.listUserGroupMembers(usergroup._id)
    			.then(function(result) {
    				usergroup.members = result.data;
    			});
    			
    		});
    	});
    };
    
    $scope.search = function() {
    	var crit = {};
    	if ($scope.criteria.name !== "") crit.name = $scope.criteria.name;    	
    	dosearch(crit);	    	    	    	
    };
    
    $scope.addIndividuals = function(prov) {
    	var toAdd = [];
    	angular.forEach(prov.members, function(member) { toAdd.push(member.user); });
    	$scope.view.setup.callback(toAdd);
    	views.disableView($scope.view.id);    	
    };
    
    $scope.addGroup = function(group) {
    	$scope.view.setup.callback(group, true);
    	views.disableView($scope.view.id);    	
    };
    
    if ($scope.view.active) $scope.search(); else { $scope.status.isBusy = false; }
    
		
}]);