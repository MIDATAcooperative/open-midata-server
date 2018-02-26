'use strict';

angular.module('chartApp')
.factory('chartcalc', ['$filter', function($filter) {
	
	var chartcalc = {};
	   
	/*
	 * def
	 * {
	 *    label : label-dimension
	 *    series : series-dimension
	 *    timeUnit : month | year | day | none
	 *    timeUnits : boolean
	 *    
	 *    filter,filterValue
	 *    
	 *    
	 *    dimensions : {
	 *     dim-name : [values]
	 *    }
	 * }
	 * 
	 * 
	 * 
	 * setup
	 * {
	 *   all, label, series, dim-name : {
	 *     shorten : func(value, dim-name) returns shortend title
	 *     getLabel : func(value, dim-name) returns label
	 *   }
	 * }
	 * 
	 * report
	 * {
	 *    label : label array for chart.js
	 *    series : series array for chart.js
	 *    data : data array for chart.js
	 *    type : type string for chart.js (?)
	 * }
	 * 
	 * 
	 */
	
	 chartcalc.aggregations = {
		simple : function(s, l, entry, d, h) {		  
	       d[s][l] = entry.value;	       
		},
		
		newest : function(s, l, entry, d, h) {			      
	        if (!h[s][l] || entry.dateTime > h[s][l]) {
	            d[s][l] = entry.value;
	            h[s][l] = entry.dateTime;
	        }	      
		},
		
	    avg : function(s, l, entry, d, h) {		
	    	
	        var hx = h[s][l];
	        if (!hx) {
	           d[s][l] = entry.value;
	           h[s][l] = [ entry.value, 1 ];
	        } else {
	           h[s][l] = [hx[0] + entry.value, hx[1] + 1];
	           d[s][l] = hx[0] / hx[1];
	        }	        
	    },
	    
	    sum : function(s, l, entry, d, h) {		    	
	        var hx = d[s][l];
	        if (!hx) {
	           d[s][l] = entry.value;                    
	        } else {                    
	           d[s][l] = hx + entry.value;
	        }	        
	    },
	    
	    count : function(s, l, entry, d, h) {	
	    	
	        var hx = d[s][l];
	        if (!hx) {
	          d[s][l] = 1;                    
	        } else {                    
	          d[s][l] = hx + 1;
	        }	        
	    },
	    
	    max : function(s, l, entry, d, h) {	
	          
	      
	         var hx = d[s][l];
	         if (!hx) {
	             d[s][l] = entry.value;                    
	         } else {                    
	             d[s][l] = entry.value > hx ? entry.value : hx;;
	         }	      
	    },
	    
	    min : function(s, l, entry, d, h) {	    	    
	         var hx = d[s][l];
	         if (!hx) {
	            d[s][l] = entry.value;                    
	         } else {                    
	            d[s][l] = entry.value < hx ? entry.value : hx;;
	         }
	    }	    
	 
	 };
	
	 chartcalc.toIndexMap = function(valuearray) {
         var result = {};
         for (var i=0;i<valuearray.length;i++) { result[valuearray[i]] = i; }
         return result;
     };
     
     chartcalc.ident = function(x) { return x; };
     
     
     chartcalc.extractDataFromRecords = function(records) {
         var entries = [];
         var idx = 0;
         
         var addEntry = function(record,cmp,cdate) {
	       	  var q = cmp.valueQuantity || { value : 1 };
	       	  var cnt = "";
	       	  if (cmp.code && cmp.code.coding && cmp.code.coding[0].display) cnt = cmp.code.coding[0].display;
	       	  var sdate = record.data.effectivePeriod ? record.data.effectivePeriod.start : null;
	       	  var dateTime = record.data.effectiveDateTime || sdate || cdate;
	       	  var e = {
	                     value : Number(q.value),
	                     unit : q.unit,	
	                     content : record.content,
	                     context : cnt,
	                     dateTime : dateTime,  
	                     owner : record.owner ? record.owner : "?"
	          };
	          if (Number.isFinite(e.value)) entries[idx++] = e;
         };
         
         angular.forEach(records, function(record) {
             var cdate = new Date(record.created).toISOString();
             if (record.data.resourceType === "Observation") {           	             	  
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
     
     chartcalc.makeSimpleTime = function(entries, def) {
    	 def = def || "month";
    	 var granularity = def.timeUnit || def;
    	 
         if (granularity === "year") {
             angular.forEach(entries, function(entry) {
                 entry.dateTime = new Date(entry.dateTime).getFullYear();                   
             });
         } else if (granularity === "month"){
             angular.forEach(entries, function(entry) {
                 var dt = new Date(entry.dateTime);
                 var year = dt.getFullYear();
                 entry.dateTime = year+"-"+(dt.getMonth() + 1);  
             });
         }
     };
     
	 
     chartcalc.buildAxes = function(entries) {
    	     	 
         var aTime = {};
         var aOwner = {};
         var aContext = {};
         var aFormat = {};
         var aUnit = {};
         angular.forEach(entries, function(entry) {
             aTime[entry.dateTime] = true;
             aOwner[entry.owner] = true;
             aContext[entry.context] = true;
             aFormat[entry.content] = true;
             aUnit[entry.unit] = true;
         });
         var result = {
                 dateTime : [],
                 owner : [],
                 context : [],
                 content : [],
                 units : []
         };
         var dateTimeIdx = 0, ownerIdx = 0, contextIdx = 0, contentIdx = 0, unitIdx = 0;
         angular.forEach(aTime, function(v,k) { result.dateTime[dateTimeIdx++] = k; });
         angular.forEach(aOwner, function(v,k) { result.owner[ownerIdx++] = k; });
         angular.forEach(aContext , function(v,k) { result.context[contextIdx++] = k; });
         angular.forEach(aFormat , function(v,k) { result.content[contentIdx++] = k; });
         angular.forEach(aUnit , function(v,k) { result.units[unitIdx++] = k; });
         
         result.dateTime.sort();
         result.owner.sort();
         result.context.sort();
         result.content.sort();
         
         result.hasMultipleDates = result.dateTime.length > 1;
         result.hasMultipleOwners = result.owner.length > 1;
         result.hasMultipleContexts = result.context.length > 1;
         result.hasMultipleFormats = result.content.length > 1;
                
         return result;
     };
     
     
	
	chartcalc.build = function(def, setup, data /*labelAxis, seriesAxis, info, entries, alg*/) {
		
		      var chart = {};
		      
		      var dimensions = def.dimensions;
		      
		      var fromSetup = function(name, axis, dimension, def) {
		 		 if (setup[dimension] && setup[dimension][name]) return setup[dimension][name];
		 		 if (setup[axis] && setup[axis][name]) return setup[axis][name];
		 		 if (setup["all"] && setup["all"][name]) return setup["all"][name];
		 		 return def;
		 	  };
		      
	          console.log("build");	          	          
	          
	          var dates = function(x) {
	     		 return $filter('date')(x, "dd.MM.yyyy"); 	     		 
	     	   };
	          
	          var shorten = function(x, dim) {
	        	  if (dim == "dateTime" && (def.timeUnit !== "month" && def.timeUnit !== "year")) return dates(x);
	        	  
	              if (x.length && x.length > 15) return x.substr(0,13) + "...";
                  return x;
	          };
	          
	        
	          
	          var applyToAll = function(shortenFunc, labelFunc, dimension, values) {
	        	  var r = [];
	        	  var l = values.length;
	              for (var i=0;i<l;i++) {
	            	  r[i] = shortenFunc(labelFunc(values[i], dimension), dimension);	            	   
	              }
	              return r;
	          };
	          
	          // Labels
	          var short = fromSetup("shorten", "label", def.label, shorten);
	          var labelFunc = fromSetup("getLabel", "label", def.label, chartcalc.ident);
	          
	          chart.labels = applyToAll(short, labelFunc, def.label, dimensions[def.label]);
	          
	          short = fromSetup("shorten", "series", def.series, shorten);
	          labelFunc = fromSetup("getLabel", "series", def.series, chartcalc.ident);
	          chart.series = applyToAll(short, labelFunc, def.series, dimensions[def.series]);
	          
	          var labelMap = chartcalc.toIndexMap(dimensions[def.label]);
	          var seriesMap = chartcalc.toIndexMap(dimensions[def.series]);
	          var d = chart.data = [];
	          var h = [];
	          var aggr = chartcalc.aggregations[def.aggregation || "simple"];
	          	          
		      angular.forEach(chart.series, function() { 
		         d.push(new Array(chart.labels.length).fill(0));
		         h.push(new Array(chart.labels.length));
		      });
		      angular.forEach(data, function(entry) {
		    	  var s = seriesMap[entry[def.series]];
                  var l = labelMap[entry[def.label]];		          
                  aggr(s, l, entry, d, h);
              });
	          	      
		      console.log(chart);
	          
	          return chart;
	    };
	    
	return chartcalc;
	
	          
	
}]);