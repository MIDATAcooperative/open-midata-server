var tracker = angular.module('tracker', [ 'midata', 'ui.bootstrap', 'pascalprecht.translate' ]);
tracker.config(['$translateProvider', function($translateProvider) {	    
			    
		$translateProvider
		.useSanitizeValueStrategy('escape')	   	    
		.registerAvailableLanguageKeys(['en', 'de', 'it', 'fr'], {
		  'en_*': 'en',
		  'de_*': 'de',
		  'fr_*': 'fr',
		  'it_*': 'it',
		})
		.translations('en', en)
		.translations('de', de)
		.translations('it', it)
		.translations('fr', fr)
		.fallbackLanguage('en');
}]);
tracker.run(['$translate', 'midataPortal', function($translate, midataPortal) {
 	console.log("Language: "+midataPortal.language);
	$translate.use(midataPortal.language);	   	  
}]);
tracker.controller('CreateCtrl', ['$scope', '$http', '$location', '$filter', '$timeout', '$translate', 'midataServer', 'midataPortal',
	function($scope, $http, $location, $filter, $timeout, $translate, midataServer, midataPortal) {
		
		// init
		$scope.error = null;
		$scope.status = { mode : "view "};
		$scope.datePickers = {};
	    $scope.dateOptions = {
	       formatYear: 'yy',
	       startingDay: 1
	    };
	    
	    var create_code = function(display) {
	    	var code = display.toLowerCase();
			return code.replace(' ','-');			
	    };

	    $translate(["example1", "example2", "example3", "example4", "example5", "example6" ])
	    .then(function(t) {
		$scope.codes = [
		   {			    
				display : t.example1,
				system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
				code : create_code(t.example1)
		   },
		   {			    
				display : t.example2,
				system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
				code : create_code(t.example2)
		   },
		   {			    
				display : t.example3,
				system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
				code : create_code(t.example3)
		   },
		   {			    
				display : t.example4,
				system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
				code : create_code(t.example4)
		   },
		   {			    
				display : t.example5,
				system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
				code : create_code(t.example5)
		   },
		   {			    
				display : t.example6,
				system : "urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4",
				code : create_code(t.example6)
		   }
		];
	    });
		
				
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
						
			
			$scope.status.mode = '';
			
		};
		
		
				
		$scope.add = function() {
			$scope.success = false;
			$scope.error = null;
			$scope.isBusy = 0;
			var theDate = new Date($scope.newentry.date);
			if (isNaN(theDate)) {
				$scope.error = "Please enter a valid date! (YYYY-MM-DD)";
				return;
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
			midataServer.createRecord(authToken, { "name" : code.display, "content" : "user-observation", format : "fhir/Observation" }, data)
			.then(function() { 
				$scope.isBusy--; 
				if ($scope.isBusy === 0) {
				  $scope.success = true; 
				  $scope.reset(); 
				  $timeout(function() { $scope.success = false; }, 2000); 
			    }
			});			
			});
		};
		
		$scope.addCode = function() {
			var code = $scope.newcode.display.toLowerCase();
			code = code.replace(' ','-');
			$scope.codes.push({ system:"urn:uuid:817a5c29-b5da-4074-81bf-92d6978759f4", display : $scope.newcode.display, code : code });
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
			.then(function() { $scope.status.mode=''; });
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
		midataServer.getRecords(authToken, { "format" : "fhir/Observation", "code" : "http://midata.coop user-observation", "index" : { "effectiveDateTime" : { "!!!ge" : $scope.today }} }, ["data"])
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