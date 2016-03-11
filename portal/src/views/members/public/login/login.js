angular.module('portal')
.controller('LoginCtrl', ['$scope', 'server', '$state', 'status', 'session', function($scope, server, $state, status, session) {
	
	// init
	$scope.login = {};	
	$scope.error = null;
	$scope.status = new status(false);
	
	$scope.offline = (window.jsRoutes == undefined) || (window.jsRoutes.controllers == undefined);
	
	// login
	$scope.login = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = "Please provide an email address and a password.";
			return;
		}
		
		// send the request
		var data = {"email": $scope.login.email, "password": $scope.login.password};
		$scope.status.doAction("login", server.post(jsRoutes.controllers.Application.authenticate().url, JSON.stringify(data))).
		then(function(result) {
			session.postLogin(result, $state);
		}).
		catch(function(err) { $scope.error = err.data; });
	};
}]);
