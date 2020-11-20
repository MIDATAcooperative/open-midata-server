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
.factory('circles', ['server', function(server) {
	var service = {};
	
	service.get = function(properties, fields) {
		var data = properties;
		return server.post(jsRoutes.controllers.Circles.get().url, JSON.stringify(data));
	};
	
	service.listConsents = function(properties, fields) {
		var data = { properties : properties, fields : fields };
		return server.post(jsRoutes.controllers.Circles.listConsents().url, JSON.stringify(data));
	};
	
	service.createNew = function(data) {		
		return server.post(jsRoutes.controllers.Circles.add().url, JSON.stringify(data));
	};
	
	service.addUsers = function(circleId, users, entityType) {		
		return server.post(jsRoutes.controllers.Circles.addUsers(circleId).url, JSON.stringify({ users : users, entityType : (entityType || "USER") }));
	};
	
	service.joinByPasscode = function(ownerId, passcode, usergroup) {
		var data = { "owner" : ownerId, "passcode" : passcode, "usergroup" : usergroup };
		return server.post(jsRoutes.controllers.Circles.joinByPasscode().url, JSON.stringify(data));
	};
	
	return service;
}]);