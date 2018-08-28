angular.module('portal')
.controller('AdminStudyCtrl', ['$scope', '$state', 'server', 'status', 'users', 'usergroups', '$translatePartialLoader', function($scope, $state, server, status, users, usergroups, $translatePartialLoader) {
	
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
			$state.go("admin.studies");
		}, function(err) {
			$scope.error = err.data;			
		});
	};
	
	$scope.backToDraft = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.research.Studies.backToDraft($scope.studyid).url).
		then(function(data) { 				
			$state.go("admin.studies");
		}, function(err) {
			$scope.error = err.data;			
		});
	};
	
		
	$scope.delete = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.admin.Administration.deleteStudy($scope.studyid).url).
		then(function(data) { 				
		    $state.go("admin.studies");
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
	
	$scope.reload();
	
}]);