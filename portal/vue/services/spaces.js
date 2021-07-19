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

import apps from "./apps";
import server from "./server";


	var service = {};

	service.getSpacesOfUser = function(userId) {
       var properties = {"owner": userId};
       var fields = ["name", "records", "visualization", "type", "order"];
       var data = {"properties": properties, "fields": fields};
       return server.post(jsRoutes.controllers.Spaces.get().url, data);
	};
	service.get = function(properties, fields) {	       
	       var data = {"properties": properties, "fields": fields};
	       return server.post(jsRoutes.controllers.Spaces.get().url, data);
	};
	service.getSpacesOfUserContext = function(userId, context) {
	   var properties = {"owner": userId, "context" : context };
	   var fields = ["name", "records", "visualization", "type", "order"];
	   var data = {"properties": properties, "fields": fields};
	   return server.post(jsRoutes.controllers.Spaces.get().url, data);
	};
	
	service.autoadd = function() {
		return server.post(jsRoutes.controllers.Plugins.addMissingPlugins().url, {});
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
		if (urlInfo.id) url+="&id="+urlInfo.id;
		if (urlInfo.resourceType) url+="&resourceType="+urlInfo.resourceType;
		if (params) {
			for (let k in params) {
				let v = params[k];			
				if (k != "path") url += "&"+k+"="+encodeURIComponent(v);
			}
		}
		return url;
	};
	
	service.add = function(def) {
		return server.post(jsRoutes.controllers.Spaces.add().url, def);
	};
	
	service.deleteSpace = function(space) {
		return server["delete"](jsRoutes.controllers.Spaces["delete"](space).url);
	};
	
	service.openAppLink = function($router, $route, userId, data) {
		console.log("open app link");
		if (data.app === "market") {
			  if (data.params.appId) {
				  $router.push({ path : "./visualization", query : { visualizationId : data.params.appId } });
			  } else $router.push({ path : "./market", query : { tag : data.params.tag, context : (data.params.context || "me") } });
		} else if (data.app === "newconsent") {
			  $router.push("./newconsent", { query : { share : data.params.share } });
		} else if (data.app === "profile") {
			  $router.push({ path : "./user",  query : { userId : userId } });			  
		} else if (data.app === "consent") {
			  $router.push({ path : "./consent", query : { consentId : data.params.consentId } });
		} else if (data.app === "apps") {
			  //if (data.params.studyId) $state.go("^.studydetails", { studyId : data.params.studyId });
			  $router.push({ path : "./apps" });
		} else if (data.app === "studies") {
			  if (data.params.studyId) $router.push({ path : "./studydetails", query : { studyId : data.params.studyId } });
			  else $router.push({ path : "./studies"  });
		} else if (data.app === "newrequest") {
			  $router.push({ path : "./newconsent", query : { share : data.params.share, request : true } });
		} else {
			let p = null;
			if (data.app) {
				p = server.post(jsRoutes.controllers.Plugins.get().url, { "properties" : { "filename" : data.app }, "fields": ["_id", "type"] })
			  	.then(function(result) {				
					if (result.data.length == 1) {
						return result.data[0];
				  	} else return null;
				});
			} else if (data.plugin) p = Promise.resolve(data.plugin);
			if (!p)	return Promise.resolve();
			return p.then(function(app) { 
				return service.get({ "owner": userId, "visualization" : app._id,  "context" : data.context }, ["_id"])
			    .then(function(spaceresult) {
					if (spaceresult.data.length > 0) {
						var target = spaceresult.data[0];
						if (app.type === "oauth1" || app.type === "oauth2") {
							$router.push({ path : "./importrecords", query : { "spaceId" : target._id, "params" : JSON.stringify(data.params) } });
						} else {
							$router.push({ path : "./spaces", query : { spaceId : target._id, user : data.user, params : JSON.stringify(data.params || {}) } });
						}
					} else {														
						return apps.installPlugin(app._id, { applyRules : true, context : data.context, study : data.study })
						.then(function(result) {				
							//session.login();
							if (result.data && result.data._id) {
								if (app.type === "oauth1" || app.type === "oauth2") {
									$router.push({ path : "./importrecords", query : { "spaceId" : result.data._id, params : JSON.stringify(data.params) } });
								} else { 
									$router.push({ path : './spaces', query : { spaceId : result.data._id, user : data.user, params : JSON.stringify(data.params || {})  } });
								}
							} else {
								if (app.type === "external" || app.type === "service") {
									$router.push({ path : './apps' });
								} else {					
									$router.push({ path : "./timeline" });                         
								}
								//$router.push({ path : './dashboard', query : { dashId : $scope.options.context } });
							}
						});							 							 				
					}
				});
			});
		}
		return Promise.resolve();
	}
	
	export default service;