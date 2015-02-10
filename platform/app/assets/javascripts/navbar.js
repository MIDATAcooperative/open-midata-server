var navbar = angular.module('navbar', []);
navbar.controller('NavbarCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.user = {};
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) {
			$scope.user._id = userId;
			getName(userId);
		});
	
	// get user's name
	getName = function(userId) {
		var properties = {"_id": userId};
		var fields = ["name", "midataID"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) 
					{ $scope.user.name = users[0].name;
					  $scope.user.midataID = users[0].midataID;
			        });
	}
	
	// initialize global search with typeahead plugin
	$("#globalSearch").typeahead({"name": "data", remote: {
		"url": null,
		"replace": function(url, query) {
			return jsRoutes.controllers.GlobalSearch.complete(query).url;
		}
	}}).
	on("typeahead:selected", function(event, datum) {
		if (datum.type !== "other") {
			window.location.href = "/" + datum.type + "s/" + datum.id;
		}
	});
	
	// start a search
	$scope.startSearch = function() {
		// need to use jQuery instead of ng-model (typeahead overrides ng-model somehow)
		var query = $("#globalSearch").val();
		window.location.href = jsRoutes.controllers.GlobalSearch.index(query).url;
	}
	
}]);

// manually bootstrap this angular app since only one app can be automatically initialized
$("#navbar").ready(function() {
	angular.bootstrap($("#navbar")[0], ['navbar']);
});
