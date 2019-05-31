angular.module('services')
.factory('administration', ['$q', 'server', function($q, server) {
	
	var service = {};
	
	service.changeStatus = function(userId, status, contractStatus, agbStatus, emailStatus, authType, subroles) {
		var data = { user : userId, status : status };
		if (contractStatus) { data.contractStatus = contractStatus; }
		if (agbStatus) { data.agbStatus = agbStatus; }
		if (emailStatus) { data.emailStatus = emailStatus; }
		if (authType) { data.authType = authType; }
		if (subroles) { data.subroles = subroles; }
		return server.post(jsRoutes.controllers.admin.Administration.changeStatus().url, JSON.stringify(data));
	};
	
	service.addComment = function(userId, comment) {
		var data = { user : userId, comment : comment };		
		return server.post(jsRoutes.controllers.admin.Administration.addComment().url, JSON.stringify(data));
	};
	
	service.wipe = function(userId) {
		var data = { user : userId };		
		return server.post(jsRoutes.controllers.admin.Administration.adminWipeAccount().url, JSON.stringify(data));
	};
		
	return service;
	
}]);