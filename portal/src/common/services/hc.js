angular.module('services')
.factory('hc', function($q, $http) {
	
	var service = {};
	
	service.list = function() {
	    return $http.get(jsRoutes.controllers.members.HealthProvider.list().url);
	};
	
	service.confirm = function(providerId) {
		var data = {"provider": providerId };

		return $http.post(jsRoutes.controllers.members.HealthProvider.confirmMemberKey().url, JSON.stringify(data));
	};
	
	service.reject = function(providerId) {
		var data = {"provider": providerId };

		return $http.post(jsRoutes.controllers.members.HealthProvider.rejectMemberKey().url, JSON.stringify(data));
	};
		
	return service;
	
});