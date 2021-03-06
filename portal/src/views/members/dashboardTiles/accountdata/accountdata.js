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
.controller('AccountDataCtrl', ['$scope', 'server', '$attrs', 'users', 'views', 'status', 'session', function($scope, server, $attrs, users, views, status, session) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    
    session.currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.reload = function() { 
    	if (!$scope.view.active || !$scope.userId) return;	
    	
    	$scope.status.doBusy(users.getMembers({ "_id" : $scope.userId }, ["midataID", "firstname", "lastname", "birthday", "address1", "address2", "zip", "city", "country"]))
    	.then(function(results) { $scope.member = results.data[0]; });
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);