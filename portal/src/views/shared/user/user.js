angular.module('portal')
.controller('UserCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	// init
	$scope.error = null;
	$scope.user = {};
	
	// parse user id (format: /users/:id) and load the user details
	var userId = $state.params.userId;
	var data = {"properties": {"_id": {"$oid": userId}}, "fields": ["name", "email"]};
	server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
		success(function(users) { $scope.user = users[0]; }).
		error(function(err) { $scope.error = "Failed to load user details: " + err; });
	
}]);