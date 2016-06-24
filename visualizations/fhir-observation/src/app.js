var fhir = angular.module('fhir', [ 'midata', 'pascalprecht.translate' ]);
fhir.config(['$translateProvider', function($translateProvider) {	    
    
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
fhir.run(['$translate', 'midataPortal', function($translate, midataPortal) {
	console.log("Language: "+midataPortal.language);
    $translate.use(midataPortal.language);	   	  
}]);
fhir.controller('ObservationCtrl', ['$scope', '$filter', '$location', 'midataServer', 'midataPortal',
 	function($scope, $filter, $location, midataServer, midataPortal) {
 		
 	    midataPortal.autoresize();
 	     	
 		$scope.records = [];
 		
 		var authToken = $location.path().split("/")[1];
 		console.log(authToken);
 				
 		$scope.getRecords = function() { 	
 			midataServer.getRecords(authToken, { "format" : "fhir/Observation" }, ["name", "data"])
 			.then(function(results) {
 				$scope.records = results.data; 				
 			}, function(err) {
 				$scope.error = "Failed to load records: " + err.data; 			
 			});
 		};
 		
 		$scope.getCodeableConcept = function(what) {
 			if (what == null) return null;
 			if (what.text) return what.text;
 			if (what.coding && what.coding.length > 0) {
 				return $scope.getCoding(what.coding[0]); 				
 			}
 			return "?";
 		};
 		
 		$scope.getCoding = function(what) {
 			if (what == null) return null;
 			if (what.display != null) return what.display;
			return what.code;
 		};
 		
 		$scope.getReference = function(what) {
 			if (what == null) return null;
 			if (what.display) return what.display;
 			return what.reference;
 		};
 		 	
 		$scope.getRecords();
 		
}]);