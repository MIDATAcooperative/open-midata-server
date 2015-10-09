angular.module('portal')
.controller('MessageCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	// init
	$scope.error = null;
	$scope.message = {};
	
	// parse message id (format: /messages/:id) and load the app
	var messageId = $state.params.messageId;
	var data = {"properties": {"_id": {"$oid": messageId}}, "fields": ["sender", "receivers", "created", "title", "content"]};
	server.post(jsRoutes.controllers.Messages.get().url, JSON.stringify(data)).
		success(function(messages) {
			$scope.message = messages[0];
			getSenderName();
			getReceiverNames();
			//rewriteCreated();
		}).
		error(function(err) { $scope.error = "Failed to load message details: " + err; });
	
	getSenderName = function() {
		var data = {"properties": {"_id": $scope.message.sender}, "fields": ["name"]};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) { $scope.message.sender.name = users[0].name; }).
			error(function(err) { $scope.error = "Failed to load sender name: " + err; });
	};
	
	getReceiverNames = function() {
		var data = {"properties": {"_id": $scope.message.receivers}, "fields": ["name"]};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) {
					var receiver = _.find($scope.message.receivers, function(rec) { return rec.$oid === user._id.$oid; });
					receiver.name = user.name;
				});
			}).
			error(function(err) { $scope.error = "Failed to load receiver names: " + err; });
	};
	
	/*rewriteCreated = function() {
		var split = $scope.message.created.split(" ");
		$scope.message.created = split[0] + " at " + split[1];
	}*/
	
}]);