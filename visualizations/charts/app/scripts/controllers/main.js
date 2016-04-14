'use strict';

angular.module('chartApp')
  .controller('MainCtrl', ['$scope', '$filter', '$routeParams', '$timeout', 'midataServer', 'midataPortal', 
    function ($scope, $filter, $routeParams, $timeout, midataServer, midataPortal) {
      window.scope = $scope;
      $scope.authToken = $routeParams.authToken;
      
      $scope.labels = [];
      $scope.series = [];
      $scope.data = [];
      $scope.raw = [];
      $scope.chartType = "none";
      $scope.filter = [];
      $scope.selectedFilter = null;
      $scope.filter2 = [];
      $scope.selectedFilter2 = null;
      $scope.allSeries = [];
      $scope.selectedSeries = [];
      
      $scope.allLabels = [];
      $scope.selectedLabels = [];
      
      $scope.report = {};
      $scope.reports = [];
      $scope.unit = null;
      $scope.name = "";
      $scope.config = {};
      $scope.timeUnit = "";
      $scope.timeUnits = [ { name : "" }, { name : "month"}, {name : "year" }];
      $scope.algs = [ { name : "-", value:"simple" }, { name : "Average", value:"avg" }, { name : "Newest", value : "newest"}, { name : "Count", value :"count"}, { name : "Sum", value : "sum" }];
      $scope.timings = [
         { value : 7, label : "Last 7 days"},
         { value : 30, label : "Last 30 days"},
         { value : 90, label : "Last 90 days"},
         { value : 365, label : "Last 365 days"},
         { value : 0, label : "User defined" }
      ];             
      $scope.saving = false;
      $scope.saving2 = false;
      $scope.readonly = false;
      $scope.contextCache = [];
      
      $scope.valuesToLabel = { owner : {}, content: {}, context:{} };
      //ownerNameToOwner = {};
      
      $scope.datePickers = {};
      $scope.dateOptions = {
    	 formatYear: 'yy',
    	 startingDay: 1
      };
      $scope.minDate = null;
      $scope.maxDate = null;
      $scope.nextUpdate = null;
      $scope.lastDataDate = null;
      $scope.timing = 30;
      
      midataPortal.autoresize();
      
      $scope.reloadSummary = function() {
          var p = { format : ["fhir/Observation"] /*, subformat : ["Quantity"] */ };
          midataServer.getSummary($scope.authToken, "SINGLE", p, ["ownerName" ])
          .then(function(results) {
              var entries = results.data;
              console.log(entries);         
              var info = $scope.buildAxesFromSummary(entries);
              $scope.info = info;            
              console.log(info);
              $scope.showBest(info);
              $scope.loadLabels(info).then(function() { 
            	  $scope.prepareReport(); 
              });
                          
          });
      };
      
      $scope.delayedUpdate = function() {
    	  if ($scope.timing > 0 && $scope.timing < 90) $scope.timeUnit = "";
    	  $scope.chartType = "update";
      };
      
      $scope.doDelayedUpdate = function() {
    	 $scope.prepareFilter(); 
      };
      
      /*
      $scope.reload = function() {
          var p = { };
          if (document.location.href.indexOf("/preview") >= 0) p = { "limit" : 100 };
          midataServer.getRecords($scope.authToken, p, ["owner", "created", "ownerName", "content", "format", "data"])
          .then(function(results) {
              $scope.raw = results.data;
              $scope.prepare();
          });
      };*/
      
      $scope.extractData = function(records) {
          var entries = [];
          var idx = 0;
          angular.forEach(records, function(record) {
              var cdate = new Date(record.created).toISOString();
              if (record.data.resourceType == "Observation") {
            	  var q = record.data.valueQuantity || { value : 1 };
            	  var cnt = "";
            	  if (record.data.code && record.data.code.coding && record.data.code.coding[0].display) cnt = record.data.code.coding[0].display; 
            	  var dateTime = record.data.effectiveDateTime || cdate;
            	  var e = {
                          value : Number(q.value),
                          unit : q.unit,                              
                          content : record.content,
                          context : cnt,
                          dateTime : dateTime,                              
                          owner : record.owner ? record.owner.$oid : "?"
                  };
                  if (Number.isFinite(e.value)) entries[idx++] = e;   
              } else {
              angular.forEach(record.data, function(lst, content) {
                  angular.forEach(lst, function(entry) {
                      var dateTime = entry.dateTime || entry.date || new Date(record.created);
            
                      var e = {
                              value : (Number(entry.value) || Number(entry.amount) || Number(entry[content])),
                              unit : entry.unit,                              
                              content : record.content,
                              context : entry.context,
                              dateTime : dateTime,                              
                              owner : record.owner ? record.owner.$oid : "?"
                      };
                      if (Number.isFinite(e.value)) entries[idx++] = e;
                  });
              });
              }
          });
          return entries;
      };
      
      $scope.makeSimpleTime = function(entries, granularity) {
          if (granularity == "year") {
              angular.forEach(entries, function(entry) {
                  entry.dateTime = new Date(entry.dateTime).getFullYear();                   
              });
          } else if (granularity == "month"){
              angular.forEach(entries, function(entry) {
                  var dt = new Date(entry.dateTime);
                  var year = dt.getFullYear();
                  entry.dateTime = year+"-"+(dt.getMonth() + 1);  
              });
          }
      };
      
      $scope.buildAxes = function(entries) {
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
          
          //$scope.contextCache = result.context;
          return result;
      };
      
      $scope.buildAxesFromSummary = function(info) {
          var aTime = {};
          var aOwner = {};
          var aContext = {};
          var aFormat = {};
          var aUnit = {};
          angular.forEach(info, function(entry) {
              aTime[entry.oldest] = true;
              aTime[entry.newest] = true;
              
              $scope.valuesToLabel.owner[entry.owners[0]] = entry.ownerNames[0];
              
              angular.forEach(entry.owners, function(on) { aOwner[on] = true; });
              angular.forEach(entry.contents, function(on) { aFormat[on] = true; });
              //aContext[entry.context] = true;
              //aFormat[entry.content] = true;
              //aUnit[entry.unit] = true;
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
          //angular.forEach(aContext , function(v,k) { result.context.push(k); });
          angular.forEach(aFormat , function(v,k) { result.content[contentIdx++] = k; });
          //angular.forEach(aUnit , function(v,k) { result.units.push(k); });
          
          result.context = $scope.contextCache;
          result.dateTime.sort();
          result.owner.sort();
          //result.context.sort();
          result.content.sort();
          
          result.hasMultipleDates = result.dateTime.length > 1;
          result.hasMultipleOwners = result.owner.length > 1;
          //result.hasMultipleContexts = result.context.length > 1;
          result.hasMultipleFormats = result.content.length > 1;
          return result;
      };
      
      $scope.loadLabels = function(info) {
    	 return midataServer.searchContent($scope.authToken, { content : info.content }, [ "content", "label" ])
    	 .then(function(result) {
    		angular.forEach(result.data, function(d) {
    			$scope.valuesToLabel.content[d.content] = d.label["en"];
    		});
    	 });
      };
      
      $scope.map = function(valuearray) {
          var result = {};
          for (var i=0;i<valuearray.length;i++) { result[valuearray[i]] = i; }
          return result;
      };
      
      $scope.doFilter = function(entries, prop, value) {
          return $filter('filter')(entries, function(entry) { return entry[prop] == value; });
      };
      
      $scope.dateFilter = function(entries, minDate, maxDate) {
          return $filter('filter')(entries, function(entry) {
        	  var d = new Date(entry.dateTime);
        	  if (minDate && d < minDate) return false;
        	  if (maxDate && d > maxDate) return false;
        	  return true;
          });
      };
      
      $scope.getLabel = function(dimension, value) {
    	  
    	 var dimVals = $scope.valuesToLabel[dimension];
    	 if (dimVals != null) {
    		var r = dimVals[value];
    		if (r!=null) return r;
    	 } 
    	 return value;
      };
      
      
      
      $scope.build = function(labelAxis, seriesAxis, info, entries, alg) {
          console.log("build");
          if (info.units.length == 1) { $scope.unit = info.units[0]; } else { $scope.unit = ""; }
          
          if (alg == "first") {
              $scope.entry = entries[0];
              return;
          }
          var shorten = function(dim, a) {
              var r = [];
              angular.forEach(a, function(x) {
            	  x = $scope.getLabel(dim, x);
            	  if (x.length && x.length > 15) r.push(x.substr(0,13) + "..."); else r.push(x); 
              });
              return r;
          };
          
          $scope.labels = shorten(labelAxis, info[labelAxis]);
          $scope.series = shorten(seriesAxis, info[seriesAxis]);
          
          var labelMap = $scope.map(info[labelAxis]);
          var seriesMap = $scope.map(info[seriesAxis]);
          var d = $scope.data = [];
          var h = [];
          if (alg == null || alg == "simple") {
              angular.forEach($scope.series, function() { d.push(new Array($scope.labels.length).fill(0)); });
              angular.forEach(entries, function(entry) {
                  d[seriesMap[entry[seriesAxis]]][labelMap[entry[labelAxis]]] = entry.value;
              });
          } else if (alg == "newest") {
              angular.forEach($scope.series, function() { 
                  d.push(new Array($scope.labels.length).fill(0));
                  h.push(new Array($scope.labels.length));
              });
              angular.forEach(entries, function(entry) {
                  var s = seriesMap[entry[seriesAxis]];
                  var l = labelMap[entry[labelAxis]];
                  if (!h[s][l] || entry.dateTime > h[s][l]) {
                    d[s][l] = entry.value;
                    h[s][l] = entry.dateTime;
                  }
              });
          } else if (alg == "avg") {
              angular.forEach($scope.series, function() { 
                  d.push(new Array($scope.labels.length).fill(0));
                  h.push(new Array($scope.labels.length));
              });
              angular.forEach(entries, function(entry) {
                  var s = seriesMap[entry[seriesAxis]];
                  var l = labelMap[entry[labelAxis]];
                  var hx = h[s][l];
                  if (!hx) {
                    d[s][l] = entry.value;
                    h[s][l] = [ entry.value, 1 ];
                  } else {
                    h[s][l] = [hx[0] + entry.value, hx[1] + 1];
                    d[s][l] = hx[0] / hx[1];
                  }
              });
          } else if (alg == "sum") {
              angular.forEach($scope.series, function() { 
                  d.push(new Array($scope.labels.length).fill(0));
                  //h.push(new Array($scope.labels.length));
              });
              angular.forEach(entries, function(entry) {
                  var s = seriesMap[entry[seriesAxis]];
                  var l = labelMap[entry[labelAxis]];
                  var hx = d[s][l];
                  if (!hx) {
                    d[s][l] = entry.value;                    
                  } else {                    
                    d[s][l] = hx + entry.value;
                  }
              });
          } else if (alg == "count") {
              angular.forEach($scope.series, function() { 
                  d.push(new Array($scope.labels.length).fill(0));
                  //h.push(new Array($scope.labels.length));
              });
              angular.forEach(entries, function(entry) {
                  var s = seriesMap[entry[seriesAxis]];
                  var l = labelMap[entry[labelAxis]];
                  var hx = d[s][l];
                  if (!hx) {
                    d[s][l] = 1;                    
                  } else {                    
                    d[s][l] = hx + 1;
                  }
              });
          }
          console.log(alg);
          //console.log(d);
      };
               
      $scope.prepare = function() {
         var entries = $scope.extractData($scope.raw);
         //console.log(entries);         
         var info = $scope.buildAxes(entries);
         $scope.info = info;
         $scope.entries = entries;
         if (entries.length == 0) {
             $scope.chartType = "none";
             return;
         }
         //console.log(info);
         $scope.showBest(info);
         console.log($scope.report);
                 
         $scope.prepareReport();
      };
      
      $scope.initialSelection = function(all) {
    	if (all.length <= 5) return all.slice();
    	return all.slice(0,5);
      };
      
      $scope.prepareReport = function() {
          console.log("prepareReport");
          $scope.chartType = $scope.report.type;
          console.log($scope.report);
          $scope.allSeries = $scope.info[$scope.report.series];   
          $scope.selectedSeries = $scope.config.selectedSeries != null ? $scope.config.selectedSeries : $scope.initialSelection($scope.allSeries);
          
          $scope.allLabels = $scope.info[$scope.report.label];   
          $scope.selectedLabels = $scope.config.selectedLabels != null ? $scope.config.selectedLabels : $scope.initialSelection($scope.allLabels);
            
          if ($scope.report.filter) {
                 $scope.filter = $scope.info[$scope.report.filter];
                 $scope.selectedFilter = $scope.config.filter != null ? $scope.config.filter : $scope.filter[0];
          }
                  
          $scope.prepareFilter();              
          
          
                    
      };
      
      $scope.prepareFilter = function() {    	  
    	  
    	  console.log($scope.info);
          var p = { };
          $scope.chartType = "calculation";
          if ($scope.report.filter != null && $scope.report.filter == "content") p.content = [ $scope.selectedFilter ];
          if ($scope.report.filter != null && $scope.report.filter == "owner") p.owner = [ $scope.selectedFilter ];
          if ($scope.report.series == "content") {
              p.content = $scope.selectedSeries;
          } else if ($scope.report.series == "owner") {
              p.owner = [];
              angular.forEach($scope.selectedSeries, function(owner) {
            	  var owref = owner;
            	  
            	  p.owner.push(owref); 
              });
          }
          if ($scope.report.label == "content") {
              p.content = $scope.selectedLabels;
          } else if ($scope.report.label == "owner") {
              p.owner = [];
              angular.forEach($scope.selectedLabels, function(owner) {
            	  var owref = owner;
            	  
            	  p.owner.push(owref); 
              });
          }
          
          if (document.location.href.indexOf("/preview") >= 0) {
        	  if ($scope.timeUnit == "" && $scope.timing==0) p.limit = 100;        	  
          }
          var min = $scope.minDate;
          var max = $scope.maxDate;
          if ($scope.timing != 0) {
        	max = new Date();
        	min = new Date(max.getTime() - 1000 * 60 *60*24*$scope.timing);
          }
          var divisor = (p.content ? p.content.length : 1) * (p.owner ? p.owner.length : 1);
          if (divisor == 0) divisor = 1;
          
          if (min!= null && max!=null) {
        	  p.index = { "effectiveDateTime" : { "$ge" : min, "$le" : max }};
          }
          console.log(p);
          midataServer.getRecords($scope.authToken, p, ["owner", "created", "ownerName", "content", "format", "data"])
          .then(function(results) {
              $scope.raw = results.data;
              $scope.entries = $scope.extractData($scope.raw);
              $scope.entries = $scope.dateFilter($scope.entries, min, max);
              console.log($scope.timeUnit);
              console.log("Divisor:"+divisor);
              console.log("Entries:"+$scope.entries.length);
              console.log("Ratio:"+ ($scope.entries.length/divisor));
              if ($scope.timeUnit == "" && $scope.entries.length / divisor > 60) $scope.timeUnit = "month";
              
              if ($scope.timeUnit != "") {
                  $scope.makeSimpleTime($scope.entries, $scope.timeUnit);
              }
              $scope.chartType = $scope.report.type;
          
          
          console.log("prepareFilter");
                    
          var filteredEntries = $scope.report.filter ? $scope.doFilter($scope.entries, $scope.report.filter, $scope.selectedFilter) : $scope.entries;
          var filteredInfo = $scope.report.filter ? $scope.buildAxes(filteredEntries) : $scope.buildAxes($scope.entries);
          
          $scope.info.context = filteredInfo.context;
          $scope.info.hasMultipleContexts = filteredInfo.context.length > 1;
          if ($scope.info.hasMultipleContexts) {
        	  $scope.showBest($scope.info, true);
          }
          
          if ($scope.report.filter2) {
             $scope.filter2 = filteredInfo[$scope.report.filter2];
             $scope.selectedFilter2 = $scope.config.filter2 != null ? $scope.config.filter2 : $scope.filter2[0];
          }
          $scope.entries1 = filteredEntries;
          $scope.info1 = filteredInfo;
          
          $scope.update();
          
          });
      };
      
      $scope.update = function() {
          console.log("update");
          console.log($scope.report);
          var filteredEntries = $scope.report.filter2 != null ? $scope.doFilter($scope.entries1, $scope.report.filter2, $scope.selectedFilter2) : $scope.entries1;
          var filteredInfo = $scope.report.filter2 != null ? $scope.buildAxes(filteredEntries) : $scope.info1;
          var alg = $scope.report.alg || "simple";
          if (alg == "simple" && $scope.timeUnit != "") alg = "avg";
          $scope.build($scope.report.label, $scope.report.series, filteredInfo, filteredEntries, alg);
          //console.log(filteredEntries);
          //console.log(filteredInfo);
          //console.log($scope.series);
          $scope.config = {};
      };
      
      $scope.showBest = function(info, updateonly) {
         var r = [];
         if (info.hasMultipleDates) {
             if (info.hasMultipleOwners) {
                r.push( { name:"Time Series per Person", type : "line", label : "dateTime", series : "owner", seriesName : "People", filter: info.hasMultipleFormats ? "content" : null, filterLabel: info.hasMultipleFormats ? "Measure" : null, filter2: info.hasMultipleContexts ? "context" : null, filterLabel2: info.hasMultipleContexts ? "Context" : null, timeUnits : true } );
             }
             
             if (info.hasMultipleFormats) {
                r.push( { name:"Time Series per Measure", type : "line", label : "dateTime", series : "content", seriesName : "Measures", filter: info.hasMultipleOwners ? "owner" : null, filterLabel: info.hasMultipleOwners ? "Person" : null, filter2: info.hasMultipleContexts ? "context" : null, filterLabel2: info.hasMultipleContexts ? "Context" : null, timeUnits : true } );
             }
             
             if (info.hasMultipleContexts) {
                   r.push( { name:"Time Series per Context", type : "line", label : "dateTime", series : "context", seriesName : "Measures", filter: info.hasMultipleFormats ? "content" : null , filterLabel: info.hasMultipleFormats ? "Measure" : null, filter2: info.hasMultipleOwners ? "owner" : null, filterLabel2: info.hasMultipleOwners ? "Person" : null, timeUnits : true } );                 
             }
             
             if (!info.hasMultipleOwners) {
                 r.push( { name:"Time Series per Person", type : "line", label : "dateTime", series : "owner", seriesName : "People", filter: info.hasMultipleFormats ? "content" : null, filterLabel: info.hasMultipleFormats ? "Measure" : null, filter2: info.hasMultipleContexts ? "context" : null, filterLabel2: info.hasMultipleContexts ? "Context" : null, timeUnits : true } ); 
             }
         }
         
         if (info.hasMultipleFormats) {
             if (info.hasMultiplePersons) {                 
                 r.push( { name:"Radar-Chart: Format/Person", type : "radar", label : "content", labelsName : "Measures", series : "owner", seriesName : "People", filter: null, filterLabel: null, alg : "newest" } );
             }
             r.push( { name:"Bar-Chart: Format/Person", type : "bar", label : "content", labelsName : "Measures", series : "owner", seriesName : "People", filter: null, filterLabel: null, alg : "newest" } );            
         }
         
         if (info.hasMultipleContexts) {
             if (info.hasMultiplePersons) {                 
                 r.push( { name:"Radar-Chart: Context/Person", type : "radar", label : "context", labelsName : "Contexts", series : "owner", seriesName : "People", filter: info.hasMultipleFormats ? "content" : null, filterLabel: info.hasMultipleFormats ? "Measure" : null, alg : "newest" } );
             }
             r.push( { name:"Bar-Chart: Context/Person", type : "bar", label : "context", labelsName : "Contexts", series : "owner", seriesName : "People", filter: info.hasMultipleFormats ? "content" : null, filterLabel: info.hasMultipleFormats ? "Measure" : null, alg : "newest" });                          
         }
         
         if (info.hasMultiplePersons && !info.hasMultipleFormats && !info.hasMultipleContexts) {
             r.push( { name:"Bar-Chart: Persons", type : "bar", label : "owner", labelsName : "People", series : "content", seriesName : "Contexts", filter: null, filterLabel: null, alg : "newest" });
         }
         
         if (r.length==0) {
             r.push( { name:"Single Value", type:"simple", label:"content", series:"owner", filter:null, filterLabel : null, alg : "first" });
         }
         
         
         
         if (updateonly) {
           angular.forEach(r, function(report) {
        	  if ($filter('filter')($scope.reports, function(rep) { return rep.name  == report.name; }).length == 0) {
        		  $scope.reports.push(report);
        	  }  
           });        	 
         } else {
           $scope.reports = r;
           $scope.report = ($scope.config.report != null) ? ( $filter('filter')($scope.reports, function(r) { return r.name  == $scope.config.report.name; })[0] ) : r[0];
         }
      };
      
      $scope.loadConfig = function() {
          midataServer.getConfig($scope.authToken)
          .then(function (result) {
            if (result.data) {
                if (result.data && result.data.readonly) {
                    $scope.readonly = true;
                } else {
                    $scope.config = result.data;
                    $scope.timeUnit = result.data.timeUnit || "";
                    $scope.timing = Number(result.data.timing) || 30;
                }
                /*$scope.report = result.report;
                $scope.selectedFilter = result.filter;
                $scope.selectedFilter2 = result.filter2;*/
            }  
            $scope.reloadSummary();
          });
          
      };
      
      $scope.saveConfig = function() {
         var config = { report : $scope.report, filter : $scope.selectedFilter, filter2: $scope.selectedFilter2, timeUnit : $scope.timeUnit, timing : $scope.timing, selectedSeries : $scope.selectedSeries, selectedLabels : $scope.selectedLabels };
         $scope.saving = true;
         midataServer.setConfig($scope.authToken, config)
         .then(function() { $scope.saving = false; });
      };
      
      $scope.add = function(name) {
         var config = { report : $scope.report, filter : $scope.selectedFilter, filter2: $scope.selectedFilter2, timeUnit : $scope.timeUnit, timing : $scope.timing, selectedSeries : $scope.selectedSeries, selectedLabels : $scope.selectedLabels };
         $scope.saving2 = true;
         midataServer.cloneAs($scope.authToken, name, config)
         .then(function() { $scope.saving2 = false; });
      };
      
      $scope.toggleSeriesSelection = function(what) {
          var idx = $scope.selectedSeries.indexOf(what);
          if (idx >= 0) {
              $scope.selectedSeries.splice(idx, 1);
          } else {
              $scope.selectedSeries.push(what);
          }
          $scope.delayedUpdate();
      };
      
      $scope.isSelectedSeries = function(what) {
          return $scope.selectedSeries.indexOf(what) >= 0;
      };
      
      $scope.toggleLabelSelection = function(what) {
          var idx = $scope.selectedLabels.indexOf(what);
          if (idx >= 0) {
              $scope.selectedLabels.splice(idx, 1);
          } else {
              $scope.selectedLabels.push(what);
          }
          $scope.delayedUpdate();
      };
      
      $scope.isSelectedLabel = function(what) {
          return $scope.selectedLabels.indexOf(what) >= 0;
      };
      
      $scope.loadConfig();
      
    }]);


 
