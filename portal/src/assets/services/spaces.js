/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('services')
.factory('spaces', ['server', '$q', 'apps', function(server, $q, apps) {
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
	
	service.autoadd = function() {
		return server.post(jsRoutes.controllers.Plugins.addMissingPlugins().url, JSON.stringify({}));
	};
	
	service.getUrl = function(spaceId, user) {
	   return server.get(jsRoutes.controllers.Spaces.getUrl(spaceId, user).url);
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
		if (urlInfo.owner) url+="&owner="+urlInfo.owner;
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
			  if (data.params.appId) {
				  $state.go("^.visualization", { visualizationId : data.params.appId });
			  } else $state.go("^.market", { tag : data.params.tag, context : (data.params.context || "me") });
		  } else if (data.app === "newconsent") {
			  $state.go("^.newconsent", { share : data.params.share });
		  } else if (data.app === "profile") {
			  $state.go("^.user", { userId : userId });			  
		  } else if (data.app === "consent") {
			  $state.go("^.consent", { consentId : data.params.consentId });
		  } else if (data.app === "apps") {
			  //if (data.params.studyId) $state.go("^.studydetails", { studyId : data.params.studyId });
			  $state.go("^.apps", {  });
		  } else if (data.app === "studies") {
			  if (data.params.studyId) $state.go("^.studydetails", { studyId : data.params.studyId });
			  else $state.go("^.studies", {  });
		  } else if (data.app === "newrequest") {
			  $state.go("^.newconsent", { share : data.params.share, request : true });
		  } else {
			  server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify({ "properties" : { "filename" : data.app }, "fields": ["_id", "type"] }))
			  .then(function(result) {				
				  if (result.data.length == 1) {
					  var app = result.data[0];
					  service.get({ "owner": userId, "visualization" : result.data[0]._id,  "context" : data.context }, ["_id"])
					  .then(function(spaceresult) {
						 if (spaceresult.data.length > 0) {
							 var target = spaceresult.data[0];
							 if (result.data[0].type === "oauth1" || result.data[0].type === "oauth2") {
							   $state.go("^.importrecords", { "spaceId" : target._id, "params" : JSON.stringify(data.params) });
							 } else {
							   $state.go("^.spaces", { spaceId : target._id, params : JSON.stringify(data.params), user : data.user });
							 }
						 } else {
							 
							 
							 apps.installPlugin(app._id, { applyRules : true, context : data.context, study : data.study })
								.then(function(result) {				
									//session.login();
									if (result.data && result.data._id) {
									  if (app.type === "oauth1" || app.type === "oauth2") {
										 $state.go("^.importrecords", { "spaceId" : result.data._id, params : JSON.stringify(data.params) });
									  } else { 
									     $state.go('^.spaces', { spaceId : result.data._id, params : JSON.stringify(data.params), user : data.user });
									  }
									} else {
									  $state.go('^.dashboard', { dashId : $scope.options.context });
									}
								});
							 
							 
							 
							 
							 
							 
							 
							  // $state.go("^.visualization", { "visualizationId" : result.data[0]._id, "params" : JSON.stringify(data.params) });
							 
						 }
					  });
				  } 
			  });
			  }
	};
	
	return service;
	    	
}]);