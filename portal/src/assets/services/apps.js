angular.module('services')
.factory('apps', ['server', '$q', function(server, $q) {
	var service = {};

    service.getApps = function(properties, fields) {
   	   var data = {"properties": properties, "fields": fields};
	   return server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data));
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
    	return server.get(jsRoutes.controllers.Plugins.isInstalled(visId).url);
    };
    
    service.updatePlugin = function(plugin) {
    	return server.put(jsRoutes.controllers.Market.updatePlugin(plugin._id.$oid).url, JSON.stringify(plugin));
    };
    
    service.registerPlugin = function(plugin) {
    	return server.post(jsRoutes.controllers.Market.registerPlugin(plugin._id.$oid).url, JSON.stringify(plugin));
    };
    
    service.installPlugin = function(appId, options) {
    	return server.post(jsRoutes.controllers.Plugins.install(appId).url, JSON.stringify(options));
    };
    
	return service;
}]);