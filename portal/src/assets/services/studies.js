angular.module('services')
.factory('studies', ['server', function(server) {
	var service = {};
	
	service.search = function(properties, fields) {
		var data = {"properties": properties, "fields": fields };
		return server.post(jsRoutes.controllers.common.Studies.search().url, JSON.stringify(data));
	};
	
	return service;
}]);