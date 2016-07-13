angular.module('portal')
.controller('MemberAddressCtrl', ['$scope', '$state', '$stateParams', 'views', 'status', 'users', 'administration', function($scope, $state, $stateParams, views, status, users, administration) {

	$scope.status = new status(true);
    $scope.criteria = { _id : { $oid : $stateParams.userId }  };		
	$scope.stati = [ "NEW", "ACTIVE", "BLOCKED", "DELETED" ];
	$scope.contractStati = [ "NEW", "PRINTED", "SIGNED" ];
    
	$scope.reload = function() {
		
		$scope.status.doBusy(users.getMembers($scope.criteria, [ "midataID", "firstname", "lastname", "email", "role", "status", "address1", "address2", "city", "confirmationCode", "contractStatus", "emailStatus", "country", "email", "gender", "phone", "zip", "registeredAt", "login" ]))
		.then(function(data) {
			$scope.member = data.data[0];						
		});
	};
	
	$scope.changeUser = function(user) {
		console.log(user);
		administration.changeStatus(user._id.$oid, user.status, user.contractStatus);
	};	
	
	$scope.reload();

}]);