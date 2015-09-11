angular.module('services')
.factory('provideraccess', ['$q', 'server', function($q, server) {
	
	var service = {};
	
	service.search = function(criteria) {
	    return server.post(jsRoutes.controllers.providers.Providers.search().url, criteria);
	};
	
	service.getMember = function(id) {
		return server.post(jsRoutes.controllers.providers.Providers.getMember(id).url);
	};
	
	return service;
	
}]);