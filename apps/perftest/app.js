var jsonRecords = angular.module('jsonRecords', []);
jsonRecords.factory('server', [ '$http', function($http) {
	
	var service = {};
	
	var rand = function(min,max) {
	   return Math.floor((Math.random() * (max-min)) + min); 
	};
	
	var twodigit = function(d) {
		return d < 10 ? ("0" + d) : d;
	}
	
	var wrap = function(promise) {
		var t = new Date();
		return promise.then(function(response) {
	    	  //service.out += ".";
	    	  service.requests++;
	    	  service.time += new Date().getTime() - t.getTime(); 
	    	  return response.data; 
	       }, function(response) {
	    	  service.out += "F";
	    	  service.requests++;
	    	  service.time += new Date().getTime() - t.getTime();
		      return response.data;
	     });
	};
	
	service.out = "";
	
	service.requests = 0;
	service.time = 0;
	
	service.createRegister = function(userId) {
		// construct json
		var data = {
			"email" : "user"+userId+"@instant-mail.de", 
			"firstname" : "Vorname"+userId, 
			"lastname" : "Nachname"+userId, 
			"gender" : "MALE", 
			"city" : "Stadt", 
			"zip" : "12345", 
			"country" : "ch", 
			"address1" : "Strasse", 
			"language" : "en",
			"birthday" : "2001-01-01",
			"password" : "Secret123",
			"app" : "mymobile",
			"device" : "abcdefgh"
		};			
			
		return wrap($http.post(service.baseurl+"/api/members/join", data))		       
	};
	
	service.loginUserPortal = function(userId) {
		var data = {
			"email" : "user"+userId+"@instant-mail.de", 			
			"password" : "Secret123"
		};			
		
		return wrap($http.post(service.baseurl+"/api/members/login", data));
	};
	
	service.loginResearchPortal = function() {
		var data = {
			"email" : "research@instant-mail.de", 			
			"password" : "Secret123"
		};			
		
		return wrap($http.post(service.baseurl+"/api/research/login", data));
	};
	
	service.loginUserApp = function(userId) {
		var data = {
			"username" : "user"+userId+"@instant-mail.de", 			
			"password" : "Secret123",
			"appname" : "mymobile",
			"secret" : "12345",
			"device" : "abcdefgh"
		};			
		
		// submit to server
		return wrap($http.post(service.baseurl+"/v1/auth", data));
		  	
	};
	
	service.createRecord = function(userId, session, recordId) {
		var effDate = (rand(2000,2015) +"-" + twodigit(rand(1,12)) + "-" + twodigit(rand(1,28)));
		var t = new Date();	
		var payload = {
				"resourceType" : "Observation",
				"status": "preliminary", 
				"category": [ 
					{ "coding": [ 
						{ "system": "http://hl7.org/fhir/observation-category", 
						  "code": "fitness", 
						  "display": "Fitness Data" 
						}
						] } 
				], 
				"code": { 
					"coding": [ 
						{ 
						  "system": "http://loinc.org", 						
						  "code": "29463-7", 
						  "display": "Weight" 
						} 
					] 
	            }, 
	            "comment" : "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789",
	            "effectiveDateTime": effDate, 
	            "valueQuantity": { 
	            	"value": 81, 
	            	"unit": "kg", 
	            	"system": "http://unitsofmeasure.org", 
	            	"code": "kg" 
	            }	           
		};
		var data = {
			"authToken": session.authToken,
			"data": angular.toJson(payload),
			"name": "Weight "+effDate,
			"format" : "fhir/Observation",
			"content" : "body/weight"
			//,
			//"created-override" : effDate
		};
		
		// submit to server
		return wrap($http.post(service.baseurl+ "/v1/records/create", data));		
	};
	
	service.createBulkRecord = function(userId, session, recordId, count) {
		
		var t = new Date();	
		var payload = function(id) {
			    var effDate = (rand(2000,2016) +"-" + twodigit(rand(1,12)) + "-" + twodigit(rand(1,28)));
                var value = rand(1000,10000);
			    return {
			    	"request" : {
						"method" : "POST",
						"url" : "Observation"
					},			    
			    	"resource" : {								    
						"resourceType" : "Observation",
						"status": "preliminary", 
						"category": [ 
							{ "coding": [ 
								{ "system": "http://hl7.org/fhir/observation-category", 
								  "code": "fitness", 
								  "display": "Fitness Data" 
								}
								] } 
						], 
						"code": { 
							"coding": [ 
								{ 
								  "system": "http://loinc.org", 						
								  "code": "41950-7", 
								  "display": "Steps" 
								} 
							] 
			            }, 
			            "effectiveDateTime": effDate, 
			            "valueQuantity": { 
			            	"value": value, 
			            	"unit": "steps"
			            }
			    	}
			    }
		};
		
		var actions = [];
		for (var i=0;i<count;i++) actions.push(payload(i));
		
		var data =  {
				   "resourceType": "Bundle",
				   "id": "bundle-transaction",
				   "type": "transaction",
				   "entry": actions
		}; 
												
		return wrap($http({
			  "method" : "POST",
			  "url" : service.baseurl+"/fhir",
			  "headers" : {
				  "Authorization" : "Bearer "+session.authToken
			  },
			  "data" : data
		}));
	};
	
	service.wipeUser = function(session, userId) {
		var data = {
			"email" : "user"+userId+"@instant-mail.de", 			
			"password" : "Secret123"
		};			
		var t = new Date();			
		return wrap($http({
			  "method" : "DELETE",
			  "url" : service.baseurl+"/api/shared/users/wipe",
			  "headers" : {
				  "X-Session-Token" : session.sessionToken
			  }
		}));
	};
	
	service.joinStudy = function(session, study) {
		
		return wrap($http({
			"method" : "POST",
			"url" : service.baseurl+"/api/members/participation/"+study+"/request",
			"headers" : {
				  "X-Session-Token" : session.sessionToken
			}
		}));        
	};
	
	service.setGroup = function(researchSession, user, study, group) {
		return wrap($http({
			"method" : "POST",
			"url" : service.baseurl+"/api/research/studies/"+study+"/update",
			"headers" : {
				  "X-Session-Token" : researchSession.sessionToken
			},
			"data" : JSON.stringify({
				"member" : user,
				"group" : group
			})
		}));
	};
	
	service.confirmParticipation = function(researchSession, study, user) {
		return wrap($http({
			"method" : "POST",
			"url" : service.baseurl+"/api/research/studies/"+study+"/approve",
			"headers" : {
				  "X-Session-Token" : researchSession.sessionToken
			},
			"data" : JSON.stringify({
				"member" : user				
			})
		}));
	};
	
	service.listParticipants = function(researchSession, study) {
		return wrap($http({
		  "method" : "GET",
		  "url" : service.baseurl + "/api/research/studies/"+study+"/participants",
		  "headers" : {
			  "X-Session-Token" : researchSession.sessionToken
		  }
		}));
	}; 
		
		
		
	return service;	
}]);
jsonRecords.controller('CreateCtrl', ['$scope', '$http', '$location', '$filter', '$q', 'server',
	function($scope, $http, $location, $filter, $q, server) {
		
	    var rand = function(min,max) {
		   return Math.floor((Math.random() * (max-min)) + min); 
		};
		
		var twodigit = function(d) {
			return d < 10 ? ("0" + d) : d;
		};
		
		var scedule = function(start, count, func) {
			var r = null;
			for (var j=start;j < start + count;j++) {
				r = (r != null) ? r.then(func(j)) : func(j)();
			};	
			return r;
		}; 
	
		$scope.server = server;
		
		server.baseurl = "https://localhost:9000";
		
		$scope.setup = { usersCreate : 1, numCreate : 1, numSync : 1, numBulk : 100, study : "", studyGroup: "all" };
				
		$scope.success = false;		
					
		$scope.executeCreateUsers = function() {
												
			server.out = "";
			server.time = server.requests = 0;
			
			var f = function(i) { return function() { return server.createRegister(i); } };
			
			var part = Math.floor($scope.setup.usersCreate / $scope.setup.numSync); 
			for (var x=0;x<$scope.setup.numSync;x++) {
				scedule(part * x, part, f)
				.then(function() { server.out += "ok"; $scope.success = true; });	
			}
									
		};
		
		$scope.executeCreateRecords = function() {
			
			server.out = "";
			server.time = server.requests = 0;
			
			var f = function(i) { 
				return function() { 
					return server.loginUserApp(i).then(function(session) {
						
						var f2 = function(j) {
							return function() {
								return server.createRecord(i, session, j);
							}
						};
						
						return scedule(0, $scope.setup.numCreate, f2);						
					}); 
				} 
			};
		
			var part = Math.floor($scope.setup.usersCreate / $scope.setup.numSync); 
			for (var x=0;x<$scope.setup.numSync;x++) {
				scedule(part * x, part, f)
				.then(function() { server.out += "ok"; $scope.success = true; });	
			}
						
		};
		
        $scope.executeCreateBulk = function() {
			
			server.out = "";
			server.time = server.requests = 0;
			
			var f = function(i) { 
				return function() { 
					return server.loginUserApp(i).then(function(session) {
						
						var f2 = function(j) {
							return function() {
								return server.createBulkRecord(i, session, j, $scope.setup.numBulk);
							}
						};
						
						return scedule(0, $scope.setup.numCreate, f2);						
					}); 
				} 
			};
		
			var part = Math.floor($scope.setup.usersCreate / $scope.setup.numSync); 
			for (var x=0;x<$scope.setup.numSync;x++) {
				scedule(part * x, part, f)
				.then(function() { server.out += "ok"; $scope.success = true; });	
			}
						
		};
		
        $scope.executeWipeUsers = function() {
			
			server.out = "";
			server.time = server.requests = 0;
			
			var f = function(i) { 
				return function() { 
					return server.loginUserPortal(i).then(function(session) {						
						return server.wipeUser(session);						
					}); 
				} 
			};
		
			var part = Math.floor($scope.setup.usersCreate / $scope.setup.numSync); 
			for (var x=0;x<$scope.setup.numSync;x++) {
				scedule(part * x, part, f)
				.then(function() { server.out += "ok"; $scope.success = true; });	
			}
						
		};
							
		$scope.executeJoinStudy = function() {
			
			server.out = "";
			server.time = server.requests = 0;
			
			var f = function(i) { 
				return function() { 
					return server.loginUserPortal(i).then(function(session) {						
						return server.joinStudy(session, $scope.setup.study);						
					}); 
				} 
			};
		
			var part = Math.floor($scope.setup.usersCreate / $scope.setup.numSync); 
			for (var x=0;x<$scope.setup.numSync;x++) {
				scedule(part * x, part, f)
				.then(function() { server.out += "ok"; $scope.success = true; });	
			}
			
		};
		
        $scope.executeConfirmStudy = function() {
			
			server.out = "";
			server.time = server.requests = 0;
			
			server.loginResearchPortal().then(function(session) {
				server.listParticipants(session, $scope.setup.study).then(function(response) {
					
					var q = $q.when();
					var f = function(i) {
						return function() {
							return server.setGroup(session, i, $scope.setup.study, $scope.setup.studyGroup)
							.then(function() {
								server.confirmParticipation(session, $scope.setup.study, i);
							});
						}
					};
					
					for (var i=0;i<response.length;i++) {
						if (response[i].pstatus == "REQUEST") {
						  q = q.then(f(response[i]._id));
						}
					}
					
					return q;
				});
			});
									
		};
	}
]);
