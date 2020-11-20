/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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