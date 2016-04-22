angular.module('services')
.factory('formats', ['server', function(server) {
	var service = {};
	
	service.listGroups = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listGroups().url);		
	};
	
	service.listContents = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listContents().url);		
	};
	
	service.listFormats = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listFormats().url);		
	};
	
	service.listCodes = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listCodes().url);		
	};
	
	service.createCode = function(code) {
		return server.post(jsRoutes.controllers.FormatAPI.createCode().url, JSON.stringify(code));
	};
	
	service.createContent = function(content) {
		return server.post(jsRoutes.controllers.FormatAPI.createContent().url, JSON.stringify(content));
	};
	
	service.createGroup = function(group) {
		return server.post(jsRoutes.controllers.FormatAPI.createGroup().url, JSON.stringify(group));
	};
	
	service.updateCode = function(code) {
		return server.post(jsRoutes.controllers.FormatAPI.updateCode(code._id.$oid).url, JSON.stringify(code));
	};
	
	service.updateContent = function(content) {
		return server.post(jsRoutes.controllers.FormatAPI.updateContent(content._id.$oid).url, JSON.stringify(content));
	};
	
	service.updateGroup = function(group) {
		return server.post(jsRoutes.controllers.FormatAPI.updateGroup(group._id.$oid).url, JSON.stringify(group));
	};
	
	service.deleteCode = function(code) {
		return server.delete(jsRoutes.controllers.FormatAPI.deleteCode(code._id.$oid).url);
	};
	
	service.deleteContent = function(content) {
		return server.delete(jsRoutes.controllers.FormatAPI.deleteContent(content._id.$oid).url);
	};
	
	service.deleteGroup = function(group) {
		return server.delete(jsRoutes.controllers.FormatAPI.deleteGroup(group._id.$oid).url, JSON.stringify(group));
	};
	
		
	return service;
}]);