var messages = angular.module('messages', []);
messages.controller('MessagesCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.loading = true;
	$scope.inbox = [];
	$scope.archive = [];
	$scope.trash = [];
	$scope.messages = {};
	$scope.names = {};
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) { getFolders(userId); });
	
	// get messages
	getFolders = function(userId) {
		var properties = {"_id": userId};
		var fields = ["messages"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data)).
			success(function(users) {
				$scope.inbox = users[0].messages.inbox;
				$scope.archive = users[0].messages.archive;
				$scope.trash = users[0].messages.trash;
				var messageIds = _.flatten([$scope.inbox, $scope.archive, $scope.trash]);
				getMessages(messageIds);
			}).
			error(function(err) {
				$scope.error = "Failed to load message: " + err;
				$scope.loading = false;
			});
	}
	
	getMessages = function(messageIds) {
		var properties = {"_id": messageIds};
		var fields = ["sender", "created", "title"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Messages.get().url, JSON.stringify(data)).
			success(function(messages) {
				_.each(messages, function(message) { $scope.messages[message._id.$oid] = message; });
				var senderIds = _.map(messages, function(message) { return message.sender; });
				senderIds = _.uniq(senderIds, false, function(senderId) { return senderId.$oid; });
				getSenderNames(senderIds);
			}).
			error(function(err) {
				$scope.error = "Failed to load message: " + err;
				$scope.loading = false;
			});
	}
	
	getSenderNames = function(senderIds) {
		var data = {"properties": {"_id": senderIds}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) { $scope.names[user._id.$oid] = user.name; });
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load user names: " + err;
				$scope.loading = false;
			});
	}
	
	// open message details
	$scope.showMessage = function(messageId) {
		window.location.href = jsRoutes.controllers.Messages.details(messageId.$oid).url;
	}
	
	// move message to another folder
	$scope.move = function(messageId, from, to) {
		$http(jsRoutes.controllers.Messages.move(messageId.$oid, from, to)).
			success(function() {
				$scope[from].splice($scope[from].indexOf(messageId), 1);
				$scope[to].push(messageId);
			}).
			error(function(err) { $scope.error = "Failed to move the message from " + from + " to " + to + ": " + err; });
	}
	
	// remove message
	$scope.remove = function(messageId) {
		$http(jsRoutes.controllers.Messages.remove(messageId.$oid)).
			success(function() {
				delete $scope.messages[messageId.$oid];
				$scope.trash.splice($scope.trash.indexOf(messageId), 1);
			}).
			error(function(err) { $scope.error = "Failed to delete message: " + err; });
	}
	
}]);
messages.controller('CreateMessageCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.success = null;
	$scope.message = {};
	$scope.message.receivers = [];
	$scope.contacts = [];
	
	// prefetch contacts
	$http(jsRoutes.controllers.Users.loadContacts()).
		success(function(contacts) {
			$scope.contacts = contacts;
			initTypeahead();
		}).
		error(function(err) { $scope.error = "Failed to load contacts: " + err; });
	
	// initialize typeahead for receivers field
	initTypeahead = function() {
		$("#query").typeahead([{
			name: "contacts",
			local: $scope.contacts,
			header: '<span class="text-info">Contacts</span>'
		},{
			name: "all-users",
			remote: {
				"url": null,
				"replace": function(url, unusedQuery) {
					// use query before URI encoding (done by play framework)
					return jsRoutes.controllers.Users.complete($scope.message.query).url;
				}
			},
			header: '<span class="text-info">All users</span>'
		}]).on("typeahead:selected", function(event, datum) {
			$scope.$apply(function() {
				if (!_.some($scope.message.receivers, function(receiver) { return receiver.id === datum.id; })) {
					$scope.message.receivers.push(datum);
				}
				$scope.message.query = null;
				$("#query").typeahead('setQuery', "");
			});
		});
	}
	
	// remove a receiver from the list
	$scope.remove = function(receiver) {
		$scope.message.receivers.splice($scope.message.receivers.indexOf(receiver), 1);
	}
	
	// send the message
	$scope.sendMessage = function() {
		$scope.success = null;
		// check input
		if (!$scope.message.receivers.length) {
			$scope.error = "Please add a receiver for your message.";
			return;
		} else if (!$scope.message.title) {
			$scope.error = "Please enter a subject for your message.";
			return;
		} else {
			$scope.error = null;
		}
		
		// send request
		var receiverIds = _.uniq(_.map($scope.message.receivers, function(receiver) { return receiver.id; }));
		var receivers = _.map(receiverIds, function(receiverId) { return {"$oid": receiverId}; });
		var data = {"receivers": receivers, "title": $scope.message.title, "content": $scope.message.content};
		$http.post(jsRoutes.controllers.Messages.send().url, JSON.stringify(data)).
			success(function() {
				$scope.success = "Your message was sent.";
				$scope.message = {};
			}).
			error(function(err) { $scope.error = err; });
	}
	
}]);