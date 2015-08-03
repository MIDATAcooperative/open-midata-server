angular.module('services')
.factory('formats', function($http) {
	var service = {};
	
	service.listGroups = function() {
		return $http.get(jsRoutes.controllers.FormatAPI.listGroups().url);		
	};
	
	return service;
});