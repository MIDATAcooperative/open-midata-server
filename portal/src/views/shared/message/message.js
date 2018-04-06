angular.module('portal')
.controller('MessageCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	// init
	$scope.error = null;
	$scope.message = {};
	
	// parse message id (format: /messages/:id) and load the app
	var messageId = $state.params.messageId;
	var data = {"properties": {"_id":  messageId}, "fields": ["sender", "receivers", "created", "title", "content"]};
	server.post(jsRoutes.controllers.Messages.get().url, JSON.stringify(data)).
		then(function(messages) {
			$scope.message = messages.data[0];
			getSenderName();
			getReceiverNames();
			//rewriteCreated();
		},function(err) { $scope.error = "Failed to load message details: " + err; });
	
	getSenderName = function() {
		var data = {"properties": {"_id": $scope.message.sender}, "fields": ["name"]};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			then(function(users) { $scope.message.sender.name = users.data[0].name; }, function(err) { $scope.error = "Failed to load sender name: " + err.data; });
	};
	
	getReceiverNames = function() {
		var data = {"properties": {"_id": $scope.message.receivers}, "fields": ["name"]};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			then(function(users1) {
				var users = users1.data;
				_.each(users, function(user) {
					var receiver = _.find($scope.message.receivers, function(rec) { return rec === user._id; });
					receiver.name = user.name;
				});
			},function(err) { $scope.error = "Failed to load receiver names: " + err; });
	};
	
	/*rewriteCreated = function() {
		var split = $scope.message.created.split(" ");
		$scope.message.created = split[0] + " at " + split[1];
	}*/
	
}]);