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
.controller('ListParticipantsCtrl', ['$scope', '$state', 'server', 'status', 'session', 'paginationService', function($scope, $state, server, status, session, paginationService) {
	
	$scope.studyid = $state.params.studyId;
	$scope.results =[];
    $scope.status = new status(false, $scope);    
    $scope.acceptall = {};
    $scope.searches = [ 
  	  { 
  		name : "studyparticipants.all",
  		criteria : {  }
  	  },
  	  {
  		name : "studyparticipants.request",
  		criteria : { pstatus : "REQUEST"  }
  	  },	 
  	  {
  		name : "studyparticipants.rejected",
  		criteria : { pstatus : ["MEMBER_REJECTED", "RESEARCH_REJECTED"] }
  	  },
  	  {
  		name : "studyparticipants.retreated",
  		criteria : { pstatus : "MEMBER_RETREATED" }
  	  },  	  
  	];
    $scope.page = { nr : 1 };
	$scope.search = $scope.searches[0];
    
	$scope.reload = function(searchName, comeback) {
			
		if (searchName) $scope.search = session.map($scope.searches, "name")[searchName];
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
		.then(function(data) { 				
			$scope.study = data.data;
			if ($scope.study.autoJoinGroup) {
				$scope.acceptall = { autoJoinGroup : $scope.study.autoJoinGroup, autoJoin : true };
			}
		});
		
		$scope.status.doBusy(server.post(jsRoutes.controllers.research.Studies.countParticipants($scope.studyid).url, JSON.stringify({ properties : $scope.search.criteria })))
		.then(function(cdata) {
		  $scope.total = cdata.data.total;
		$scope.status.doBusy(server.post(jsRoutes.controllers.research.Studies.listParticipants($scope.studyid).url, JSON.stringify({ properties : $scope.search.criteria })))
		.then(function(data) { 	
			if (!comeback) paginationService.setCurrentPage("membertable", 1);
			$scope.results = data.data;		
			console.log($scope.results);
		});
		
		});
	};
	
	
	$scope.mayApproveParticipation = function(participation) {
	   return $scope.study && $scope.study.myRole.participants && participation.pstatus == "REQUEST";
	
	};
	
	$scope.mayAddParticipants = function() {
		   return $scope.study && $scope.study.myRole.participants && $scope.study.participantSearchStatus == "SEARCHING";
		
	};
	
	$scope.mayViewParticipants = function() {
		   return $scope.study && $scope.study.myRole.participants && !$scope.study.myRole.pseudo;		
	};
	
    $scope.mayRejectParticipation = function(participation) {
      return $scope.study && $scope.study.myRole.participants && participation.pstatus == "REQUEST";
	};
	
	
	$scope.rejectParticipation = function(participation) {
		$scope.error = null;
		var params = { member : participation._id };
		
		$scope.status.doAction("reject", server.post(jsRoutes.controllers.research.Studies.rejectParticipation($scope.studyid).url, params))
		.then(function(data) { 				
		    $scope.reload();
		});
	};
	
	$scope.approveParticipation = function(participation) {
		$scope.error = null;
	
		var params = { member : participation._id };
		
		$scope.status.doAction("approve", server.post(jsRoutes.controllers.research.Studies.approveParticipation($scope.studyid).url, params))
		.then(function(data) { 				
		    $scope.reload();
		});
	};
	
	$scope.changeGroup = function(participation) {
		var params = { member : participation._id, group : participation.group };
		$scope.status.doAction("change", server.post(jsRoutes.controllers.research.Studies.updateParticipation($scope.studyid).url, JSON.stringify(params)))
		.then(function(data) { 				
		    //$scope.reload();
		});
	};
	
	$scope.acceptAll = function() {		
		$scope.status.doAction("change", server.post(jsRoutes.controllers.research.Studies.updateNonSetup($scope.studyid).url, JSON.stringify($scope.acceptall)))
		.then(function(data) { 				
		   $scope.reload();
		   $scope.saveOk = true;
		});
	};
	
	session.load("ListParticipantsCtrl", $scope, ["search", "page"]);
	$scope.searchName = $scope.search.name;
	$scope.reload(undefined, true);
	
}]);