angular.module('services')
.factory('session', ['$q', 'server', function($q, server) {
	
	var _states = {};
	
	var session = {
		currentUser : null,
		user : null,
		cache : {},
			
		postLogin : function(result, $state) {
			if (result.data.sessionToken) {
				sessionStorage.token = result.data.sessionToken;
				console.log("Session started");
			}
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
		
		login : function(requiredRole) {			
			var def  = $q.defer();		
			server.get(jsRoutes.controllers.Users.getCurrentUser().url).
			success(function(result) {
				var userId = result.user;
				if (requiredRole && result.role != requiredRole) {
				   document.location.href="/#/public/login";
				   return;
				}
				//$cookies.put("session", userId);
				//session.storedCookie = userId;
				//userId = { "$oid" : userId };			
				var data = {"properties": { "_id" : userId }, "fields": ["email", "firstname", "lastname", "visualizations", "apps", "midataID", "name", "role", "subroles"] };
				server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data))
				.then(function(data) {
				   session.user = data.data[0];				 
				   def.resolve(userId);
				});																					
			}).
			error(function() { document.location.href="/#/public/login"; });		
			session.currentUser = def.promise;
		},
		
		logout : function() {
			console.log("Session ended");
			session.currentUser = null;
			sessionStorage.token = null;
			session.cache = {};
			_states = {};
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
		},
		
		save : function(name, scope, fields){
            if(!_states[name])
                _states[name] = {};
            for(var i=0; i<fields.length; i++){
                _states[name][fields[i]] = scope[fields[i]];
            }
        },
    
        load : function(name, scope, fields){
        	
        	scope.$on('$destroy', function() {
        		console.log("save: "+name);
                session.save(name, scope, fields);
            });
        	console.log("load: "+name);
            if(!_states[name])
                return scope;
            for(var i=0; i<fields.length; i++){
                if(typeof _states[name][fields[i]] !== 'undefined')
                    scope[fields[i]] = _states[name][fields[i]];
            }
            return scope;
        },
        
        map : function(array, name) {
        	var lookup = {};
        	for (var i = 0, len = array.length; i < len; i++) {
        	    lookup[array[i][name]] = array[i];
        	}
        	return lookup;
        }
    };
	
	
	 		
	return session;
}]);












