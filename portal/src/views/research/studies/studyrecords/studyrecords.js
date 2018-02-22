angular.module('portal')
.controller('StudyRecordsCtrl', ['$scope', '$state', 'server', 'ENV', 'session', 'records', 'status', function($scope, $state, server, ENV, session, records, status) {
	
	$scope.studyId = $state.params.studyId;
	$scope.status = new status(true);
	
	$scope.reload = function() {
		
		session.currentUser.then(function(userId) {
		
			$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
			.then(function(data) { 				
				$scope.study = data.data;
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
		$scope.status.doAction("download", server.token())
		.then(function(response) {
		  document.location.href = ENV.apiurl + jsRoutes.controllers.research.Studies.downloadFHIR($scope.studyId, what.group, mode).url + "?token=" + encodeURIComponent(response.data.token);
		});
	};
	
	$scope.reload();
	
}]);