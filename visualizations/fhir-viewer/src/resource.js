angular.module('fhirViewer')
.controller('ResourceCtrl', ['$scope', '$state', '$filter', 'midataServer', 'midataPortal',  
 	function($scope, $state, $filter, midataServer, midataPortal) {
	
  $scope.getTemplate = function(field, inarray) {
	if (field.array && !inarray) {
		return "array.html";
	}
	if (field.template) return field.template;
	if (field.object) {
		return "object.html";
	}
	return "simple.html";
  };
  
  $scope.getLabelTemplate = function(field, toplevel) {
	 
	if (field.id.startsWith("value") && field.id != "value" && !field.showlabel) return "nolabel.html";
	if (field.id == "identifier" || field.id == "telecom") return "nolabel.html";
	if (field.object && toplevel && !field.template) return "toplabel.html";
	return toplevel ? "default2.html" : "default.html";  
  };
	
  $scope.probe = function() {
	midataServer.getRecords(midataServer.authToken, {}, ["_id", "format", "owner"]).then(function(result) {
		var l = result.data[0];
		$scope.loadResource(l.format, l.format == "fhir/Patient" ? l.owner : l._id);
	});  
  };
  
  $scope.loadResource = function(type, id) {
	  var resourceType = type.substring(5);
	midataServer.fhirRead(midataServer.authToken, resourceType, id).then(function(result) {
		$scope.resource = result.data;
		$scope.convert($scope.resource);
		console.log($scope.resource);
		//console.log($scope.fields($scope.resource, "Observation"));
	});  
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
    
  
  $scope.convert = function(res, path) {	  	  
	  if (!path) path = res.resourceType;
	  var result = [];
	  var remove = [];
	  angular.forEach(res, function(v,k) {
		 console.log(k);
		 if (use(path,k)) {
	
			 var f = { path : path0(path,k), id : k };
			 var vt = (angular.isArray(v) && v.length) ? v[0] : v; 
	
			 f.label = $scope.label(f.path, k);
			 
			 if (k.startsWith("value") && res.code) {
				 f.label = $scope.makeLabelFromCC(res.code);
				 f.showlabel = true;
				 remove.push("code");				 
			 }
	
			 if (isCCx(f.path, vt)) { f.template = "cc.html"; }	
			 else if (isCoding(f.path, vt)) { f.template = "cc.html"; }
			 else if (isRef(f.path, vt)) { f.template="ref.html"; }
			 else if (isQuantity(f.path, vt)) { f.template="quantity.html"; }
			 else if (isDate(f.path, vt)) { f.template="date.html"; }
			 else if (isPeriod(f.path, vt)) { f.template="period.html"; }
			 else if (isAddress(f.path, vt)) { f.template="address.html"; }
			 else if (isHumanName(f.path, vt)) { f.template="humanname.html"; }
			 else if (isIdentifier(f.path, vt)) { f.template="identifier.html"; }
		
			 if (angular.isArray(v)) {
				 f.array = true;
				 angular.forEach(v, function(v2) {
					if (angular.isObject(v2)) {
						f.object = true;
						$scope.convert(v2, f.path);
					} 
				 });
			 } else if (angular.isObject(v)) {
				f.object = true;
				$scope.convert(v, f.path);
			 }
			 console.log(f);
			 result.push(f);
		 }
		 console.log("end:"+k); 		 
	  });
	  if (remove.length) {
		  angular.forEach(remove, function(r) {
			 for (var i=0;i<result.length;i++)  {
				if (r == result[i].id) result.splice(i,1); 
			 }
		  });
	  }
	  res._fields = result;
	  
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
  
  $scope.probe();
    
}]);