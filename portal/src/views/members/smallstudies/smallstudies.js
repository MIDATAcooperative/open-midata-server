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

angular.module('views')
.controller('SmallStudiesCtrl', ['$scope', '$state', 'server', 'views', 'studies', 'status', '$translate', function($scope, $state, server, views, studies, status, $translate) {
	
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
		    
		    $scope.status.doBusy(studies.search({ participantSearchStatus : "SEARCHING" }, ["name", "type", "infos", "description", "participantSearchStatus", "executionStatus", "createdAt", "joinMethods"])).
			then(function (result) {
				angular.forEach(result.data, function(study) {
					var part = studyById[study._id];
					if (!part) {
						part = study;
						part.pstatus = study.joinMethods.indexOf('PORTAL') >= 0 ? "MATCH" : "INFO";
						part.studyName = part.name;
						part.study = study._id;
						$scope.results.push(part);
					} else {
						part.description = study.description;
						part.infos = study.infos;
						part.type = study.type;
						part.participantSearchStatus = study.participantSearchStatus;
						part.executionStatus = study.executionStatus;
						part.createdAt = study.createdAt;
					}
				});				 
			});
		    
		    $scope.status.doBusy(studies.search({ _id : ids  }, ["name", "description", "participantSearchStatus", "executionStatus", "type", "infos", "joinMethods"])).
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
	
	$scope.getSummary = function(study) {
		if (study.infos) {
			for (var i=0;i<study.infos.length;i++) {
				if (study.infos[i].type === "SUMMARY") {
					return study.infos[i].value[$translate.use()] || study.infos[i].value.int || study.description;
				}
			}
		}
		return study.description;
	};
	
	$scope.setTab(0);
	
	$scope.reload();	
	
}]);