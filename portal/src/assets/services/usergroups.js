angular.module('services')
.factory('usergroups', ['$q', 'server', function($q, server) {
	
	var service = {};
		
	service.ALLPUBLIC = ["creator", "history", "name", "registeredAt", "searchable", "status", "type"];
	service.MINIMAL = [ "name"];
		
	service.search = function(properties, fields) {
		var data = {"properties": properties, "fields": fields};
		return server.post(jsRoutes.controllers.UserGroups.search().url, JSON.stringify(data));
	};
	
	service.listUserGroupMembers = function(groupId) {
		var data = {"usergroup": groupId };
		return server.post(jsRoutes.controllers.UserGroups.listUserGroupMembers().url, JSON.stringify(data));
	};
	
	service.createUserGroup = function(usergroup) {		
		return server.post(jsRoutes.controllers.UserGroups.createUserGroup().url, JSON.stringify(usergroup));
	};
	
	service.addMembersToUserGroup = function(group, members) {
		var data = {"group": group, "members" : members };
		return server.post(jsRoutes.controllers.UserGroups.addMembersToUserGroup().url, JSON.stringify(data));
	};

				
	return service;
	
}]);