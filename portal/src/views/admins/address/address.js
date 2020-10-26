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
.controller('MemberAddressCtrl', ['$scope', '$state', '$stateParams', 'views', 'status', 'users', 'administration', function($scope, $state, $stateParams, views, status, users, administration) {

	$scope.status = new status(true);
    $scope.criteria = { _id : $stateParams.userId };		
	$scope.stati = [ "NEW", "ACTIVE", "BLOCKED", "DELETED" ];
	$scope.contractStati = [ "NEW", "REQUESTED", "PRINTED", "SIGNED" ];
	$scope.authTypes = ["NONE", "SMS"]
    
	$scope.reload = function() {
		
		$scope.status.doBusy(users.getMembers($scope.criteria, [ "midataID", "firstname", "lastname", "email", "role", "subroles", "status", "address1", "address2", "city", "confirmationCode", "agbStatus", "contractStatus", "emailStatus", "mobileStatus", "country", "email", "gender", "phone", "zip", "registeredAt", "login", "confirmedAt", "developer", "security", "authType", "marketingEmail" ]))
		.then(function(data) {
			$scope.member = data.data[0];
			if ($scope.member.role == "DEVELOPER") {
				$scope.status.doBusy(users.getMembers({ _id : $stateParams.userId, role : "DEVELOPER" }, [ "midataID", "firstname", "lastname", "email", "role", "subroles", "status", "address1", "address2", "city", "confirmationCode", "agbStatus", "contractStatus", "emailStatus", "mobileStatus", "country", "email", "gender", "phone", "zip", "registeredAt", "login", "confirmedAt", "coach", "reason", "security", "authType", "marketingEmail" ]))
				.then(function(data2) { $scope.member = data2.data[0]; });
			}
			if ($scope.member.developer) {				
				users.getMembers({ _id : $scope.member.developer }, ["email", "firstname", "lastname"])
				.then(function(data3) {
					$scope.member.developerName = data3.data[0].firstname + " " + data3.data[0].lastname;						
				});
			}
		});
	};
	
	$scope.changeUser = function(user) {		
		administration.changeStatus(user._id, user.status, user.contractStatus, user.agbStatus, undefined, user.authType).then(function() { $scope.reload(); });
	};	
	
	$scope.confirmEmail = function(user) {
		administration.changeStatus(user._id, user.status, null, null, "VALIDATED")
		.then(function() {
		  user.emailStatus = "VALIDATED";
		});
	};
	
	$scope.addComment = function() {
		administration.addComment($scope.member._id, $scope.comment)
		.then(function() {
			$scope.comment = "";
			$scope.member._id = null;
		    $scope.reload();
		});		
	};
	
	$scope.wipe = function() {
		$scope.status.doAction("wipe", administration.wipe($scope.member._id))
		.then(function() {
			$state.go("^.members");
		});
	};
	
	$scope.addSubRole = function(subrole) {
		if (!$scope.member.subroles) $scope.member.subroles = [];
		if (!$scope.member.subroles.indexOf(subrole)>=0) $scope.member.subroles.push(subrole);
		administration.changeStatus($scope.member._id, $scope.member.status, null, null, null, null, $scope.member.subroles);
	};
	
	$scope.removeSubRole = function(subrole) {
		if (!$scope.member.subroles) return;
		if ($scope.member.subroles.indexOf(subrole)>=0) $scope.member.subroles.splice($scope.member.subroles.indexOf(subrole), 1);
		administration.changeStatus($scope.member._id, $scope.member.status, null, null, null, null, $scope.member.subroles);
	};
	
	$scope.reload();

}]);