angular.module('services')
.factory('fhir', ['$httpParamSerializer', 'server', function($httpParamSerializer, server) {
	
	var service = {};
	
	service.unwrap = function(result) {
		var res = [];
		angular.forEach(result.data.entry, function(entry) {
			res.push(entry.resource);
		});
		return res;
	};
	
	service.search = function(resource, properties) {	
		var p = $httpParamSerializer(properties);
		return server.get("/fhir/"+resource+(p?("?"+p):"")).then(function(result) {
			return service.unwrap(result);
		});
	};	
	
	service.post = function(resource, data) {
		return server.post("/fhir/"+resource, JSON.stringify(data));
	};
		
	return service;
	
}]);