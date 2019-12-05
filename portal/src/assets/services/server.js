angular.module('services')
.factory('server', ['$q', '$http', 'ENV', function($q, $http, ENV) {
	var service = {};	
	
	service.get = function(url) {	
		return $http.get(ENV.apiurl + url, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.getR4 = function(url) {	
		return $http.get(ENV.apiurl + url, { headers : { "X-Session-Token" : sessionStorage.token, "Accept" : "application/fhir+json; fhirVersion=4.0" } });
	};
	
	service.post = function(url, body) {
		return $http.post(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token, "Prefer" : "return=representation" } });
	};
	
	service.postR4 = function(url, body) {
		return $http.post(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token, "Prefer" : "return=representation", "Content-Type" : "application/fhir+json; fhirVersion=4.0" } });
	};
				
	service.put = function(url, body) {
		return $http.put(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.patch = function(url, body) {
		return $http.patch(ENV.apiurl + url, body, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.delete = function(url, body) {
		return $http.delete(ENV.apiurl + url, { headers : { "X-Session-Token" : sessionStorage.token } });
	};
	
	service.token = function() {
		return service.post(jsRoutes.controllers.Application.downloadToken().url);
	};
				 	
	return service;
}]);

