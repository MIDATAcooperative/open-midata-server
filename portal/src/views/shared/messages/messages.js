angular.module('portal')
.controller('MessagesCtrl', ['$scope', '$state', 'server', 'session', function($scope, $state, server, session) {
	
	// init
	$scope.error = null;
	$scope.loading = true;
	$scope.inbox = [];
	$scope.archive = [];
	$scope.trash = [];
	$scope.messages = {};
	$scope.names = {};
	$scope.tab = 'inbox';
	
	// get current user
	session.currentUser.then(
		function(userId) { getFolders(userId); });
	
	// get messages
	getFolders = function(userId) {
		var properties = {"_id": userId};
		var fields = ["messages"];
		var data = {"properties": properties, "fields": fields};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
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
	};
	
	getMessages = function(messageIds) {
		var properties = {"_id": messageIds};
		var fields = ["sender", "created", "title"];
		var data = {"properties": properties, "fields": fields};
		server.post(jsRoutes.controllers.Messages.get().url, JSON.stringify(data)).
			success(function(messages) {
				_.each(messages, function(message) { $scope.messages[message._id] = message; });
				var senderIds = _.map(messages, function(message) { return message.sender; });
				senderIds = _.uniq(senderIds, false, function(senderId) { return senderId; });
				getSenderNames(senderIds);
			}).
			error(function(err) {
				$scope.error = "Failed to load message: " + err;
				$scope.loading = false;
			});
	};
	
	getSenderNames = function(senderIds) {
		var data = {"properties": {"_id": senderIds}, "fields": ["name"]};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) { $scope.names[user._id] = user.name; });
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load user names: " + err;
				$scope.loading = false;
			});
	};
	
	// open message details
	$scope.showMessage = function(messageId) {
		$state.go('^.message', { messageId : messageId });
	};
	
	// move message to another folder
	$scope.move = function(messageId, from, to) {
		server.post(jsRoutes.controllers.Messages.move(messageId, from, to).url).
			success(function() {
				$scope[from].splice($scope[from].indexOf(messageId), 1);
				$scope[to].push(messageId);
			}).
			error(function(err) { $scope.error = "Failed to move the message from " + from + " to " + to + ": " + err; });
	};
	
	// remove message
	$scope.remove = function(messageId) {
		server.delete(jsRoutes.controllers.Messages.remove(messageId).url).
			success(function() {
				delete $scope.messages[messageId];
				$scope.trash.splice($scope.trash.indexOf(messageId), 1);
			}).
			error(function(err) { $scope.error = "Failed to delete message: " + err; });
	};
	
}]);
