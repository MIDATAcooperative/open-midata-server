angular.module('services')
.factory('circles', ['server', function(server) {
	var service = {};
	
	service.get = function(properties, fields) {
		var data = properties;
		return server.post(jsRoutes.controllers.Circles.get().url, JSON.stringify(data));
	};
	
	service.createNew = function(name) {
		var data = {"name": name};
		return server.post(jsRoutes.controllers.Circles.add().url, JSON.stringify(data));
	};
	
	return service;
}]);