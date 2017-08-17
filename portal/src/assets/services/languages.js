angular.module('services')
.factory('languages', ['ENV', function(ENV) {
	var service = {};
 
	var arr = function(obj) {
		if (angular.isArray(obj.length)) return obj;
		return obj.split(",");
	};
	
	service.all = [];
	   
	service.array = arr(ENV.languages);
	
	for (var i=0;i<service.array.length;i++) service.all.push({ value : service.array[i], name : "enum.language."+service.array[i].toUpperCase() });
	
	service.countries = arr(ENV.countries);
	
	return service;
}]);