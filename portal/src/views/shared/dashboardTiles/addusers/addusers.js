angular.module('views')
.controller('AddUsersCtrl', ['$scope', '$state', 'server', '$attrs', 'views', 'records', 'status', function($scope, $state, server, $attrs, views, records, status) {
			

	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.crit = {};
	

		
	
	$scope.reload = function() {
		console.log("RELOAD!");
		server.get(jsRoutes.controllers.Users.loadContacts().url).
		then(function(result) {
			console.log(result.data);
			$scope.contacts = result.data;
		});
	};
	
	// check whether user is not already in active circle
	$scope.isntMember = function(user) {
		if (!$scope.view || !$scope.view.setup) return true;
		var activeCircle = $scope.view.setup.consent;
		var memberIds = _.map(activeCircle.authorized, function(member) { return member; });
		return !_.contains(memberIds, user._id);
	};
	
	// search for users
	$scope.searchUsers = function() {
		$scope.searching = true;
		
		server.get(jsRoutes.controllers.Users.search($scope.crit.userQuery).url).
			then(function(users) {
				$scope.error = null;
				$scope.foundUsers = users.data;
				$scope.searching = false;
			}, function(err) {
				$scope.error = "User search failed: " + err.data;
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
		userIds = _.uniq(userIds, false, function(userId) { return userId; });
		console.log(usersToAdd);
		if (usersToAdd.length === 0) return;
		if ($scope.view.setup.callback) {
		   $scope.view.setup.callback(usersToAdd);
		   $scope.error = null;
			$scope.foundUsers = [];
			_.each($scope.contacts, function(contact) { contact.checked = false; });			
			//_.each(usersToAdd, function(user) { $scope.userNames[user._id] = user.name; });
		} else {
		
		var data = {"users": userIds};
		server.post(jsRoutes.controllers.Circles.addUsers(circle._id).url, JSON.stringify(data)).
			then(function() {
				$scope.error = null;
				$scope.foundUsers = [];
				_.each($scope.contacts, function(contact) { contact.checked = false; });
				_.each(userIds, function(userId) { circle.authorized.push(userId); });
				//_.each(usersToAdd, function(user) { $scope.userNames[user._id] = user.name; });
			},function(err) { $scope.error = "Failed to add users: " + err; });
		}
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);