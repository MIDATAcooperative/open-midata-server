var req = require('request-promise');
var request = require('request');
var JSONStream = require('JSONStream');
var es = require('event-stream');
var assert = require('assert');
	
var findNext = function(json) {
	if (!json || !json.link) return null;
	for (var i=0;i<json.link.length;i++) {
		var link = json.link[i];
		if (link.relation == "next") return link.url;
	}
	return null;
}

var post = function(session, url, body) {
	return req({
		   uri : session.baseurl + url,
		   method : "post",
		   json : true,
		   strictSSL : false,
		   body : body,
		   headers : { "Authorization" : "Bearer "+session.authToken, "Prefer" : "return=representation" }
	}).catch(function(error) {
		  console.log("error during POST request to "+url+" error="+error);
		  process.exit();
	});
}

var put = function(session, url, body) {
	return req({
		   uri : session.baseurl + url,
		   method : "put",
		   json : true,
		   strictSSL : false,
		   body : body,
		   headers : { "Authorization" : "Bearer "+session.authToken, "Prefer" : "return=representation" }
	}).catch(function(error) {
		  console.log("error during PUT request to "+url+" error="+error);
		  process.exit();
	});;
}

var get = function(session, url) {
	return req({
		   uri : session.baseurl + url,
		   method : "get",		   
		   strictSSL : false,		
		   headers : { "Authorization" : "Bearer "+session.authToken }
	}).catch(function(error) {
		  console.log("error during GET request to "+url+" error="+error);
		  process.exit();
	});;
}

class Midata {
	
	constructor(session, promise) {
		this.session = session;
		this.promise = promise;
	}
	
	useServer(baseurl) {
		this.session.baseurl = baseurl;
		console.log("MIDATA server: "+baseurl);
		return this;
	}
	
	loginResearcher(user, password, otherRole) {
		var session = this.session;
		
		session.username = user;
		session.password = password;
		session.role = otherRole || "RESEARCH";
		
		assert(session.username, "No username set");
		assert(session.password, "No password set");
		assert(session.baseurl, "Set baseurl with useServer(url) first");
		assert(session.appname, "No appname set");
		assert(session.secret, "No app secret set");
		assert(session.device, "No device set");
		
		var work = function() {
					
			return post(session, "/v1/auth", { 
					   appname : session.appname, 
					   secret : session.secret,
					   username : session.username,
					   role : session.role,
					   password : session.password,
					   device : session.device
			}).then(function(result) {
			  if (!result.status || result.status != "ACTIVE") return Promise.reject();
			  session.authToken = result.authToken;
			  console.log("successfully logged in as '"+session.username+"'");
			}).catch(function(error) {
			  console.log("error during login: "+error);
			  process.exit();
			});
			
		};
		
		return new Midata(this.session, this.promise.then(work));
	}
	
	forEachMatch(resource, query, func) {
		var session = this.session;
						
		var work = function() { return new Promise(function(resolve, reject) {
				
			if (typeof query == "function") query = query();
			
			var matches = 0;
			var pages = 0;
			
			var onepage = function(fromServer) {
			    var nextUrl = null;
			    pages++;
							
				fromServer.pipe(JSONStream.parse('entry.*.resource'))
				.on("header", function(header) {					
					nextUrl = findNext(header);
					if (nextUrl != null) nextUrl = nextUrl.replace("localhost", "localhost:9000");	
					//console.log(nextUrl);
				})
			    .pipe(es.mapSync(function(data) {
			       matches++;
			       func(data);
			    }))
			    .on("end", function() {			    	
			    	if (nextUrl != null) {
			    	  console.log("requested next page:"+(pages+1));
			    	  onepage(request({
							uri : nextUrl,
							method : "get",							
							strictSSL : false,
							headers : { "Authorization" : "Bearer "+session.authToken }
					  }));
			    	  
			    	} else {
			    		console.log("search returned "+matches+" resources");
			    		resolve();
			    	}
			    });			
		    };
		
		    console.log("start search /fhir/"+resource+" criteria:"+JSON.stringify(query || {}));
		    onepage(request({
				uri : session.baseurl + "/fhir/"+resource,
				method : "get",
				qs : query,
				strictSSL : false,
				headers : { "Authorization" : "Bearer "+session.authToken }
			}));
		})};
	      		
		return new Midata(this.session, this.promise.then(work));
	}
	
	createResource(resource) {
		var session = this.session;
		
		var work = function() {
			return post(session, "/fhir/"+resource.resourceType, resource);
		};
		
		return new Midata(this.session, this.promise.then(work));		
	}
	
	modifyDBBundle(func) {
		
	    var session = this.session;	    
		var actions = [];		
		var stats = { created : 0, updated : 0, deleted : 0 };
		var tr = {
			
			create : function(resource) {
				stats.created++;
				actions.push({					
						"resource" : resource,
						"request" : {
							"method" : "POST",
							"url" : resource.resourceType
						}					
				});
			},
			
			update : function(resource) {
				stats.updated++;
				actions.push({				
						"resource" : resource,
						"request" : {
							"method" : "PUT",
							"url" : resource.resourceType+"/"+resource.id
						}				
				});
			}
		};
		
		var work = function() {
			  func(tr);
			
			  if (actions.length > 0) {
				  var tosend = {
						   "resourceType": "Bundle",
					   "id": "bundle-transaction",
					   "type": "transaction",
					   "entry": actions
				  };
				  
				  return post(session, "/fhir", tosend)
				  .then(function() {
					 console.log("modifyDBBundle: "+stats.created+" created, "+stats.updated+" updated, "+stats.deleted+" deleted."); 
				  });
			  } else {
				  console.log("modifyDBBundle: nothing to do");
				  return;
			  }
		};
		
		return new Midata(this.session, this.promise.then(work));
	}
	
    modifyDB(func) {
		
	    var session = this.session;	    
	    var cp = Promise.resolve();	
	    var stats = { created : 0, updated : 0, deleted : 0 };
		var tr = {
			
			create : function(resource) {
				stats.created++;
				cp = cp.then(function() {
					return post(session, "/fhir/"+resource.resourceType, resource);					
				});
				
				return cp;
			},
			
			update : function(resource) {
				stats.updated++;
				cp = cp.then(function() {
					return put(session, "/fhir/"+resource.resourceType+"/"+resource.id, resource);
				});
						
				return cp;
			}
		};
		
		var work = function() {
			  func(tr);			
			  return cp.then(function() {
				  console.log("modifyDB: "+stats.created+" created, "+stats.updated+" updated, "+stats.deleted+" deleted.");
			  });
		};
		
		return new Midata(this.session, this.promise.then(work));
	}
	
	then(func) {
		var work = function() { return func(); };
		
		return new Midata(this.session, this.promise.then(work));
	}
}

exports.app = function(appname, secret, device) {
	return new Midata({ appname : appname, secret : secret, device : device }, Promise.resolve());
};
