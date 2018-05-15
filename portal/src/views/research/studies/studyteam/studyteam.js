angular.module('portal')
.controller('StudyTeamCtrl', ['$scope', '$state', 'server', 'status', 'usergroups', 'studies', 'session', 'users', '$document', function($scope, $state, server, status, usergroups, studies, session, users, $document) {
	
	$scope.studyId = $state.params.studyId;
	$scope.status = new status(false, $scope);
	
	$scope.authpersons = [];
	$scope.datePickers = {  };
	$scope.dateOptions = {
	  	 formatYear: 'yy',
	  	 startingDay: 1
	};
	$scope.members = [];
	$scope.form = {};
    $scope.roles = studies.roles;
    $scope.rights = ["setup", "readData", "writeData", "unpseudo", "export", "changeTeam", "participants", "auditLog" ];
    $scope.add = { role:{} };
    $scope.sortby="user.lastname";   
	
	$scope.init = function() {
		
		session.currentUser.then(function(userId) {			
			$scope.user = session.user;		
		});
		
		
		$scope.groupId = $scope.studyId;
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
		.then(function(data) { 				
				$scope.study = data.data;	
				$scope.lockChanges = !$scope.study.myRole.changeTeam;
		});
						
		$scope.status.doBusy(usergroups.listUserGroupMembers($scope.studyId))
		.then(function(data) {
			$scope.members = data.data;
			angular.forEach($scope.members, function(member) { member.role.unpseudo = !member.role.pseudo; });
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
	
	$scope.formChange = function() {
		$scope.saveOk = false;
	};
	
	$scope.addPerson = function() {		
		$scope.myform = $scope.form.myform;
		$scope.submitted = $scope.form.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
		
       			
		$scope.status.doAction("add", users.getMembers({ email : $scope.add.personemail, role : "RESEARCH" },["email", "role"]))
		.then(function(result) {
			if (result.data && result.data.length) {
				$scope.add.person = result.data[0];
			
			
				$scope.status.doAction("add", usergroups.addMembersToUserGroup($scope.groupId, [ $scope.add.person._id ], $scope.add.role)).
				then(function() {
					$scope.add = { role:{} };
					$scope.init();
					$scope.submitted = $scope.form.submitted = false;				
					$scope.saveOk = true;
					$scope.myform.$setPristine();
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
	
	$scope.select = function(member) {
	   $scope.add = { personemail : member.user.email, role : JSON.parse(JSON.stringify(member.role))  };
	   angular.forEach($scope.roles, function(r) { if (r.id == member.role.id) $scope.add.roleTemplate = r; });	   
	};
	
	$scope.setSort = function(key) {
		console.log(key);
		if ($scope.sortby==key) $scope.sortby = "-"+key;
		else { $scope.sortby = key; }
	};
		
	session.currentUser.then(function(userId) {	 
	  $scope.init();
	});
}]);
	

