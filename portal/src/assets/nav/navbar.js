angular.module('portal')
.controller('NavbarCtrl', ['$scope', '$state', 'server', 'currentUser', function($scope, $state, server, currentUser) {
	
	// init
	$scope.user = {};
	console.log("INIT NAV");
	
	// get current user
	currentUser.then(function(userId) {
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
		.then(function() { $state.go('public.login'); });
	};
	
	$scope.search = function(value) {
		return server.get(jsRoutes.controllers.GlobalSearch.complete(value).url)
		.then(function(response) { return response.data; });
	};
	
	// initialize global search with typeahead plugin
	/*
	$("#globalSearch").typeahead({"name": "data", remote: {
		"url": null,
		"prepare" : function(query, settings) {
			console.log(settings);
			settings.xhrFields = { withCredentials : true };
			return settings;
		},		
		"replace": function(url, query) {
			return apiurl + jsRoutes.controllers.GlobalSearch.complete(query).url;
		}
	}}).
	on("typeahead:selected", function(event, datum) {
		if (datum.type !== "other") {
			window.location.href = "/" + datum.type + "s/" + datum.id;
		}
	});*/
			
	// start a search
	$scope.startSearch = function() {
		
		$state.go('^.search', { query : $scope.query });		
	};
	
}]);
