var midata = angular.module('midata', []);
midata.factory('midataServer', [ '$http', '$q', function($http, $q) {
	
	var service = {};
	
	var actionDef = $q.defer();
	var actionChain = actionDef.promise;
	actionDef.resolve();
	
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
		var f = function() { return $http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/records/create", data); };
		actionChain = actionChain.then(f);	
		return actionChain;
	};
	
	/*
	service.createConversion = function(authToken, name, description, content, format, data, appendToId) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"content" : content,
			"format" : format,
			"description": (description || ""),
			"document" : appendToId,
			"part" : format
		};
		
		// submit to server
		return $http.post("https://" + window.location.hostname + ":9000/api/apps/create", data);
	};
	*/
	
	service.getRecords = function(authToken, properties,fields) {
		 var data = { "authToken" : authToken, "properties" : properties, fields : fields };		
		 return $http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/records/search", data);
	};
	
	service.getSummary = function(authToken, level, properties) {
		 var data = { "authToken" : authToken, "properties" : ( properties || {} ), "summarize" : level.toUpperCase() };		
		 return $http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/records/summary", data);
	};
	
	service.getConfig = function(authToken) {
		 var data = { "authToken" : authToken  };		
		 return $http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/config/get", data);
	};
	
	service.setConfig = function(authToken, config) {
		 var data = { "authToken" : authToken, "config" : config  };		
		 return $http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/config/set", data);
	};
	
	service.cloneAs = function(authToken, name, config) {
		 var data = { "authToken" : authToken, "name" : name, "config" : config };		
		 return $http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/clone", data);
	};
	
	service.oauth2Request = function(authToken, url) {	
		var data = { "authToken": authToken, "url": url };		
	    return $http.post("https://" + window.location.hostname + ":9000/v1/plugin_api/request/oauth2", data);
	};
	
	return service;	
}]);
midata.factory('midataPortal', [ '$window', '$interval', function($window, $interval) {
	
	var service = {};
	var height = 0;
	
	service.autoresize = function() {		
		$window.setInterval(function() { service.resize(); return true; }, 300);
	};
	
	service.resize = function() {
		//var newheight1 = $window.document.documentElement.scrollHeight+"px";
		var newheight = $window.document.documentElement.offsetHeight+"px";
		//console.log(newheight1);
		//console.log(newheight);
		//if (newheight1 > newheight) newheight = newheight1;		
		if (newheight !== height) {				  
		  $window.parent.postMessage({ type: "height", viewHeight : newheight }, "*");		
		  height = newheight;
		}
	};
	
	return service;
}]);