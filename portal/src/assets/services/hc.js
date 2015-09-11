angular.module('services')
.factory('hc', ['$q', 'server', function($q, server) {
	
	var service = {};
	
	service.list = function() {
	    return server.get(jsRoutes.controllers.members.HealthProvider.list().url);
	};
	
	service.search = function(props, fields) {
		var data = { properties : props, fields : fields };
	    return server.post(jsRoutes.controllers.members.HealthProvider.search().url, JSON.stringify(data));
	};
	
	service.confirm = function(consentId) {
		var data = {"consent": consentId };

		return server.post(jsRoutes.controllers.members.HealthProvider.confirmConsent().url, JSON.stringify(data));
	};
	
	service.reject = function(consentId) {
		var data = {"consent": consentId };

		return server.post(jsRoutes.controllers.members.HealthProvider.rejectConsent().url, JSON.stringify(data));
	};
		
	return service;
	
}]);