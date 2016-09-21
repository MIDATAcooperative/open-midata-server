angular.module('fhir')

.factory('fhirModule', ['$http', '$filter', '$q', 'midataServer',    
	function($http, $filter, $q, midataServer) {
	    var definitions = {};
	    var uriCache = {};
	    var fhirModule = {};
	    			    	   	    	    
	    fhirModule.pool = {};
	    fhirModule.poolByType = {};	    
	    fhirModule.allResources = [];
	    fhirModule.definitions = definitions;
	    fhirModule.uriCache = uriCache;
	    	    	 
	    fhirModule.makeLabel = function(val) {
	      if (val == null) return "Undefined";
	      var l = val.replace(/([a-z])([A-Z])/g, '$1 $2');
	      return l[0].toUpperCase()+l.substring(1);
	    };
	    	    
	    fhirModule.makeHRName = function(res) {
	    	var name = res.id;
            if (res.code && res.code.text) {
	    	  name = res.code.text;	
	    	}
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
	    	
	    	return fhirModule.makeLabel(name);
	    };
	    
	    fhirModule.addToPool = function(newResource, origRecord) {
	    	if (origRecord) {
	    		if (!newResource.meta) {
	    			newResource.meta = {};
	    		}
	    		newResource.meta.versionId = origRecord.version;
	    	    newResource.meta.lastUpdated = origRecord.lastUpdated || origRecord.created;
	    	    newResource.id = origRecord._id;
	    	}
	    	
	    	fhirModule.pool[newResource.resourceType+"/"+newResource.id] = newResource;
	    	if (newResource.resourceType != null) {
	    		var p = fhirModule.poolByType[newResource.resourceType];
	    		if (p == null) p = fhirModule.poolByType[newResource.resourceType] = [];
	    		p.push(newResource);	    		
	    	}
	    };
	    
	    fhirModule.pools = function(types) {
	    	var r = [];	 	    	
	    	angular.forEach(types, function(type) {
	    	   type = uriCache[type].resourceType;
	    	   
	    	   var add = fhirModule.poolByType[type];	    	   
	    	   if (add != null) r = r.concat(add);
	    	});	    	
	    	return r;
	    };
	    
	    fhirModule.processResource = function(res) {
	    	if (!res.meta) res.meta = {};
	    	//res.meta.versionId = new Date().getTime();
	    	
	    	if (res.resourceType != null) {
		    	if (!res.text) {
		    		res.text = {};
		    	}
		    	
		    	res.text.status = "generated";
		    	
		    	var summary = function(res, def) {
		    		var result = "<div>";
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
		    		result += "</div>";
		    		return result;
		    	}
		    	
		    	res.text.div = summary(res, definitions[res.resourceType]);
	    	}
	    };
	    
	    fhirModule.getDefinition = function(fielddef) {	    	
	    	return definitions[fielddef.type];
	    };
	    
	    fhirModule.getResource = function(resource, fielddef) {
	    	if (!resource[fielddef.field]) resource[fielddef.field] = {};
	    	return resource[fielddef.field];
	    };
	    
	    fhirModule.createResource = function(type) {
	    	var def = definitions[type] || uriCache[type];
	    	var newResource = {};
	    	if (def.resourceType) {
	    		newResource.resourceType = def.resourceType;
	    		newResource.$$fhirIsNew = true;
	    	}
	    		    	
	    	angular.forEach(def.fields, function(f) {
	    	 if (f.min != 0) fhirModule.addResource(newResource, f); 
	    	});
	    	
	    	if (def.resourceType) {
	    	  return midataServer.newId(fhirModule.authToken)
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
	    	   
	    
	    fhirModule.addResource = function(resource, fielddef) {
	    	var newResource = null;
	    	if (fielddef.fixed) {
	    		newResource = JSON.parse(JSON.stringify(fielddef.fixed));
	    	} else if (fielddef.defaultValue) {
	    	    newResource = JSON.parse(JSON.stringify(fielddef.defaultValue));
	    	} else
	    	if (fielddef.type == "string" || fielddef.type == "code" || fielddef.type == "uri") {
	    		newResource = "";
	    	} else if (fielddef.type == "boolean") {
	    		newResource = false;
	    	} else if (fielddef.type == "positiveInt" || fielddef.type == "integer" || fielddef.type == "decimal" || fielddef.type == "unsignedInt") {
	    		newResource = 1;
	    	} else if (fielddef.type == "dateTime" || fielddef.type == "date" || fielddef.type == "instant") {
	    		newResource = "";
	    	} else if (fielddef.type == "CodeableConcept" && fielddef.valueSet != null) {
	    		var systems = fhirModule.getOptionCodeSystem(fielddef);
	    		newResource = { text : "" };
	    		if (systems.length == 1) { newResource.coding = [ { system : systems[0] } ]; }
	    		else if (systems.length > 1) { newResource.coding = [ { }]; }
	    	} else if (fielddef.type == "Reference") {
	    		newResource = { display:"" };
	    	} else {
	    	  newResource = {};
	    	  
	    	  var def = definitions[fielddef.type];
	    	  if (def != null) {
	    		  angular.forEach(def.fields, function(f) {
	    			 if (f.min != 0) fhirModule.addResource(newResource, f); 
	    		  });
	    	  }
	    	}
	    	
	    	if (fielddef.max == 1) {
	    		resource[fielddef.field] = newResource;
	    	} else {
	    		if (resource[fielddef.field] == null) {
	    			resource[fielddef.field] = [ newResource ];
	    		} else {
	    			resource[fielddef.field].push(newResource);
	    		}
	    	}
	    };
	    
	    fhirModule.removeResource = function(parentResource, fielddef, resource) {
	    	if (angular.isArray(parentResource[fielddef.field])) {
		    	var i = parentResource[fielddef.field].indexOf(resource);
		    	if (i>=0) {
		    		parentResource[fielddef.field].splice(i, 1);
		    	}
	    	} else {
	    		parentResource[fielddef.field] = null;
	    	}
	    };
	    
	    fhirModule.mayAddResource = function(resource, fielddef) {
	    	if (fielddef.max == "*") return true;
	    	if (!resource[fielddef.field]) return true;
	    	if (resource[fielddef.field].length == 0) return true;
	    	return false;
	    };
	    
	    fhirModule.addString = function(resource, fielddef) {
	    	if (!resource[fielddef.field]) resource[fielddef.field] = [];
	    	resource[fielddef.field].push("");
	    	//console.log(resource[fielddef.field]);
	    };
	    
	    fhirModule.checkString = function(resource, fielddef, idx) {
	    	//console.log(resource);
	    	if (resource[fielddef.field][idx] == "") {
	    		//console.log("checked: "+idx);
	    		resource[fielddef.field].splice(idx, 1);
	    	}
	    };
	    
	    fhirModule.getOptions = function(fielddef) {
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
					  var other = fhirModule.getOptions(imp);
					  res.push.apply(res, other);					  
					});
				}				
				if (orig.compose.include) {
					angular.forEach(orig.compose.include, function(inc) {
						if (inc.concept) {
							var other = fhirModule.getOptions(inc.system);
							angular.forEach(inc.concept, function(code) {
								var match = $filter("filter")(other, function(x) { return x.code == code.code });
								if (match.length > 0) match = match[0];
								var entry = { system:inc.system, code : code.code, display: code.display || match.display || (inc.system + code.code), definition : code.definition || match.definition };
								res.push(entry);
							});	
						} else {
							var other = fhirModule.getOptions(inc.system);
							res.push.apply(res, other);
						}
					});
				} 
									
			}
			orig.computed = res;
	    	return res;
	    };
	    
	    fhirModule.getOptionCodeSystem = function(fielddef) {
	    	//console.log(fielddef);
	    	var orig = uriCache[fielddef.valueSet || fielddef];
	    	//console.log(orig);
	    	if (orig == null) return [];
	    	if (orig.computedCS) return orig.computedCS;
	    	
	    	var res = [];	    	
			if (orig.codeSystem) {
				res.push(orig.codeSystem.system);
				
			}			
			if (orig.compose) {
				if (orig.compose.import) {
					angular.forEach(orig.compose.import, function(imp) {					  
					  res.push(imp);					  
					});
				}				
				if (orig.compose.include) {
					angular.forEach(orig.compose.include, function(inc) {
						res.push(inc.system);						
					});
				} 
									
			}
			orig.computedCS = res;
	    	return res;
	    };
	    
	    
	    
	    fhirModule.chooseOption = function(field, clear, set) {
	    	if (clear) {
	    		field.coding = undefined;
	    		field.$$fhirSelected = undefined;
	    		return;
	    	}
	    	if (set) {
	    		field.$$fhirSelected = set;
	    		field.$$fhirSearch = undefined;
	    	}
	    	field.coding[0].system = field.$$fhirSelected.system;
	    	field.coding[0].display = field.$$fhirSelected.display;
	    	field.coding[0].code = field.$$fhirSelected.code;
	    	field.text = field.$$fhirSelected.display;
	    };
	    
	    fhirModule.initOption = function(field, options) {
	    	if (field.coding && field.coding[0] && field.coding[0].code && !field.$$fhirSelected) {
	    	  var coding = field.coding[0];
	    	  var opt = $filter('filter')(options, function(x) { return x.system == coding.system && x.code == coding.code; });
	    	  if (opt && opt.length > 0) field.$$fhirSelected = opt[0];
	    	} 
	    };
	    	  
	    	    
	    fhirModule.clearReference = function(access, fielddef) {
	    	access.reference = undefined;
	    	access.display = undefined;	    	
	    };
	    	    
	    var finish = 0;
	    
	    fhirModule.processElem = function(elem, path) {	    
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
	    	   
	    	   
	    
	    	
	    	if (name == "id" || name == "extension" || name == "meta" || name == "implicitRules" || name == "text" || name == "contained" || name == "modifierExtension" || name == "language") return;
			
			//console.log(elem.type);
			if (!elem.type) { console.log(elem);return; }
			var lastElemDef = null;
			angular.forEach(elem.type, function(type) {
				if (!type.code) return;				
				var codeCase = type.code.charAt(0).toUpperCase() + type.code.slice(1);
			var field = name.replace("[x]", codeCase);
			var def = elem["defaultValue"+codeCase];
			var fixed = elem["fixed"+codeCase];
			
			var elemDef = { field : field, label : fhirModule.makeLabel(field), min : elem.min, max : elem.max,type : type.code, description: elem.definition };
			if (def != null) elemDef.defaultValue = def;
			if (fixed != null) elemDef.fixed = fixed;
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
	    
	    fhirModule.process = function(result) {
	    	//console.log(result.data);
    		angular.forEach(result.data.entry, function(proc) {
    			if (proc.resource.resourceType == "StructureDefinition" || proc.resource.resourceType == "DataElement") {
    				var name = proc.resource.id;
    				var res = definitions[name];
    				if (res == null) res = definitions[name] = { fields:[], summary:[] };    				
    				uriCache[proc.resource.url] = res;
    				if (proc.resource.resourceType == "StructureDefinition") { 
    					res.resourceType = name;
    					fhirModule.allResources.push(res);
    				}
    				definitions[name] = res;
    				if (proc.resource.element) {
    				  angular.forEach(proc.resource.element, function(elem) {
    					 if (elem.path.indexOf(".") < 0) {
    						 definitions[elem.path].description = elem.definition;
    						 definitions[elem.path].short = elem.short;    						 
    						 return;
    					 }    					
    					 fhirModule.processElem(elem, elem.path);    				                              	    							    				  
       				  });    					
    				}    				
    				//console.log(res);
    			} else if (proc.resource.resourceType == "ValueSet") {    				
    				uriCache[proc.resource.url] = proc.resource;
    				if (proc.resource.codeSystem && proc.resource.codeSystem.system) {
    					uriCache[proc.resource.codeSystem.system] = proc.resource;
    				}
    				
    				
    			}
    			
    			//"resourceType": "StructureDefinition",
    		});
    		
	    };
	    
	    fhirModule.resourceList = function(names) {
	    	var result = [];
	    	console.log(definitions);
	    	angular.forEach(names, function(name) {
	    		if (definitions[name] == null) console.log(name);
	    		else result.push(definitions[name]);
	    	});
	    	return result;
	    };
	    	    	    
	    fhirModule.loadResources = function(type) {
	    	var prom = $q.defer();
	    	var finish = 0;
	    	
	    	$http.get("/fhir/js/dataelements.json")
	    	.then(function(result) {
	    		fhirModule.process(result);
	    		finish++;
	    		if (finish == 4) prom.resolve();
	    	});
	    	$http.get("/fhir/js/profiles-resources.json")
	    	.then(function(result) {
	    		fhirModule.process(result);
	    		finish++;
	    		if (finish == 4) prom.resolve();
	    	});
	    	$http.get("/fhir/js/valuesets.json")
	    	.then(function(result) {
	    		fhirModule.process(result);
	    		finish++;
	    		if (finish == 4) prom.resolve();
	    	});
	    	$http.get("/fhir/js/v3-codesystems.json")
	    	.then(function(result) {
	    		fhirModule.process(result);
	    		finish++;
	    		if (finish == 4) prom.resolve();
	    	});
	    	
	    	return prom.promise;
	    };
	    
	   	    
	    fhirModule.saveAllModified = function() {
	       console.log("saveAll");
	       angular.forEach(fhirModule.pool, function(res, k) {
	    	   console.log(res);
	    	 if (!res.$$fhirUnchanged) {
	    		 if (res.$$fhirIsNew) {
	    		    midataServer.createRecord(fhirModule.authToken, { "name" : fhirModule.makeLabel(res.resourceType), "content" : res.resourceType, "format" : "fhir/"+res.resourceType }, res, res.id);
	    		 } else {
      			    midataServer.updateRecord(fhirModule.authToken, res.id, res.meta.versionId, res);
	    		 }
	    	 }  
	       });	
	    };
	    	  	   	    
	    return fhirModule;
    }    
]);
