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
	
	service.mainUrl = function(urlInfo, lang, params) {		
		return service.url(urlInfo, urlInfo.main, params, lang);		
	};
	
	service.url = function(urlInfo, path, params, lang) {
		var url = urlInfo.base + path.replace(":authToken", urlInfo.token).replace(":path", (params && params.path) ? params.path : "");
		if (url.indexOf("?")>=0) url += "&lang="+encodeURIComponent(lang); else url+="?lang="+encodeURIComponent(lang);
		if (params) {
			angular.forEach(params, function(v,k) {
				if (k != "path") url += "&"+k+"="+encodeURIComponent(v);
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
	
	service.openAppLink = function($state, userId, data) {
		  if (data.app === "market") {
				$state.go("^.market", { tag : data.params.tag, context : "me" });
		  } else if (data.app === "newconsent") {
			  $state.go("^.newconsent", { content : data.params.content });
		  } else {
			  server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify({ "properties" : { "filename" : data.app }, "fields": ["_id", "type"] }))
			  .then(function(result) {
				  console.log(result); 
				  if (result.data.length == 1) {
					  service.get({ "owner": userId, "visualization" : result.data[0]._id }, ["_id"])
					  .then(function(spaceresult) {
						 if (spaceresult.data.length > 0) {
							 var target = spaceresult.data[0];
							 $state.go("^.spaces", { spaceId : target._id, params : JSON.stringify(data.params) });
						 } else {
							 if (result.data[0].type === "oauth1" || result.data[0].type === "oauth2") {
							   $state.go("^.importrecords", { "spaceId" : result.data[0]._id, "context" : "me", "params" : JSON.stringify(data.params) });
							 } else {
							   $state.go("^.visualization", { "visualizationId" : result.data[0]._id, "context" : "me", "params" : JSON.stringify(data.params) });
							 }
						 }
					  });
				  } 
			  });
			  }
	};
	
	return service;
	    	
}]);