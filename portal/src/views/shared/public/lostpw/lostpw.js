angular.module('portal')
.controller('LostPasswordCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
	// init
	$scope.lostpw = {};
	$scope.error = null;
		
	// submit
	$scope.submit = function() {
		// check user input
		if (!$scope.lostpw.email) {
			$scope.error = { code : "error.missing.email" };
			return;
		}
		
		$scope.error = null;
		
		// send the request
		var data = { "email": $scope.lostpw.email, "role" : $state.current.data.role };
		server.post(jsRoutes.controllers.Application.requestPasswordResetToken().url, JSON.stringify(data)).
			then(function() { $scope.lostpw.success = true; }, function(err) { $scope.error = err; });
	};
			
}]);

