angular.module('portal')
.controller('StudyTeamCtrl', ['$scope', '$state', 'server', 'status', 'usergroups', 'studies', 'session', 'users', function($scope, $state, server, status, usergroups, studies, session, users) {
	
	$scope.studyId = $state.params.studyId;
	$scope.status = new status(false, $scope);
	
	$scope.authpersons = [];
	$scope.datePickers = {  };
	$scope.dateOptions = {
	  	 formatYear: 'yy',
	  	 startingDay: 1
	};
	$scope.members = [];
    $scope.roles = studies.roles;
    $scope.rights = ["setup", "readData", "writeData", "unpseudo", "export", "changeTeam", "participants", "auditLog" ];
    $scope.add = { role:{} };
	
	$scope.init = function() {
		
		session.currentUser.then(function(userId) {			
			$scope.user = session.user;		
		});
		
		
		$scope.groupId = $scope.studyId;
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
		.then(function(data) { 				
				$scope.study = data.data;			
		});
						
		$scope.status.doBusy(usergroups.listUserGroupMembers($scope.studyId))
		.then(function(data) {
			$scope.members = data.data;
		});
		
		$scope.status.doBusy(users.getMembers({ role : "RESEARCH", organization : session.org }, users.MINIMAL ))
		.then(function(data) {
			$scope.persons = data.data;
		});
			
			/*
			$scope.status.doBusy(usergroups.search({ "_id" : $scope.studyId }, ["name", "status" ]))
			.then(function(data) {				
				$scope.usergroup = $scope.myform = data.data[0];								                						
			});
			*/
		
				
	};
			
	$scope.removePerson = function(person) {
		
		$scope.status.doAction("delete", server.post(jsRoutes.controllers.UserGroups.deleteUserGroupMembership().url, JSON.stringify({ member : person.member, group : $scope.groupId })))
	    .then(function() {				
			$scope.members.splice($scope.members.indexOf(person), 1);
		});
		
	};
	
	$scope.updateRole = function() {
		console.log($scope.add);
		var role =$scope.add.roleTemplate;
		$scope.add.role.roleName = role.roleName;
		$scope.add.role.id = role.id;
		for (var i in $scope.rights) {
			$scope.add.role[$scope.rights[i]] = role[$scope.rights[i]];
		}
	};
	
	$scope.addPerson = function() {			
        $scope.error = null;				
		$scope.status.doAction("add", users.getMembers({ email : $scope.add.personemail, role : "RESEARCH" },["email", "role"]))
		.then(function(result) {
			if (result.data && result.data.length) {
				$scope.add.person = result.data[0];
			
			
				$scope.status.doAction("add", usergroups.addMembersToUserGroup($scope.groupId, [ $scope.add.person._id ], $scope.add.role)).
				then(function() {
					$scope.add = {};
					$scope.init();
				});
			} else {
				$scope.error = { code : "error.unknown.user" };
			}
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
		
	session.currentUser.then(function(userId) {	 
	  $scope.init();
	});
}]);
	

