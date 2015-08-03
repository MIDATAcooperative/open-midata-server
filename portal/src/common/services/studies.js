angular.module('services')
.factory('studies', function($http) {
	var service = {};
	
	service.search = function(properties, fields) {
		var data = {"properties": properties, "fields": fields };
		return $http.post(jsRoutes.controllers.common.Studies.search().url, JSON.stringify(data));
	};
	
	return service;
});