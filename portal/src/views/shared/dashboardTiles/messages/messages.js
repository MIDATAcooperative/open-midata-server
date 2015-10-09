angular.module('views')
.controller('MessagesCtrl', ['$scope', '$state', 'server', '$attrs', 'session', 'views', 'status', function($scope, $state, server, $attrs, session, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.limit = 4;
    
    session.currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.reload = function() { 
    	if (!$scope.view.active || !$scope.userId) return;
    	$scope.limit = $scope.view.position == "small" ? 4 : 20;
    	
    	getFolders($scope.userId);
    };
    
    // get messages
	getFolders = function(userId) {
		var properties = {"_id": userId};
		var fields = ["messages"];
		var data = {"properties": properties, "fields": fields};
		$scope.status.doBusy(server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data))).
		then(function(results) {
			    var users = results.data;
				$scope.inbox = users[0].messages.inbox;
				//$scope.archive = users[0].messages.archive;
				//$scope.trash = users[0].messages.trash;
				var messageIds = $scope.inbox;
				getMessages(messageIds);
		});
	};
	
	getMessages = function(messageIds) {
		var properties = {"_id": messageIds};
		var fields = ["sender", "created", "title"];
		var data = {"properties": properties, "fields": fields};
		$scope.status.doBusy(server.post(jsRoutes.controllers.Messages.get().url, JSON.stringify(data))).
		then(function(results) {
			    $scope.messages = results.data;
			    //var messages = results.data;
				//_.each(messages, function(message) { $scope.messages[message._id.$oid] = message; });
				//var senderIds = _.map(messages, function(message) { return message.sender; });
				//senderIds = _.uniq(senderIds, false, function(senderId) { return senderId.$oid; });
				//getSenderNames(senderIds);
		});
	};
	
	$scope.showMessage = function(messageId) {
		$state.go('^.message', { messageId : messageId });		
	};
            
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);