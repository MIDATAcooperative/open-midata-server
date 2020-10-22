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
.factory('fhir', ['$httpParamSerializer', 'server', function($httpParamSerializer, server) {
	
	var service = {};
	
	service.unwrap = function(result) {
		var res = [];
		angular.forEach(result.data.entry, function(entry) {
			res.push(entry.resource);
		});
		return res;
	};
	
	service.search = function(resource, properties) {	
		var p = $httpParamSerializer(properties);
		return server.getR4("/fhir/"+resource+(p?("?"+p):"")).then(function(result) {
			return service.unwrap(result);
		});
	};	
	
	service.postR4 = function(resource, data) {
		return server.post("/fhir/"+resource, JSON.stringify(data));
	};
		
	return service;
	
}]);