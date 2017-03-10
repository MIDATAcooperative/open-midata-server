angular.module('fhirObservation')
.controller('OverviewCtrl', ['$scope', '$filter', '$timeout', '$state', 'midataServer', 'midataPortal', 'configuration', 'data', 'fhirinfo',
 	function($scope, $filter, $timeout, $state, midataServer, midataPortal, configuration, data, fhirinfo) {
 			    	     
	    $scope.allPreviews = data.allPreviews;
	    
 		$scope.init = function() {
 			console.log("INIT");
 			 				
 				data.loadSummary().then(function(records) {
 					$scope.isEmpty = records.length === 0;
 					$scope.records = records;
 					var img = {};
 					angular.forEach(data.allPreviews, function(p) { img[p.content] = p; });
 					angular.forEach($scope.records, function(r) { r.img = img[r.content].icon; r.label = img[r.content].display; });
 					//midataPortal.setLink("view", "hide", "", { });	
 					//$timeout(function() { midataPortal.resize(); }, 0);
 				}); 	 				 				
 			 						 
 		};
 		
 		$scope.show = function(rec) {
 			midataPortal.openApp("page", "fitness", { measure : rec.content });
 			//midataPortal.openApp("page", "fhir-observation", { measure : rec.content, path :"/chart" });
 		};
 		$scope.init(); 		 		
 	 		
 			
 		
 		$scope.getLabel = data.getLabel; 		
 		$scope.getCodeableConcept = data.getCodeableConcept;
        $scope.data = data; 
                
 		
}]);