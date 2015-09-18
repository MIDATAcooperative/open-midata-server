angular.module('services')
.factory('studies', ['server', function(server) {
	var service = {};
	
	service.search = function(properties, fields) {
		var data = {"properties": properties, "fields": fields };
		return server.post(jsRoutes.controllers.common.Studies.search().url, JSON.stringify(data));
	};
	
	service.updateParticipation = function(studyId, data) {
		return server.patch(jsRoutes.controllers.members.Studies.updateParticipation(studyId).url, JSON.stringify(data));
	};
	
	service.research = {
			list : function() {	
		       return server.get(jsRoutes.controllers.research.Studies.list().url);
	        }
	};
	
	return service;
}]);