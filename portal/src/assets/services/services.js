angular.module('services')
.factory('session', ['$q', 'server', function($q, server) {
	
	var session = {
		currentUser : null,
		cache : {},
			
		login : function() {			
			var def  = $q.defer();		
			server.get(jsRoutes.controllers.Users.getCurrentUser().url).
			success(function(userId) {				
				def.resolve(userId);			
			}).
			error(function() { document.location.href="/#/public/login"; });		
			session.currentUser = def.promise;
		},
		
		logout : function() {
			session.currentUser = null;
			session.cache = {};
		},
		
		cacheGet : function(name) {
			return session.cache[name];
		},
		
		cachePut : function(name, promise) {
			session.cache[name] = promise;
			return promise;
		},
		
		cacheClear : function(name) {
			session.cache[name] = null;
		}
	};
	
	 		
	return session;
}]);












