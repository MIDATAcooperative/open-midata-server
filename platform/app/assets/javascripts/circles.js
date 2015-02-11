var circles = angular.module('circles', []);
circles.controller('CirclesCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.loading = true;
	$scope.newCircleName = null;
	$scope.circles = [];
	$scope.contacts = [];
	$scope.userNames = {};
	$scope.foundUsers = [];
	$scope.searching = false;
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) { loadCircles(userId); }).
		error(function(err) {
			$scope.error = "Failed to load current user: " + err;
			$scope.loading = false;
		});
	
	// get circles and make either given or first circle active
	loadCircles = function(userId) {
		var properties = {"owner": userId};
		var fields = ["name", "members", "order"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Circles.get().url, JSON.stringify(data)).
			success(function(circles) {
				$scope.circles = circles;
				loadContacts();
				if ($scope.circles.length > 0) {
					var activeCircle = window.location.pathname.split("/")[3];
					if (activeCircle) {
						$scope.makeActive(_.find($scope.circles, function(circle) { return circle._id.$oid === activeCircle; }));
					} else {
						$scope.makeActive($scope.circles[0]);
					}
				}
			}).
			error(function(err) {
				$scope.error = "Failed to load circles: " + err;
				$scope.loading = false;
			});
	}
	
	// get names for users in circles
	loadContacts = function() {
		var contactIds = _.map($scope.circles, function(circle) { return circle.members; });
		contactIds = _.flatten(contactIds);
		contactIds = _.uniq(contactIds, false, function(contactId) { return contactId.$oid; });
		var properties = {"_id": contactIds};
		var fields = ["name"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(contacts) {
				$scope.contacts = contacts;
				_.each(contacts, function(contact) { $scope.userNames[contact._id.$oid] = contact.name; });
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load contacts: " + err;
				$scope.loading = false;
			});
	}
	
	// make circle tab active
	$scope.makeActive = function(circle) {
		_.each($scope.circles, function(circle) { circle.active = false; });
		circle.active = true;
	}
	
	// add a new circle
	$scope.addCircle = function() {
		$("#circleModal").modal("hide");
		var name = $scope.newCircleName;
		if (!name || name.length === 0) {
			$scope.error = "Please provide a name for the new circle.";
		} else {
			var data = {"name": name};
			$http.post(jsRoutes.controllers.Circles.add().url, JSON.stringify(data)).
				success(function(newCircle) {
					$scope.error = null;
					$scope.newCircleName = null;
					$scope.circles.push(newCircle);
					$scope.makeActive(newCircle);
				}).
				error(function(err) { $scope.error = "Failed to add circle '" + name + "': " + err; });
		}
	}
	
	// delete a circle
	$scope.deleteCircle = function(circle) {
		$http(jsRoutes.controllers.Circles["delete"](circle._id.$oid)).
			success(function() {
				$scope.error = null;
				$scope.circles.splice($scope.circles.indexOf(circle), 1);
				if ($scope.circles.length > 0) {
					$scope.circles[0].active = true;
				}
			}).
			error(function(err) { $scope.error = "Failed to delete circle '" + circle.name + "': " + err; });
	}
	
	// check whether user is not already in active circle
	$scope.isntMember = function(user) {
		var activeCircle = _.find($scope.circles, function(circle) { return circle.active; });
		var memberIds = _.map(activeCircle.members, function(member) { return member.$oid; });
		return !_.contains(memberIds, user._id.$oid);
	}
	
	// search for users
	$scope.searchUsers = function(circle) {
		$scope.searching = true;
		var query = circle.userQuery;
		if (query) {
		$http(jsRoutes.controllers.Users.search(query)).
			success(function(users) {
				$scope.error = null;
				$scope.foundUsers = users;
				$scope.searching = false;
			}).
			error(function(err) {
				$scope.error = "User search failed: " + err;
				$scope.searching = false;
			});
		}
	}
	
	// add a user
	$scope.addUsers = function(circle) {
		// get the users that should be added to the circle
		var contactsToAdd = _.filter($scope.contacts, function(contact) { return contact.checked; });
		var foundUsersToAdd = _.filter($scope.foundUsers, function(user) { return user.checked; });
		var usersToAdd = _.union(contactsToAdd, foundUsersToAdd);
		var userIds = _.map(usersToAdd, function(user) { return user._id; });
		userIds = _.uniq(userIds, false, function(userId) { return userId.$oid; });
		
		var data = {"users": userIds};
		$http.post(jsRoutes.controllers.Circles.addUsers(circle._id.$oid).url, JSON.stringify(data)).
			success(function() {
				$scope.error = null;
				$scope.foundUsers = [];
				_.each($scope.contacts, function(contact) { contact.checked = false; });
				_.each(userIds, function(userId) { circle.members.push(userId); });
				_.each(usersToAdd, function(user) { $scope.userNames[user._id.$oid] = user.name; });
			}).
			error(function(err) { $scope.error = "Failed to add users: " + err; });
	}
	
	// remove a user
	$scope.removeMember = function(circle, userId) {
		$http(jsRoutes.controllers.Circles.removeMember(circle._id.$oid, userId.$oid)).
			success(function() {
				$scope.error = null;
				circle.members.splice(circle.members.indexOf(userId), 1);
			}).
			error(function(err) { $scope.error = "Failed to remove the selected member from circle '" + circle.name + "': " + err; });
	}
}]);