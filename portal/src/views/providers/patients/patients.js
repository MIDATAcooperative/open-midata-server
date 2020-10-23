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
.controller('PatientsCtrl', ['$scope', '$state', 'fhir', 'status', 'server', function($scope, $state, fhir, status, server) {
	
	$scope.criteria = {};
	$scope.member = null;
	$scope.error = null;
	$scope.status = new status(true);
	
	$scope.dosearch = function() {
		
		/*$scope.status.doBusy(server.post(jsRoutes.controllers.providers.Providers.list().url, $scope.criteria)).
		then(function(result) { 			
			$scope.patients = result.data;
		});*/
								
		$scope.status.doBusy(fhir.search("Patient", {})).
		then(function(result) { 			
		   $scope.patients = result; 			 		    		  
		});
	};
	
	$scope.selectPatient = function(patient) {
		$state.go('^.memberdetails', { user : patient.id });		
	};
	
    $scope.dosearch();	
	
}]);