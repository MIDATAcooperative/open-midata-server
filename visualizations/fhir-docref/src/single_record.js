angular.module('fhirDocref')
.controller('SingleRecordController', ['$scope', '$http', '$filter', '$state', 'midataServer', 'midataPortal', 'configuration', 'data', 'fhirinfo',
 	function($scope, $http, $filter, $state, midataServer, midataPortal, configuration, data, fhirinfo) {
 			   
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
	    		if ($scope.record && $scope.record.data && $scope.record.data.content && $scope.record.data.content[0].attachment && $scope.record.data.content[0].attachment.contentType==="text/xml") {
	    			$scope.loadXSL($scope.record._id);	    			
	    		}
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
        
        $scope.loadXSL = function(id) {
			$http.get("CDA.xsl").then(function(result) {
				console.log(result.data);
				var parser = new DOMParser();
				$scope.xsl = parser.parseFromString(result.data, "text/xml");				
				$scope.displayCDA(id);
			});
		};
		
		$scope.displayCDA = function(id) {
			$scope.loading++;
			
			$http.get("https://" + window.location.hostname + ":9000/v1/plugin_api/records/file?authToken="+encodeURIComponent(midataServer.authToken)+"&id="+encodeURIComponent(id)).
			then(function(result) {		
				$scope.isCDA = true;
				var parser = new DOMParser();				
				var xml = parser.parseFromString(result.data, "text/xml");				
				var xsltProcessor = new XSLTProcessor();
				xsltProcessor.importStylesheet($scope.xsl);
				var  resultDocument = xsltProcessor.transformToFragment(xml, document);
				document.getElementById("myxml").appendChild(resultDocument);
				$scope.loading--;								
			}, function() { $scope.loading--; });
		};
 		
 		$scope.getLabel = data.getLabel;
 		$scope.configuration = configuration; 	
 		$scope.getCodeableConcept = data.getCodeableConcept;
       
        
        $scope.init();
                 		
}]);