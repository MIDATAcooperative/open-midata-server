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
.controller('AdminListStudiesCtrl', ['$scope', 'server', 'status', function($scope, server, status) {
	
	$scope.results =[];
	$scope.status = new status(true);
	
	$scope.searches = [
		{
			id : "enum.studyvalidationstatus.VALIDATION",
			properties : {
			   "validationStatus" : "VALIDATION"
			}
		},
		{
			id :"enum.participantsearchstatus.SEARCHING",
			properties : {
				"participantSearchStatus" : "SEARCHING"
			}
		},
		{
			id : "enum.studyexecutionstatus.RUNNING",
			properties : {
				"executionStatus" : "RUNNING"
			}
		},
		{
			id : "enum.studyvalidationstatus.DRAFT",
			properties : {
				"validationStatus" : "DRAFT"
			}
		},
		{
			id : "enum.studyvalidationstatus.REJECTED",
			properties : {
				"validationStatus" : "REJECTED"
			}
		},
		{
			id : "enum.studyvalidationstatus.VALIDATED",
			properties : {
				"validationStatus" : "VALIDATED"
			}
		}
			
	];
	
	
	$scope.selection = { criteria : $scope.searches[0] };
	
	$scope.doreload = function() {
			
		$scope.status.doBusy(server.post(jsRoutes.controllers.research.Studies.listAdmin().url, JSON.stringify($scope.selection.criteria)))
		.then(function(data) { 				
				$scope.results = data.data;	
		});
	};
	
	$scope.doreload();
	
}]);