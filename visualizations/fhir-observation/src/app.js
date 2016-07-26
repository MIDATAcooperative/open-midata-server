
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
	      url: '/chart?measure&authToken',	   
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
.factory('fhirinfo', ['$q', 'midataServer', function($q, midataServer) {
   var result = {};
      
   
   var  meta = {
	  "body/weight" :  
	  {
			label : "Weight",
			content : "body/weight",
			type : "Quantity",
			code : {
			    "text": "Body weight Measured",
			    "coding": [
			      {
			        "system": "http://loinc.org",
			        "display": "Body weight Measured",
			        "code": "3141-9"
			      }
			    ]
			},
			valueQuantity : {		   
			   unit : "kg"
			}    							
	 },
	 "activities/steps" :
	 {
		   label : "Step Count",		   
		   content : "activities/steps",
		   type : "Quantity",
		   code : {
			  "text": "Step Count",
			  "coding": [
			      {
			        "system": "http://loinc.org",
			        "display": "Step Count",
			        "code": "55423-8"
			      }
			  ]
		  },
		  valueQuantity : {
			  
		  }
	 },
	 "body/height" :
	 {
		   label : "Height",		   
		   content : "body/height",
		   type : "Quantity",
		   code : {
			  "text": "Body height",
			  "coding": [
			      {
			        "system": "http://loinc.org",
			        "display": "Body height",
			        "code": "8302-2"
			      }
			  ]
		  },
		  valueQuantity : {		   
			   unit : "cm"
		  }   
	   },		 
	   "activities/heartrate" :
	   {
		   label : "Heartrate",		  
		   content : "activities/heartrate",	
		   type : "Quantity",
		   code : {
			    "text": "Heart rate",
			    "coding": [
			      {
			        "system": "http://loinc.org",
			        "display": "Heart rate",
			        "code": "8867-4"
			      }
			    ]
		  },
		  valueQuantity : {		   
			   unit : "{beats}/min"
		  } 
	   },
	   "body/temperature" :
	   {
		   label : "Body Temperature",		   
		   content : "body/temperature",
		   type : "Quantity",
		   code : {
			    "text": "Body temperature",
			    "coding": [
			      {
			        "system": "http://loinc.org",
			        "display": "Body temperature",
			        "code": "8310-5"
			      }
			    ]
		   },
		  valueQuantity : {		   
			   unit : "Cel"
		  } 
	   },		  
	   "body/bloodpressure" :	  
	   {
		   label : "Blood pressure",
		   content : "body/bloodpressure",
		   type : "component",
		   code : {
			    "text": "Blood pressure",
			    "coding": [
			      {
			        "system": "http://loinc.org",
			        "display": "Blood pressure",
			        "code": "55417-0"
			      }
			    ]
		   },
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
   
   result.getInfo = function(contentType) {	   	 
	  return $q.when(meta[contentType]);		 
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
   
   result.loadLabels = function(language, types) {
	  	 return midataServer.searchContent(midataServer.authToken, { content : types }, [ "content", "label" ])
	  	 .then(function(res) {
	  		angular.forEach(res.data, function(d) {
	  			result.codeToLabel[d.content] = d.label[language] || d.label.en;
	  		});
	  	 });
	};
	
	result.getLabel = function(value) {	 	  	  	  
	  	 return result.codeToLabel[value];
	};
     
   console.log("fhirinfo");
   return result;
}])
.factory('data', ['$q', 'fhirinfo', 'midataServer', 'midataPortal', function($q, fhirinfo, midataServer, midataPortal) {
		
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
		});
		return categories;
	};
	
	result.loadSummary = function(alwaysAddMeasures) {
		return midataServer.getSummary(midataServer.authToken, "SINGLE", { format : ["fhir/Observation"], subformat : ["Quantity", "component"], owner : "self" }) 			
		.then(function(sumResult) {
			var ids = [];
			var contents = {};
			angular.forEach(sumResult.data, function(entry) { ids.push(entry.newestRecord.$oid);contents[entry.contents[0]] = true; }); 	
			
			var res = [];
			if (alwaysAddMeasures) {	
				angular.forEach(alwaysAddMeasures , function(measure) {
	 				if (!contents[measure]) {
	 				   res.push(
	 					   fhirinfo.getInfo(measure)
	 					   .then(function(info) { return { content : measure, data : { code:info.code } }; }) 
	 				   );
	 				}
	 		    });
	 		}
			
			res.push(result.getRecords({ ids : ids }));
			
			return $q.all(res).then(function(r) { return [].concat.apply([], r); });
		});
	};
	
	result.getRecords = function(params) { 	
		
		var query = { "format" : "fhir/Observation" };
		if (params.content) query.content = params.content;
		if (params.owner) query.owner = params.owner;
		if (params.ids) query._id = params.ids;
		console.log(params);
		return midataServer.getRecords(midataServer.authToken, query, ["name", "created", "content", "data", "owner", "ownerName"])
		.then(function(results) {
			angular.forEach(results.data, function(rec) { result.owners[rec.owner.$oid] = rec.ownerName; });
			console.log(result.owners);
			return results.data;			
		}, function(err) {
			result.error = "Failed to load records: " + err.data; 			
		});
	};
			
	result.getCodeableConcept = function(what) {
		if (what == null) return null;
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
