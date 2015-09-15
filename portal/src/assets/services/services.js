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
		},
		
		resolve : function(id, callback) {
		   var c = session.cache[id];
		   if (c != null) return c;
		   c = session.cache[id] = {};
		   callback().then(function(data) {
			   data = data.data[0];
			  for (var attr in data) { c[attr] = data[attr]; }
		   });
		   return c;
		}
	};
	
	 		
	return session;
}]);












