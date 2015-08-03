angular.module('services')
.factory('circles', function($http) {
	var service = {};
	
	service.get = function(properties, fields) {
		var data = properties;
		return $http.post(jsRoutes.controllers.Circles.get().url, JSON.stringify(data));
	};
	
	service.createNew = function(name) {
		var data = {"name": name};
		return $http.post(jsRoutes.controllers.Circles.add().url, JSON.stringify(data));
	};
	
	return service;
});