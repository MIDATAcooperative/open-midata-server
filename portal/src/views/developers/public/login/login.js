angular.module('portal')
.controller('DeveloperLoginCtrl', ['$scope', 'server', '$state', 'status', function($scope, server, $state, status) {
	
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
		$scope.status.doAction("login", server.post(jsRoutes.controllers.Developers.login().url, JSON.stringify(data))).
		then(function(result) {
			if (result.data.status) {
				  $state.go("public.postregister", { progress : result.data }, { location : false });			
			} else if (result.data.role == "admin") {
				if (result.data.keyType == 1) {
					$state.go('public_developer.passphrase_admin');
				} else {
					$state.go('admin.members');	
				}
			} else if (result.data.keyType == 1) {
				$state.go('public_developer.passphrase');
			} else {
   			    $state.go('developer.yourapps');	
			} 
		}).
		catch(function(err) { $scope.error = err.data; });
	};
}]);
