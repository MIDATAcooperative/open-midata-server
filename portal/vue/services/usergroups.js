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

import server from "./server";
	
	var service = {};
		
	service.ALLPUBLIC = ["creator", "name", "registeredAt", "searchable", "status", "type"];
	service.MINIMAL = [ "name"];
		
	service.search = function(properties, fields) {
		var data = {"properties": properties, "fields": fields};
		return server.post(jsRoutes.controllers.UserGroups.search().url, data);
	};
	
	service.deleteUserGroup = function(id) {	
		return server.post(jsRoutes.controllers.UserGroups.deleteUserGroup(id).url);
	};
	
	service.editUserGroup = function(group) {	
		return server.post(jsRoutes.controllers.UserGroups.editUserGroup(group._id).url, group);
	};
	
	service.listUserGroupMembers = function(groupId) {
		var data = {"usergroup": groupId };
		return server.post(jsRoutes.controllers.UserGroups.listUserGroupMembers().url, data);
	};
	
	service.createUserGroup = function(usergroup) {		
		return server.post(jsRoutes.controllers.UserGroups.createUserGroup().url, usergroup);
	};
	
	service.addMembersToUserGroup = function(group, members, role) {
		var data = {"group": group, "members" : members };
		if (role) data.role = role;
		return server.post(jsRoutes.controllers.UserGroups.addMembersToUserGroup().url, data);
	};

export default service;