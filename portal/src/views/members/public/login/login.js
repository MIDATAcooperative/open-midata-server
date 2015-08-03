angular.module('portal')
.controller('LoginCtrl', ['$scope', '$http', 'status', 'apiurl',  function($scope, $http, status, apiurl) {
	
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
		$scope.status.doAction("login", $http.post(apiurl + jsRoutes.controllers.Application.authenticate().url, JSON.stringify(data))).
		then(function() { window.location.replace(portalRoutes.controllers.News.index().url); }).
		catch(function(err) { $scope.error = err.data; });
	};
}]);
