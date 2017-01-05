angular.module('portal')
.controller('EditUserGroupCtrl', ['$scope', '$state', 'server', 'status', 'usergroups', 'hc', 'views', 'session', 'users', function($scope, $state, server, status, usergroups, hc, views, session, users) {
	
			
	$scope.status = new status(false, $scope);
	$scope.authpersons = [];
	$scope.datePickers = {  };
	$scope.dateOptions = {
	  	 formatYear: 'yy',
	  	 startingDay: 1
	};
	$scope.members = [];
	views.reset();
	
	$scope.init = function() {
		
		if ($state.params.groupId) {
			$scope.groupId = $state.params.groupId;
			
			$scope.status.doBusy(usergroups.listUserGroupMembers($state.params.groupId))
			.then(function(data) {
				$scope.members = data.data;
			});
			
			$scope.status.doBusy(usergroups.search({ "_id" : $state.params.groupId }, ["name", "status" ]))
			.then(function(data) {				
				$scope.usergroup = $scope.myform = data.data[0];								                						
			});
			
		} else {
			$scope.usergroup = { };
			
		}
				
	};
	
	$scope.create = function() {	
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;		
		if (! $scope.myform.$valid) return;
		
		$scope.status.doAction("create", usergroups.createUserGroup($scope.usergroup)).
		then(function(data) {			
			$state.go("^.editusergroup", { groupId : data.data._id });						 
		});
				
	};
	
	$scope.removePerson = function(person) {
		if ($scope.consentId) {
		server.delete(jsRoutes.controllers.Circles.removeMember($scope.consent._id, person._id).url).
			success(function() {				
				$scope.authpersons.splice($scope.authpersons.indexOf(person), 1);
			}).
			error(function(err) { $scope.error = { code : "error.internal" }; });
		} else {
			$scope.authpersons.splice($scope.authpersons.indexOf(person), 1);			
		}
	};
	
	var addPerson = function(persons) {			
		if (!persons.length) persons = [ persons ];					
		var personIds = [];
		angular.forEach(persons, function(p) { personIds.push(p._id); });
		usergroups.addMembersToUserGroup($scope.groupId, personIds ).
		then(function() {
			$scope.init();
		});				
	};
	
	$scope.confirmPeopleChange = function() {
		$scope.confirmNeeded = false;
	};
	
	$scope.addPeople = function() {
		//if ($scope.consent.type != "CIRCLE") {
		views.setView("providersearch", { callback : addPerson });	
		/*} else {
		  views.setView("addusers", { consent : $scope.consent, callback : addPerson });
		}*/		
	};
	
	/*
	$scope.deleteConsent = function() {
		server.delete(jsRoutes.controllers.Circles["delete"]($scope.consent._id).url).
		then(function() {
			$state.go("^.circles");
		});
	};
	
	$scope.rejectConsent = function() {
		hc.reject($scope.consent._id).then(function() { $scope.init(); });
	};
	
	$scope.confirmConsent = function() {
		hc.confirm($scope.consent._id).then(function() { $scope.init(); });	
	};
	*/
	
	
	session.currentUser.then(function(userId) {
	  /*if (session.user.subroles.indexOf("TRIALUSER") >= 0) {
		  $scope.locked = true;
	  } else $scope.locked = false;*/
	  $scope.init();
	});
}]);
	
