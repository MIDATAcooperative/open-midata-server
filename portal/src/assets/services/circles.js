angular.module('services')
.factory('circles', ['server', function(server) {
	var service = {};
	
	service.get = function(properties, fields) {
		var data = properties;
		return server.post(jsRoutes.controllers.Circles.get().url, JSON.stringify(data));
	};
	
	service.createNew = function(data) {		
		return server.post(jsRoutes.controllers.Circles.add().url, JSON.stringify(data));
	};
	
	service.addUsers = function(circleId, users) {		
		return server.post(jsRoutes.controllers.Circles.addUsers(circleId).url, JSON.stringify({ users : users }));
	};
	
	service.joinByPasscode = function(ownerId, passcode) {
		var data = { "owner" : ownerId, "passcode" : passcode };
		return server.post(jsRoutes.controllers.Circles.joinByPasscode().url, JSON.stringify(data));
	};
	
	return service;
}]);