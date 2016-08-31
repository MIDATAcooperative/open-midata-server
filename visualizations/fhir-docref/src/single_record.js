angular.module('fhirDocref')
.controller('SingleRecordController', ['$scope', '$filter', '$state', 'midataServer', 'midataPortal', 'configuration', 'data', 'fhirinfo',
 	function($scope, $filter, $state, midataServer, midataPortal, configuration, data, fhirinfo) {
 			    	    
	    $scope.init = function() {
	    	var recordId = $scope.recordId = $state.params.id;
	    	console.log(recordId);
	    	data.getRecords({ ids : recordId })
	    	.then(function(records) {
	    		$scope.record = records[0];
	    	});
	    	
	    };
	    
 		$scope.showAll = function() {
 			$state.go("^.chart", { measure : $scope.record.content });
 		}; 		 		
 		 		 
 		$scope.getLabel = data.getLabel;
 		$scope.configuration = configuration; 	
 		$scope.getCodeableConcept = data.getCodeableConcept;
       
        
        $scope.init();
                 		
}]);