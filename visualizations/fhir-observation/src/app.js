
angular.module('fhirObservation', [ 'midata', 'ui.router','ui.bootstrap', 'chart.js', 'pascalprecht.translate' ])
.config(['$stateProvider', '$urlRouterProvider', '$translateProvider', function($stateProvider, $urlRouterProvider, $translateProvider) {	    
    
	$translateProvider
	.useSanitizeValueStrategy('escape')	   	    
	.registerAvailableLanguageKeys(['en', 'de', 'it', 'fr'], {
	  'en_*': 'en',
	  'de_*': 'de',
	  'fr_*': 'fr',
	  'it_*': 'it',
	})
	.translations('en', en)
	.translations('de', de)
	.translations('it', it)
	.translations('fr', fr)
	.fallbackLanguage('en');
	
	 $stateProvider
	    .state('record', {
	      url: '/record?id&authToken',	   
	      templateUrl: 'single_record.html'
	    })
	    .state('chart', {
	      url: '/chart?measure&authToken&mode',	   
	      templateUrl: 'chart.html'
	    })
	    .state('overview', {
	      url: '/overview?lang&authToken',	   
	      templateUrl: 'overview.html'
	    })
	    .state('create', {
	      url: '/create?measure&authToken',	    
	      templateUrl: 'create.html'
	    });
	 
	 $urlRouterProvider
	 .otherwise('/overview');  
}])
.run(['$translate', '$location', 'midataPortal', 'midataServer', function($translate, $location, midataPortal, midataServer) {
	console.log("Language: "+midataPortal.language);
    
	$translate.use(midataPortal.language);	
    midataPortal.autoresize();
				
	midataServer.authToken = $location.search().authToken;
	//var params = $location.search();
    
}])
.factory('configuration', ['$q','midataServer', function($q, midataServer) {
   var result = {};
   
   result.load = function() {
	   
	   return midataServer.getConfig(midataServer.authToken)
       .then(function (res) {
    	 result.config = { measures : [], owner : "self" }; 
         if (res.data) {
             if (res.data && res.data.readonly) {
                 result.readonly = true;
             } else {
                 result.config = res.data;                 
             }             
         }  
         return result.config;
       });
	   	  			
   };
   
   result.save = function() {	   
       result.saving = true;
       midataServer.setConfig(midataServer.authToken, result.config)
       .then(function() { result.saving = false; });  
   };
   
   result.getConfig = function() {
	   result.configPromise = result.configPromise || result.load();
	   return result.configPromise;
   };
   console.log("configuration");
   return result;
}])
.factory('fhirinfo', ['$q', '$translate', 'midataServer', function($q, $translate, midataServer) {
   var result = {};
   var meta = {};   
   
   var  override = {
	 
	   "body/bloodpressure" :	  
	   {		  
		   type : "component",		   
		   component : [
			 {
				 code : {
				    "text": "Systolic blood pressure",
				    "coding": [
				      {
				        "system": "http://loinc.org",
				        "display": "Systolic blood pressure",
				        "code": "8480-6"
				      }
				    ]
				},
				valueQuantity : {
					unit : "mm[Hg]"
				}
			 },
			 {
				 code : {
					    "text": "Diastolic blood pressure",
					    "coding": [
					      {
					        "system": "http://loinc.org",
					        "display": "Diastolic blood pressure",
					        "code": "8462-4"
					      }
					    ]
				 },
				 valueQuantity : {
					unit : "mm[Hg]"
				 }
			 }
		   ]
	   }
   };
   
   result.getInfos = function(language, contentTypes) {	 
	  console.log(contentTypes);
	  return midataServer.searchContent(midataServer.authToken, { content : contentTypes }, [ "content", "label", "defaultCode", "resourceType", "subType", "defaultUnit", "category" ])
	  	 .then(function(res) {
	  		var returnValues = [];
	  		angular.forEach(res.data, function(d) {
	  			
	  			var label = d.label[language] || d.label.en;	  			
	  			var codes = (d.defaultCode || "http://midata.coop unknown").split(" ");
	  			var type = d.subType || "Quantity";
	  			result.codeToLabel[d.content] = label;
	  			
	  			var newcontent = meta[d.content] =  {
	  				   label : label,		   
	  				   content : d.content,
	  				   type : type,
	  				   code : {
	  					    "text": label,
	  					    "coding": [
	  					      {
	  					        "system": codes[0],
	  					        "display": label,
	  					        "code": codes[1]
	  					      }
	  					    ]
	  				   }
	  			 };
	  			if (type !== "component") {
	  			  newcontent["value"+d.subType] = {};
	  			  if (d.subType == "Quantity") newcontent["value"+d.subType].unit = (d.defaultUnit || ""); 
	  			}
	  			if (d.category) {
	  				
	  				newcontent.category = {
							coding : [
							           {
							             "system": "http://hl7.org/fhir/observation-category",
							             "code": d.category,
							             "display": ""
							           }
							],		
							text : ""
					};
	  				
	  				$translate("category_names."+d.category).then(function(v) { newcontent.category.text = newcontent.category.coding[0].display = v; });
	  			}
	  			var overrideData = override[d.content];
	  			if (overrideData) {
	  				angular.extend(newcontent, overrideData);
	  			}
	  			
	  			returnValues.push(newcontent);
	  		});
	  		
	  		return returnValues;
	  	 });
	   
	   
	   
	  //return $q.when(meta[contentType]);		 
   };
   
   var setcategory = {
			"body/bloodpressure" : "Vital Signs",
			"body/temperature" : "Vital Signs",
			"activities/heartrate" : "Vital Signs",
			"body/weight" : "Vital Signs",
			"body/height" : "Vital Signs"
	};
   
   result.getCategory = function(contentType) {
	  return setcategory[contentType]; 
   };
   
   result.codeToLabel = {  };	
    
	
	result.getLabel = function(value) {	 	  	  	  
	  	 return result.codeToLabel[value];
	};
     
   console.log("fhirinfo");
   return result;
}])
.factory('data', ['$q', '$filter', 'fhirinfo', 'midataServer', 'midataPortal', function($q, $filter, fhirinfo, midataServer, midataPortal) {
		
	var result = {};
	
	result.owners = {};
	
	result.groupByCategory = function(records) {
		var categories = {};
		angular.forEach(records, function(record) {
			var cat = "other";		
			if (record.data.category) cat = result.getCodeableConcept(record.data.category);
			var proposed = fhirinfo.getCategory(record.content);
			if (proposed) cat = proposed; 
			var targetCategory = categories[cat];
			if (!targetCategory) {
				targetCategory = categories[cat] = { label : cat, records:[] };
			}
			targetCategory.records.push(record);
			record.label = result.getCodeableConcept(record.data.code);
		});
		return categories;
	};
	
	result.loadSummary = function(alwaysAddMeasures) {
		return midataServer.getSummary(midataServer.authToken, "SINGLE", { format : ["fhir/Observation"], subformat : ["Quantity", "component"], owner : "self" }) 			
		.then(function(sumResult) {
			var ids = [];
			var contents = {};
			angular.forEach(sumResult.data, function(entry) { ids.push(entry.newestRecord);contents[entry.contents[0]] = entry.count; }); 	
			
			var res = [];
			if (alwaysAddMeasures) {
				var measures = $filter('filter')(alwaysAddMeasures, function(measure) { return !contents[measure]; });
				if (measures && measures.length > 0) {
					res.push(fhirinfo.getInfos(midataPortal.language, measures)
					.then(function(infos) {
						var data = [];
						angular.forEach(infos, function(info) { data.push({ content : info.content, data : { code:info.code } }); });
						return data;
					}));
				}
				
	 		}
			
			if (ids.length > 0) {
			  res.push(result.getRecords({ ids : ids }));
			}
			  
			return $q.all(res).then(function(r) { return [].concat.apply([], r); });
		});
	};
	
	result.getRecords = function(params) { 	
		
		var query = { "format" : "fhir/Observation" };
		if (params.content) query.content = params.content;
		if (params.owner) query.owner = params.owner;
		if (params.ids) query._id = params.ids;
		if (params.after && params.before) query.index = { "effectiveDateTime" : { "!!!ge" : params.after, "!!!lt" : params.before }};
		console.log(params);
		return midataServer.getRecords(midataServer.authToken, query, ["name", "created", "content", "data", "owner", "ownerName"])
		.then(function(results) {
			angular.forEach(results.data, function(rec) { result.owners[rec.owner] = rec.ownerName; });
			console.log(result.owners);
			return results.data;			
		}, function(err) {
			result.error = "Failed to load records: " + err.data; 			
		});
	};
			
	result.getCodeableConcept = function(what) {
		if (what == null) return null;
		if (angular.isArray(what) && what.length > 0) return result.getCodeableConcept(what[0]);
		if (what.text) return what.text;
		if (what.coding && what.coding.length > 0) {
			return result.getCoding(what.coding[0]); 				
		}
		return "?";
	};
	
	result.getCoding = function(what) {
		if (what == null) return null;
		if (what.display != null) return what.display;
	    return what.code;
	};
	
	result.getReference = function(what) {
		if (what == null) return null;
		if (what.display) return what.display;
		return what.reference;
	};
		      
	
   console.log("data");
	return result;
}]);
