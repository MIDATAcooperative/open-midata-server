angular.module('portal')
.controller('UserCtrl', ['$scope', '$state', 'users', 'status', function($scope, $state, users, status) {
	// init
	$scope.status = new status(true);
	$scope.user = {};
	
	// parse user id (format: /users/:id) and load the user details
	var userId = $state.params.userId;	
	
	$scope.status.doBusy(users.getMembers({"_id": {"$oid": userId}}, ["name", "email"]))
	.then(function(results) {
		$scope.user = results.data[0];
	});
	
}]);