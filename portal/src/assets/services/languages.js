angular.module('services')
.factory('languages', function() {
	var service = {};
 
	service.all = [
	   {
		  value : "en",
		  name : "enum.language.EN"
	   },
	   {		   	   
		  value : "de",
		  name : "enum.language.DE"
	   },
	   {
		  value : "fr",
		  name : "enum.language.FR"
	   },
	   {
		  value : "it",
		  name : "enum.language.IT"
	   }
	];
	
	return service;
});