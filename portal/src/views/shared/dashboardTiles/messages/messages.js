/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
				//_.each(messages, function(message) { $scope.messages[message._id] = message; });
				//var senderIds = _.map(messages, function(message) { return message.sender; });
				//senderIds = _.uniq(senderIds, false, function(senderId) { return senderId; });
				//getSenderNames(senderIds);
		});
	};
	
	$scope.showMessage = function(messageId) {
		$state.go('^.message', { messageId : messageId });		
	};
            
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);