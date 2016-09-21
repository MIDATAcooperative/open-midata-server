angular.module('fhirDocref')
.controller('PreviewCtrl', ['$scope', '$filter', '$location', 'midataServer', 'midataPortal',
 	function($scope, $filter, $location, midataServer, midataPortal) {
 		
 	    
 		$scope.mode = 'loading';
 		$scope.error = null;
 		$scope.errors = {};
 		$scope.records = [];
 		 			
 		// get the data for the records in this space
 		$scope.getInfos = function() {
 	        midataServer.getSummary(midataServer.authToken, "ALL", { "format" : "fhir/DocumentReference" })
 	        .then(function(result) {
 	        	if (result.data && result.data.length > 0) {
 	        	   $scope.summary = result.data[0]; 	
 	        	   console.log($scope.summary);
 	        	   var newestId = $scope.summary.newestRecord;
 	        	   midataServer.getRecords(midataServer.authToken, { "_id" : newestId, "format" : "fhir/DocumentReference" }, ["name", "data"])
 	        	   .then(function(result2) {
 	        		   $scope.record = result2.data[0];
 	        	   });
 	        	} else {
 	        		$scope.summary = { count : 0 };
 	        	}
 	        }); 			 			
 		};
 		
 		$scope.download = function(record) {
        	document.location.href = midataServer.baseurl+"/v1/plugin_api/records/file?authToken="+encodeURIComponent(midataServer.authToken)+"&id="+encodeURIComponent(record._id);
        };
 		
 		midataPortal.setLink("add", "page", "dist/index.html#/create?authToken=:authToken");
 		       		
 		$scope.getInfos();
 	}
 ]);
