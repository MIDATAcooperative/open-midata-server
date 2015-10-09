angular.module('portal')
.controller('MembersListCtrl', ['$scope', '$state', 'views', 'status', 'users', 'administration', function($scope, $state, views, status, users, administration) {

	$scope.status = new status(true);
    $scope.criteria = { role : "MEMBER", status : "NEW" };	
	$scope.roles = [ "MEMBER", "PROVIDER", "RESEARCH", "DEVELOPER", "ADMIN"];
	$scope.stati = [ "NEW", "ACTIVE", "BLOCKED", "DELETED" ];
    
	$scope.reload = function() {
		
		$scope.status.doBusy(users.getMembers($scope.criteria, [ "midataID", "firstname", "lastname", "email", "role", "status" ]))
		.then(function(data) {
			$scope.members = data.data;						
		});
	};
	
	$scope.changeUser = function(user) {
		console.log(user);
		administration.changeStatus(user._id.$oid, user.status);
	};	
	
	$scope.reload();

}]);