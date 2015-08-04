angular.module('services')
.factory('server', function($q, $http, apiurl) {
	var service = {};
	console.log(apiurl);
	
	service.get = function(url) {
		console.log(url);
		return $http.get(apiurl + url);
	};
	
	service.post = function(url, body) {
		return $http.post(apiurl + url, body);
	};
	
	service.delete = function(url, body) {
		return $http.delete(apiurl + url, body);
	};
				 	
	return service;
});

