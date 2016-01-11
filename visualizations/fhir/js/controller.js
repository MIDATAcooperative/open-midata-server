var fhir = angular.module('fhir', [ 'midata', 'ui.bootstrap' ]);

fhir.controller('FHIRCtrl', ['$scope', '$http', '$location', '$filter', 'midataServer', 'midataPortal',   
	function($scope, $http, $location, $filter, midataServer, midataPortal) {
	    var definitions = {};
	    var uriCache = {};
	    $scope.uriCache = uriCache;
		// init
	    midataPortal.autoresize();
	    var authToken = $location.path().split("/")[1];
	    
	    $scope.resource = {};
	    
	    $scope.pool = {};
	    $scope.poolByType = {};
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
	    	if (fielddef.type == "CodeableConcept") {
	    	   if (fielddef.valueSet != null && $scope.getOptions(fielddef.valueSet).length > 0) return "CodeableConceptVS.html";
	    	   return "CodeableConcept.html";	    	   
	    	} 
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
	      if (val == null) return "Undefined";
	      var l = val.replace(/([a-z])([A-Z])/g, '$1 $2');
	      return l[0].toUpperCase()+l.substring(1);
	    };
	    	    
	    $scope.makeHRName = function(res) {
	    	var name = res.id;
	    	if (res.name != null) {
	    		var n = res.name;
	    		if (angular.isArray(n)) {
	    			var n = n[0];
	    		}
	    		if (angular.isString(n)) {
	    			if (n != "") name = n;
	    		} else {
	    			if (n.text) { name = n.text; }
	    			else {
	    				var family = n.family || [];
	    				var given = n.given || [];
	    				name = family.join(" ")+", "+given.join(" ");
	    			}
	    		}
	    	}
	    	return $scope.makeLabel(name);
	    };
	    
	    $scope.addToPool = function(newResource) {
	    	$scope.pool[newResource.id] = newResource;
	    	if (newResource.resourceType != null) {
	    		var p = $scope.poolByType[newResource.resourceType];
	    		if (p == null) p = $scope.poolByType[newResource.resourceType] = [];
	    		p.push(newResource);	    		
	    	}
	    };
	    
	    $scope.pools = function(types) {
	    	var r = [];	 	    	
	    	angular.forEach(types, function(type) {
	    	   type = uriCache[type].resourceType;
	    	   
	    	   var add = $scope.poolByType[type];	    	   
	    	   if (add != null) r = r.concat(add);
	    	});	    	
	    	return r;
	    };
	    
	    $scope.processResource = function(res) {
	    	if (!res.meta) res.meta = {};
	    	res.meta.versionId = new Date().getTime();
	    	
	    	if (res.resourceType != null) {
		    	if (!res.text) {
		    		res.text = {};
		    	}
		    	
		    	res.text.status = "generated";
		    	
		    	var summary = function(res, def) {
		    		var result = "";
		    		angular.forEach(def.fields, function(fn) {		    			
		    		   if (res[fn.field] != null) {		    		   
		    			 var d = angular.isArray(res[fn.field]) ? res[fn.field] : [ res[fn.field] ];
		    		     var type = definitions[fn.type];		    		    
		    		     angular.forEach(d, function(r) {
		    		     if (type != null && type.fields.length > 0) {
		    			  result += "<div>"+fn.label+":"+summary(r, type)+"</div>";
		    		     } else {
		    		      result += "<p>"+fn.label+":"+r+"</p>";
		    		     }
		    		     });
		    		   }
		    		});
		    		return result;
		    	}
		    	
		    	res.text.div = summary(res, definitions[res.resourceType]);
	    	}
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
	    	if (def.resourceType) {
	    		newResource.resourceType = def.resourceType;	    		
	    	}
	    		    	
	    	angular.forEach(def.fields, function(f) {
	    	 if (f.card == "1..1") $scope.addResource(newResource, f); 
	    	});
	    	
	    	if (def.resourceType) {
	    	  return midataServer.newId(authToken)
	    	  .then(function(result) {
	    		  newResource.id = result.data;
	    		  return newResource;
	    	  }); 	    	  
	    	} else { 
	    	  var r = $q.defer();
	    	  r.resolve(newPromise);
	    	  return r.promise;
	    	}
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
	    		newResource = { coding : [ { userSelected : true } ], text : "" };
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
	    	resource[fielddef.field].push("");
	    	//console.log(resource[fielddef.field]);
	    };
	    
	    $scope.checkString = function(resource, fielddef, idx) {
	    	//console.log(resource);
	    	if (resource[fielddef.field][idx] == "") {
	    		//console.log("checked: "+idx);
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
					var entry = { system : orig.codeSystem.system, code : code.code, display: code.display, definition : code.definition };
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
							var other = $scope.getOptions(inc.system);
							angular.forEach(inc.concept, function(code) {
								var match = $filter("filter")(other, function(x) { return x.code == code.code });
								if (match.length > 0) match = match[0];
								var entry = { system:inc.system, code : code.code, display: code.display || match.display, definition : code.definition || match.definition };
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
	    
	    
	    
	    $scope.chooseOption = function(field, clear) {
	    	if (clear) {
	    		field.coding = undefined;
	    		field.$$fhirSelected = undefined;
	    		return;
	    	}
	    	field.coding[0].system = field.$$fhirSelected.system;
	    	field.coding[0].display = field.$$fhirSelected.display;
	    	field.coding[0].code = field.$$fhirSelected.code;
	    	field.text = field.$$fhirSelected.display;
	    };
	    
	    $scope.createReference = function(access, fielddef, type) {
	    	$scope.createResource(type || fielddef.resource)
	    	.then(function(newResource) {
	    		//console.log(newResource);
	    		$scope.addToPool(newResource);		    	
		    	access.reference = newResource.id;
		    	$scope.returnStack.push($scope.resource);
		    	$scope.resource = newResource;
		    	$scope.currentDefinition = definitions[$scope.resource.resourceType];
	    	});
	    };
	    
	    $scope.setReference = function(access, fielddef, id) {
	    	var newResource = $scope.pool[id];	    		    	
	    	access.reference = id;
	    	access.display = $scope.makeHRName(newResource);
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
	    
	    $scope.clearReference = function(access, fielddef) {
	    	access.reference = undefined;
	    	access.display = undefined;	    	
	    };
	    
	    $scope.editResource = function(id) {       	    	 	    	
	    	$scope.resource = $scope.pool[id];	    		    		    	
	    	$scope.currentDefinition = definitions[$scope.resource.resourceType];
	    };
	    
	    $scope.returnToPrevious = function() {
	    	if ($scope.resource != null) {
	    		$scope.processResource($scope.resource);
	    	}
	    	
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
	    		   //console.log(prefix);
	    		definitions[prefix] = res = { fields:[] };   
	    	   }
	    	   
	    	   
	    
	    	
	    	if (name == "id" || name == "extension" || name == "meta" || name == "implicitRules" || name == "text" || name == "contained" || name == "modifierExtension") return;
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
			if (elemDef != null) {
				res.fields.push(elemDef);
				if (elem.isSummary) res.summary.push(elemDef);
			}
			});
	    	}
	    };
	    
	    $scope.process = function(result) {
	    	//console.log(result.data);
    		angular.forEach(result.data.entry, function(proc) {
    			if (proc.resource.resourceType == "StructureDefinition" || proc.resource.resourceType == "DataElement") {
    				var name = proc.resource.id;
    				var res = definitions[name];
    				if (res == null) res = definitions[name] = { fields:[], summary:[] };    				
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
	    	return $scope.createResource(def.resourceType)
	    	.then(function(newResource) {
		    	$scope.resource = newResource;
	    	    $scope.currentDefinition = definitions[$scope.resource.resourceType];
	    	    $scope.addToPool($scope.resource);	    	    
	    	    console.log($scope.currentDefinition);
	    	    return newResource;
	    	});
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
	    
	    $scope.saveAllModified = function() {
	       console.log("saveAll");
	       angular.forEach($scope.pool, function(res, k) {
	    	   console.log(res);
	    	 if (!res.$$fhirUnchanged) {
	    		 midataServer.createRecord(authToken, $scope.makeLabel(res.resourceType), "fhir resource", res.resourceType, "fhir", res, res.id);
	    	 }  
	       });	
	    };
	    
	    $scope.load = function() {
	    	midataServer.getRecords(authToken, { format : "fhir" }, ["_id", "name", "data"])
	    	.then(function(results) {
	    		angular.forEach(results.data, function(rec) {	
	    			$scope.addToPool(rec.data);
	    			//$scope.pool[rec._id.$oid] = rec.data;
	    			rec.data.$$fhirUnchanged = true;
	    		});
	    	});
	    };
	        
	    $scope.loadResources();
	    $scope.load();
    }    
]);
