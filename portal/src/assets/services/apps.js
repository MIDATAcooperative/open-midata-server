angular.module('services')
.factory('apps', function(server, $q) {
	var service = {};

    service.getApps = function(properties, fields) {
   	   var data = {"properties": properties, "fields": fields};
	   return server.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data));
    };
    
    service.getAppsOfUser = function(userId, types, fields) {
    	var result = $q.defer();
    	var uproperties = {"_id": userId};
		var ufields = ["apps"];
		var udata = {"properties": uproperties, "fields": ufields};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(udata))
		.then(function(results) {
			console.log(results);
			var appIds = results.data[0].apps;
			var properties2 = { "_id": appIds, "type" : types };					
			service.getApps(properties2, fields)
			.then(function(results2) { result.resolve(results2); });			
		});
		return result.promise;
    };
    
    service.isVisualizationInstalled = function(visId) {
    	return server.get(jsRoutes.controllers.Visualizations.isInstalled(visId).url);
    };
    
	return service;
});