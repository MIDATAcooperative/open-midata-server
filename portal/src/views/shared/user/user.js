angular.module('portal')
.controller('UserCtrl', ['$scope', '$state', 'users', 'status', 'session', function($scope, $state, users, status, session) {
	// init
	$scope.status = new status(true);
	$scope.user = {};
	
	// parse user id (format: /users/:id) and load the user details
	var userId = $state.params.userId;	
	
	$scope.status.doBusy(users.getMembers({"_id": {"$oid": userId}}, ["name", "email"]))
	.then(function(results) {
		$scope.user = results.data[0];
	});
	
	session.currentUser.then(function(myUserId) { 
		$scope.isSelf = myUserId.$oid == userId;
		console.log(myUserId);
	});
	
}]);