angular.module('services')
.factory('server', ['$q', '$http', 'ENV', function($q, $http, ENV) {
	var service = {};	
	
	service.get = function(url) {	
		return $http.get(ENV.apiurl + url, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.post = function(url, body) {
		return $http.post(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.put = function(url, body) {
		return $http.put(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.patch = function(url, body) {
		return $http.patch(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.delete = function(url, body) {
		return $http.delete(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
				 	
	return service;
}]);

