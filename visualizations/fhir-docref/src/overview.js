angular.module('fhirDocref')
.controller('OverviewCtrl', ['$scope', '$filter', 'midataServer', 'midataPortal', 'configuration', 'data', 'fhirinfo',
 	function($scope, $filter, midataServer, midataPortal, configuration, data, fhirinfo) {
 			    	     	    
 		$scope.init = function() {
 			console.log("INIT");
 			
 			data.getRecords({}).then(function(records) {
 				$scope.records = records; //categories = data.groupByCategory(records);
 				midataPortal.setLink("view", "hide", "", { });	
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
 		
 		$scope.showAddPopup = function() {
 			
 			midataPortal.openLink("modal", "dist/index.html#/create?authToken=:authToken");
 		};
 		 			 		 		
 		$scope.changePerson = function() {
 			$scope.changeperson = true;
 		}; 		 		
 		
 		$scope.getLabel = data.getLabel; 		
 		$scope.getCodeableConcept = data.getCodeableConcept;
        $scope.data = data; 
        
        $scope.download = function(record) {
        	document.location.href = midataServer.baseurl+"/v1/plugin_api/records/file?authToken="+encodeURIComponent(midataServer.authToken)+"&id="+encodeURIComponent(record._id);
        };
                
 		
}]);