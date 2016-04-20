angular.module('services')
.factory('formats', ['server', function(server) {
	var service = {};
	
	service.listGroups = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listGroups().url);		
	};
	
	service.listContents = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listContents().url);		
	};
	
	service.listFormats = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listFormats().url);		
	};
	
	service.listCodes = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listCodes().url);		
	};
	
		
	return service;
}]);