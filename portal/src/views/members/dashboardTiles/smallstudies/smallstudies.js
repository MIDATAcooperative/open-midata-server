angular.module('views')
.controller('SmallStudiesCtrl', ['$scope', '$state', 'server', '$attrs', 'views', 'studies', 'status', function($scope, $state, server, $attrs, views, studies, status) {
	
	$scope.studies = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);	
	
	$scope.criteria = { };
	$scope.tab = 0;
	
	var studyById = {};
	
	var tabs = [
		function(study) {
			return study.participantSearchStatus == "SEARCHING";
		},
		function(study) {
			return study.pstatus == "ACCEPTED";
		},
		function(study) {
			return study.pstatus == "ACCEPTED";
		},
		function(study) {
			return study.pstatus == "MEMBER_REJECTED" || study.pstatus == "RESEARCH_REJECTED";
		}		
	];
	
	
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
										
		$scope.status.doBusy(server.get(jsRoutes.controllers.members.Studies.list().url)).
		then(function(results) { 				
		    $scope.results = results.data;
		    angular.forEach(results.data, function(study) {
		    	studyById[study.study] = study;
		    });
		    
		    $scope.status.doBusy(studies.search({ participantSearchStatus : "SEARCHING" }, ["name", "description", "participantSearchStatus"])).
			then(function (result) {
				angular.forEach(result.data, function(study) {
					var part = studyById[study._id];
					if (!part) {
						part = study;
						part.pstatus = "MATCH";
						part.studyName = part.name;
						$scope.results.push(part);
					} else {
						part.description = study.description;
						part.participantSearchStatus = study.participantSearchStatus;
						
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
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);