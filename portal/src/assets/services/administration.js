angular.module('services')
.factory('administration', ['$q', 'server', function($q, server) {
	
	var service = {};
	
	service.changeStatus = function(userId, status, contractStatus, agbStatus) {
		var data = { user : userId, status : status };
		if (contractStatus) { data.contractStatus = contractStatus; }
		if (agbStatus) { data.agbStatus = agbStatus; }
		return server.post(jsRoutes.controllers.admin.Administration.changeStatus().url, JSON.stringify(data));
	};
		
	return service;
	
}]);