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
.controller('CreateStudyCtrl', ['$scope', '$state', 'server', 'status', 'studies', function($scope, $state, server, status, studies) {
	
	$scope.study = {};
	$scope.studyId = $state.params.studyId;
	$scope.error = null;
	$scope.submitted = false;
	$scope.status = new status(true, $scope);
	$scope.studytypes = studies.studytypes;
	$scope.nav = $state.$current.name.split(".")[0];
	
	$scope.reload = function() {
		
		if ($scope.studyId) {
			$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
		    .then(function(data) { 				
				$scope.study = data.data;												
			});			
		}
		
	};
	
	// register new user
	$scope.createstudy = function() {
	
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
						
		// send the request
	    var data;
		
		if ($scope.studyId) {
		
			   data = { name : $scope.study.name, description : $scope.study.description, type : $scope.study.type };
			   $scope.status.doAction("update", server.put(jsRoutes.controllers.research.Studies.update($scope.studyId).url, JSON.stringify(data)))
			   .then(function(result) { $state.go($state.$current.name.split(".")[0]+'.study.overview', { studyId : $scope.studyId }); });
		
		} else {
			data = $scope.study;		
			$scope.status.doAction("submit", server.post(jsRoutes.controllers.research.Studies.create().url, JSON.stringify(data)))
			.then(function(result) { $state.go($state.$current.name.split(".")[0]+'.study.overview', { studyId : result.data._id }); });
			
		}
	};
	
	$scope.reload();
	
}]);