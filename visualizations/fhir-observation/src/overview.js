angular.module('fhirObservation')
.controller('OverviewCtrl', ['$scope', '$filter', '$timeout', 'midataServer', 'midataPortal', 'configuration', 'data', 'fhirinfo',
 	function($scope, $filter, $timeout, midataServer, midataPortal, configuration, data, fhirinfo) {
 			    	     	    
 		$scope.init = function() {
 			console.log("INIT");
 			configuration.getConfig().then(function(config) {
 				data.loadSummary(config.measures).then(function(records) {
 					$scope.isEmpty = records.length === 0;
 					$scope.categories = data.groupByCategory(records);
 					midataPortal.setLink("view", "hide", "", { });	
 					$timeout(function() { midataPortal.resize(); }, 0);
 				}); 				
 			});
 			 						 	
 		};
 		
 		$scope.init(); 		 		
 	 		
 		$scope.showSingle = function(record) {
 			$scope.record = record;
 			$scope.mode = "record";
 		};
 		
 		$scope.showDetailsPopup = function(record) {
 			console.log(record);
 			midataPortal.openLink("page", "dist/index.html#/chart?authToken=:authToken", { measure : record.content });
 		};
 		
 		$scope.showAddPopup = function(record) {
 			console.log(record);
 			midataPortal.openLink("modal", "dist/index.html#/create?authToken=:authToken", { measure : record.content });
 		};
 		 			 		 		
 		$scope.changePerson = function() {
 			$scope.changeperson = true;
 		}; 		 		
 		
 		$scope.getLabel = data.getLabel; 		
 		$scope.getCodeableConcept = data.getCodeableConcept;
        $scope.data = data; 
                
 		
}]);