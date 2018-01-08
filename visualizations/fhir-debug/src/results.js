angular.module('fhirDebug')
.controller('ResultsCtrl', ['$scope', '$state', '$filter', 'midataServer', 'midataPortal', 'fhirModule', 
 	function($scope, $state, $filter, midataServer, midataPortal, fhirModule) {
	$scope.uriCache = fhirModule.uriCache;

    fhirModule.authToken = midataServer.authToken;
    
    $scope.resource = {};	    	       
    $scope.allResources = [];
    
    $scope.pool = fhirModule.pool;
    $scope.uriCache = fhirModule.uriCache;
    
    
    
    $scope.makeLabel = fhirModule.makeLabel;	    	    
    $scope.makeHRName = fhirModule.makeHRName;
    $scope.addResource = fhirModule.addResource;
    $scope.getResource = fhirModule.getResource;
    $scope.removeResource = fhirModule.removeResource;
    $scope.getDefinition = fhirModule.getDefinition;
    $scope.getOptions = fhirModule.getOptions;
    $scope.chooseOption = fhirModule.chooseOption;	    
    $scope.initOption = fhirModule.initOption;
    	    	    
    $scope.pools = fhirModule.pools;
   	    
              
    $scope.editResource = function(res) {      	    			
		fhirModule.addToPool(res);    			
		res.$$fhirUnchanged = true;			
		$state.go("resource", { id : res.resourceType+"/"+res.id });			    	    
    };
    
           
    $scope.makeResource = function(def) {
    	$state.go("resource", { type : def.resourceType });
    	/*return fhirModule.createResource(def.resourceType)
    	.then(function(newResource) {
	    	$scope.resource = newResource;
    	    $scope.currentDefinition = fhirModule.definitions[$scope.resource.resourceType];
    	    fhirModule.addToPool($scope.resource);	    	    
    	    console.log($scope.currentDefinition);
    	    return newResource;
    	});*/
    };
    	            
    $scope.saveAllModified = function() {
    	midataServer.fhirTransaction(midataServer.authToken, $scope.transaction);
    	
    };
    
    $scope.init = function() {    
    	$scope.query = fhirModule.query; // Hack
    	$scope.queryStr = fhirModule.queryStr;
    	$scope.resourceType = $state.params.type;
    	$scope.runQuery();    	
    };
    
    $scope.runQuery = function() {
    	midataServer.fhirSearch(midataServer.authToken, $scope.resourceType, $scope.query)
    	.then(function(result) {
    		$scope.bundle = result.data;
    		$scope.results = [];
    		angular.forEach(result.data.entry, function(entry) {
    			$scope.results.push(entry.resource);
    		});    		
    	}, function(error, more) {
    		console.log(error);
    		console.log(more);
    		$scope.bundle = error;
    		$scope.results = [];
    	});
    };
            
    $scope.init();
            
}]);