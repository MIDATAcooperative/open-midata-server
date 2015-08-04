angular.module('services')
.factory('currentUser', function($q, server) {
	
	var deferred = $q.defer();
	console.log("INIT");
	server.get(jsRoutes.controllers.Users.getCurrentUser().url).
	success(function(userId) {
		console.log("RESULT");
		console.log(userId);
		deferred.resolve(userId);			
	});	
	 		
	return deferred.promise;
});












