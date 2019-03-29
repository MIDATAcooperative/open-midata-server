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
	
	$scope.logout = function() {		
		server.post('/api/logout')
		.then(function() { 
			session.logout();
			if ($state.includes("provider") || $state.includes("public_provider")) document.location.href="/#/provider/login";
			else if ($state.includes("research") || $state.includes("public_research")) document.location.href="/#/research/login";
			else if ($state.includes("admin") || $state.includes("developer") || $state.includes("public_developer")) document.location.href="/#/developer/login";
			else document.location.href="/#/public/login"; });
	};
			
	$scope.init();
}]);