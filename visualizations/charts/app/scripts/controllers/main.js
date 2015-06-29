'use strict';

angular.module('chartApp')
  .controller('MainCtrl', ['$scope', '$filter', '$routeParams', 'server', 
    function ($scope, $filter, $routeParams, server) {
      
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
	  $scope.report = {};
	  $scope.reports = [];
	  $scope.unit = null;
	  $scope.name = "";
	  $scope.config = {};
	  
	  $scope.saving = false;
	  $scope.saving2 = false;
	  $scope.readonly = false;
	  
	  $scope.reload = function() {
		  server.getRecords($scope.authToken, { }, ["owner", "created", "ownerName", "format", "data"])
		  .then(function(results) {
			  $scope.raw = results.data;
			  $scope.prepare();
		  });
	  };
	  
	  $scope.extractData = function(records) {
		  var entries = [];
		  angular.forEach(records, function(record) {
			  var cdate = new Date(record.created).toISOString();
			  angular.forEach(record.data, function(lst, format) {
				  angular.forEach(lst, function(entry) {
					  var e = {
							  value : (Number(entry.value) || Number(entry.amount) || Number(entry[format])),
							  unit : entry.unit,
							  format : format,
							  context : entry.context,
							  dateTime : (entry.dateTime || entry.date || record.created),
							  owner : record.ownerName
					  };
					  if (Number.isFinite(e.value)) entries.push(e);
				  });
			  });
		  });
		  return entries;
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
			  aFormat[entry.format] = true;
			  aUnit[entry.unit] = true;
		  });
		  var result = {
				  dateTime : [],
		          owner : [],
		          context : [],
		          format : [],
		          units : []
		  };
		  angular.forEach(aTime, function(v,k) { result.dateTime.push(k); });
          angular.forEach(aOwner, function(v,k) { result.owner.push(k); });
          angular.forEach(aContext , function(v,k) { result.context.push(k); });
          angular.forEach(aFormat , function(v,k) { result.format.push(k); });
          angular.forEach(aUnit , function(v,k) { result.units.push(k); });
          
          result.dateTime.sort();
          result.owner.sort();
          result.context.sort();
          result.format.sort();
          
          result.hasMultipleDates = result.dateTime.length > 1;
          result.hasMultipleOwners = result.owner.length > 1;
          result.hasMultipleContexts = result.context.length > 1;
          result.hasMultipleFormats = result.format.length > 1;
		  return result;
	  };
	  
	  $scope.map = function(valuearray) {
		  var result = {};
		  for (var i=0;i<valuearray.length;i++) { result[valuearray[i]] = i; }
		  return result;
	  };
	  
	  $scope.doFilter = function(entries, prop, value) {
		  return $filter('filter')(entries, function(entry) { return entry[prop] == value; });
	  };
	  
	  $scope.build = function(labelAxis, seriesAxis, info, entries, alg) {
		  if (info.units.length == 1) { $scope.unit = info.units[0]; } else { $scope.unit = ""; }
		  
		  if (alg == "first") {
			  $scope.entry = entries[0];
			  return;
		  }
		  var shorten = function(a) {
			  var r = [];
			  angular.forEach(a, function(x) { if (x.length && x.length > 15) r.push(x.substr(0,13) + "..."); else r.push(x); });
			  return r;
		  }
		  
		  $scope.labels = shorten(info[labelAxis]);
		  $scope.series = shorten(info[seriesAxis]);
		  
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
			  // TODO
		  }
		  console.log(alg);
		  console.log(d);
	  };
	  		 
	  $scope.prepare = function() {
		 var entries = $scope.extractData($scope.raw);
		 console.log(entries);		 
		 var info = $scope.buildAxes(entries);
		 $scope.info = info;
		 $scope.entries = entries;
		 if (entries.length == 0) {
			 $scope.chartType = "none";
			 return;
		 }
		 console.log(info);
		 $scope.showBest(info);
		 console.log($scope.report);
				 
		 $scope.prepareReport();
	  };
	  
	  $scope.prepareReport = function() {
		  console.log("prepareReport");
		  $scope.chartType = $scope.report.type;
		  
		  if ($scope.report.filter) {
		    $scope.filter = $scope.info[$scope.report.filter];
		    $scope.selectedFilter = $scope.config.filter != null ? $scope.config.filter : $scope.filter[0];
		  }
		  
		  $scope.prepareFilter();
	  };
	  
	  $scope.prepareFilter = function() {
		  console.log("prepareFilter");
		  		  
		  var filteredEntries = $scope.report.filter ? $scope.doFilter($scope.entries, $scope.report.filter, $scope.selectedFilter) : $scope.entries;
		  var filteredInfo = $scope.report.filter ? $scope.buildAxes(filteredEntries) : $scope.info;
		  if ($scope.report.filter2) {
			 $scope.filter2 = filteredInfo[$scope.report.filter2];
			 $scope.selectedFilter2 = $scope.config.filter2 != null ? $scope.config.filter2 : $scope.filter2[0];
		  }
		  $scope.entries1 = filteredEntries;
		  $scope.info1 = filteredInfo;
		  
		  $scope.update();
	  };
	  
	  $scope.update = function() {
		  console.log("update");
		  console.log($scope.report);
		  var filteredEntries = $scope.report.filter2 ? $scope.doFilter($scope.entries1, $scope.report.filter2, $scope.selectedFilter2) : $scope.entries1;
		  var filteredInfo = $scope.report.filter2 ? $scope.buildAxes(filteredEntries) : $scope.info1;
		  $scope.build($scope.report.label, $scope.report.series, filteredInfo, filteredEntries, $scope.report.alg);
		  console.log(filteredEntries);
		  console.log(filteredInfo);
		  console.log($scope.series);
	  };
	  
	  $scope.showBest = function(info) {
		 var r = [];
		 if (info.hasMultipleDates) {
			 if (info.hasMultipleOwners) {
			    r.push( { name:"Time Series per Person", type : "line", label : "dateTime", series : "owner", filter: info.hasMultipleFormats ? "format" : null, filterLabel: info.hasMultipleFormats ? "Measure" : null, filter2: info.hasMultipleContexts ? "context" : null, filterLabel2: info.hasMultipleContexts ? "Context" : null } );
			 }
			 
			 if (info.hasMultipleFormats) {
				r.push( { name:"Time Series per Measure", type : "line", label : "dateTime", series : "format", filter: info.hasMultipleOwners ? "owner" : null, filterLabel: info.hasMultipleOwners ? "Person" : null, filter2: info.hasMultipleContexts ? "context" : null, filterLabel2: info.hasMultipleContexts ? "Context" : null } );
			 }
			 
			 if (info.hasMultipleContexts) {
   			    r.push( { name:"Time Series per Context", type : "line", label : "dateTime", series : "context", filter: info.hasMultipleFormats ? "format" : null , filterLabel: info.hasMultipleFormats ? "Measure" : null, filter2: info.hasMultipleOwners ? "owner" : null, filterLabel2: info.hasMultipleOwners ? "Person" : null } );				 
			 }
			 
			 if (!info.hasMultipleOwners) {
				 r.push( { name:"Time Series per Person", type : "line", label : "dateTime", series : "owner", filter: info.hasMultipleFormats ? "format" : null, filterLabel: info.hasMultipleFormats ? "Measure" : null, filter2: info.hasMultipleContexts ? "context" : null, filterLabel2: info.hasMultipleContexts ? "Context" : null } ); 
			 }
		 }
		 
		 if (info.hasMultipleFormats) {
			 if (info.hasMultiplePersons) {				 
				 r.push( { name:"Radar-Chart: Format/Person", type : "radar", label : "format", series : "owner", filter: null, filterLabel: null, alg : "newest" } );
			 }
			 r.push( { name:"Bar-Chart: Format/Person", type : "bar", label : "format", series : "owner", filter: null, filterLabel: null, alg : "newest" } );			
		 }
		 
		 if (info.hasMultipleContexts) {
			 if (info.hasMultiplePersons) {				 
				 r.push( { name:"Radar-Chart: Context/Person", type : "radar", label : "context", series : "owner", filter: info.hasMultipleFormats ? "format" : null, filterLabel: info.hasMultipleFormats ? "Measure" : null, alg : "newest" } );
			 }
			 r.push( { name:"Bar-Chart: Context/Person", type : "bar", label : "context", series : "owner", filter: info.hasMultipleFormats ? "format" : null, filterLabel: info.hasMultipleFormats ? "Measure" : null, alg : "newest" });			 			 
		 }
		 
		 if (info.hasMultiplePersons && !info.hasMultipleFormats && !info.hasMultipleContexts) {
			 r.push( { name:"Bar-Chart: Persons", type : "bar", label : "owner", series : "format", filter: null, filterLabel: null, alg : "newest" });
		 }
         
		 if (r.length==0) {
			 r.push( { name:"Single Value", type:"simple", label:"format", series:"owner", filter:null, filterLabel : null, alg : "first" });
		 }
		 
		 $scope.reports = r;
		 $scope.report = $scope.config.report != null ? $filter('filter')($scope.reports, function(r) { return r.name  == $scope.config.report.name })[0] : r[0];
	  };
	  
	  $scope.loadConfig = function() {
		  server.getConfig($scope.authToken)
		  .then(function (result) {
			if (result.data) {
				if (result.data && result.data.readonly) {
					$scope.readonly = true;
				} else {
				    $scope.config = result.data;
				}
				/*$scope.report = result.report;
				$scope.selectedFilter = result.filter;
				$scope.selectedFilter2 = result.filter2;*/
			}  
			$scope.reload();
		  });
		  
	  };
	  
	  $scope.saveConfig = function() {
		 var config = { report : $scope.report, filter : $scope.selectedFilter, filter2: $scope.selectedFilter2 };
		 $scope.saving = true;
		 server.setConfig($scope.authToken, config)
		 .then(function() { $scope.saving = false; });
	  };
	  
	  $scope.add = function(name) {
		 var config = { report : $scope.report, filter : $scope.selectedFilter, filter2: $scope.selectedFilter2 };
		 $scope.saving2 = true;
		 server.cloneAs($scope.authToken, name, config)
		 .then(function() { $scope.saving2 = false; });;
	  };
	  
	  $scope.loadConfig();
	  
    }]);


 
