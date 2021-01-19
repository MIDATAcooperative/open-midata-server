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

import server from './server';

	var service = {};

	service.userfeatures = ['EMAIL_ENTERED', 'EMAIL_VERIFIED', 'PHONE_ENTERED', 'PHONE_VERIFIED', 'AUTH2FACTOR', 'ADDRESS_ENTERED' ,'ADDRESS_VERIFIED', 'BIRTHDAY_SET', 'PASSPORT_VERIFIED', 'MIDATA_COOPERATIVE_MEMBER', 'ADMIN_VERIFIED'];
	
	service.writemodes = ['NONE', 'UPDATE_EXISTING', 'UPDATE_AND_CREATE' /*, 'WRITE_ANY' */];
	
    service.getApps = function(properties, fields) {
   	   var data = {"properties": properties, "fields": fields};
	   return server.post(jsRoutes.controllers.Plugins.get().url, data);
    };
    
    service.getAppInfo = function(name, type) {
    	   var data = { "name": name };
    	   if (type) data.type = type;
 	   return server.post(jsRoutes.controllers.Plugins.getInfo().url, data);
     };
    
    service.getAppsOfUser = function(session, types, fields) {
		var appIds = session.user.apps;
		var properties2 = { "_id": session.user.apps, "type" : types };		
		return service.getApps(properties2, fields);			
    };
    
    service.listUserApps = function(fields) {
		var data = { fields : fields };
		return server.post(jsRoutes.controllers.Circles.listApps().url, data);
	};
    
    service.isVisualizationInstalled = function(session, visId) {
    	var def = $q.defer();
    	var inApps = $filter("filter")(session.user.apps, function(x){  return x == visId; });
    	if (inApps.length > 0) {
    		def.resolve({ data : true });
    		return def.promise;
    	}
    	var inVis  = $filter("filter")(session.user.visualizations, function(x){  return x == visId; });
    	if (inVis.length > 0) {
    		def.resolve({ data : true });
    	} else { def.resolve({ data : false }); }
    	return def.promise;
    };
    
    service.updatePlugin = function(plugin) {
    	return server.put(jsRoutes.controllers.Market.updatePlugin(plugin._id).url, plugin);
    };
    
    service.updatePluginStatus = function(plugin) {
    	return server.put(jsRoutes.controllers.Market.updatePluginStatus(plugin._id).url, plugin);
    };
    
    service.deletePlugin = function(plugin) {
    	return server.delete(jsRoutes.controllers.Market.deletePlugin(plugin._id).url);
    };
    
    service.deletePluginDeveloper = function(plugin) {
    	return server.delete(jsRoutes.controllers.Market.deletePluginDeveloper(plugin._id).url);
    };
    
    service.registerPlugin = function(plugin) {
    	return server.post(jsRoutes.controllers.Market.registerPlugin().url, plugin);
    };
    
    service.installPlugin = function(appId, options) {    	
    	return server.put(jsRoutes.controllers.Plugins.install(appId).url, options);
    };
    
    service.uninstallPlugin = function(appId) {    	
    	return server.delete(jsRoutes.controllers.Plugins.install(appId).url);
    };
    
	export default service;