/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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