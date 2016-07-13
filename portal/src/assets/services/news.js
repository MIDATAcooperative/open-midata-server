angular.module('services')
.factory('news', ['server', function(server) {
	var service = {};
	
	service.get = function(properties, fields) {
		var data = { properties : properties, fields : fields };
		return server.post(jsRoutes.controllers.News.get().url, JSON.stringify(data));
	};
	
	service.add = function(news) {
		return server.post(jsRoutes.controllers.News.add().url, JSON.stringify(news));
	};
	
	service.update = function(news) {
		return server.post(jsRoutes.controllers.News.update().url, JSON.stringify(news));
	};
	
	service.delete = function(newsId) {
		return server.post(jsRoutes.controllers.News.delete(newsId).url);
	};
			
	return service;
}]);