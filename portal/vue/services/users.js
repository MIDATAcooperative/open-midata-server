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
	var dinfo;
	
	service.ALLPUBLIC = ["address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "phone", "role", "zip"];
	service.MINIMAL = [ "firstname", "lastname", "role", "email"];
	
	
	service.getMembers = function(properties, fields) {
		var data = {"properties": properties, "fields": fields};

		return server.post(jsRoutes.controllers.Users.get().url, data);
	};
	
	service.getDashboardInfo = function(id) {
		if (!dinfo) {
		  dinfo = service.getMembers({"_id": id}, ["login", "news", "pushed", "shared", "apps", "visualizations"]);
		}
		return dinfo;
	};
	
	service.updateSettings = function(user) {
		var data = {"language": user.language, "searchable": user.searchable, "authType" : user.authType, "notifications" : user.notifications };
		return server.post(jsRoutes.controllers.Users.updateSettings().url, data);
	};
	
	service.updateAddress = function(user) {		
		return server.post(jsRoutes.controllers.Users.updateAddress().url, user);
	};
	
	service.updateBirthday = function(user) {		
		return server.post(jsRoutes.controllers.admin.Administration.changeBirthday().url, user);
	};
	
	service.requestMembership = function(user) {		
		return server.post(jsRoutes.controllers.Users.requestMembership().url, user);
	};
		
export default service;