angular.module('views')
.controller('AddUsersCtrl', ['$scope', '$state', 'server', '$attrs', 'views', 'records', 'status', function($scope, $state, server, $attrs, views, records, status) {
			

	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.crit = {};
	$scope.userNames = {};

	
	// get names for users in circles
	loadContacts = function() {
		var contactIds = _.map($scope.circles, function(circle) { return circle.authorized; });
		contactIds = _.flatten(contactIds);
		contactIds = _.uniq(contactIds, false, function(contactId) { return contactId.$oid; });
		var properties = {"_id": contactIds};
		var fields = ["firstname", "lastname"];
		var data = {"properties": properties, "fields": fields};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(contacts) {
				$scope.contacts = contacts;
				_.each(contacts, function(contact) { $scope.userNames[contact._id.$oid] = (contact.firstname + " " + contact.lastname).trim(); });
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load contacts: " + err;
				$scope.loading = false;
			});
	};
	
	$scope.reload = function() {
		
	};
	
	// check whether user is not already in active circle
	$scope.isntMember = function(user) {
		var activeCircle = $scope.view.setup.consent;
		var memberIds = _.map(activeCircle.authorized, function(member) { return member.$oid; });
		return !_.contains(memberIds, user._id.$oid);
	};
	
	// search for users
	$scope.searchUsers = function() {
		$scope.searching = true;
		
		server.get(jsRoutes.controllers.Users.search($scope.crit.userQuery).url).
			success(function(users) {
				$scope.error = null;
				$scope.foundUsers = users;
				$scope.searching = false;
			}).
			error(function(err) {
				$scope.error = "User search failed: " + err;
				$scope.searching = false;
			});
		
	};
	
	// add a user
	$scope.addUsers = function() {
		var circle = $scope.view.setup.consent;
		// get the users that should be added to the circle
		var contactsToAdd = _.filter($scope.contacts, function(contact) { return contact.checked; });
		var foundUsersToAdd = _.filter($scope.foundUsers, function(user) { return user.checked; });
		var usersToAdd = _.union(contactsToAdd, foundUsersToAdd);
		var userIds = _.map(usersToAdd, function(user) { return user._id; });
		userIds = _.uniq(userIds, false, function(userId) { return userId.$oid; });
		
		if ($scope.view.setup.callback) {
		   $scope.view.setup.callback(usersToAdd);
		   $scope.error = null;
			$scope.foundUsers = [];
			_.each($scope.contacts, function(contact) { contact.checked = false; });			
			_.each(usersToAdd, function(user) { $scope.userNames[user._id.$oid] = user.name; });
		} else {
		
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
		}
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);