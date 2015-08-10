angular.module('portal')
.controller('StudyDetailCtrl', ['$scope', '$state', 'server', 'views', function($scope, $state, server, views) {
	
	$scope.studyid = $state.params.studyId;
	$scope.study = {};
	$scope.participation = {};
	$scope.loading = true;
	$scope.error = null;
		
	views.link("1", "record", "record");
	views.link("1", "shareFrom", "share");
	views.link("share", "record", "record");
	
	$scope.reload = function() {
			
		server.get(jsRoutes.controllers.members.Studies.get($scope.studyid).url).
			success(function(data) { 				
				$scope.study = data.study;
				$scope.participation = data.participation;
				$scope.research = data.research;
				$scope.loading = false;
				$scope.error = null;
				
				if ($scope.participation && !($scope.participation.status == "CODE" || $scope.participation.status == "MATCH" )) {
				  views.setView("1", { aps : $scope.participation._id.$oid, properties : { } , type:"participations", allowAdd : true, allowRemove : false, fields : [ "ownerName", "created", "id", "name" ]});
				} else {
				  views.disableView("1");
				}
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
	
	$scope.needs = function(what) {
		return $scope.study.requiredInformation && $scope.study.requiredInformation == what;
	};
	
	$scope.mayRequestParticipation = function() {
		return ($scope.participation != null && ( $scope.participation.status == "MATCH" || $scope.participation.status == "CODE" )) ||
		   ($scope.participation == null && $scope.study.participantSearchStatus == 'SEARCHING');
	};
	
	$scope.mayDeclineParticipation = function() {
		return $scope.participation != null && ( $scope.participation.status == "MATCH" || $scope.participation.status == "CODE" );
	};
	
	$scope.requestParticipation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.members.Studies.requestParticipation($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.noParticipation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.members.Studies.noParticipation($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.reload();
	
}]);