angular.module('services')
.factory('tasking', ['server', '$q', function(server, $q) {
	var service = {};

	service.list = function() {       
       return server.post(jsRoutes.controllers.Tasking.list().url);
	};
	
	service.execute = function(taskId) {	       	      
	   return server.post(jsRoutes.controllers.Tasking.execute(taskId).url);
	};
		
	service.add = function(def) {
		return server.post(jsRoutes.controllers.Tasking.add().url, JSON.stringify(def));
	};
	
	service.frequencies = [
	  { value : "ONCE", label : "enum.frequency.ONCE" },
	  { value : "DAILY", label : "enum.frequency.DAILY" },
	  { value : "WEEKLY", label : "enum.frequency.WEEKLY" },
	  { value : "MONTHLY", label : "enum.frequency.MONTHLY" },
	  { value : "YEARLY", label : "enum.frequency.YEARLY" }	 
	];
			
	return service;
	    	
}]);