var jsonRecords = angular.module('jsonRecords', [ 'midata', 'pascalprecht.translate' ]);
jsonRecords.config(['$translateProvider', function($translateProvider) {	    
			    
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
jsonRecords.run(['$translate', 'midataPortal', function($translate, midataPortal) {
 	console.log("Language: "+midataPortal.language);
	$translate.use(midataPortal.language);	   	  
}]);
jsonRecords.controller('CreateCtrl', ['$scope', '$http', '$location', '$filter', '$timeout', 'midataServer', 'midataPortal',
	function($scope, $http, $location, $filter, $timeout, midataServer, midataPortal) {
		
		// init
		$scope.errors = null;
				
		$scope.formats = [
		   {
				label : "Weight",
				unit : "kg",
				content : "body/weight",
				objkey : "weight",
				code : {
				    "text": "Body weight Measured",
				    "coding": [
				      {
				        "system": "http://loinc.org",
				        "display": "Body weight Measured",
				        "code": "3141-9"
				      }
				    ]
				}
		   },
		   {
			   label : "Height",
			   unit : "cm",
			   content : "body/height",
			   objkey : "height",
			   code : {
				  "text": "Body height",
				  "coding": [
				      {
				        "system": "http://loinc.org",
				        "display": "Body height",
				        "code": "8302-2"
				      }
				  ]
			  }
		   },		  
		   {
			   label : "Heartrate",
			   unit : "{beats}/min",
			   content : "activities/heartrate",
			   objkey : "heartrate",
			   code : {
				    "text": "Heart rate",
				    "coding": [
				      {
				        "system": "http://loinc.org",
				        "display": "Heart rate",
				        "code": "8867-4"
				      }
				    ]
			  }
		   },		  
		   {
			   label : "Body Temperature",
			   unit : "Cel",
			   content : "body/temperature",
			   objkey : "temperature",
			   code : {
				    "text": "Body temperature",
				    "coding": [
				      {
				        "system": "http://loinc.org",
				        "display": "Body temperature",
				        "code": "8310-5"
				      }
				    ]
			   }
		   },		  
		   {
			   label : "Body Mass Index",
			   unit : "kg/m2",
			   content : "body/bmi",
			   objkey : "bmi",
			   code : {
				    "text": "Body mass index (BMI) [Ratio]",
				    "coding": [
				      {
				        "system": "http://loinc.org",
				        "display": "Body mass index (BMI) [Ratio]",
				        "code": "39156-5"
				      }
				    ]
			   }
		   },		  
		   {
			   label : "Systolic blood pressure",
			   unit : "mm[Hg]",
			   content : "body/blood/systolic",
			   objkey : "bmi",
			   code : {
				    "text": "Systolic blood pressure",
				    "coding": [
				      {
				        "system": "http://loinc.org",
				        "display": "Systolic blood pressure",
				        "code": "8480-6"
				      }
				    ]
				  }
		   },		  
		   {
			   label : "Diastolic blood pressure",
			   unit : "mm[Hg]",
			   content : "body/blood/diastolic",
			   objkey : "bmi",
			   code : {
				    "text": "Diastolic blood pressure",
				    "coding": [
				      {
				        "system": "http://loinc.org",
				        "display": "Diastolic blood pressure",
				        "code": "8462-4"
				      }
				    ]
				}
		   }
		];		
				
		// get authorization token
		var authToken = $location.path().split("/")[1];
		var preselect = $location.path().split("/")[2];
		var preselectFormat = $filter('filter')($scope.formats,{ format : preselect });
		if (preselectFormat.length === 0) preselectFormat = $scope.formats[0]; else preselectFormat = preselectFormat[0]; 
		console.log(authToken);
		$scope.authToken = authToken;		
		$scope.isValid = true;
		$scope.isBusy = false;
		$scope.success = false;
		
		midataPortal.autoresize();
		
		$scope.reset = function() {
			$scope.newentry = { 
					format : preselectFormat,
					value : 0,
					context : "",
					date : $filter('date')(new Date(), "yyyy-MM-dd")
			};			
		};
		
		
		$scope.add = function() {
			$scope.success = false;
			$scope.error = $scope.errorValue = null;
			var theDate = new Date($scope.newentry.date);
			if (isNaN(theDate)) {
				$scope.error = "Please enter a valid date! (YYYY-MM-DD)";
				return;
			}
			if ($scope.newentry.value > 0) { } else {
				$scope.errorValue = "Please enter a valid value!";
				return;
			}
			
			var data = { 
					resourceType : "Observation",
					status : "preliminary",
					category : {
						coding : [{
						  system : "http://hl7.org/fhir/observation-category",
						  code : "vital-signs",
						  display : "Vital Signs"
						}],
						text : "Vital Signs"
					},
					code : $scope.newentry.format.code,
					effectiveDateTime : $scope.newentry.date,					
					valueQuantity : {
						value : $scope.newentry.value,
						unit : $scope.newentry.format.unit						
					}
			};
			
			if ($scope.newentry.context != null && $scope.newentry.context !== "") data.context = $scope.newentry.context;
			
			var envelope = data;
			//envelope[$scope.newentry.format.objkey] = [ data ];
			
			$scope.isBusy = true;
			midataServer.createRecord(authToken, { "name" : $scope.newentry.format.label, "description" :"Manually entered "+$scope.newentry.format.label, "content" : $scope.newentry.format.content, format : "fhir/Observation" }, envelope)
			.then(function() { $scope.success = true; $scope.isBusy = false; $scope.reset(); $timeout(function() { $scope.success = false; }, 2000); });			
		};
					
		$scope.reset();
								
	}
]);
