angular.module('portal')
.controller('StudyOverviewCtrl', ['$scope', '$state', 'server', 'status', 'usergroups', 'apps', function($scope, $state, server, status, usergroups, apps) {
	
	$scope.studyid = $state.params.studyId;
	$scope.study = {};
	$scope.status = new status(true);
	$scope.auditlog = {};
		
	var loadUserNames = function() {
		var data = {"properties": {"_id": [$scope.study.createdBy]}, "fields": ["firstname", "lastname"]};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) {
					if ($scope.study.createdBy === user._id) { $scope.study.creatorName = (user.firstname+" "+user.lastname).trim(); }					
				});
			});
	};
	
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
		.then(function(data) { 				
				$scope.study = data.data;
				loadUserNames();

				$scope.tests = {};
				
				$scope.status.doBusy(usergroups.listUserGroupMembers($scope.studyid))
				.then(function(data) {
					console.log("X");
					console.log(data);
					$scope.tests.team = data.data.length > 1;
				}).then(function() {
				
				apps.getApps( { "linkedStudy" : $scope.studyid }, [ "filename", "name" ])
				.then(function(data) {
					$scope.tests.applinked = data.data.length > 0;
				}).then(function() {
				
				server.post(jsRoutes.controllers.research.Studies.listParticipants($scope.studyid).url, JSON.stringify({ properties : { pstatus : "REQUEST" } }))
						.then(function(data) {
							$scope.tests.allassigned = data.data.length === 0;
						}).then(function()  {
					
					
				$scope.checklist = [
					{ title : "study_checklist.phase1", heading : true  },
					{ title : "study_checklist.name", required : true, done : $scope.study.name && $scope.study.description },
					{ title : "study_checklist.teamsetup", done : $scope.tests.team },
                    { title : "study_checklist.groups", required : true, done : $scope.study.groups.length },
                    { title : "study_checklist.sharingQuery", required : true, done : ($scope.study.recordQuery && ($scope.study.recordQuery.content || $scope.study.recordQuery.group)) },
                    { title : "study_checklist.dates", required : true, done : $scope.study.startDate || $scope.study.endDate || $scope.study.dataCreatedBefore },
                    { title : "study_checklist.terms", done : $scope.study.termsOfUse },
                    { title : "study_checklist.validation", required : true, done : $scope.study.validationStatus !== "DRAFT" },
                    { title : "study_checklist.validation_passed", required : true, done : $scope.study.validationStatus == "VALIDATED" },
					{ title : "study_checklist.phase2", heading : true },
					{ title : "study_checklist.applications" },
					{ title : "study_checklist.applinked", done : $scope.tests.applinked },
					{ title : "study_checklist.partsearchstart", required : true, done : $scope.study.participantSearchStatus != "PRE" },
					{ title : "study_checklist.phase3", heading : true },
					{ title : "study_checklist.executionstart", required : true, done : $scope.study.executionStatus != "PRE"  },
					{ title : "study_checklist.acceptedpart", required : true, done : $scope.study.participantSearchStatus != "PRE" && $scope.tests.allassigned },
					{ title : "study_checklist.phase4", heading : true },
					{ title : "study_checklist.partsearchend", required : true, done : $scope.study.participantSearchStatus == "CLOSED" },
					{ title : "study_checklist.execend", required : true, done : $scope.study.executionStatus == "FINISHED"  },
					{ title : "study_checklist.exportdata" }
				];
				
				});
				});
				});
				
		});
	};
	
	$scope.readyForValidation = function() {
		return $scope.study.myRole && $scope.study.myRole.setup && ($scope.study.validationStatus == "DRAFT" || $scope.study.validationStatus == "REJECTED");
	};
	
	$scope.readyForParticipantSearch = function() {
		return $scope.study.myRole && $scope.study.myRole.setup && $scope.study.validationStatus == "VALIDATED" &&
		       $scope.study.executionStatus == "PRE" &&
		       (
				 $scope.study.participantSearchStatus == "PRE" ||
				 $scope.study.participantSearchStatus == "CLOSED"
			   );
	};
	
	$scope.readyForEndParticipantSearch = function() {
		return $scope.study.myRole && $scope.study.myRole.setup && $scope.study.validationStatus == "VALIDATED" && $scope.study.participantSearchStatus == "SEARCHING";
	};
	
	$scope.readyForDelete = function() {
		return $scope.study.myRole && $scope.study.myRole.setup && ($scope.study.executionStatus == "PRE" || $scope.study.executionStatus == "ABORTED");
	};
	
	$scope.readyForAbort = function() {
		return $scope.study.myRole && $scope.study.myRole.setup && ($scope.study.validationStatus == "VALIDATED" && $scope.study.executionStatus != "ABORTED" && $scope.study.participantSearchStatus != "SEARCHING");
	};
	
	$scope.readyForStartExecution = function() {
		return $scope.study.validationStatus == "VALIDATED" && 
		       $scope.study.participantSearchStatus != "PRE" &&
		       $scope.study.executionStatus == "PRE";
	};
	
	$scope.readyForFinishExecution = function() {
		return $scope.study.myRole && $scope.study.myRole.setup && $scope.study.validationStatus == "VALIDATED" && 
		       $scope.study.participantSearchStatus == "CLOSED" &&
		       $scope.study.executionStatus == "RUNNING";
	};
	
	$scope.startValidation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.startValidation($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		    $scope.auditlog.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.startParticipantSearch = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.startParticipantSearch($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		    $scope.auditlog.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.endParticipantSearch = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.endParticipantSearch($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		    $scope.auditlog.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.startExecution = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.startExecution($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		    $scope.auditlog.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.finishExecution = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.finishExecution($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		    $scope.auditlog.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.abortExecution = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.abortExecution($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		    $scope.auditlog.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.delete = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.delete($scope.studyid).url).
		success(function(data) { 				
		    $state.go("research.studies");
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.reload();
	
}]);