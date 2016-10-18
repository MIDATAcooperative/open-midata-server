angular.module('portal')
.controller('MemberAddressCtrl', ['$scope', '$state', '$stateParams', 'views', 'status', 'users', 'administration', function($scope, $state, $stateParams, views, status, users, administration) {

	$scope.status = new status(true);
    $scope.criteria = { _id : $stateParams.userId };		
	$scope.stati = [ "NEW", "ACTIVE", "BLOCKED", "DELETED" ];
	$scope.contractStati = [ "NEW", "REQUESTED", "PRINTED", "SIGNED" ];
    
	$scope.reload = function() {
		
		$scope.status.doBusy(users.getMembers($scope.criteria, [ "midataID", "firstname", "lastname", "email", "role", "subroles", "status", "address1", "address2", "city", "confirmationCode", "agbStatus", "contractStatus", "emailStatus", "country", "email", "gender", "phone", "zip", "registeredAt", "login", "confirmedAt", "history" ]))
		.then(function(data) {
			$scope.member = data.data[0];						
		});
	};
	
	$scope.changeUser = function(user) {
		console.log(user);
		administration.changeStatus(user._id, user.status, user.contractStatus, user.agbStatus);
	};	
	
	$scope.confirmEmail = function(user) {
		administration.changeStatus(user._id, user.status, null, null, "VALIDATED")
		.then(function() {
		  user.emailStatus = "VALIDATED";
		});
	};
	
	$scope.reload();

}]);