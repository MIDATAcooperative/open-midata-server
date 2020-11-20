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
		
		session.currentUser.then(function(userId) {			
			$scope.user = session.user;		
		});
		
		if ($state.params.groupId) {
			$scope.groupId = $state.params.groupId;
						
			$scope.status.doBusy(usergroups.listUserGroupMembers($state.params.groupId))
			.then(function(data) {
				$scope.members = data.data;
			});
			
			$scope.status.doBusy(usergroups.search({ "_id" : $state.params.groupId }, ["name", "status", "searchable" ]))
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
	
	$scope.edit = function() {
		$scope.saveOk = false;
		usergroups.editUserGroup($scope.usergroup).then(function() { $scope.saveOk = true; });
	};
	
	$scope.removePerson = function(person) {
		
		server.post(jsRoutes.controllers.UserGroups.deleteUserGroupMembership().url, JSON.stringify({ member : person.member, group : $scope.groupId }))
	    .then(function() {				
			$scope.members.splice($scope.members.indexOf(person), 1);
		});
		
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
		
		views.setView("providersearch", { callback : addPerson });	
			
	};
		
	
	
	session.currentUser.then(function(userId) {	 
	  $scope.init();
	});
}]);
	
