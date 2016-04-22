var tracker = angular.module('tracker', [ 'midata' ]);

tracker.controller('CreateCtrl', ['$scope', '$http', '$location', '$filter', '$timeout', 'midataServer', 'midataPortal',
	function($scope, $http, $location, $filter, $timeout, midataServer, midataPortal) {
		
		// init
		$scope.error = null;
				
		$scope.codes = [
		   {			    
				display : "Headache",
				system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
				code : "headache"
		   },
		   {			    
				display : "Feeling sick",
				system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
				code : "feeling-sick"
		   },
		   {
			   display : "Beginning of menstruation",
			   system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
			   code : "beginning-of-menstruation"
		   },		   
		   {
			   display : "Ate more than I wanted",
			   system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
			   code : "ate-more-than-i-wanted"
		   },
		   {			    
				display : "Thunderstorm outside",
				system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
				code : "thunderstorm-outside"
		   },
		   {			    
				display : "Click 'Edit Event List' and change this list into something useful for you!",
				system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
				code : "dummy"
		   }
		];
		
				
		// get authorization token
		var authToken = $location.path().split("/")[1];
		 
		console.log(authToken);
		$scope.authToken = authToken;		
		$scope.isValid = true;
		$scope.isBusy = 0;
		$scope.success = false;
		$scope.today = $filter('date')(new Date(), "yyyy-MM-dd");
		midataPortal.autoresize();
		
		$scope.reset = function() {
			$scope.newentry = { 									
					date : $scope.today
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
			$scope.error = null;
			var theDate = new Date($scope.newentry.date);
			if (isNaN(theDate)) {
				$scope.error = "Please enter a valid date! (YYYY-MM-DD)";
				return
			}
			
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
					effectiveDateTime : theDate.toJSON(),					
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
			$scope.codes.push({ system:"urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4", display : $scope.newcode.display, code : code })
			$scope.newcode = {};
		};
		
		$scope.removeCode = function(idx) {
			$scope.codes.splice(idx,1);
		};
		
		$scope.saveCodes = function() {
			$scope.error = null;
			if (! $scope.editForm.$valid) return;
			angular.forEach($scope.codes, function(code) { code.selected = false; });
			
			midataServer.setConfig(authToken, { codes : $scope.codes })
			.then(function() { $scope.mode=''; });
		};
					
		$scope.reset();
								
	}
])
.controller('PreviewCtrl', ['$scope', '$http', '$location', '$filter', '$timeout', 'midataServer', 'midataPortal',
    function($scope, $http, $location, $filter, $timeout, midataServer, midataPortal) {

	var authToken = $location.path().split("/")[1];
	 
	$scope.authToken = authToken;			
	$scope.today = $filter('date')(new Date(), "yyyy-MM-dd");
	$scope.tracked = [];
	midataPortal.autoresize();
	
	$scope.preview = function() {
		$scope.tracked = [];
		midataServer.getRecords(authToken, { "format" : "fhir/Observation", "code" : "http://midata.coop user-observation", "index" : { "effectiveDateTime" : { "$ge" : $scope.today }} }, ["data"])
		.then(function(results) {
		  angular.forEach(results.data, function(dat) {
			 if (dat.data.effectiveDateTime && dat.data.code && dat.data.code.coding && dat.data.status && (dat.data.status == "final" || dat.data.status=="preliminary" )) {
				 $scope.tracked.push(dat.data);
			 } 
		  });
		});
	};
	
	$scope.preview();
}]);