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
.controller('DefineStudyCtrl', ['$scope', '$state', 'status', 'server', function($scope, $state, status, server) {

	$scope.status = new status(false, $scope);
	$scope.error = null;

	$scope.studydef = { value : "", researcher : "" };
	
	$scope.submit = function() {
		
	   $scope.submitted = true;	
	   if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
	   $scope.error = null;
	   $scope.status.doAction("import", server.post(jsRoutes.controllers.research.Studies.importStudy().url, JSON.stringify({ "base64" : $scope.studydef.value, "researcher" : $scope.studydef.researcher })))
	   .then(function(result) {
		  $state.go("^.study.overview", { studyId : result.data._id }); 
	   });	
	};
	
}]);