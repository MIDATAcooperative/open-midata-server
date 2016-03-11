angular.module('services')
.factory('session', ['$q', 'server', function($q, server) {
	
	var session = {
		currentUser : null,
		user : null,
		cache : {},
			
		postLogin : function(result, $state) {
			if (result.data.status) {
				  $state.go("public.postregister", { progress : result.data }, { location : false });			
			} else if (result.data.role == "admin") {
				if (result.data.keyType == 1) {
					$state.go('public_developer.passphrase_admin');
				} else {
					$state.go('admin.members');	
				}
			} else if (result.data.role == "developer") {
				if (result.data.keyType == 1) {			
				  $state.go('public_developer.passphrase');
			    } else {
 			      $state.go('developer.yourapps');	
			    }
			} else if (result.data.role == "member") {
				if (result.data.keyType == 1) {
				  $state.go('public.passphrase');
				} else {
				  $state.go('member.overview');
				}
			} else if (result.data.role == "provider") {
				if (result.data.keyType == 1) {
				  $state.go('public_provider.passphrase');
				} else {
				  $state.go('provider.patientsearch');
				}
			} else if (result.data.role == "research") {
				if (result.data.keyType == 1) {
				  $state.go('public_research.passphrase');
				} else {
				  $state.go('research.studies');
				}
			}
		},
		
		login : function() {			
			var def  = $q.defer();		
			server.get(jsRoutes.controllers.Users.getCurrentUser().url).
			success(function(userId) {	
				console.log("GOT USERID");
				var data = {"properties": { "_id" : userId }, "fields": ["email", "firstname", "lastname", "visualizations", "apps", "midataID", "name"] };
				server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data))
				.then(function(data) {
				   session.user = data.data[0];
				   console.log("GOT USER");
				   def.resolve(userId);
				});																					
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
			   console.log(data);
			  for (var attr in data) { c[attr] = data[attr]; }
		   });
		   return c;
		}
	};
	
	 		
	return session;
}]);












