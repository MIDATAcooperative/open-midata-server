angular.module('fhirDebug')
.controller('ResourceCtrl', ['$scope', '$state', '$filter', 'midataServer', 'midataPortal', 'fhirModule', 
 	function($scope, $state, $filter, midataServer, midataPortal, fhirModule) {
	
	$scope.uriCache = fhirModule.uriCache;
        
    $scope.resource = {};	    	    
    $scope.returnStack = fhirModule.returnStack;
    
    $scope.pool = fhirModule.pool;
    $scope.uriCache = fhirModule.uriCache;
    
    $scope.mayAddResource = fhirModule.mayAddResource;
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
	
    $scope.isEmpty = function(resource, fielddef) {
    	if (resource == null) return true;
    	if (resource[fielddef.field] == null || (resource[fielddef.field]!== "" && resource[fielddef.field].length === 0)) return true;
    	return false;
    };
    
    $scope.getTemplate = function(fielddef, resource) {
    	if (fielddef.min == 0 && $scope.isEmpty(resource, fielddef)) return "empty.html";
    	if (fielddef.type == "Period") return "Period.html";
    	if (fielddef.type == "Quantity") {	    		
    		return "Quantity.html";	    
    	}
    	//if (definitions[fielddef.type]) return "single.html";
    	var t = fielddef.type;
    	if (fielddef.max == "*" && (t=="string" || t=="uri" || t=="code" || t=="dateTime" || t == "date" || t=="instant" || t=="positiveInt" || t=="CodeableConcept" || t=="Reference")) return "multi.html";
    	return "single.html";
    };
    		    
    $scope.getDetailTemplate = function(fielddef, resource) {	    	
    	if (fielddef.type == "string" || fielddef.type == "uri" ) {	    		
	    	return "string.html";
    	}
    	if (fielddef.type == "boolean") return "boolean.html";
    	if (fielddef.type == "code") {
    		if ($scope.getOptions(fielddef).length == 0) return "string.html";
    		return "code.html";
    	}
    	if (fielddef.type == "dateTime" || fielddef.type == "instant") return "dateTime.html";
    	if (fielddef.type == "date") return "date.html";
    	if (fielddef.type == "time") return "time.html";
    	if (fielddef.type == "positiveInt" || fielddef.type == "unsignedInt" || fielddef.type == "integer" || fielddef.type == "decimal" ) return "string.html";	  
    	if (fielddef.type == "CodeableConcept") {
    		
    		
    	   if (fielddef.valueSet != null) {	    		   
    	      if ($scope.getOptions(fielddef.valueSet).length > 0) {	    		   
    		     return "CodeableConceptVS.html";
    	      } else {
    	    	 return "CodeableConcept.html";
    	      }
    	   } else {
    		   return "CodeableConcept.html";
    	   }
    	} 
    	if (fielddef.type == "Reference") {	    		
    		return "reference.html";	    
    	}
    		
    	if (fhirModule.definitions[fielddef.type]) {
    		if (fhirModule.definitions[fielddef.type].inline) return "inlinesubform.html";
    		if (fielddef.max == "*") return "msubform.html";
    		return "subform.html";
    	}	    		    		    	
    	return "string.html";
    };
    
    $scope.createReference = function(access, fielddef, type) {
    	fhirModule.createResource(type || fielddef.resource)
    	.then(function(newResource) {
    		//console.log(newResource);
    		fhirModule.addToPool(newResource);		    	
	    	access.reference = newResource.resourceType+"/"+newResource.id;
	    	$scope.returnStack.push($scope.resource);
	    	$scope.resource = newResource;
	    	$scope.currentDefinition = fhirModule.definitions[$scope.resource.resourceType];
    	});
    };
    
    $scope.setReference = function(access, fielddef, idRef) {
    	var newResource = fhirModule.pool[idRef];	    		    	
    	access.reference = idRef;
    	access.display = fhirModule.makeHRName(newResource);
    	//$scope.returnStack.push($scope.resource);
    	//$scope.resource = newResource;
    	//$scope.currentDefinition = fhirModule.definitions[$scope.resource.resourceType];
    };
    
    $scope.followReference = function(access, fielddef) {
    	var id = access.reference;
    	$scope.returnStack.push($scope.resource);
    	$scope.resource = fhirModule.pool[id];	
    	
    	access.display = fhirModule.makeHRName($scope.resource);
    	
    	$scope.currentDefinition = fhirModule.definitions[$scope.resource.resourceType];
    };
    
    $scope.clearReference = function(access, fielddef) {
    	access.reference = undefined;
    	access.display = undefined;	    	
    };
    
    $scope.editResource = function(id) {       	    	 	    	
    	$scope.resource = $scope.pool[id];	    		    		    	
    	$scope.currentDefinition = fhirModule.definitions[$scope.resource.resourceType];
    };
    
    $scope.returnToPrevious = function() {
    	if ($scope.resource != null) {
    		fhirModule.processResource($scope.resource);
    	}
    	
    	if ($scope.returnStack.length > 0) {
    	  $scope.resource = $scope.returnStack.pop();
    	  $scope.currentDefinition = fhirModule.definitions[$scope.resource.resourceType];
    	} else {
    	  $scope.resource = null;
    	  $scope.currentDefinition = null;
    	  $state.go("resources");
    	}
    };
    
    $scope.addCoding = function(cc, system) {
    	if (!cc.coding) cc.coding = [];
    	cc.coding.push({ system: (system || ""), value : "", display : "" });
    };
    
    $scope.addText = function(cc) {
    	cc.text = "Text";
    };
       
    $scope.removeCoding = function(where, coding) {
    	var i = where.indexOf(coding);
    	if (i>=0) {
    		where.splice(i, 1);
    	}
    }; 
    
    $scope.makeResource = function(def) {
    	console.log(def);
    	return fhirModule.createResource(def)
    	.then(function(newResource) {
	    	$scope.resource = newResource;
    	    $scope.currentDefinition = fhirModule.definitions[$scope.resource.resourceType];
    	    fhirModule.addToPool($scope.resource);	    	    
    	    console.log($scope.currentDefinition);
    	    return newResource;
    	});
    };
    	    
    $scope.change = function() {
       $scope.resource.$$fhirUnchanged = false;	
    };
    
    $scope.searchCoding = function(field, fielddef, text) {
    	console.log(fielddef);
    	var cs = fhirModule.getOptionCodeSystem(fielddef.valueSet);
    	console.log(field.text);
    	console.log(cs);
    	midataServer.searchCoding(midataServer.authToken, { system : cs, "$text" : { "$search" : '"'+text.replace('"','')+'"' } }, ["system", "code", "version", "display"])
    	.then(function(res) {
    		field.$$fhirSearch = res.data;
    	});
    };
    
    $scope.getFHIR = function() {
    	return $scope.resource;
    };
    
    $scope.saveAllModified = fhirModule.saveAllModified;
    
    
    if ($state.params.id) {
    	$scope.editResource($state.params.id);
    } else {
    	$scope.makeResource($state.params.type);
    }
    
}]);