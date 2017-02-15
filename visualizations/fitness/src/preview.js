angular.module('fhirObservation')
.controller('PreviewCtrl', ['$scope', '$state', '$filter', '$timeout', 'midataServer', 'midataPortal', 'configuration', 'data', 'fhirinfo',
 	function($scope, $state, $filter, $timeout, midataServer, midataPortal, configuration, data, fhirinfo) {
 			
	
	    $scope.allPreviews = data.allPreviews;    
	    $scope.previews = [];
		   
	    
	    $scope.checkGoal = function(what, goal) {
	    	if (!goal || !what) return { "font-weight" : "bold" };
	    	var v = what.count ? (what.value / what.count) : what.value;
	    	if (goal.type === "max" && v > goal.value) return { "color" : "red", "font-weight" : "bold" };
	    	if (goal.type === "min" && v < goal.value) return { "color" : "red", "font-weight" : "bold" };
	    	return { "color" : "green", "font-weight" : "bold" };	    	
	    };
	    
	    $scope.check = function(what, goal) {
	    	if (!goal || !what) return 0;
	    	var v = what.count ? (what.value / what.count) : what.value;
	    	if (goal.type === "max" && v > goal.value) return 0;
	    	if (goal.type === "min" && v < goal.value) return 0;
	    	return 1;	    	
	    }; 
	    
 		$scope.init = function() {
 			
 				
 			configuration.getConfig().then(function(config) {
 				$scope.config = config;
 				$scope.previews = $scope.mergeConfigIntoPreviews(config, $scope.allPreviews); 
 				
 				midataServer.getRecords(midataServer.authToken, { "format" : "fhir/Patient", "owner" : "self" }, ["data"])
 				.then(function(result) {
 					var patient = result.data[0].data;
 					
 					var ageDifMs = Date.now() - new Date(patient.birthDate).getTime();
 				    var ageDate = new Date(ageDifMs); // miliseconds from epoch
 				    $scope.age = Math.abs(ageDate.getUTCFullYear() - 1970); 					
 					$scope.gender = patient.gender;
 					
 					
 					$scope.loadRecords($scope.previews);
 				});
 				 				 				
 			});
 			 						 	
 		};
 		
 		$scope.mergeConfigIntoPreviews = function(config,previews) {
 			angular.forEach(previews, function(p) {
 				p.count = 0;
 				p.best = { value:0 , unit : "", date : null };
 				p.worst = { value:99999 , unit : "", date : null };
 				p.last = { value:0 , unit : "", date : null };
 				p.avg = { value:0, count : 0 };
 				p.trend = { sumX : 0, sumY : 0, sumXY : 0, sumXSq : 0};
 				if (config[p.display] && config[p.display].goal) p.goal = config[p.display].goal;
 			
 			});
 			return previews;
 		};
 		
 		$scope.loadRecords = function(previews) {
 			var contents = [];
 			var after = new Date();
 			after.setDate(after.getDate()-30);
 			angular.forEach(previews, function(p) { 
 				if (p.content) contents.push(p.content);
 				if (p.chart) {
 					angular.forEach(p.chart,  function(c) { contents.push(c); });
 				}
 			});
 			data.getRecords({ content : contents, after : after })
 			.then(function(recs) {
 				$scope.previews = $scope.mergeRecordsIntoPreviews(recs, previews);
 			});
 		};
 		
 		$scope.mergeRecordsIntoPreviews = function(recs, previews) {
 			var pmap = {};
 			var cmap = {};
 			angular.forEach(previews, function(p) { 
 				pmap[p.content] = p;
 				p.recs = [];
 				if (p.chart) {
 					angular.forEach(p.chart,  function(c) { cmap[c] = p; });
 				}
 			});
 			angular.forEach(recs, function(record) {
 			  var p = pmap[record.content]; 	
 			  
 			  if (p) {
 			  p.display = p.display || data.getCodeableConcept(record.data.code);
 			  
 			  if (record.data.valueQuantity) {
 				 p.count++;
 				 if (record.data.effectiveDateTime) p.recs.push(record);
 				 var date = new Date(record.data.effectiveDateTime);
 				 var val = record.data.valueQuantity.value;
 				 var unit = record.data.valueQuantity.unit; 				
 				 if (!p.last.date || date > p.last.date) {
 					 p.last = { value : val, unit : unit, date : date }; 					 
 				 }
 				 if (!p.best.value || val > p.best.value) {
 					 p.best = { value : val, unit : unit, date : date };
 				 }
 				 if (val < p.worst.value) {
					 p.worst = { value : val, unit : unit, date : date };
				 }
 				 p.avg.value += val;
 				 p.avg.count += 1;
 				  				  				
 				 var tim = date.getTime() / (1000 * 60 * 60 * 24);
 			     p.trend.sumX += tim;
 			     p.trend.sumY += val;
 			     p.trend.sumXY += tim * val;
 				 p.trend.sumXSq += tim * tim;
 				  				 				
 			  }
 			  } else {
 				 if (record.data.valueQuantity && record.data.effectiveDateTime) {
 				   var c = cmap[record.content];
 				   c.recs.push(record);
 				 } 				  
 			  }
 			});
 			
 			var selected = [];
 			var later = [];
 			angular.forEach(previews, function(p) { 
 				if (p.count < 2) {
 					p.trend = undefined;
 				} else {
 				    p.trend.value = (((p.trend.sumXY - p.trend.sumX * p.trend.sumY / p.count) ) / (p.trend.sumXSq - p.trend.sumX * p.trend.sumX / p.count));
 				    if (Math.abs(p.trend.value) > 100) p.trend.value = p.trend.value.toFixed(1); else p.trend.value = p.trend.value.toFixed(3); 
 				}
 				if (p.goal && p.count > 0) {
	 				p.stars = $scope.check(p.last,p.goal) + $scope.check(p.avg,p.goal) + $scope.check(p.best,p.goal)+ $scope.check(p.worst,p.goal)+((p.trend && p.trend.value > 0) ? 1 : 0);	 				
	 				p.starsFull = new Array(p.stars);
	 				p.starsEmpty = new Array(5-p.stars);
 				} else p.stars = undefined;
 				
 				p.info = $scope.buildAxes(p);
 				$scope.build(p.info, p);
 				
 				p.hint = "default_hint."+p.display;
 				console.log($state.params);
 				if ($state.params.measure === p.content) {
 					selected.splice(0,0,p);
 					p.tab = "chart";
 				} else {
	 				if (!p.hide || (p.recs && p.recs.length>0)) {
	 					p.tab = p.recs.length > 1 ? "chart" : "intro";
	 					if (p.recs && p.recs.length > 0) selected.push(p); else later.push(p); 
	 				}
 				}
 			});
 			
 			return selected.concat(later);
 		};
 		
 		$scope.init(); 		 		
 	 		
 		$scope.showSingle = function(record) {
 			$scope.record = record;
 			$scope.mode = "record";
 		};
 		
 		$scope.showDetails = function(preview) {
 		

 			midataPortal.openApp("page", "fhir-observation", { measure : preview.content, path :"/chart" });
 			/*if (record.content) {
 			midataPortal.openLink("page", "dist/index.html#/chart?authToken=:authToken", { measure : record.content });
 			} else {
 				midataPortal.openLink("page", "dist/index.html#/overview?authToken=:authToken", {  });
 			}*/
 		};
 		
 		$scope.showAdd = function(preview) {
 		
 			midataPortal.openApp("modal", "fhir-observation", { measure : preview.content, path :"/chart" });
 		};
 		
 		$scope.showInstall = function(preview) {
 			
 			midataPortal.openApp("page", "market", { "tag" : "Import" });
 		};
 		
 		$scope.showCalendar = function(preview) { 	
 			
 			var c = (preview.content ? [ preview.content ] : []).concat((preview.chart ? preview.chart : [])); 			
 			midataPortal.openApp("page", "calendar", { "content" : c.join(",") });
 		};
 		
 		$scope.addConsent = function(preview) { 			
 			midataPortal.openApp("page", "newconsent", { "content" : preview.content });
 		};
 		
 		$scope.showChart = function(preview) {
 			var report = { 
 					type : "line", 			
 	 				series : "owner",
 	 				label : "dateTime",
 	 				filterLabel : "measure",
 	 				timeUnits : true 
 	 		};
 			midataPortal.openApp("page", "charts", report);
 		};
 		
 		$scope.saveGoal = function(entry) {
 			entry.goal.value = Number(entry.goal.value);
 		   if (!configuration.config[entry.display]) configuration.config[entry.display] = {}; 
 		   configuration.config[entry.display].goal = entry.goal; 		   
 		   configuration.save();
 		};
 		 			 		 		 				 	 		
 		$scope.getLabel = data.getLabel; 		
 		$scope.getCodeableConcept = data.getCodeableConcept;
        $scope.data = data; 
          
        

    	$scope.map = function(valuearray) {
    	   var result = {};
    	   for (var i=0;i<valuearray.length;i++) { result[valuearray[i]] = i; }
    	   return result;
    	};
    	
    	$scope.buildAxes = function(preview) {
    	  var aTime = {};       
    	  var aContext = {};
    	
    	  var aUnit = {};
    	  angular.forEach(preview.recs, function(entry) {
    	      aTime[entry.data.effectiveDateTime] = true;           
    	      aContext[entry.content] = true;            
    	  });
    	  var result = {
    	          dateTime : [],            
    	          context : [],            
    	          units : []
    	  };
    	  var dateTimeIdx = 0, contextIdx = 0, unitIdx = 0;
    	  angular.forEach(aTime, function(v,k) { result.dateTime[dateTimeIdx++] = k; });        
    	  angular.forEach(aContext , function(v,k) { result.context[contextIdx++] = k; });            	  
    	  
    	  result.dateTime.sort();        
    	  result.context.sort();        
    	          
    	  return result;
    	};
    	
    	$scope.build = function(info, preview) {
    	              
    	   var dates = function(a) {
    		 var r = [];
    		 angular.forEach(a, function(x) {
    			r.push($filter('date')(x, "dd.MM.yyyy")); 
    		 });
    		 return r;
    	   };
    	   
    	   var shorten = function(a) {
    	       var r = [];
    	       angular.forEach(a, function(x) {
    	     	  //x = $scope.getLabel(dim, x);
    	    	  x = fhirinfo.codeToLabel[x] || x;
    	     	  if (x.length && x.length > 15) r.push(x.substr(0,13) + "..."); else r.push(x); 
    	       });
    	       return r;
    	   };
    	   var labelAxis = "dateTime";
    	   var seriesAxis = "context";
    	   
    	   preview.labels = dates(info[labelAxis]);
    	   preview.series = shorten(info[seriesAxis]);
    	   
    	   var labelMap = $scope.map(info[labelAxis]);
    	   var seriesMap = $scope.map(info[seriesAxis]);
    	   var d = preview.chartdat = [];
    	   var h = [];
    	  
    	   angular.forEach(preview.series, function() { d.push(new Array(preview.labels.length).fill(0)); });
    	   angular.forEach(preview.recs, function(entry) {
    	       d[seriesMap[entry.content]][labelMap[entry.data.effectiveDateTime]] = entry.data.valueQuantity.value;
    	   });
    	      
    	 
    	};
 		
}]);