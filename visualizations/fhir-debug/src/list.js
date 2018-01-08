angular.module('fhirDebug')
.controller('ListCtrl', ['$scope', '$state', '$filter', 'midataServer', 'midataPortal', 'fhirModule', 
 	function($scope, $state, $filter, midataServer, midataPortal, fhirModule) {
	$scope.uriCache = fhirModule.uriCache;

    fhirModule.authToken = midataServer.authToken;
    
    $scope.resource = {};	    	    
    $scope.returnStack = [];
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
   	    
              
    $scope.editResource = function(id) {       	    	 	    	
    	$scope.resource = $scope.pool[id];	    		    		    	
    	$scope.currentDefinition = fhirModule.definitions[$scope.resource.resourceType];
    };
    
           
    $scope.makeResource = function(def) {
    	
    	$state.go("resource", { type : def.resourceType });
    	
    	//$state.go("resource", { type : def.resourceType });
    	    	
    };
    	            
    $scope.saveAllModified = fhirModule.saveAllModified;
        
    $scope.loadResources = function() {
    	fhirModule.loadResources()
    	.then(function() {
    		$scope.allResources = fhirModule.resourceList(
    		  ["AllergyIntolerance","Appointment","AppointmentRespone","CarePlan","Claim","ClaimResponse","ClinicalImpression","Communication","CommunicationRequest","Condition","Contract","Coverage","DetectedIssue","DeviceUseRequest","DeviceUseStatement","DiagnosticOrder","DiagnosticReport","DocumentReference","Encounter","EpisodeOfCare","FamilyMemberHistory","Flag","Goal","Immunization","ImmunizationRecommendation","Media","MedicationAdministration","MedicationDispense","MedicationOrder","MedicationStatement","NutritionOrder","Observation","Patient","Procedure","ProcedureRequest","Questionnaire","QuestionnaireResponse","ReferralRequest","RelatedPerson","Schedule","VisionPrescription"]
    		);		
    	});
    };	 
    
    $scope.selectResource = function(def) {    	
    	$state.go("query", { type : def.resourceType });    	    	    	    	
    };
    
    $scope.loadResources();
    
}]);