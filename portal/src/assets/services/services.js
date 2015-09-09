angular.module('services')
.factory('session', ['$q', 'server', function($q, server) {
	
	var session = {
		currentUser : null,
			
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
		}
	};
	
	 		
	return session;
}]);












