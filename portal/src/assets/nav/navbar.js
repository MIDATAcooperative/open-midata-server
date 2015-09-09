angular.module('portal')
.controller('NavbarCtrl', ['$scope', '$state', 'server', 'session', function($scope, $state, server, session) {
	
	// init
	$scope.user = {};
	console.log("INIT NAV");
	session.login();
	
	// get current user
	session.currentUser.then(function(userId) {
		console.log("DONE NAV");
		$scope.user._id = userId;
		getName(userId);
	});
	
	// get user's name
	getName = function(userId) {
		var properties = {"_id": userId};
		var fields = ["name", "midataID"];
		var data = {"properties": properties, "fields": fields};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) 
					{ $scope.user.name = users[0].name;
					  $scope.user.midataID = users[0].midataID;
			        });
	};
	
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
	console.log("PUBLIC NAV");
	session.logout();
}]);