angular.module('services')
.factory('terms', ['server', function(server) {
	var service = {};
	
	service.search = function(properties, fields) {
		var data = { properties : properties, fields : fields };
		return server.post(jsRoutes.controllers.Terms.search().url, JSON.stringify(data))
		.then(function(result) {
			for (var i=0;i<result.data.length;i++) {
				var t = result.data[i];
				t.id = t.name+"--"+t.version;
				t.fullname = t.name+" ("+t.version+")";
			}
			return result;
		});
	};
	
	service.add = function(news) {
		return server.post(jsRoutes.controllers.Terms.add().url, JSON.stringify(news));
	};
	
	service.get = function(name,version,language) {
		return server.post(jsRoutes.controllers.Terms.get().url, JSON.stringify({ "name" : name, "version" : version, "language" : language }));
	};
				
	return service;
}]);