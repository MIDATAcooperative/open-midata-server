angular.module('services')
.factory('hc', function($q, server) {
	
	var service = {};
	
	service.list = function() {
	    return server.get(jsRoutes.controllers.members.HealthProvider.list().url);
	};
	
	service.confirm = function(providerId) {
		var data = {"provider": providerId };

		return server.post(jsRoutes.controllers.members.HealthProvider.confirmMemberKey().url, JSON.stringify(data));
	};
	
	service.reject = function(providerId) {
		var data = {"provider": providerId };

		return server.post(jsRoutes.controllers.members.HealthProvider.rejectMemberKey().url, JSON.stringify(data));
	};
		
	return service;
	
});