angular.module('services')
.factory('formats', ['server', function(server) {
	var service = {};
	
	service.listGroups = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listGroups().url);		
	};
	
	return service;
}]);