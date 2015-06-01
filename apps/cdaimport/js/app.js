var jsonRecords = angular.module('jsonRecords', []);
jsonRecords.factory('server', [ '$http', function($http) {
	
	var service = {};
	
	service.createRecord = function(authToken, name, description, format, data) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"format" : format,
			"description": (description || "")
		};
		
		// submit to server
		return $http.post("https://" + window.location.hostname + ":9000/api/apps/create", data);
	};
	
	service.createConversion = function(authToken, name, description, format, data, appendToId) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"format" : format,
			"description": (description || ""),
			"document" : appendToId,
			"part" : format
		};
		
		// submit to server
		return $http.post("https://" + window.location.hostname + ":9000/api/apps/create", data);
	};
		
		
	return service;	
}]);
jsonRecords.controller('CreateCtrl', ['$scope', '$http', '$location', '$filter', 'server',
	function($scope, $http, $location, $filter, server) {
		
		// init
		$scope.errors = {};
		$scope.data = {};
				
		// get authorization token
		var authToken = $location.path().split("/")[1];
		$scope.authToken = authToken;
		$scope.record = { name : "CDA", description : "Some description", format:"CDA" };
		$scope.isValid = false;
		
		$scope.init = function() {
			 var hFs = function (evt) {
			      var files = evt.target.files; // FileList object
			     
			      var file = files[0]; 
			      var reader = new FileReader();

			       // Closure to capture the file information.
			      reader.onload = (function(theFile) {
			            return function(e) {
			              // Print the contents of the file
			              $scope.$apply(function() { $scope.parseCDA(e.target.result) });
			            };
			          })(file);

			          // Read in the file
			          
			          reader.readAsText(file);			       
			      }

			      document.getElementById('files').addEventListener('change', hFs, false);
		};
		
		var txt = function(node) {
			return node != null ? node.textContent : "unknown";
		};
		
		$scope.parseCDA = function(cdaAsText) {
			var parser = new DOMParser();
			$scope.doc = parser.parseFromString(cdaAsText, "application/xml");
			
			var patientDOM = $scope.doc.querySelector("recordTarget patient");
			if (patientDOM == null) {
				$scope.noCDA = true;
				return;
			}
						
			var familyName = patientDOM.querySelector("name family");
			var givenName = patientDOM.querySelector("name given");
			//var birthtime = patientDOM.querySelector("birthTime");
						
						
			$scope.patient = { 
					name: txt(givenName)+" "+txt(familyName)
			}
			
			//$scope.convert(null, $scope.doc);
			
			$scope.isValid = true;
			$scope.noCDA = false;
			
			
		};
		
		$scope.upload = function() {
			console.log("UPLOAD");
			var data = new FormData(document.forms[0]);
			
			$http.post("https://" + window.location.hostname + ":9000/api/apps/upload", data , { transformRequest: angular.identity, headers: {'Content-Type': undefined} })
			.then(function (result) {
				var id = result.data._id;
				
				//$scope.convert(id, $scope.doc);
				//server.createConversion($scope.authToken, "conversion", "test", "text", {"title": "Test", "content": "This is a test"}, id);
				//console.log("done");
				
				$scope.isValid = false;
				$scope.patient = null;
			});
		};
		
		$scope.convert = function(id, doc) {
			console.log("CONVERT");
			var obs = doc.getElementsByTagName("observation");			
			angular.forEach(obs, function(observation) {
				
				var code = observation.querySelector("code translation");
				var codeEntry = code.getAttribute("code");
				console.log(codeEntry);
				if (codeEntry == null) return;
				
				var vals = observation.querySelectorAll("value");
				angular.forEach(vals, function(val) {
				
					var value = val.getAttribute("value");
					var unit = val.getAttribute("unit");
					if (value != null && unit!=null) {
					   var data = {
							   unit : unit,
							   value : value,
							   code : codeEntry						 
					   };
					   console.log(data);
					}
				
				});
				
				
			});
		};
		
		$scope.init();
								
	}
]);
