angular.module('portal')
.controller('LostPasswordCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.lostpw = {};
	$scope.error = null;
		
	// submit
	$scope.submit = function() {
		// check user input
		if (!$scope.lostpw.email) {
			$scope.error = "Please provide your email address.";
			return;
		}
		
		$scope.error = null;
		
		// send the request
		var data = { "email": $scope.lostpw.email, "role" : $scope.role };
		$http.post(jsRoutes.controllers.Application.requestPasswordResetToken().url, JSON.stringify(data)).
			success(function() { $scope.lostpw.success = true; }).
			error(function(err) { $scope.error = err; });
	};
			
}]);

