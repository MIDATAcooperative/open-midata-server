angular.module('services')
.factory('currentUser', function($q, $http) {
	
	var deferred = $q.defer();
	
	$http(jsRoutes.controllers.Users.getCurrentUser()).
	success(function(userId) {
		deferred.resolve(userId);			
	});	
	 		
	return deferred.promise;
});












