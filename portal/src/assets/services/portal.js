angular.module('services')
.factory('portal', ['server', 'session', function(server, session) {
	var service = {};
	
	service.getConfig = function() {		
		return session.cacheGet("portalconfig") ||  session.cachePut("portalconfig", server.get(jsRoutes.controllers.PortalConfig.getConfig().url));
	};
	
	service.setConfig = function(conf) {
		session.cacheClear("portalconfig");
		var data = { config : conf };
		return server.post(jsRoutes.controllers.PortalConfig.setConfig().url, JSON.stringify(data));
	};
	
	service.remove = function(context, view) {
		console.log("remove context:"+context+" view="+view);
	};
	
	return service;
}]);