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
.controller('AuditLogCtrl', ['$scope', '$state', 'views', 'status', 'fhir', 'administration', 'paginationService', 'session', function($scope, $state, views, status, fhir, administration, paginationService, session) {

	$scope.status = new status(true);    

	$scope.auditlog = {};
	var now = new Date();
	$scope.criteria = { from: new Date(), to : new Date(), days:2 };	
	$scope.page = { nr : 1 };
	
	$scope.datePickers = {};
    $scope.dateOptions = {
       formatYear: 'yy',
       startingDay: 1
    };
	
    $scope.refresh = function() {
    	$scope.auditlog.reload();
    };	
    
    $scope.recalc = function() {
    	$scope.criteria.from = new Date($scope.criteria.to);
    	$scope.criteria.from.setDate($scope.criteria.to.getDate() - $scope.criteria.days);
    };
	
    $scope.recalc();

}]);