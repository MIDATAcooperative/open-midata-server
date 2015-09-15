angular.module('services')
.factory('users', ['$q', 'server', function($q, server) {
	
	var service = {};
	var dinfo;
	
	service.ALLPUBLIC = ["address1", "address2", "city", "country", "email", "firstname", "gender", "lastname", "mobile", "phone", "role", "zip"];
	
	service.getMembers = function(properties, fields) {
		var data = {"properties": properties, "fields": fields};

		return server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data));
	};
	
	service.getDashboardInfo = function(id) {
		if (!dinfo) {
		  dinfo = service.getMembers({"_id": id}, ["login", "news", "pushed", "shared", "apps", "visualizations"]);
		}
		return dinfo;
	};
		
	return service;
	
}]);