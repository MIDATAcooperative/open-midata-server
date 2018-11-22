angular.module('services')
.factory('session', ['$q', 'server', 'crypto', function($q, server, crypto) {
	
	var _states = {};
	
	var session = {
		currentUser : null,
		user : null,
		cache : {},
			
		performLogin : function(func, params, pw) {
			return func(params).then(function(result) {
				if (result.data == "compatibility-mode") {
					params.nonHashed = pw;
					return func(params);
				} else if (result.data.challenge) {
					if (result.data.tryrecover) {
						params.sessionToken = crypto.keyChallengeLocal(result.data.userid, result.data.recoverKey, result.data.challenge);
						if (!params.sessionToken) return { data : { requirements : ["KEYRECOVERY"] , status : "BLOCKED" } };
						return session.performLogin(func, params, pw);
					} else {
						params.sessionToken = crypto.keyChallenge(result.data.keyEncrypted, pw, result.data.challenge);
						if (result.data.recoverKey && result.data.recoverKey != "null") crypto.checkLocalRecovery(result.data.userid, result.data.recoverKey,result.data.keyEncrypted, pw);
						return func(params);
					}
				} else {
					return result;
				}
			})		
		},
		
		postLogin : function(result, $state) {
			if (result.data.sessionToken) {
				sessionStorage.token = result.data.sessionToken;
				console.log("Session started");
			}
			if (result.data.status) {
				  $state.go("public.postregister", { progress : result.data }, { location : false });			
			} else if (result.data.role == "admin") {
				if (result.data.keyType == 1) {
					$state.go('public_developer.passphrase_admin', $state.params);
				} else {
					$state.go('admin.stats');	
				}
			} else if (result.data.role == "developer") {
				if (result.data.keyType == 1) {			
				  $state.go('public_developer.passphrase', $state.params);
			    } else {
 			      $state.go('developer.yourapps');	
			    }
			} else if (result.data.role == "member") {
				if (result.data.keyType == 1) {
				  $state.go('public.passphrase', $state.params);
				} else {
					console.log($state.params);
				  if ($state.params.action) $state.go("member.service2", $state.params);
				  else $state.go('member.overview');
				}
			} else if (result.data.role == "provider") {
				if (result.data.keyType == 1) {
				  $state.go('public_provider.passphrase', $state.params);
				} else {
				  $state.go('provider.patientsearch');
				}
			} else if (result.data.role == "research") {
				if (result.data.keyType == 1) {
				  $state.go('public_research.passphrase', $state.params);
				} else {
				  $state.go('research.studies');
				}
			}
		},
		
		login : function(requiredRole) {			
			var def  = $q.defer();		
			server.get(jsRoutes.controllers.Users.getCurrentUser().url).
			then(function(result1) {
				var result = result1.data;
				var userId = result.user;
				if (requiredRole && result.role != requiredRole) {
				   document.location.href="/#/public/login";
				   return;
				}
				//$cookies.put("session", userId);
				//session.storedCookie = userId;
				//userId = { "$oid" : userId };			
				var data = {"properties": { "_id" : userId }, "fields": ["email", "firstname", "lastname", "visualizations", "apps", "midataID", "name", "role", "subroles", "developer"] };
				session.org = result.org;
				server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data))
				.then(function(data) {
				   session.user = data.data[0];				 
				   def.resolve(userId);
				});																					
			}, function() { document.location.href="/#/public/login"; });		
			session.currentUser = def.promise;
		},
		
		logout : function() {
			console.log("Session ended");
			session.currentUser = null;
			sessionStorage.token = null;
			session.org = null;
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












