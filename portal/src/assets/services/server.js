angular.module('services')
.factory('server', ['$q', '$http', 'ENV', function($q, $http, ENV) {
	var service = {};
	console.log(ENV.apiurl);
	
	service.get = function(url) {
		console.log(url);
		return $http.get(ENV.apiurl + url);
	};
	
	service.post = function(url, body) {
		return $http.post(ENV.apiurl + url, body);
	};
	
	service.put = function(url, body) {
		return $http.put(ENV.apiurl + url, body);
	};
	
	service.delete = function(url, body) {
		return $http.delete(ENV.apiurl + url, body);
	};
				 	
	return service;
}]);

