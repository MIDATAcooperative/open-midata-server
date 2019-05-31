angular.module('portal')
.controller('LostPasswordCtrl', ['$scope', '$state', 'server', '$window', 'status', function($scope, $state, server, $window, status) {
	
	// init
	$scope.lostpw = {};
	$scope.error = null;
	$scope.status = new status(false, $scope);
		
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
		$scope.status.doAction("pw",server.post(jsRoutes.controllers.Application.requestPasswordResetToken().url, JSON.stringify(data))).
			then(function() { 
				$scope.lostpw.success = true; 
			}, function(err) { $scope.error = err.data; });
	};
	
	$scope.back = function() {
		$window.history.back();
	};
			
}]);

