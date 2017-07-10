var jsonRecords = angular.module('jsonRecords', []);
jsonRecords.factory('server', [ '$http', function($http) {
	
	var service = {};
	
	var rand = function(min,max) {
	   return Math.floor((Math.random() * (max-min)) + min); 
	};
	
	var twodigit = function(d) {
		return d < 10 ? ("0" + d) : d;
	}
	
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
		var t = new Date();	
		return $http.post(service.baseurl+"/api/members/join", data)
		       .then(function(response) {
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
	
	service.loginUserPortal = function(userId) {
		var data = {
			"email" : "user"+userId+"@instant-mail.de", 			
			"password" : "Secret123"
		};			
		var t = new Date();			
		return $http.post(service.baseurl+"/api/members/login", data)
				.then(function(response) {
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
	
	service.loginUserApp = function(userId) {
		var data = {
			"username" : "user"+userId+"@instant-mail.de", 			
			"password" : "Secret123",
			"appname" : "mymobile",
			"secret" : "12345",
			"device" : "abcdefgh"
		};			
		var t = new Date();	
		// submit to server
		return $http.post(service.baseurl+"/v1/auth", data)
		  .then(function(response) {
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
		return $http.post(service.baseurl+ "/v1/records/create", data)
		.then(function(response) {
		    	  //service.out += ".";
		    	  service.requests++;
		    	  service.time += new Date().getTime() - t.getTime(); 
		    	  return response.data; 
		       }, function(response) {
		    	  service.out += "F";
		    	  service.requests++;
		    	  service.time += new Date().getTime() - t.getTime();
			      return response.data;
		       });;
	};
	
	service.wipeUser = function(session, userId) {
		var data = {
			"email" : "user"+userId+"@instant-mail.de", 			
			"password" : "Secret123"
		};			
		var t = new Date();			
		return $http({
			  "method" : "DELETE",
			  "url" : service.baseurl+"/api/shared/users/wipe",
			  "headers" : {
				  "X-Session-Token" : session.sessionToken
			  }
		})
		.then(function(response) {
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
		
		
		
	return service;	
}]);
jsonRecords.controller('CreateCtrl', ['$scope', '$http', '$location', '$filter', 'server',
	function($scope, $http, $location, $filter, server) {
		
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
		
		$scope.setup = { usersCreate : 1, numCreate : 1, numSync : 1};
				
		$scope.success = false;		
					
		$scope.execute = function() {
												
			server.out = "";
			server.time = server.requests = 0;
			
			var f = function(i) { return function() { return server.createRegister(i); } };
			
			var part = Math.floor($scope.setup.usersCreate / $scope.setup.numSync); 
			for (var x=0;x<$scope.setup.numSync;x++) {
				scedule(part * x, part, f)
				.then(function() { server.out += "ok"; $scope.success = true; });	
			}
									
		};
		
		$scope.execute2 = function() {
			
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
		
        $scope.execute3 = function() {
			
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
							
		
	}
]);
