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
.controller('StudyRecordsCtrl', ['$scope', '$state', 'server', 'ENV', 'session', 'records', 'status', function($scope, $state, server, ENV, session, records, status) {
	
	$scope.studyId = $state.params.studyId;
	$scope.status = new status(true);
	$scope.datePickers = {  };
	   $scope.dateOptions = {
		  	 formatYear: 'yy',
		  	 startingDay: 1,
		  	  
	 };
	   
	$scope.filter = {};
	
	$scope.reload = function() {
		
		session.currentUser.then(function(userId) {
		
			$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
			.then(function(data) { 				
				$scope.study = data.data;
				$scope.lockChanges = !$scope.study.myRole.export;
				$scope.infos = [];
				angular.forEach($scope.study.groups, function(group) {
					var inf = { group : group.name, count:"-1" };
					$scope.infos.push(inf);
					var properties = { study : $scope.studyId, "study-group" : group.name };
					$scope.status.doBusy(records.getInfos(userId, properties, "ALL"))
					.then(function(results) {						
					    if (results.data && results.data.length == 1) {	inf.count = results.data[0].count; }
					    else if (results.data && results.data.length === 0) { inf.count = 0; }
					});
				});
			});
		});
							
		$scope.downloadUrl = ENV.apiurl + jsRoutes.controllers.research.Studies.download($scope.studyId).url;
	};
	
	$scope.download = function() {
		$scope.status.doAction("download", server.token())
		.then(function(response) {
		  document.location.href = ENV.apiurl + jsRoutes.controllers.research.Studies.download($scope.studyId).url + "?token=" + encodeURIComponent(response.data.token);
		});
	};
	
	$scope.fhirDownload = function(what, mode) {
		$scope.error = null;
		var startDateStr = document.getElementById("startDate").value;		
		if (startDateStr && startDateStr.trim().length > 0 && (!$scope.filter.startDate || !$scope.filter.startDate.getTime || !$scope.filter.startDate.getTime()>0)) {
			$scope.error = "error.invalid.date";
			return;
		}
		$scope.status.doAction("download", server.token())
		.then(function(response) {
		  var urlParams = "";
		  if ($scope.filter.startDate) urlParams += "&startDate="+encodeURIComponent($scope.filter.startDate.getTime());
		  if ($scope.filter.endDate) urlParams += "&endDate="+encodeURIComponent($scope.filter.endDate.getTime());
		  document.location.href = ENV.apiurl + jsRoutes.controllers.research.Studies.downloadFHIR($scope.studyId, what.group, mode).url + "?token=" + encodeURIComponent(response.data.token)+urlParams;
		});
	};
	
	$scope.reload();
	
}]);