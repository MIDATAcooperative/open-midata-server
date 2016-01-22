angular.module('portal')
.controller('LoginCtrl', ['$scope', 'server', '$state', 'status', function($scope, server, $state, status) {
	
	// init
	$scope.login = {};	
	$scope.error = null;
	$scope.status = new status(false);
	
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
			if (result.data.status) {
			  $state.go("public.postregister", { progress : result.data }, { location : false });	
			} else if (result.data.keyType == 1) {
			  $state.go('public.passphrase');
			} else {
			  $state.go('member.overview');
			}
		}).
		catch(function(err) { $scope.error = err.data; });
	};
}]);
