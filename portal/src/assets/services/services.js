angular.module('services')
.factory('services', ['server', function(server) {
	var service = {};
    
    service.listByStudy = function(id) {
        return server.get(jsRoutes.controllers.Services.listServiceInstancesStudy(id).url);
    };

    service.list = function() {
        return server.get(jsRoutes.controllers.Services.listServiceInstances().url);
    };
    
    service.removeService = function(instanceId) {
        return server.delete(jsRoutes.controllers.Services.removeServiceInstance(instanceId).url);
    };
    
    service.listKeys = function(instanceId) {
        return server.get(jsRoutes.controllers.Services.listApiKeys(instanceId).url);
    };

    service.addApiKey = function(instanceId) {
        return server.post(jsRoutes.controllers.Services.addApiKey(instanceId).url);
    };
    
    service.removeApiKey = function(instanceId, keyId) {
        return server.delete(jsRoutes.controllers.Services.removeApiKey(instanceId, keyId).url);
    };
    
	return service;
	    	
}]);