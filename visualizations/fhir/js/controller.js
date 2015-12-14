var fhir = angular.module('fhir', [ 'midata', 'ui.bootstrap' ]);
fhir.constant('definitions', {
	"Patient" : {
		resourceType : "Patient",
		parent : "DomainResource",
		fields : [
		   { field : "identifier", card : "0..n", type : "Identifier", description : "An identifier for this patient" },
		   { field : "active", card : "0..1", type : "<boolean>", description : "Whether this patient's record is in active use" },
		   { field : "name", card : "0..n", type : "HumanName", description : "A name associated with the patient" }
		]
	},
	"Organization" : {
	    resourceType : "Organization",
	    parent : "DomainResource",
	    fields : [		  
		   { field : "identifier", card : "0..n",type : "Identifier", description:"Identifies this organization  across multiple systems" },
		   { field : "active", card : "0..1",type : "<boolean>", description:"Whether the organization's record is still in active use" },
		   { field : "type", card : "0..1",type : "CodeableConcept", description:"Kind of organization" },
		   { field : "name", card : "0..1",type : "<string>", description:"Name used for the organization" },
		   { field : "telecom", card : "0..n",type : "ContactPoint", description:"A contact detail for the organization" },
		   { field : "address", card : "0..n",type : "Address", description:"An address for the organization" },
		   { field : "partOf", card : "0..1",type : "Reference", resource:"Organization", description:"The organization of which this organization forms a part" },
		   { field : "contact", card : "0..n",type : "Organization.contact", description:"Contact for the organization for a certain purpose" }		  
		]
	},
	"Organization.contact" : {
		fields : [
           { field :"purpose", card : "0..1",type : "CodeableConcept", description : "The type of contact" },
           { field :"name", card : "0..1",type : "HumanName", description : "A name associated with the contact" },
           { field :"telecom", card : "0..n",type : "ContactPoint", description : "Contact details (telephone, email, etc.)  for a contact" },
           { field :"address", card : "0..1",type : "Address", description : "Visiting or postal addresses for the contact" }
		]
	},			
    "HumanName" : {    
	  "resourceType" : "HumanName",
	  "parent" : "Element",
	  inline : true,
	  fields : [
	    { field : "use", card : "0..1", type : "<code>", valueSet : "nameUse" },
	    { field : "text", card : "0..1", type : "<string>", description : "Text representation of the full name" },
	    { field : "family", card : "0..n", type : "<string>", description : "Family name (often called 'Surname')" },
	    { field : "given", card : "0..n", type : "<string>", description : "Given names (not always 'first'). Includes middle names" },
	    { field : "prefix", card : "0..n", type : "<string>", description : "Parts that come before the name" },
	    { field : "suffix", card : "0..n", type : "<string>", description : "Parts that come after the name" },
	    { field : "period", card : "0..1", type : "Period" , description : "Time period when name was/is in use" }
      ]
	},
	"ContactPoint" : {
 	  "resourceType" : "ContactPoint",
	  fields : [
		{ field : "system", card : "0..1", type : "<code>", valueSet:"contactPointSystem" },
		{ field : "value", card : "0..1", type : "<string>", description : "The actual contact point details" },
		{ field : "use", card : "0..1", type : "<code>", valueSet:"contactPointUse" },
	    { field : "rank", card : "0..1", type : "<positiveInt>", description : "Specify preferred order of use (1 = highest)" },
		{ field : "period", card : "0..1", type : "Period", description: "Time period when the contact point was/is in use" }
	  ]
	},
	"Address" : {
	  "resourceType" : "Address",
	  fields : [			  
	    { field : "use", card : "0..1", type:"<code>", valueSet:"addressUse" },
	    { field : "type", card : "0..1",type:"<code>", valueSet:"addressType" },
	    { field : "text", card : "0..1",type:"<string>", description : "Text representation of the address" },
	    { field : "line", card : "0..n",type:"<string>", description : "Street name, number, direction & P.O. Box etc." },
	    { field : "city", card : "0..1",type:"<string>", description : "Name of city, town etc." },
	    { field : "district", card : "0..1",type:"<string>",description : "District name (aka county)" },
	    { field : "state", card : "0..1",type:"<string>",description : "Sub-unit of country (abbreviations ok)" },
	    { field : "postalCode", card : "0..1",type:"<string>",description : "Postal code for area" },
	    { field : "country", card : "0..1",type:"<string>",description : "Country (can be ISO 3166 3 letter code)" },
	    { field : "period", card : "0..1",type:"Period", description : "Time period when address was/is in use" }
	 ]
	},
	"Period" : {
	  // from Element: extension
	  inline : true,
	  fields : [
	     { field : "start", card : "0..1", type : "<dateTime>", description : "Starting time with inclusive boundary" },
	     { field : "end", card : "0..1", type : "<dateTime>", description : "End time with inclusive boundary, if not ongoing" }
	  ]
	},
	"Identifier" : {		
	   fields : [
	     { field : "use", card : "0..1", type : "<code>", valueSet:"identifierUse" },
	     { field : "type", card : "0..1", type : "CodeableConcept", description: "Description of identifier" },
	     { field : "system", card : "0..1", type : "<uri>", description: "The namespace for the identifier"},
	     { field : "value", card : "0..1", type : "<string>", description: "The value that is unique"},
	     { field : "period", card : "0..1", type : "Period", description: "Time period when id is/was valid for use"},
	     { field : "assigner", card : "0..1", type : "Reference", resource:"Organization", description:"Organization that issued id (may be just text)" }
	  ]
	},
	"CodeableConcept" : {		
		fields : [
          { field : "coding", card : "0..n", type : "Coding", description : "Code defined by a terminology system" }, 
          { field : "text", card : "0..1", type : "<string>", description : "Plain text representation of the concept" }
		]
	},
	"Coding" : {
		inline : true,
		fields : [
		  { field : "system", card : "0..1", type : "<uri>", description : "Identity of the terminology system"},
		  { field : "version", card : "0..1", type : "<string>", description : "Version of the system - if relevant"},
		  { field : "code", card : "0..1", type : "<string>", description : "Symbol in syntax defined by the system"},
		  { field : "display", card : "0..1", type : "<string>", description : "Representation defined by the system"},
		  { field : "userSelected", card : "0..1", type : "<boolean>", description : "If this coding was chosen directly by the user"}
       ]
	}
	  
	  
	
	
});
fhir.constant('valueSets', {
   "nameUse" : [
      { value : "usual", label:"Usual", description : "Known as/conventional/the one you normally use" },
      { value : "official", label : "Official", description : "The formal name as registered in an official (government) registry, but which name might not be commonly used. May be called \"legal name\"." },
      { value : "temp", label : "Temp", description : "A temporary name. Name.period can provide more detailed information. This may also be used for temporary names assigned at birth or in emergency situations."},
      { value : "nickname", label : "Nickname", description : "A name that is used to address the person in an informal manner, but is not part of their formal or usual name" },
      { value : "anonymous", label : "Anonymous", description : "Anonymous assigned name, alias, or pseudonym (used to protect a person's identity for privacy reasons)"},
      { value : "old", label :"Old", description : "This name is no longer in use (or was never correct, but retained for records)"},
      { value : "maiden", label : "Maiden", description : "A name used prior to marriage. Marriage naming customs vary greatly around the world. This name use is for use by applications that collect and store \"maiden\" names. Though the concept of maiden name is often gender specific, the use of this term is not gender specific. The use of this term does not imply any particular history for a person's name, nor should the maiden name be determined algorithmically." }
   ],
   "identifierUse" : [
      { value : "usual", label:"Usual", description : "The identifier recommended for display and use in real-world interactions." },
      { value : "official", label:"Official", description : "The identifier considered to be most trusted for the identification of this item." },
      { value : "temp", label:"Temp", description : "A temporary identifier." },
      { value : "secondary", label:"Secondary", description : "An identifier that was assigned in secondary use - it serves to identify the object in a relative context, but cannot be consistently assigned to the same object again in a different context." }
  ],
  "contactPointSystem" : [
      { value : "phone", label:"Phone", description : "The value is a telephone number used for voice calls. Use of full international numbers starting with + is recommended to enable automatic dialing support but not required."},
      { value : "fax", label:"Fax", description : "The value is a fax machine. Use of full international numbers starting with + is recommended to enable automatic dialing support but not required."},
      { value : "email", label:"Email", description : "The value is an email address."},
      { value : "pager", label:"Pager", description : "The value is a pager number. These may be local pager numbers that are only usable on a particular pager system."},
      { value : "other", label:"URL", description : "A contact that is not a phone, fax, or email address. The format of the value SHOULD be a URL. This is intended for various personal contacts including blogs, Twitter, Facebook, etc. Do not use for email addresses. If this is not a URL, then it will require human interpretation."}
  ],
  "contactPointUse" : [
      { value : "home", label:"Home", description : "A communication contact point at a home; attempted contacts for business purposes might intrude privacy and chances are one will contact family or other household members instead of the person one wishes to call. Typically used with urgent cases, or if no other contacts are available."},
      { value : "work", label:"Work", description : "An office contact point. First choice for business related contacts during business hours."},
      { value : "temp", label:"Temp", description : "A temporary contact point. The period can provide more detailed information."},	
      { value : "old", label:"Old", description : "This contact point is no longer in use (or was never correct, but retained for records)."},
      { value : "mobile", label:"Mobile", description : "A telecommunication device that moves and stays with its owner. May have characteristics of all other use codes, suitable for urgent matters, not the first choice for routine business."}
  ],
  "addressUse" : [
      { value : "home", label:"Home", description : "A communication address at a home."},
      { value : "work", label:"Work", description : "An office address. First choice for business related contacts during business hours."},
      { value : "temp", label:"Temporary", description : "A temporary address. The period can provide more detailed information."},
      { value : "old", label:"Old / Incorrect", description : "This address is no longer in use (or was never correct, but retained for records)."}                  
  ],
  "addressType" : [
      { value : "postal", label:"Postal", description : "Mailing addresses - PO Boxes and care-of addresses."},
      { value : "physical", label:"Physical", description : "A physical address that can be visited."},
      { value : "both", label:"Postal & Physical", description : "An address that is both physical and postal."}
  ]
});
fhir.controller('FHIRCtrl', ['$scope', '$http', '$location', 'midataServer', 'midataPortal', 'definitions', 'valueSets', 
	function($scope, $http, $location, midataServer, midataPortal, valueSets) {
	    var definitions = {};
	    var uriCache = {};
	    $scope.uriCache = uriCache;
		// init
	    midataPortal.autoresize();
	    
	    
	    $scope.resource = {};
	    
	    $scope.pool = {};
	    $scope.returnStack = [];
	    $scope.allResources = [];
	    
	    $scope.isEmpty = function(resource, fielddef) {
	    	if (resource[fielddef.field] == null || (resource[fielddef.field]!== "" && resource[fielddef.field].length === 0)) return true;
	    	return false;
	    };
	    	  
	    
	    $scope.getTemplate = function(fielddef, resource) {
	    	if ((fielddef.card == "0..n" || fielddef.card == "0..1") && $scope.isEmpty(resource, fielddef)) return "empty.html";
	    	if (fielddef.type == "Period") return "Period.html";
	    	//if (definitions[fielddef.type]) return "single.html";
	    	var t = fielddef.type;
	    	if (fielddef.card == "0..n" && (t=="string" || t=="uri" || t=="code" || t=="dateTime" || t == "date" || t=="positiveInt" || t=="CodeableConcept" || t=="Reference")) return "multi.html";
	    	return "single.html";
	    };
	    		    
	    $scope.getDetailTemplate = function(fielddef, resource) {	    	
	    	if (fielddef.type == "string" || fielddef.type == "uri" ) {	    		
		    	return "string.html";
	    	}
	    	if (fielddef.type == "boolean") return "boolean.html";
	    	if (fielddef.type == "code") return "code.html";
	    	if (fielddef.type == "dateTime") return "dateTime.html";
	    	if (fielddef.type == "date") return "date.html";
	    	if (fielddef.type == "positiveInt" || fielddef.type == "unsignedInt" || fielddef.type == "integer" || fielddef.type == "decimal" ) return "string.html";	  
	    	if (fielddef.type == "CodeableConcept" && fielddef.valueSet != null && $scope.getOptions(fielddef.valueSet).length > 0) return "CodeableConceptVS.html";
	    	if (fielddef.type == "Reference") {	    		
	    		return "reference.html";	    
	    	}	    	
	    	if (definitions[fielddef.type]) {
	    		if (definitions[fielddef.type].inline) return "inlinesubform.html";
	    		if (fielddef.card == "0..n") return "msubform.html";
	    		return "subform.html";
	    	}	    		    		    	
	    	return "string.html";
	    };
	    
	    $scope.makeLabel = function(val) {
	      var l = val.replace(/([a-z])([A-Z])/g, '$1 $2');
	      return l[0].toUpperCase()+l.substring(1);
	    };
	    
	    $scope.getDefinition = function(fielddef) {	    	
	    	return definitions[fielddef.type];
	    };
	    
	    $scope.getResource = function(resource, fielddef) {
	    	if (!resource[fielddef.field]) resource[fielddef.field] = {};
	    	return resource[fielddef.field];
	    };
	    
	    $scope.createResource = function(type) {
	    	var def = definitions[type] || uriCache[type];
	    	var newResource = {};
	    	if (def.resourceType) newResource.resourceType = def.resourceType;
	    		    	
	    	angular.forEach(def.fields, function(f) {
	    	 if (f.card == "1..1") $scope.addResource(newResource, f); 
	    	});
	    	
	    	
	    	return newResource;
	    };
	    
	    var cnt = 0;
	    $scope.createId = function() {
	    	return "id"+(cnt++);
	    };
	    
	    $scope.addResource = function(resource, fielddef) {
	    	var newResource = null;
	    	
	    	if (fielddef.type == "string" || fielddef.type == "code" || fielddef.type == "uri") {
	    		newResource = "";
	    	} else if (fielddef.type == "boolean") {
	    		newResource = false;
	    	} else if (fielddef.type == "positiveInt" || fielddef.type == "integer" || fielddef.type == "decimal" || fielddef.type == "unsignedInt") {
	    		newResource = 1;
	    	} else if (fielddef.type == "dateTime" || fielddef.type == "date") {
	    		newResource = "";
	    	} else if (fielddef.type == "CodeableConcept" && fielddef.valueSet != null) {
	    		newResource = { coding : [ { system : fielddef.valueSet, userSelected : true } ], text : "" };
	    	} else if (fielddef.type == "Reference") {
	    		newResource = { display:"" };
	    	} else {
	    	  newResource = {};
	    	  
	    	  var def = definitions[fielddef.type];
	    	  if (def != null) {
	    		  angular.forEach(def.fields, function(f) {
	    			 if (f.card == "1..1") $scope.addResource(newResource, f); 
	    		  });
	    	  }
	    	}
	    	
	    	if (fielddef.card == "0..1" || fielddef.card == "1..1") {
	    		resource[fielddef.field] = newResource;
	    	} else {
	    		if (resource[fielddef.field] == null) {
	    			resource[fielddef.field] = [ newResource ];
	    		} else {
	    			resource[fielddef.field].push(newResource);
	    		}
	    	}
	    };
	    
	    $scope.removeResource = function(parentResource, fielddef, resource) {
	    	var i = parentResource[fielddef.field].indexOf(resource);
	    	if (i>=0) {
	    		parentResource[fielddef.field].splice(i, 1);
	    	}
	    };
	    
	    $scope.mayAddResource = function(resource, fielddef) {
	    	if (fielddef.card == "0..n") return true;
	    	if (!resource[fielddef.field]) return true;
	    	if (resource[fielddef.field].length == 0) return true;
	    	return false;
	    };
	    
	    $scope.addString = function(resource, fielddef) {
	    	if (!resource[fielddef.field]) resource[fielddef.field] = [];
	    	resource[fielddef.field].push("x");
	    	console.log(resource[fielddef.field]);
	    };
	    
	    $scope.checkString = function(resource, fielddef, idx) {
	    	console.log(resource);
	    	if (resource[fielddef.field][idx] == "") {
	    		console.log("checked: "+idx);
	    		resource[fielddef.field].splice(idx, 1);
	    	}
	    };
	    
	    $scope.getOptions = function(fielddef) {
	    	//console.log(fielddef);
	    	var orig = uriCache[fielddef.valueSet || fielddef];
	    	//console.log(orig);
	    	if (orig == null) return [];
	    	if (orig.computed) return orig.computed;
	    	var res = [];	    	
			if (orig.codeSystem) {
				angular.forEach(orig.codeSystem.concept, function (code) {    						
					var entry = { value : code.code, label: code.display, description : code.definition };
					res.push(entry);
				});
			}			
			if (orig.compose) {
				if (orig.compose.import) {
					angular.forEach(orig.compose.import, function(imp) {
					  var other = $scope.getOptions(imp);
					  res.push.apply(res, other);					  
					});
				}				
				if (orig.compose.include) {
					angular.forEach(orig.compose.include, function(inc) {
						if (inc.concept) {
						angular.forEach(inc.concept, function(code) {
							var entry = { value : code.code, label: code.display, description : "" };
							res.push(entry);
						});	
						} else {
							var other = $scope.getOptions(inc.system);
							res.push.apply(res, other);
						}
					});
				} 
									
			}
			orig.computed = res;
	    	return res;
	    };
	    
	    $scope.createReference = function(access, fielddef, type) {
	    	var newResource = $scope.createResource(type || fielddef.resource);
	    	var id = $scope.createId();
	    	$scope.pool[id] = newResource;
	    	access.reference = id;
	    	$scope.returnStack.push($scope.resource);
	    	$scope.resource = newResource;
	    	$scope.currentDefinition = definitions[$scope.resource.resourceType];
	    };
	    
	    $scope.followReference = function(access, fielddef) {
	    	var id = access.reference;
	    	$scope.returnStack.push($scope.resource);
	    	$scope.resource = $scope.pool[id];	    		    		    	
	    	$scope.currentDefinition = definitions[$scope.resource.resourceType];
	    };
	    
	    $scope.returnToPrevious = function() {
	    	if ($scope.returnStack.length > 0) {
	    	  $scope.resource = $scope.returnStack.pop();
	    	  $scope.currentDefinition = definitions[$scope.resource.resourceType];
	    	} else {
	    	  $scope.resource = null;
	    	  $scope.currentDefinition = null;
	    	}
	    };
	    var finish = 0;
	    
	    $scope.processElem = function(elem, path) {	    
	    	//console.log(elem);
	    	var i = path.lastIndexOf(".");
	    	
	    	if (i >= 0) {
	    	   var name = path.substring(i+1);
	    	   var prefix = path.substring(0, i);
	    	   
	    	   var res = definitions[prefix];
	    	   if (res == null) {
	    		   console.log(prefix);
	    		definitions[prefix] = res = { fields:[] };   
	    	   }
	    	   
	    	   
	    
	    	
	    	if (name == "id" || name == "extension") return;
			var card;
			if (elem.max == "*") card ="0..n"; else card = "0..1";
			if (elem.min == 1 && card == "0..1") card = "1..1";
			//console.log(elem.type);
			if (!elem.type) { console.log(elem);return; }
			var lastElemDef = null;
			angular.forEach(elem.type, function(type) {
				if (!type.code) return;				
			var field = name.replace("[x]", type.code.charAt(0).toUpperCase() + type.code.slice(1));
			var elemDef = { field : field, label : $scope.makeLabel(field), card : card,type : type.code, description: elem.definition };
			if (lastElemDef != null && lastElemDef.field == elemDef.field) {
				elemDef = null;
			} else lastElemDef = elemDef;
			
			if (type.code == "Reference" && type.profile) {				
				var target = type.profile[0];
				if (elemDef != null) {
				   elemDef.resource = [ type.profile[0] ];
				} else {
				   lastElemDef.resource.push(target); 
				}				
			} else if (type.code == "code" || type.code == "CodeableConcept") {
				if (elem.binding && elem.binding.valueSetReference) {
				  elemDef.valueSet = elem.binding.valueSetReference.reference;
				}
			} else if (type.code == "BackboneElement") {
				elemDef.type = path;				
			}
			if (elemDef != null) res.fields.push(elemDef);
			});
	    	}
	    };
	    
	    $scope.process = function(result) {
	    	//console.log(result.data);
    		angular.forEach(result.data.entry, function(proc) {
    			if (proc.resource.resourceType == "StructureDefinition" || proc.resource.resourceType == "DataElement") {
    				var name = proc.resource.id;
    				var res = definitions[name];
    				if (res == null) res = definitions[name] = { fields:[] };    				
    				uriCache[proc.resource.url] = res;
    				if (proc.resource.resourceType == "StructureDefinition") { 
    					res.resourceType = name;
    					$scope.allResources.push(res);
    				}
    				definitions[name] = res;
    				if (proc.resource.element) {
    				  angular.forEach(proc.resource.element, function(elem) {
    					 if (elem.path.indexOf(".") < 0) {
    						 definitions[elem.path].description = elem.definition;
    						 definitions[elem.path].short = elem.short;
    						 return;
    					 }
       				     $scope.processElem(elem, elem.path);    				                              	    							    				  
       				  });    					
    				}
    				/*if (proc.resource.snapshot) {
    				  angular.forEach(proc.resource.snapshot.element, function(elem) {
    				     $scope.processElem(elem, elem.path);    				                              	    							    				  
    				  });
    				}*/
    				//console.log(res);
    			} else if (proc.resource.resourceType == "ValueSet") {    				
    				uriCache[proc.resource.url] = proc.resource;
    				if (proc.resource.codeSystem && proc.resource.codeSystem.system) {
    					uriCache[proc.resource.codeSystem.system] = proc.resource;
    				}
    				
    				
    			}
    			
    			//"resourceType": "StructureDefinition",
    		});
    		finish++;
    		if (finish == 4) {
    		//
    		}
	    };
	    
	    $scope.makeResource = function(def) {
	    	$scope.resource = $scope.createResource(def.resourceType);
    	    $scope.currentDefinition = definitions[$scope.resource.resourceType];
    	    console.log($scope.currentDefinition);
	    };
	    
	    $scope.loadResources = function(type) {
	    	$http.get("/fhir/js/dataelements.json")
	    	.then(function(result) {
	    		$scope.process(result);
	    	});
	    	$http.get("/fhir/js/profiles-resources.json")
	    	.then(function(result) {
	    		$scope.process(result);
	    	});
	    	$http.get("/fhir/js/valuesets.json")
	    	.then(function(result) {
	    		$scope.process(result);
	    	});
	    	$http.get("/fhir/js/v3-codesystems.json")
	    	.then(function(result) {
	    		$scope.process(result);
	    	});
	    		
	    		
	    
	    };
	    
	    $scope.getFHIR = function() {
	    	return $scope.resource;
	    };
	        
	    $scope.loadResources();
    }    
]);
