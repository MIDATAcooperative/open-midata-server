var midata = angular.module('midata', []);
midata.factory('midataServer', [ '$http', function($http) {
	
	var service = {};
	
	service.createRecord = function(authToken, name, description, content, format, data) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"content" : content,
			"format" : format,
			"description": (description || "")
		};
		
		// submit to server
		return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/create", data);
	};
	
	service.getRecords = function(authToken, properties,fields) {
		 var data = { "authToken" : authToken, "properties" : properties, fields : fields };		
		 return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/records", data);
	};
	
	service.getConfig = function(authToken) {
		 var data = { "authToken" : authToken  };		
		 return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/getconfig", data);
	};
	
	service.setConfig = function(authToken, config) {
		 var data = { "authToken" : authToken, "config" : config  };		
		 return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/setconfig", data);
	};
	
	service.cloneAs = function(authToken, name, config) {
		 var data = { "authToken" : authToken, "name" : name, "config" : config };		
		 return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/clone", data);
	};
	
	return service;	
}]);