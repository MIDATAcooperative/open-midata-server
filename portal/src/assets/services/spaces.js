angular.module('services')
.factory('spaces', function(server, $q) {
	var service = {};

	service.getSpacesOfUser = function(userId) {
       var properties = {"owner": userId};
       var fields = ["name", "records", "visualization", "app", "order"];
       var data = {"properties": properties, "fields": fields};
       return server.post(jsRoutes.controllers.Spaces.get().url, JSON.stringify(data));
	};
	service.get = function(properties, fields) {	       
	       var data = {"properties": properties, "fields": fields};
	       return server.post(jsRoutes.controllers.Spaces.get().url, JSON.stringify(data));
	};
	service.getSpacesOfUserContext = function(userId, context) {
	   var properties = {"owner": userId, "context" : context };
	   var fields = ["name", "records", "visualization", "app", "order"];
	   var data = {"properties": properties, "fields": fields};
	   return server.post(jsRoutes.controllers.Spaces.get().url, JSON.stringify(data));
	};
	
	service.getUrl = function(spaceId) {
	   return server.get(jsRoutes.controllers.Spaces.getUrl(spaceId).url);
	};
	
	service.getPreviewUrl = function(spaceId) {
	   return server.get(jsRoutes.controllers.Spaces.getPreviewUrl(spaceId).url);
	};
	
	service.getPreviewUrlFromSetup = function(setup) {
	   return server.post(jsRoutes.controllers.Spaces.getPreviewUrlSetup(), JSON.stringify(setup));
	};
	
	service.add = function(def) {
		return server.post(jsRoutes.controllers.Spaces.add().url, JSON.stringify(def));
	};
	
	service.deleteSpace = function(space) {
		return server["delete"](jsRoutes.controllers.Spaces["delete"](space).url);
	};
	
	return service;
	    	
});