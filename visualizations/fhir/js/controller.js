angular.module('fhir')

.controller('FHIRCtrl', ['$scope', '$http', '$location', '$filter', 'midataServer', 'midataPortal', 'fhirModule',   
	function($scope, $http, $location, $filter, midataServer, midataPortal, fhirModule) {
	    
	    $scope.uriCache = fhirModule.uriCache;
		// init
	    midataPortal.autoresize();
	    var authToken = $location.path().split("/")[1];
	    fhirModule.authToken = authToken;
	    
	    $scope.resource = {};	    	    
	    $scope.returnStack = [];
	    $scope.allResources = [];
	    
	    $scope.pool = fhirModule.pool;
	    $scope.uriCache = fhirModule.uriCache;
	    
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
	    	if (fielddef.type == "code") return "code.html";
	    	if (fielddef.type == "dateTime" || fielddef.type == "instant") return "dateTime.html";
	    	if (fielddef.type == "date") return "date.html";
	    	if (fielddef.type == "positiveInt" || fielddef.type == "unsignedInt" || fielddef.type == "integer" || fielddef.type == "decimal" ) return "string.html";	  
	    	if (fielddef.type == "CodeableConcept") {
	    	   if (fielddef.valueSet != null) {	    		   
	    	      if ($scope.getOptions(fielddef.valueSet).length > 0) {	    		   
	    		     return "CodeableConceptVS.html";
	    	      } else {
	    	    	 return "CodeableConceptFS.html";
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
	    	}
	    };
	    var finish = 0;
	    
	  
	    
	    $scope.makeResource = function(def) {
	    	return fhirModule.createResource(def.resourceType)
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
	    
	    $scope.searchCoding = function(field, fielddef) {
	    	console.log(fielddef);
	    	var cs = fhirModule.getOptionCodeSystem(fielddef.valueSet);
	    	console.log(field.text);
	    	console.log(cs);
	    	midataServer.searchCoding(authToken, { system : cs, "$text" : { "$search" : field.text } }, ["system", "code", "version", "display"])
	    	.then(function(res) {
	    		field.$$fhirSearch = res.data;
	    	});
	    };
	    
	    $scope.getFHIR = function() {
	    	return $scope.resource;
	    };
	    
	    $scope.saveAllModified = fhirModule.saveAllModified;
	    
	    $scope.load = function() {
	    	midataServer.getRecords(authToken, { "format/*" : "fhir" }, ["_id", "name", "created", "lastUpdated", "version", "data"])
	    	.then(function(results) {
	    		angular.forEach(results.data, function(rec) {		    			
	    			fhirModule.addToPool(rec.data, rec);
	    			//$scope.pool[rec._id.$oid] = rec.data;
	    			rec.data.$$fhirUnchanged = true;
	    		});
	    	});
	    };
	        
	    $scope.loadResources = function() {
	    	fhirModule.loadResources()
	    	.then(function() {
	    		$scope.allResources = fhirModule.resourceList(
	    		  ["AllergyIntolerance","Appointment","AppointmentRespone","CarePlan","Claim","ClaimResponse","ClinicalImpression","Communication","CommunicationRequest","Condition","Contract","Coverage","DetectedIssue","DeviceUseRequest","DeviceUseStatement","DiagnosticOrder","DiagnosticReport","DocumentReference","Encounter","EpisodeOfCare","FamilyMemberHistory","Flag","Goal","Immunization","ImmunizationRecommendation","Media","MedicationAdministration","MedicationDispense","MedicationOrder","MedicationStatement","NutritionOrder","Observation","Patient","Procedure","ProcedureRequest","Questionnaire","QuestionnaireResponse","ReferralRequest","RelatedPerson","Schedule","VisionPrescription"]
	    		);		
	    	});
	    }	    
	    
	    $scope.loadResources();
	    $scope.load();
    }    
]);
