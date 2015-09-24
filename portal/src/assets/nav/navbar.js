angular.module('portal')
.controller('NavbarCtrl', ['$scope', '$state', 'server', 'session', function($scope, $state, server, session) {
	
	// init
	$scope.user = {};	
	session.viewHeight = "600px";
	session.login();
	
	// get current user
	session.currentUser.then(function(userId) {
		console.log("DONE NAV");
		$scope.user = session.user;		
	});
			
	$scope.logout = function() {		
		server.get('/logout')
		.then(function() { session.logout(); document.location.href="/#/public/login"; });
	};
	
	$scope.search = function(value) {
		return server.get(jsRoutes.controllers.GlobalSearch.complete(value).url)
		.then(function(response) { return response.data; });
	};
					
	// start a search
	$scope.startSearch = function() {
		
		$state.go('^.search', { query : $scope.query });		
	};
	
	
	
}])
.controller('PublicNavbarCtrl', ['$scope', '$state', 'server', 'session', function($scope, $state, server, session) {	
	session.logout();
}]);