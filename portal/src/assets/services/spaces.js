angular.module('services')
.factory('spaces', ['server', '$q', function(server, $q) {
	var service = {};

	service.getSpacesOfUser = function(userId) {
       var properties = {"owner": userId};
       var fields = ["name", "records", "visualization", "type", "order"];
       var data = {"properties": properties, "fields": fields};
       return server.post(jsRoutes.controllers.Spaces.get().url, JSON.stringify(data));
	};
	service.get = function(properties, fields) {	       
	       var data = {"properties": properties, "fields": fields};
	       return server.post(jsRoutes.controllers.Spaces.get().url, JSON.stringify(data));
	};
	service.getSpacesOfUserContext = function(userId, context) {
	   var properties = {"owner": userId, "context" : context };
	   var fields = ["name", "records", "visualization", "type", "order"];
	   var data = {"properties": properties, "fields": fields};
	   return server.post(jsRoutes.controllers.Spaces.get().url, JSON.stringify(data));
	};
	
	service.getUrl = function(spaceId) {
	   return server.get(jsRoutes.controllers.Spaces.getUrl(spaceId).url);
	};
	
	service.regetUrl = function(spaceId) {
	   return server.get(jsRoutes.controllers.Spaces.regetUrl(spaceId).url);
	};
		
	service.previewUrl = function(urlInfo, lang) {
		if (!urlInfo.preview) return null;
		return service.url(urlInfo, urlInfo.preview, null, lang);		
	};
	
	service.mainUrl = function(urlInfo, lang) {		
		return service.url(urlInfo, urlInfo.main, null, lang);		
	};
	
	service.url = function(urlInfo, path, params, lang) {
		var url = urlInfo.base + path.replace(":authToken", urlInfo.token);
		if (url.indexOf("?")>=0) url += "&lang"+encodeURIComponent(lang); else url+="?lang="+encodeURIComponent(lang);
		if (params) {
			angular.forEach(params, function(v,k) {
				url += "&"+k+"="+encodeURIComponent(v);
			});
		}
		return url;
	};
	
	service.add = function(def) {
		return server.post(jsRoutes.controllers.Spaces.add().url, JSON.stringify(def));
	};
	
	service.deleteSpace = function(space) {
		return server["delete"](jsRoutes.controllers.Spaces["delete"](space).url);
	};
	
	return service;
	    	
}]);