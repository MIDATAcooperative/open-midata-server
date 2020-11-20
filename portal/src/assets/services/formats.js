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
.factory('formats', ['server', function(server) {
	var service = {};
	
	service.listGroups = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listGroups().url);		
	};
	
	service.listContents = function() {
		return server.get(jsRoutes.controllers.FormatAPI.listContents().url);		
	};
	
	service.searchContents = function(properties, fields) {
		return server.post(jsRoutes.controllers.FormatAPI.searchContents().url, JSON.stringify({ properties : properties, fields : fields}));		
	};
	
	service.searchCodes = function(properties, fields) {
		return server.post(jsRoutes.controllers.FormatAPI.searchCodingPortal().url, JSON.stringify({ properties : properties, fields : fields}));		
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
		return server.post(jsRoutes.controllers.FormatAPI.updateCode(code._id).url, JSON.stringify(code));
	};
	
	service.updateContent = function(content) {
		return server.post(jsRoutes.controllers.FormatAPI.updateContent(content._id).url, JSON.stringify(content));
	};
	
	service.updateGroup = function(group) {
		return server.post(jsRoutes.controllers.FormatAPI.updateGroup(group._id).url, JSON.stringify(group));
	};
	
	service.deleteCode = function(code) {
		return server.delete(jsRoutes.controllers.FormatAPI.deleteCode(code._id).url);
	};
	
	service.deleteContent = function(content) {
		return server.delete(jsRoutes.controllers.FormatAPI.deleteContent(content._id).url);
	};
	
	service.deleteGroup = function(group) {
		return server.delete(jsRoutes.controllers.FormatAPI.deleteGroup(group._id).url, JSON.stringify(group));
	};
	
	service.codesystems = [
		   { system : "http://loinc.org", label : "enum.codesystems.loinc"},
		   { system : "http://snomed.info/sct", label : "enum.codesystems.snowmed" },
		   { system : "http://midata.coop", label : "enum.codesystems.midata" },
		   { system : "http://midata.coop", label : "enum.codesystems.fhir" }		   
	];
	
		
	return service;
}]);