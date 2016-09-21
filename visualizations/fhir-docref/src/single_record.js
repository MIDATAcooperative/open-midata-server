angular.module('fhirDocref')
.controller('SingleRecordController', ['$scope', '$filter', '$state', 'midataServer', 'midataPortal', 'configuration', 'data', 'fhirinfo',
 	function($scope, $filter, $state, midataServer, midataPortal, configuration, data, fhirinfo) {
 			   
		$scope.datePickers = {};
	    $scope.dateOptions = {
	       formatYear: 'yyyy',
	       startingDay: 1,	    
	    };
	    $scope.datePopupOptions = {	 	       
	 	   popupPlacement : "auto bottom-right"
	 	};
	    
	    fhirinfo.types.then(function(types) {
   			$scope.types = types;
   		}); 
	
	    $scope.init = function() {
	    	var recordId = $scope.recordId = $state.params.id;
	    	console.log(recordId);
	    	data.getRecords({ ids : recordId })
	    	.then(function(records) {
	    		$scope.record = records[0];
	    		if ($scope.record.data.created) $scope.record.data.created = new Date($scope.record.data.created);
	    	});
	    	
	    };
	    
 		$scope.goBack = function() {
 			$state.go("^.overview");
 		}; 		 		
 		
 		$scope.download = function() {
        	document.location.href = midataServer.baseurl+"/v1/plugin_api/records/file?authToken="+encodeURIComponent(midataServer.authToken)+"&id="+encodeURIComponent($scope.record._id);
        };
        
        $scope.update = function() {
        	midataServer.updateRecord(midataServer.authToken, $scope.record._id, $scope.record.version, $scope.record.data)
        	.then(function() {        	
        	  $scope.init();
        	});
        };
 		
 		$scope.getLabel = data.getLabel;
 		$scope.configuration = configuration; 	
 		$scope.getCodeableConcept = data.getCodeableConcept;
       
        
        $scope.init();
                 		
}]);