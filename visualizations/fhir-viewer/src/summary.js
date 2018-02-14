angular.module('fhirViewer')
.controller('SummaryCtrl', ['$scope', '$state', '$filter', 'midataServer', 'midataPortal',  
 	function($scope, $state, $filter, midataServer, midataPortal) {
	
	
  $scope.loadAll = function() {
	 $scope.empty = true;
	 $scope.sections = [
		 { resource : "AllergyIntolerance", fields : ["code", ["assertedDate", "lastOccurence"], "clinicalStatus", "verificationStatus" ] },
         { resource : "Condition", fields : [ "code", ["assertedDate", "onsetDateTime", "onsetPeriod"], "clinicalStatus", "verificationStatus" ] },
         { resource : "Procedure", fields : [ "code", ["performedDateTime", "performedPeriod"] , "status" ] },         
         /*{ resource : "DiagnosticReport" , fields : [ "code", [ "effectiveDateTime", "effectivePeriod", "status" ]] },*/
         /*{ resource : "Immunization", fields : [ "vaccineCode", "date", "status"] },*/
         { resource : "Goal", fields : ["description", "startDate" , "status"] },
         { resource : "EpisodeOfCare", fields : ["type", "period" , "status"] },
         { resource : "MedicationStatement", fields : [["medicationCodeableConcept" ,"medicationReference"], ["effectiveDateTime", "effectivePeriod"] , "status"] }
	 ]; 
	 angular.forEach($scope.sections, function(section) { $scope.loadSection(section); });
  };
  
  $scope.loadSection = function(section) {	 	 
	midataServer.fhirSearch(midataServer.authToken, section.resource, { patient : midataServer.owner }, true)
	.then(function(results) {
		section.entries = results;
		if (results.length>0) $scope.empty = false;
	});  
  };
  
  $scope.getFieldName = function(entry, field) {	 
	if (!angular.isArray(field)) return field;
	if (entry == null) return field[0];
	for (var i=0;i<field.length;i++) {
		if (entry[field[i]]) return field[i];
	}
	return field[0];
  };	
	
  $scope.getTemplate = function(resource, field) {	
	  if (!resource) return "empty.html";
	  if (resource.reference) return "ref.html";
	  if (resource.text || resource.coding) return "cc.html";
	  if (resource.code || resource.display) return "cc.html";
	  if (resource.start || resource.end) return "period.html";
	return "simple.html";
  };
  
  $scope.go = function(resource) {
	console.log(resource);
	$state.go("resource", { type : resource.resourceType, id : resource.id });  
  };
  	
 
  
  var path0 = $scope.path = function(p,a) {
	return p+"."+a;  
  };
  
  var use = function(path, field) {
	  if (field == "meta" || field=="id" || field=="resourceType") return false;
	  if ((field == "code" || field == "text") && path.indexOf(".")<0) return false;
	  if (field == "linkId") return false;
	  return true;
  };
  
  var isCoding = function(path, v) {
	if (v.code && v.display) return true;
	return false;
  };
  
  var isCCx = function(path, v) {
	 if (v.text && v.coding) return true;
	 if (v.coding && v.coding.length > 0 && isCoding(path, v.coding[0])) return true;
	 return false;
  };
  
  var isIdentifier = function(path,v) {
	  if (v.system && v.value) return true;
	  return false;
  };
  
  var isRef = function(path, v) {
	  if (v.reference) return true;
	  return false;
  };
  
  var isPeriod = function(path,v) {
	  if (path.endsWith("Period")) return true;
	  if (v.start && v.end) return true;
	  return false;
  };
  
  var isDate = function(path,v) {
	  if (angular.isDate(v)) return true;
	  if (angular.isString(v)) {
		  if (/^[0-9][0-9][0-9][0-9]\-[0-9][0-9]\-[0-9][0-9]T/.test(v)) return true;
	  }
	  return false;
  };
  
  var isQuantity = function(path, v) {
	  if (path.endsWith("Quantity")) return true;
	  if (v.value && (v.unit || (v.system && v.code))) return true;
	  return false;
  };
  
  var isAddress = function(path,v) {
	if (path.endsWith(".address")) return true;
	return false;
  };
  
  var isHumanName = function(path, v) {
	if (v.family) return true;
	return false;
  };
        
  
  $scope.label = function(path,k) {
	return $scope.makeLabel(k);  
  };
  
  $scope.makeLabel = function(val) {
      //console.log(val);
      if (val == null) return "Undefined";
      var l = val.replace(/([a-z])([A-Z])/g, '$1 $2');
      return l[0].toUpperCase()+l.substring(1);
    };
    	    
    $scope.makeLabelFromCoding = function(coding) {
    	if (coding.display) return coding.display;
    	return coding.code;
    };
    
   $scope.makeLabelFromCC = function(cc) {
	   if (angular.isArray(cc)) {
		   if (cc.length > 0) return $scope.makeLabelFromCC(cc[0]);
		   return "";
	   }
	   if (cc.text) return cc.text;
	   if (cc.coding) return $scope.makeLabelFromCoding(cc.coding[0]);
	   if (cc.display) return cc.display;
	   if (angular.isArray(cc)) return $scope.makeLabelFromCoding(cc[0]);
	   return "?";
   };
  
   midataServer.getConfig(midataServer.authToken)
   .then(function (res) {
	  
     if (res.data && res.data.readonly) {
 		$state.go("resource", null, { location : "replace" });
     } else {
    	$scope.loadAll();                    
     }  
   });        
    
}]);