angular.module('portal')
.controller('MemberAddressCtrl', ['$scope', '$state', '$stateParams', 'views', 'status', 'users', 'administration', function($scope, $state, $stateParams, views, status, users, administration) {

	$scope.status = new status(true);
    $scope.criteria = { _id : $stateParams.userId };		
	$scope.stati = [ "NEW", "ACTIVE", "BLOCKED", "DELETED" ];
	$scope.contractStati = [ "NEW", "REQUESTED", "PRINTED", "SIGNED" ];
    
	$scope.reload = function() {
		
		$scope.status.doBusy(users.getMembers($scope.criteria, [ "midataID", "firstname", "lastname", "email", "role", "subroles", "status", "address1", "address2", "city", "confirmationCode", "agbStatus", "contractStatus", "emailStatus", "country", "email", "gender", "phone", "zip", "registeredAt", "login", "confirmedAt", "history", "developer" ]))
		.then(function(data) {
			$scope.member = data.data[0];
			if ($scope.member.role == "DEVELOPER") {
				$scope.status.doBusy(users.getMembers({ _id : $stateParams.userId, role : "DEVELOPER" }, [ "midataID", "firstname", "lastname", "email", "role", "subroles", "status", "address1", "address2", "city", "confirmationCode", "agbStatus", "contractStatus", "emailStatus", "country", "email", "gender", "phone", "zip", "registeredAt", "login", "confirmedAt", "history", "coach", "reason" ]))
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
		administration.changeStatus(user._id, user.status, user.contractStatus, user.agbStatus).then(function() { $scope.reload(); });
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
		administration.wipe($scope.member._id)
		.then(function() {
			$state.go("^.members");
		});
	};
	
	$scope.reload();

}]);