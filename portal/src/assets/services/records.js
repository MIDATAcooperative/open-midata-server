angular.module('services')
.factory('records', function(server) {
	var service = {};
	
	service.getRecords = function(aps, properties, fields) {
		var data = {"properties": properties, "fields": fields};
		if (aps != null) data.aps = aps;
		return server.post(jsRoutes.controllers.Records.getRecords().url, JSON.stringify(data));
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
	
	service.shareSpaceWithCircle = function(fromSpace, toCircle) {
		var data = { fromSpace : fromSpace, toCircle : toCircle };
		return server.post(jsRoutes.controllers.Records.share().url, JSON.stringify(data));
	};
	
	service.search = function(query) {
		return server.get(jsRoutes.controllers.Records.search(query).url);
	};
	
	service.getUrl = function(recordId) {
		return server.get(jsRoutes.controllers.Records.getRecordUrl(recordId).url);
	};
	
	return service;
});