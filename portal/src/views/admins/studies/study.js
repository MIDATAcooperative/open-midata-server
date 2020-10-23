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
.controller('AdminStudyCtrl', ['$scope', '$state', 'server', 'status', 'users', 'usergroups', '$translatePartialLoader', 'ENV', function($scope, $state, server, status, users, usergroups, $translatePartialLoader, ENV) {
	
	$scope.studyid = $state.params.studyId;
	$scope.study = {};
	$scope.status = new status(true);
	$scope.sortby = "user.lastname";
	
	$translatePartialLoader.addPart("researchers");
		
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.getAdmin($scope.studyid).url))
		.then(function(data) { 				
			$scope.study = data.data.study;
			
			$scope.status.doBusy(users.getMembers({ _id : $scope.study.createdBy, "role" : "RESEARCH" }, users.MINIMAL))
			.then(function(data2) {
				$scope.creator = data2.data[0];
			});
			
			$scope.status.doBusy(usergroups.listUserGroupMembers($scope.studyid))
			.then(function(data) {
				$scope.members = data.data;
				angular.forEach($scope.members, function(member) { member.role.unpseudo = !member.role.pseudo; });
			});
			
			$scope.status.doBusy(server.get(jsRoutes.controllers.Market.getStudyAppLinks("study", $scope.studyid).url))
		    .then(function(data) { 				
				$scope.links = data.data;												
			});	
			
		});
	};
	
	
	
	$scope.finishValidation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.endValidation($scope.studyid).url).
		then(function(data) { 				
			$state.go("admin.astudies");
		}, function(err) {
			$scope.error = err.data;			
		});
	};
	
	$scope.backToDraft = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.backToDraft($scope.studyid).url).
		then(function(data) { 				
			$state.go("admin.astudies");
		}, function(err) {
			$scope.error = err.data;			
		});
	};
	
		
	$scope.delete = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.admin.Administration.deleteStudy($scope.studyid).url).
		then(function(data) { 				
		    $state.go("admin.astudies");
		}, function(err) {
			$scope.error = err.data;			
		});
	};
	
	$scope.matrix = function(role) {
		   var r = "";
		   r += role.setup ? "S" : "-";
		   r += role.readData ? "R" : "-";
		   r += role.writeData ? "W" : "-";
		   r += role.unpseudo ? "U" : "-";
		   r += role["export"] ? "E" : "-";
		   r += role.changeTeam ? "T" : "-";
		   r += role.participants ? "P" : "-";
		   r += role.auditLog ? "L" : "-";	   
		   return r;
		};
	
	$scope.readyForDelete = function() {
		return true;
	};
	
	$scope.setSort = function(key) {
		if ($scope.sortby==key) $scope.sortby = "-"+key;
		else { $scope.sortby = key; }
	};
	
	$scope.remove = function(link) {
		  $scope.status.doAction("delete", server.delete(jsRoutes.controllers.Market.deleteStudyAppLink(link._id).url))
		  .then(function() {
			  $scope.reload();
		  });
	};
	   
	$scope.validate = function(link) {
	     $scope.status.doAction("validate", server.post(jsRoutes.controllers.Market.validateStudyAppLink(link._id).url))
		  .then(function() {
			  $scope.reload();
		  });
	};
	
	$scope.exportStudy = function() {
		$scope.status.doAction("download", server.token())
		.then(function(response) {
		  document.location.href = ENV.apiurl + jsRoutes.controllers.research.Studies.exportStudy($scope.studyid).url + "?token=" + encodeURIComponent(response.data.token);
		});
	};
	
	$scope.reload();
	
}]);