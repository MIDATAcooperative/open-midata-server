angular.module('portal')
.controller('ResearchLoginCtrl', ['$scope', 'server', function($scope, server) {
	
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
		server.post(jsRoutes.controllers.research.Researchers.login().url, JSON.stringify(data)).
			success(function(url) { window.location.replace(portalRoutes.controllers.ResearchFrontend.messages().url); }).
			error(function(err) { $scope.error = err; });
	};
	
}]);