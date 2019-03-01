angular.module('services')
.factory('usergroups', ['$q', 'server', function($q, server) {
	
	var service = {};
		
	service.ALLPUBLIC = ["creator", "name", "registeredAt", "searchable", "status", "type"];
	service.MINIMAL = [ "name"];
		
	service.search = function(properties, fields) {
		var data = {"properties": properties, "fields": fields};
		return server.post(jsRoutes.controllers.UserGroups.search().url, JSON.stringify(data));
	};
	
	service.deleteUserGroup = function(id) {	
		return server.post(jsRoutes.controllers.UserGroups.deleteUserGroup(id).url);
	};
	
	service.editUserGroup = function(group) {	
		return server.post(jsRoutes.controllers.UserGroups.editUserGroup(group._id).url, JSON.stringify(group));
	};
	
	service.listUserGroupMembers = function(groupId) {
		var data = {"usergroup": groupId };
		return server.post(jsRoutes.controllers.UserGroups.listUserGroupMembers().url, JSON.stringify(data));
	};
	
	service.createUserGroup = function(usergroup) {		
		return server.post(jsRoutes.controllers.UserGroups.createUserGroup().url, JSON.stringify(usergroup));
	};
	
	service.addMembersToUserGroup = function(group, members, role) {
		var data = {"group": group, "members" : members };
		if (role) angular.extend(data, role);
		return server.post(jsRoutes.controllers.UserGroups.addMembersToUserGroup().url, JSON.stringify(data));
	};

				
	return service;
	
}]);