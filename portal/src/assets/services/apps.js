angular.module('services')
.factory('apps', ['server', '$q', 'session', '$filter', function(server, $q, session, $filter) {
	var service = {};

    service.getApps = function(properties, fields) {
   	   var data = {"properties": properties, "fields": fields};
	   return server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data));
    };
    
    service.getAppInfo = function(name) {
    	   var data = { "name": name };
 	   return server.post(jsRoutes.controllers.Plugins.getInfo().url, JSON.stringify(data));
     };
    
    service.getAppsOfUser = function(types, fields) {
		var appIds = session.user.apps;
		var properties2 = { "_id": session.user.apps, "type" : types };		
		return service.getApps(properties2, fields);			
    };
    
    service.isVisualizationInstalled = function(visId) {
    	var def = $q.defer();
    	var inApps = $filter("filter")(session.user.apps, function(x){  return x.$oid == visId; });
    	if (inApps.length > 0) {
    		def.resolve({ data : true });
    		return def.promise;
    	}
    	var inVis  = $filter("filter")(session.user.visualizations, function(x){  return x.$oid == visId; });
    	if (inVis.length > 0) {
    		def.resolve({ data : true });
    	} else { def.resolve({ data : false }); }
    	return def.promise;
    };
    
    service.updatePlugin = function(plugin) {
    	return server.put(jsRoutes.controllers.Market.updatePlugin(plugin._id.$oid).url, JSON.stringify(plugin));
    };
    
    service.updatePluginStatus = function(plugin) {
    	return server.put(jsRoutes.controllers.Market.updatePluginStatus(plugin._id.$oid).url, JSON.stringify(plugin));
    };
    
    service.deletePlugin = function(plugin) {
    	return server.delete(jsRoutes.controllers.Market.deletePlugin(plugin._id.$oid).url);
    };
    
    service.registerPlugin = function(plugin) {
    	return server.post(jsRoutes.controllers.Market.registerPlugin().url, JSON.stringify(plugin));
    };
    
    service.installPlugin = function(appId, options) {    	
    	return server.put(jsRoutes.controllers.Plugins.install(appId).url, JSON.stringify(options));
    };
    
    service.uninstallPlugin = function(appId) {    	
    	return server.delete(jsRoutes.controllers.Plugins.install(appId).url);
    };
    
	return service;
}]);