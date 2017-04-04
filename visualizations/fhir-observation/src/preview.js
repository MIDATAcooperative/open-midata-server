angular.module('fhirObservation')
.controller('PreviewCtrl', ['$scope', '$filter', '$timeout', 'midataServer', 'midataPortal', 'configuration', 'data', 'fhirinfo',
 	function($scope, $filter, $timeout, midataServer, midataPortal, configuration, data, fhirinfo) {
 			
	
	    $scope.previews = [
	    {	    		
	    	    content : "body/weight",	    		
	    		icon : "weight.png"
	    	},
	    	{	    		
	    	    content : "body/height",	    		
	    		icon : "height.png"
	    	}
	    	
	    ];
	
	    $scope.groupForPreview = function(records) {
	       console.log(records);	
	       var byContent = {};
	       var tiles = [];
	       angular.forEach(records, function(summary) {
	    	   byContent[summary.content] = summary;
	       });
	       angular.forEach($scope.previews, function(prev) {
	    	  var cont = byContent[prev.content]; 
	    	  if (cont) {
	    		 
	    		 tiles.push(
	    			{
	    			   "display" : data.getCodeableConcept(cont.data.code),
	    			   "icon" : prev.icon,
	    			   "index" : tiles.length,
	    			   "value" : cont.data.valueQuantity.value+" "+cont.data.valueQuantity.unit,
	    			   "content" : prev.content
	    			}	   
	    		 );
	    	  } 
	       });
	       tiles.push({
	    	  "display" : "Measures",
	    	  "icon" : "chart.png",
	   	      "index" : tiles.length,
	   	      "value" : records.length
	       });
	       return tiles;	    	 
	    };
	    
	    $scope.getStyle = function(entry) {
	    	return {
	    		"position" : "absolute",
	    		"top" : (Math.floor(entry.index/2) * 70)+"px",
	    		"left" : ((entry.index % 2) * 175) + "px"
	    	};
	    };
	    
	    
 		$scope.init = function() {
 			console.log("INIT");
 			midataPortal.setLink("view", "page", "dist/index.html#/overview?authToken=:authToken", { });	
 			configuration.getConfig().then(function(config) {
 				data.loadSummary(config.measures).then(function(records) {
 					$scope.isEmpty = records.length === 0;
 					$scope.tiles = $scope.groupForPreview(records);
	
 					
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
 			if (record.content) {
 			midataPortal.openLink("page", "dist/index.html#/chart?authToken=:authToken", { measure : record.content });
 			} else {
 				midataPortal.openLink("page", "dist/index.html#/overview?authToken=:authToken", {  });
 			}
 		};
 		
 		$scope.showAddPopup = function(record) {
 			console.log(record);
 			midataPortal.openLink("modal", "dist/index.html#/create?authToken=:authToken", { measure : record.content });
 		};
 		 			 		 		 				 	 		
 		$scope.getLabel = data.getLabel; 		
 		$scope.getCodeableConcept = data.getCodeableConcept;
        $scope.data = data; 
                
 		
}]);