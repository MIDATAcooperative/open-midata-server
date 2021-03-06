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

angular.module('portal')
.controller('CreateMessageCtrl', ['$scope', 'server', function($scope, server) {
	
	// init
	$scope.error = null;
	$scope.success = null;
	$scope.message = {};
	$scope.message.receivers = [];
	$scope.contacts = [];
	
	// prefetch contacts
	server.get(jsRoutes.controllers.Users.loadContacts().url).
		then(function(contacts) {
			$scope.contacts = contacts.data;
			//initTypeahead();
		}, function(err) { $scope.error = "Failed to load contacts: " + err.data; });
	
	// initialize typeahead for receivers field
	$scope.showContacts = function(viewValue) {
		return server.get(jsRoutes.controllers.Users.complete($scope.message.query).url)
		.then(function(result) {
			return result.data;
		});
	};
	
	$scope.selectContact = function(item) {
		if (!_.some($scope.message.receivers, function(receiver) { return receiver.id === item.id; })) {
			$scope.message.receivers.push(item);
		}
		$scope.message.query = null;
	};
	
	/*
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
					return apiurl + jsRoutes.controllers.Users.complete($scope.message.query).url;
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
	};*/
	
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
		var receivers = receiverIds; // _.map(receiverIds, function(receiverId) { return {"$oid": receiverId}; });
		var data = {"receivers": receivers, "title": $scope.message.title, "content": $scope.message.content};
		server.post(jsRoutes.controllers.Messages.send().url, JSON.stringify(data)).
			then(function() {
				$scope.success = "Your message was sent.";
				$scope.message = {};
			}, function(err) { $scope.error = err.data; });
	};
	
}]);