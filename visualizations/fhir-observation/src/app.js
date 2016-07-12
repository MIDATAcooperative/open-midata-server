
angular.module('fhir', [ 'midata', 'ui.bootstrap', 'chart.js', 'pascalprecht.translate' ])
.config(['$translateProvider', function($translateProvider) {	    
    
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
}])
.run(['$translate', 'midataPortal', function($translate, midataPortal) {
	console.log("Language: "+midataPortal.language);
    $translate.use(midataPortal.language);	   	  
}])
.factory('configuration', ['$q','midataServer', function($q, midataServer) {
   var result = {};
   
   result.load = function() {
	   
	   return midataServer.getConfig(midataServer.authToken)
       .then(function (res) {
    	 result.config = { measure : "body/weight", owner : "self" }; 
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
   
   return result;
}])
.factory('fhirinfo', ['$q', 'midataServer', function($q, midataServer) {
   var result = {};
   
   result.meta = {
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
	   console.log("ct:");
	   console.log(contentType);	   	  
	  return $q.when(result.meta[contentType]);		 
   };
   
   
   return result;
}])
.factory('data', ['midataServer', 'midataPortal', function(midataServer, midataPortal) {
	var result = {};
	
	result.valuesToLabel = { owner : {}, content: {} };
	result.records = [];
	
	result.getRecords = function(config) { 	
		console.log(config);
		return midataServer.getRecords(midataServer.authToken, { "format" : "fhir/Observation", "content" : config.measure, "owner" : config.owner }, ["name", "created", "data", "ownerName"])
		.then(function(results) { 				
			result.records = results.data;
			if (results.data.length == 1) {
			  result.record = results.data[0];
			} else result.record = null;			
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
	
	result.loadLabels = function(types) {
  	 return midataServer.searchContent(midataServer.authToken, { content : types }, [ "content", "label" ])
  	 .then(function(res) {
  		angular.forEach(res.data, function(d) {
  			result.valuesToLabel.content[d.content] = d.label[midataPortal.language] || d.label.en;
  		});
  	 });
   };
   
   result.getLabel = function(dimension, value) {
 	  
  	 var dimVals = result.valuesToLabel[dimension];
  	 if (dimVals != null) {
  		var r = dimVals[value];
  		if (r!=null) return r;
  	 } 
  	 return value;
   };
	
	return result;
}])
.controller('ObservationCtrl', ['$scope', '$filter', '$location', '$q', 'midataServer', 'midataPortal', 'configuration', 'data',
 	function($scope, $filter, $location, $q, midataServer, midataPortal, configuration, data) {
 		
	    	    
 	    midataPortal.autoresize();
 	     	 		
 		$scope.ready = $q.defer();
 		$scope.mode = "chart";
 		
 		var authToken = $location.path().split("/")[1];
 		midataServer.authToken = authToken;
 		console.log(authToken);
 		
 		$scope.getConfig = function() {
 			configuration.getConfig()
 			.then(function() { 				
 				data.loadLabels(configuration.measure);
 				data.getRecords(configuration.config)
 				.then(function() {
 					if (data.records.length == 1) {
 						$scope.mode = "record";
 						$scope.record = data.records[0];
 					}
 					$scope.ready.resolve(); 
 				});
 			}); 			 		
 		};
 		
 		$scope.loadBySummary = function() {
 			 
 	        midataServer.getSummary(midataServer.authToken, "ALL", { format : ["fhir/Observation"], owner : "self" }) 			
 			.then(function(sumResult) {
 				var x = sumResult.data[0].newestEntry;
 				midataServer.getRecords(midataServer.authToken, { "_id" : x },["name", "data"])
 				.then(function(result) {
 					$scope.record = result.data[0];
 					$scope.mode = "record";
 				}); 				 				
 			}); 			 		
 		};
 		
 		$scope.showSingle = function(record) {
 			$scope.record = record;
 			$scope.mode = "record";
 		};
 		
 		$scope.showAll = function() {
 			$scope.mode = "chart";
 		}; 		 		
 		
 		$scope.changePerson = function() {
 			$scope.changeperson = true;
 		};
 		
 		$scope.update = function() {
 			data.getRecords(configuration.config)
			.then(function() {
				if (data.records.length == 1) $scope.mode = "record";
				$scope.ready.resolve(); 
			});
 		};
 		
 		$scope.getLabel = data.getLabel;
 		$scope.configuration = configuration; 	
 		$scope.getCodeableConcept = data.getCodeableConcept;
        $scope.data = data;
        
        if (document.location.href.indexOf("/preview") >= 0) {
          $scope.loadBySummary();
        } else {
 		  $scope.getConfig();
        }
 		
}])
.controller('PersonChangeCtrl', ['$scope', 'midataServer', 'configuration', 'data', function($scope, midataServer, configuration, data) {
	
	    var createOwnerList = function(info) {
	    	  var aOwner = {};
	    	  angular.forEach(info, function(entry) {	              
	              data.valuesToLabel.owner[entry.owners[0]] = entry.ownerNames[0];	              
	              angular.forEach(entry.owners, function(on) { aOwner[on] = true; });	          
	          });
	          var result = [];
	          var ownerIdx = 0;
	          
	          angular.forEach(aOwner, function(v,k) { result[ownerIdx++] = k; });	          
	          result.sort();	          
	          return result;
	    };

		$scope.reloadSummary = function() {
	          var p = { format : ["fhir/Observation"]  };
	          midataServer.getSummary(midataServer.authToken, "SINGLE", p, ["ownerName" ])
	          .then(function(results) {	              
	              $scope.persons = createOwnerList(results.data);    	              	                          
	          });
	    };
	    
	    $scope.choosePerson = function(owner) {
	    	
	    };
	    
	    $scope.reloadSummary();	    	
}])
.controller('ChartCtrl', ['$scope', '$filter', '$location', 'midataServer', 'midataPortal', 'data',
    function($scope, $filter, $location, midataServer, midataPortal, data) {
      
	   $scope.extractData = function(records) {
	          var entries = [];
	          var idx = 0;
	          
	          var addEntry = function(record,cmp,cdate) {
	        	  var q = cmp.valueQuantity || { value : 1 };
            	  var cnt = "";
            	  if (cmp.code && cmp.code.coding && cmp.code.coding[0].display) cnt = cmp.code.coding[0].display; 
            	  var dateTime = record.data.effectiveDateTime || cdate;
            	  var e = {
                          value : Number(q.value),
                          unit : q.unit,	                          
                          context : cnt,
                          dateTime : dateTime,                              	                         
                  };
                  if (Number.isFinite(e.value)) entries[idx++] = e;
	          };
	          
	          angular.forEach(records, function(record) {
	              var cdate = new Date(record.created).toISOString();
	              if (record.data.resourceType == "Observation") {
	            	  if (record.data.component) {
		            	  angular.forEach(record.data.component, function(comp) {
		            		  addEntry(record, comp, cdate);
		            	  });
	            	  } else {
	            		  addEntry(record, record.data, cdate);
	            	  }
	            	  	            	    
	              } 
	          });
	        return entries;
	 };
	 
	 $scope.map = function(valuearray) {
         var result = {};
         for (var i=0;i<valuearray.length;i++) { result[valuearray[i]] = i; }
         return result;
     };
	
	$scope.buildAxes = function(entries) {
        var aTime = {};       
        var aContext = {};
     
        var aUnit = {};
        angular.forEach(entries, function(entry) {
            aTime[entry.dateTime] = true;           
            aContext[entry.context] = true;            
        });
        var result = {
                dateTime : [],            
                context : [],            
                units : []
        };
        var dateTimeIdx = 0, contextIdx = 0, unitIdx = 0;
        angular.forEach(aTime, function(v,k) { result.dateTime[dateTimeIdx++] = k; });        
        angular.forEach(aContext , function(v,k) { result.context[contextIdx++] = k; });        
        angular.forEach(aUnit , function(v,k) { result.units[unitIdx++] = k; });
        
        result.dateTime.sort();        
        result.context.sort();        
                
        return result;
    };
	
	 $scope.build = function(info, entries) {
                    
         var shorten = function(dim, a) {
             var r = [];
             angular.forEach(a, function(x) {
           	  x = $scope.getLabel(dim, x);
           	  if (x.length && x.length > 15) r.push(x.substr(0,13) + "..."); else r.push(x); 
             });
             return r;
         };
         var labelAxis = "dateTime";
         var seriesAxis = "context";
         
         $scope.labels = shorten(labelAxis, info[labelAxis]);
         $scope.series = shorten(seriesAxis, info[seriesAxis]);
         
         var labelMap = $scope.map(info[labelAxis]);
         var seriesMap = $scope.map(info[seriesAxis]);
         var d = $scope.data = [];
         var h = [];
        
         angular.forEach($scope.series, function() { d.push(new Array($scope.labels.length).fill(0)); });
         angular.forEach(entries, function(entry) {
             d[seriesMap[entry[seriesAxis]]][labelMap[entry[labelAxis]]] = entry.value;
         });
                 
     };
        
     $scope.d = data;
     $scope.$watch("d.records", function() {
    	var entries = $scope.extractData(data.records);
    	var info = $scope.buildAxes(entries);
    	$scope.build(info, entries);
     });
     
     $scope.onClick = function(a,b,c) {
    	 console.log(a);
    	 console.log(b);
    	 console.log(c);
     };
                          		
}])
.controller('CreateCtrl', ['$scope', '$timeout', '$filter', 'midataServer', 'midataPortal', 'fhirinfo', 'configuration',
	function($scope, $timeout, $filter, midataServer, midataPortal, fhirinfo, configuration) {
		
		// init
		$scope.errors = null;
		$scope.datePickers = {};
	    $scope.dateOptions = {
	       formatYear: 'yy',
	       startingDay: 1,	    
	    };
	    $scope.datePopupOptions = {	 	       
	 	   popupPlacement : "auto top-right"
	 	};
											
		$scope.isValid = true;
		$scope.isBusy = false;
		$scope.success = false;
		configuration.getConfig()
		.then(function(config) {
		  fhirinfo.getInfo(config.measure)
		  .then(function(format) {
			  console.log(format);
			  $scope.format = format;
			  $scope.reset();
		  });
		});
		 
						
		$scope.reset = function() {
			$scope.newentry = { 
					resourceType : "Observation",
					status : "preliminary",
					category : {
						coding : [
				           {
				             "system": "http://hl7.org/fhir/observation-category",
				             "code": "fitness",
				             "display": "Fitness Data"
				           }
						],
/*						coding : [{
						  system : "http://hl7.org/fhir/observation-category",
						  code : "vital-signs",
						  display : "Vital Signs"
						}],*/
						text : "Fitness Data"
					},
					code : $scope.format.code,
					effectiveDateTime : new Date() /*$filter('date')(new Date(), "yyyy-MM-dd")*/
			}; 
			switch ($scope.format.type) {
			  case "Quantity" : $scope.newentry.valueQuantity = $scope.format.valueQuantity;break;
			  case "component" :  $scope.newentry.component = $scope.format.component;break;
			} 
			console.log($scope.newentry);
		};
		
		
		$scope.add = function() {
			$scope.success = false;
			$scope.error = $scope.errorValue = null;
			var theDate = new Date($scope.newentry.effectiveDateTime);
			if (isNaN(theDate)) {
				$scope.error = "Please enter a valid date! (YYYY-MM-DD)";
				return;
			}
			/*if ($scope.newentry.valueQuantity.value > 0) { } else {
				$scope.errorValue = "Please enter a valid value!";
				return;
			}*/
																					
			$scope.isBusy = true;
			midataServer.createRecord(midataServer.authToken, { "name" : $scope.format.label, "content" : $scope.format.content, format : "fhir/Observation", subformat : $scope.format.type }, $scope.newentry )
			.then(function() { 
				$scope.success = true; 
				$scope.isBusy = false; 
				$scope.reset(); 
				$scope.getConfig();
				$timeout(function() { $scope.success = false; }, 2000); 
			});			
		};
														
	}
]);
