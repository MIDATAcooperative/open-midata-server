angular.module('fhirDebug')
.controller('QueryCtrl', ['$scope', '$state', '$filter', '$httpParamSerializer', 'midataServer', 'midataPortal', 'fhirModule', 
 	function($scope, $state, $filter, $httpParamSerializer, midataServer, midataPortal, fhirModule) {
	$scope.uriCache = fhirModule.uriCache;

    fhirModule.authToken = midataServer.authToken;
    
   
    
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
   	    
       
    $scope.query = {};
    $scope.editQuery = {};
    $scope.resourceType = $state.params.type;
    $scope.searches = fhirModule.searches[$scope.resourceType];
  
    $scope.addToQuery = function(def) {
    	console.log(def);
    	$scope.editQuery[def.code] = { def : def };
    	$scope.change();
    };
    
    $scope.getTemplate = function(def) {
    	if (def.type == "reference") return "query-reference.html";
    	if (def.type == "token") return "query-token.html";
    	if (def.type == "quantity") return "query-quantity.html";
    	return "query-string.html";
    };
    
    $scope.buildQuery = function() {
    	$scope.query = {};
    	angular.forEach($scope.editQuery, function(v,k) {
    		var res = v.value;
    		var key = k;
    		if (v.comparator) res = v.comparator+res;    		
    		if (v.chain) key = k+"."+v.chain;
    		if (v.modifier) key = key+":"+v.modifier;
    		if (v.system) res = v.system+"|"+res;
    		if (v.unitcode) {
    			res = res + "|";
    			if (v.unitsystem) res = res + v.unitsystem;
    			res = res + "|" + unitcode;
    		}
    		$scope.query[key] = res;
    	});
    };
    
    $scope.change = function() {
    	$scope.buildQuery();
    	$scope.queryStr = "?"+$httpParamSerializer($scope.query);
    	if ($scope.queryStr == "?") $scope.queryStr = "";
    };
    
    $scope.runQuery = function() {
    	$scope.buildQuery();
    	fhirModule.query = $scope.query;
    	fhirModule.queryStr = $scope.queryStr;
    	$state.go("results", { query : $scope.queryStr, type : $scope.resourceType });    	
    };
            
}]);