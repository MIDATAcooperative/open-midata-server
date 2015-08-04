angular.module('portal')
.controller('CreateMessageCtrl', ['$scope', '$http', function($scope, $http) {
	
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
	};
	
	// remove a receiver from the list
	$scope.remove = function(receiver) {
		$scope.message.receivers.splice($scope.message.receivers.indexOf(receiver), 1);
	};
	
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
	};
	
}]);