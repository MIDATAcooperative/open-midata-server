/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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
		service.getConfig().then(function(res) {
			var d =res.data.dashboards[context];
			if (d != null && d.add != null) {
			 var idx = d.add.indexOf(view);
			 if (idx >= 0) {
				 d.add.splice(idx, 1);
				 service.setConfig(res.data);
			 }
			}
		});
	};
	
	return service;
}]);