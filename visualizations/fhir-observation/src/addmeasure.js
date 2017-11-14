angular.module('fhirObservation')
.controller('AddMeasureCtrl', ['$scope', '$filter', '$timeout', '$state', 'midataServer', 'midataPortal', 'configuration', 'data', 'fhirinfo',
 	function($scope, $filter, $timeout, $state, midataServer, midataPortal, configuration, data, fhirinfo) {
 			    	     	    
 		$scope.init = function() { 	
 			
 			configuration.getConfig().then(function(config) {
 				$scope.config = config;
 			
 			
	 			fhirinfo.getInfos(midataPortal.language)
				.then(function(infos) {
							var result = [];
							angular.forEach(infos, function(info) { result.push({ content : info.content, data : { code:info.code } }); });
							$scope.categories = data.groupByCategory(result);						
				});
 			
 			}); 			
 			 						 	
 		};
 		
 		$scope.init(); 		 		
 	 		 		 		
 		$scope.showAddPopup = function(record) {
 			console.log(record);
 			$state.go("^.create", { measure : record.content });
 			
 			//midataPortal.openLink("modal", "dist/index.html#/create?authToken=:authToken", { measure : record.content });
 		};
 		 			 		 		
 		 		 	 		
 		$scope.getLabel = data.getLabel; 		
 		$scope.getCodeableConcept = data.getCodeableConcept;
        $scope.data = data; 
                
 		
}]);