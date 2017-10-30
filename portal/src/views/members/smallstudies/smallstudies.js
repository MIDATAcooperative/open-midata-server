angular.module('views')
.controller('SmallStudiesCtrl', ['$scope', '$state', 'server', 'views', 'studies', 'status', function($scope, $state, server, views, studies, status) {
	
	$scope.studies = [];	
	//$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);	
	$scope.greeting.text = "smallstudies.greeting";
	$scope.criteria = { };
	$scope.tab = 0;
	
	var studyById = {};
	
	var tabs = [
		function(study) {
			return study.participantSearchStatus == "SEARCHING";
		},
		function(study) {
			return study.pstatus == "ACCEPTED" && study.executionStatus == "RUNNING";
		},
		function(study) {
			return study.pstatus == "ACCEPTED" && study.executionStatus == "FINISHED";
		},
		function(study) {
			return study.pstatus == "MEMBER_REJECTED" || study.pstatus == "RESEARCH_REJECTED" || study.executionStatus == "ABORTED" || study.pstatus == "MEMBER_RETREATED";
		}		
	];
	
	
	
	$scope.reload = function() {		
		
										
		$scope.status.doBusy(server.get(jsRoutes.controllers.members.Studies.list().url)).
		then(function(results) { 				
		    $scope.results = results.data;
		    var ids = [];
		    angular.forEach(results.data, function(study) {
		    	studyById[study.study] = study;
		    	ids.push(study.study);
		    });
		    
		    $scope.status.doBusy(studies.search({ participantSearchStatus : "SEARCHING" }, ["name", "description", "participantSearchStatus", "executionStatus"])).
			then(function (result) {
				angular.forEach(result.data, function(study) {
					var part = studyById[study._id];
					if (!part) {
						part = study;
						part.pstatus = "MATCH";
						part.studyName = part.name;
						part.study = study._id;
						$scope.results.push(part);
					} else {
						part.description = study.description;
						part.participantSearchStatus = study.participantSearchStatus;
						part.executionStatus = study.executionStatus;
						
					}
				});				 
			});
		    
		    $scope.status.doBusy(studies.search({ _id : ids  }, ["name", "description", "participantSearchStatus", "executionStatus"])).
			then(function (result) {
				angular.forEach(result.data, function(study) {
					var part = studyById[study._id];
					if (!part) {
						part = study;
						part.pstatus = "MATCH";
						part.studyName = part.name;
						part.study = study._id;
						$scope.results.push(part);
					} else {
						part.description = study.description;
						part.participantSearchStatus = study.participantSearchStatus;
						part.executionStatus = study.executionStatus;
						
					}
				});				 
			});
		});
	};
	
	$scope.setTab = function(tabnr) {
		$scope.tab = tabnr;
		$scope.selection = tabs[tabnr];
		console.log($scope.selection);
	};
	
	$scope.showDetails = function(study) {
		$state.go('^.studydetails', { studyId : study._id });		
	};
	
	$scope.setTab(0);
	
	$scope.reload();	
	
}]);