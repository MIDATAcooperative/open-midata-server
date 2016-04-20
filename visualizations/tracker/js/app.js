var tracker = angular.module('tracker', [ 'midata' ]);

tracker.controller('CreateCtrl', ['$scope', '$http', '$location', '$filter', '$timeout', 'midataServer', 'midataPortal',
	function($scope, $http, $location, $filter, $timeout, midataServer, midataPortal) {
		
		// init
		$scope.errors = {};
				
		$scope.codes = [
		   {			    
				display : "Headache",
				system : "urn:uuid:1234",
				code : "headache"
		   },
		   {
			   display : "Tired",
			   system : "urn:uuid:1234",
			   code : "tired"
		   }
		];
		
				
		// get authorization token
		var authToken = $location.path().split("/")[1];
		 
		console.log(authToken);
		$scope.authToken = authToken;		
		$scope.isValid = true;
		$scope.isBusy = 0;
		$scope.success = false;
		midataPortal.autoresize();
		
		$scope.reset = function() {
			$scope.newentry = { 									
					date : $filter('date')(new Date(), "yyyy-MM-dd")
			};	
			$scope.newcode = { display:"" };
			
			midataServer.getConfig(authToken)
			.then(function(response) {
				if (response.data && response.data.codes) {
					$scope.codes = response.data.codes;
				}
			});
			
			$scope.mode = '';
			
		};
				
		$scope.add = function() {
			$scope.success = false;
			console.log("Add");
			
			angular.forEach($scope.codes, function(code) {
				if (!code.selected) return;
			  
			var data = { 
					resourceType : "Observation",
					status : "preliminary",
					category : {
						coding : [{
						  system : "http://hl7.org/fhir/observation-category",
						  code : "survey",
						  display : "Survey"
						}]
					},
					code : {
					  coding : [
					  {
						   system : code.system,
						   code : code.code,
						   display : code.display,
						   userSelected : true
					  },
					  {
					    system : "http://midata.coop",
					    code : "user-observation",
					    display : "User Observation"
					  }],
					  text : code.display
					},
					effectiveDateTime : $scope.newentry.date,					
					valueCodeableConcept : {						
					    coding : [{
						   system : "http://hl7.org/fhir/v2/0136",
						   code : "Y",
						   display : "Yes"
					    }]
					}
			};
															
			$scope.isBusy++;
			midataServer.createRecord(authToken, { "name" : code.display, "content" : "user-observation", format : "fhir/Observation", subformat : "CodeableConcept" }, data)
			.then(function() { 
				$scope.isBusy--; 
				if ($scope.isBusy == 0) {
				  success = true; 
				  $scope.reset(); 
				  $timeout(function() { $scope.success = false; }, 2000); 
			    }
			});			
			});
		};
		
		$scope.addCode = function() {
			var code = $scope.newcode.display.toLowerCase();
			code = code.replace(' ','-');
			$scope.codes.push({ system:"urn:uuid:1234", display : $scope.newcode.display, code : code })
			$scope.newcode = {};
		};
		
		$scope.removeCode = function(idx) {
			$scope.codes.splice(idx,1);
		};
		
		$scope.saveCodes = function() {
			midataServer.setConfig(authToken, { codes : $scope.codes })
			.then(function() { $scope.mode=''; });
		};
					
		$scope.reset();
								
	}
]);
