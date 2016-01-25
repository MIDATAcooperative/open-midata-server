angular.module('portal')
.controller('ResearchLoginCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
	// init
	$scope.login = {};
	$scope.error = null;
	
	// login
	$scope.dologin = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.error = "Please provide an email address and a password.";
			return;
		}
		
		// send the request
		var data = {"email": $scope.login.email, "password": $scope.login.password};
		server.post(jsRoutes.controllers.research.Researchers.login().url, JSON.stringify(data))
			.then(function(result) {
				if (result.data.status) {
					  $state.go("public.postregister", { progress : result.data }, { location : false });			
				} else if (result.data.keyType == 1) {
					  $state.go('public_research.passphrase');
					} else {
					  $state.go('research.studies');
					}
				}).catch(function(err) { $scope.error = err.data; });
	};
	
}]);