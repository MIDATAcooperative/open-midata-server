angular.module('portal')
.controller('TestUsersCtrl', ['$scope', '$state', 'views', 'session', 'users', 'server', 'status', '$translatePartialLoader', function($scope, $state, views, session, users, server, status, $translatePartialLoader) {

	$scope.status = new status(true);    
	    
	$scope.reload = function(userId) {		
		
		$scope.status.doBusy(users.getMembers({ "developer" : userId }, [ "firstname", "lastname", "email", "role" ]))
		.then(function(data) {
			$scope.members = data.data;						
		});
	};	
	
	$scope.resetPassword = function(member) {
		
		$scope.status.doBusy(server.post(jsRoutes.controllers.Developers.resetTestAccountPassword().url, JSON.stringify({ user : member._id })))
		.then(function(data) {
			document.location.href=data.data;
		});
	};
	
	session.currentUser.then(function(userId) { $scope.userId = userId; $scope.reload(userId); });
	
	$translatePartialLoader.addPart("providers");
	$translatePartialLoader.addPart("researchers");
}]);