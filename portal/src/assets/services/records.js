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
.factory('records', ['server', function(server) {
	var service = {};
	
	service.getRecords = function(aps, properties, fields) {
		var data = {"properties": properties, "fields": fields};
		if (aps != null) data.aps = aps;
		return server.post(jsRoutes.controllers.Records.getRecords().url, JSON.stringify(data));
	};
	
	service.getInfos = function(aps, properties, summarize) {
		var data = {"properties": properties};
		if (aps != null) data.aps = aps;
		if (summarize != null) data.summarize = summarize;
		return server.post(jsRoutes.controllers.Records.getInfo().url, JSON.stringify(data));
	};
	
	service.getRecord = function(recordId) {
		var data = {"_id": recordId };		
		return server.post(jsRoutes.controllers.Records.get().url, JSON.stringify(data));
	};
	
	service.unshare = function(aps, records, type, query) {
	  if (! angular.isArray(aps)) aps = [ aps ];
	  if (records != null && ! angular.isArray(records)) records = [ records ];
	  var data = { records:records, started:[], stopped:aps, type:type, query:query };		
	  return server.post(jsRoutes.controllers.Records.updateSharing().url, JSON.stringify(data));
	};
	
	service.share = function(aps, records, type, query) {
		  if (! angular.isArray(aps)) aps = [ aps ];
		  if (records != null && ! angular.isArray(records)) records = [ records ];
		  var data = { records:records, started:aps, stopped:[], type:type, query:query };		
		  return server.post(jsRoutes.controllers.Records.updateSharing().url, JSON.stringify(data));
	};
	
	service.shareSpaceWithCircle = function(fromSpace, toConsent) {
		var data = { fromSpace : fromSpace, toConsent : toConsent };
		return server.post(jsRoutes.controllers.Records.share().url, JSON.stringify(data));
	};
	
	service.search = function(query) {
		return server.get(jsRoutes.controllers.Records.search(query).url);
	};
	
	service.getUrl = function(recordId) {
		return server.get(jsRoutes.controllers.Records.getRecordUrl(recordId).url);
	};
	
	return service;
}]);