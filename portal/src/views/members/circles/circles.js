angular.module('portal')
.controller('CirclesCtrl', ['$scope', '$state', 'server', 'currentUser', 'views', function($scope, $state, server, currentUser, views) {
	
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
	currentUser.then(function(userId) { loadCircles(userId); });
	
	// get circles and make either given or first circle active
	loadCircles = function(userId) {		
		//var fields = ["name", "members", "order"];
		var data = {"owner": userId.$oid};
		server.post(jsRoutes.controllers.Circles.get().url, JSON.stringify(data)).
			success(function(circles) {
				$scope.circles = circles;
				loadContacts();
				if ($scope.circles.length > 0) {
					var activeCircle = $state.params.circleId;
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
	};
	
	// get names for users in circles
	loadContacts = function() {
		var contactIds = _.map($scope.circles, function(circle) { return circle.authorized; });
		contactIds = _.flatten(contactIds);
		contactIds = _.uniq(contactIds, false, function(contactId) { return contactId.$oid; });
		var properties = {"_id": contactIds};
		var fields = ["firstname", "sirname"];
		var data = {"properties": properties, "fields": fields};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(contacts) {
				$scope.contacts = contacts;
				_.each(contacts, function(contact) { $scope.userNames[contact._id.$oid] = (contact.firstname + " " + contact.sirname).trim(); });
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load contacts: " + err;
				$scope.loading = false;
			});
	};
	
	// make circle tab active
	$scope.makeActive = function(circle) {
		_.each($scope.circles, function(circle) { circle.active = false; });
		circle.active = true;
		views.setView("1", { aps : circle.aps.$oid, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowRemove : false, allowAdd : true, type : "circles" });
	};
	
	// add a new circle
	$scope.addCircle = function() {
		$("#circleModal").modal("hide");
		var name = $scope.newCircleName;
		if (!name || name.length === 0) {
			$scope.error = "Please provide a name for the new circle.";
		} else {
			var data = {"name": name};
			server.post(jsRoutes.controllers.Circles.add().url, JSON.stringify(data)).
				success(function(newCircle) {
					$scope.error = null;
					$scope.newCircleName = null;
					$scope.circles.push(newCircle);
					$scope.makeActive(newCircle);
				}).
				error(function(err) { $scope.error = "Failed to add circle '" + name + "': " + err; });
		}
	};
	
	// delete a circle
	$scope.deleteCircle = function(circle) {
		server.delete(jsRoutes.controllers.Circles["delete"](circle._id.$oid).url).
			success(function() {
				$scope.error = null;
				$scope.circles.splice($scope.circles.indexOf(circle), 1);
				if ($scope.circles.length > 0) {
					$scope.makeActive($scope.circles[0]);
				}
			}).
			error(function(err) { $scope.error = "Failed to delete circle '" + circle.name + "': " + err; });
	};
	
	// check whether user is not already in active circle
	$scope.isntMember = function(user) {
		var activeCircle = _.find($scope.circles, function(circle) { return circle.active; });
		var memberIds = _.map(activeCircle.authorized, function(member) { return member.$oid; });
		return !_.contains(memberIds, user._id.$oid);
	};
	
	// search for users
	$scope.searchUsers = function(circle) {
		$scope.searching = true;
		var query = circle.userQuery;
		if (query) {
		server.get(jsRoutes.controllers.Users.search(query).url).
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
	};
	
	// add a user
	$scope.addUsers = function(circle) {
		// get the users that should be added to the circle
		var contactsToAdd = _.filter($scope.contacts, function(contact) { return contact.checked; });
		var foundUsersToAdd = _.filter($scope.foundUsers, function(user) { return user.checked; });
		var usersToAdd = _.union(contactsToAdd, foundUsersToAdd);
		var userIds = _.map(usersToAdd, function(user) { return user._id; });
		userIds = _.uniq(userIds, false, function(userId) { return userId.$oid; });
		
		var data = {"users": userIds};
		server.post(jsRoutes.controllers.Circles.addUsers(circle._id.$oid).url, JSON.stringify(data)).
			success(function() {
				$scope.error = null;
				$scope.foundUsers = [];
				_.each($scope.contacts, function(contact) { contact.checked = false; });
				_.each(userIds, function(userId) { circle.authorized.push(userId); });
				_.each(usersToAdd, function(user) { $scope.userNames[user._id.$oid] = user.name; });
			}).
			error(function(err) { $scope.error = "Failed to add users: " + err; });
	};
	
	// remove a user
	$scope.removeMember = function(circle, userId) {
		server.post(jsRoutes.controllers.Circles.removeMember(circle._id.$oid, userId.$oid).url).
			success(function() {
				$scope.error = null;
				circle.authorized.splice(circle.authorized.indexOf(userId), 1);
			}).
			error(function(err) { $scope.error = "Failed to remove the selected member from circle '" + circle.name + "': " + err; });
	};
}]);