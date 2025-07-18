/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

import crypto from './crypto';
import server from './server';
import actions from './actions';
import oauth from './oauth';

	
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
					params.loginToken = result.data.sessionToken;
					if (result.data.tryrecover) {
						params.sessionToken = crypto.keyChallengeLocal(result.data.userid, result.data.recoverKey, result.data.challenge);
						if (!params.sessionToken) {
							params.sessionToken="no-recovery";
							params.loginToken = undefined;
							func(params);
							return { data : { requirements : ["KEYRECOVERY"] , status : "BLOCKED" } };
						}
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
		
		retryLogin : function(params) {
			
			return server.post("/v1/continue", params || {});
		},
		
		postLogin : function(result, $router, $route) {			
			session.progress = null;
			if (result.data.sessionToken) {
				sessionStorage.token = result.data.sessionToken;
				console.log("Session started");
			}
			if (result.data.status) {				
				session.progress = result.data;
				  let postregParams = { actions : $route.query.actions };
				  for (let param of ['street','zip','city','phone','mobile']) {
					  if ($route.query[param]) {
						  postregParams[param] = $route.query[param];
					  }
				  }
				  postregParams.ts = Date.now();
				  if (result.data && result.data.requirements && (result.data.requirements.indexOf("APP_CONFIRM")>=0 || result.data.requirements.indexOf("APP_NO_PROJECT_CONFIRM")>=0)) {
					 $router.push({ path : "./oauthconfirm", query : $route.query, location : false });					 
				  } else {	
				     $router.push({ path : "./postregister", query : postregParams , location : false });
		          }		
            } else if (oauth.getAppname()) {
	           oauth.postLogin(result);	
			} else if (result.data.role == "admin") {				
				if (result.data.keyType == 1) {
					$router.push({ name : 'public_developer.passphrase_admin', query : $route.query });
				} else {					
					$router.push({ path : '/admin/stats' });	
				}
			} else if (result.data.role == "developer") {
				if (result.data.keyType == 1) {			
				  $router.push({ name :'public_developer.passphrase', query : $route.query });
			    } else {
 			      $router.push({ name : 'developer.yourapps' });	
			    }
			} else if (result.data.role == "member") {
				if (result.data.keyType == 1) {
				  $router.push({ name : 'public.passphrase', query : $route.query });
				} else {					
				  if ($route.query.actions) {
					  if (actions.showAction($router, $route, "member")) return;
				  }				 
				  $router.push({ name :'member.overview' });
				}
			} else if (result.data.role == "provider") {
				if (result.data.keyType == 1) {
				  $router.push({ name :'public_provider.passphrase', query :$route.query });
				} else {
				  if ($route.query.actions) {
					  if (actions.showAction($router, $route, "provider")) return;
				  }	
				  $router.push({ name :'provider.patientsearch' });
				}
			} else if (result.data.role == "research") {
				if (result.data.keyType == 1) {
				  $router.push({ name :'public_research.passphrase', query : $route.query });
				} else {
				  $router.push({ name :'research.studies' });
				}
			}
		},
		
		login : function(requiredRole) {
			
			session.progress = null;	
			var def  = new Promise((resolve, reject) => {
				server.get(jsRoutes.controllers.Users.getCurrentUser().url).
				then(function(result1) {
					var result = result1.data;
					var userId = result.user;
					if (requiredRole && result.role != requiredRole.toUpperCase()) {
						console.log("Wrong role "+result.role+" vs "+requiredRole.toUpperCase());
						sessionStorage.token = undefined;
						document.location.href="/#/public/login";
						return;
					}
					//$cookies.put("session", userId);
					//session.storedCookie = userId;
					//userId = { "$oid" : userId };			
					var data = {"properties": { "_id" : userId }, "fields": ["email", "firstname", "lastname", "visualizations", "apps", "midataID", "name", "role", "subroles", "developer", "security", "language", "testUserApp", "testUserCustomer"] };
					session.org = result.org;
					server.post(jsRoutes.controllers.Users.get().url, data)
					.then(function(data) {
					session.user = data.data[0];	
					//if (session.user.language) $translate.use(session.user.language);
					resolve(userId);
					});																					
				}, function() {
					console.log("session.login error branch");
					sessionStorage.token = undefined;
					document.location.href="/#/public/login"; 
				});		
			});
		
			session.currentUser = def;
		},
		
		logout : function() {
			console.log("Session ended");
						
			session.progress = null;	
			session.currentUser = null;
			sessionStorage.token = undefined;
			session.org = null;
			session.cache = {};
			actions.logout();
			_states = {};			
			
		},
		 
		debugReturn : function() {		
			console.log("debug - return");
			session.logout();
			sessionStorage.token = sessionStorage.oldToken;
			sessionStorage.oldToken = undefined;						
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
		
		save : function(name, component, fields){			
            if(!_states[name])
                _states[name] = {};
            for(var i=0; i<fields.length; i++){
                _states[name][fields[i]] = JSON.stringify(component.$data[fields[i]]);
            }			
        },
    
        load : function(name, component, fields){        	        	
            if(!_states[name])
                return component;
            for(var i=0; i<fields.length; i++){            	
                if(typeof _states[name][fields[i]] !== 'undefined') {
				  component.$data[fields[i]] = JSON.parse(_states[name][fields[i]]);				
				}
            }
            return component;
        },
        
        map : function(array, name) {
        	var lookup = {};
        	for (var i = 0, len = array.length; i < len; i++) {
        	    lookup[array[i][name]] = array[i];
        	}
        	return lookup;
        },
        
        hasSubRole : function(subRole) {	
    		return session.user && session.user.subroles && session.user.subroles.indexOf(subRole) >= 0;
    	},
    	
    	failurePage : function($router, $route, error) {
    		var reason = null;
    		
    		if (error && error.code) {
    			if (error.code == "error.blocked.joinmethod") reason = "joinmethod";
    			else if (error.code == "error.blocked.projectconsent") reason = "withdrawn";
                else if (error.code == "error.expired.app") reason = "expired_app";
    		}
    		
    		if (!reason) return;
    	    var p = JSON.parse(JSON.stringify($route.query));
    	    p.reason = reason;
    		$router.push({ path : "./failure", query : p });
    	}
    };
	
export default session;











