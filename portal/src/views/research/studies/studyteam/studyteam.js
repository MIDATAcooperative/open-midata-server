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
    $scope.rights = ["readData", "writeData", "unpseudo", "export", "changeTeam", "auditLog", "participants" ];
    $scope.add = { roles:{} };
	
	$scope.init = function() {
		
		session.currentUser.then(function(userId) {			
			$scope.user = session.user;		
		});
		
		
		$scope.groupId = $scope.studyId;
						
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
	
	$scope.addPerson = function() {			
								
		$scope.status.doAction("add", usergroups.addMembersToUserGroup($scope.groupId, [ $scope.add.person._id ], $scope.add.role)).
		then(function() {
			$scope.add = {};
			$scope.init();
		});				
	};
		
	session.currentUser.then(function(userId) {	 
	  $scope.init();
	});
}]);
	
