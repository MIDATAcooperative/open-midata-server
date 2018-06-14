angular.module('portal')
.controller('ServiceLeaveCtrl', ['$scope', 'server', '$state', '$window', 'session', function($scope, server, $state, $window, session) {
	
	
	$scope.init = function() {
		console.log("init leave");
		if ($state.params.callback) {
			$scope.callback = $state.params.callback;			
		}
	};
	
	$scope.close = function() {
		server.post('/api/logout')
		.then(function() { 
			session.logout();
		    $window.close();
		});
	};
	
	$scope.leave = function() {
		server.post('/api/logout')
		.then(function() { 
			session.logout();
		    document.location.href = $scope.callback;
		});
	};
			
	$scope.init();
}]);