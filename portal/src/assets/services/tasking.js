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
	  { value : "ONCE", label : "Once" },
	  { value : "DAILY", label : "Daily" },
	  { value : "WEEKLY", label : "Every Week" },
	  { value : "MONTHLY", label : "Every Month" },
	  { value : "YEARLY", label : "Every Year" }	 
	];
			
	return service;
	    	
}]);