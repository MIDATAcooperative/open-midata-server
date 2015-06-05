'use strict';

angular.module('chartApp')
  .controller('MainCtrl', ['$scope', '$routeParams', 'server', 
    function ($scope, $routeParams, server) {
      
	  $scope.authToken = $location.path().split("/")[1];
	  
	  $scope.labels = [];
	  $scope.series = [];
	  $scope.data = [];
	  $scope.raw = [];
	  $scope.chartType = "none";
	  $scope.filter = [];
	  $scope.selectedFilter = null;
	  $scope.report = {};
	  
	  $scope.reload = function() {
		  server.getRecords($scope.authToken, { }, ["owner", "ownerName", "format", "data"])
		  .then(function(results) {
			  $scope.raw = results.data;
			  $scope.showBest();
		  });
	  };
	  
	  $scope.extractData = function(records) {
		  var entries = [];
		  angular.forEach(records, function(record) {
			  angular.forEach(record, function(lst, format) {
				  angular.forEach(lst, function(entry) {
					  var e = {
							  value : entry.value,
							  unit : entry.unit,
							  context : format,
							  dateTime : entry.dateTime,
							  owner : record.owner
					  };
					  entries.push(e);
				  });
			  });
		  });
		  return entries;
	  };
	  
	  $scope.buildAxes = function(entries) {
		  var aTime = {};
		  var aOwner = {};
		  var aContext = {};
		  angular.forEach(entries, function(entry) {
			  aTime[entry.dateTime] = true;
			  aOwner[entry.owner] = true;
			  aContext[entry.context] = true;
		  });
		  var result = {
				  dateTime : [],
		          owner : [],
		          context : []
		  };
		  angular.forEach(aTime, function(v,k) { result.dateTime.push(k); });
          angular.forEach(aOwner, function(v,k) { result.owner.push(k); });
          angular.forEach(aContext , function(v,k) { result.context.push(k); });
          
          result.dateTime.sort();
          result.owner.sort();
          result.context.sort();
          
          result.hasMultipleDates = result.dateTime.length > 1;
          result.hasMultipleOwners = result.owner.length > 1;
          result.hasMultipleContexts = result.context.length > 1;
		  return result;
	  };
	  
	  $scope.map = function(valuearray) {
		  var result = {};
		  for (var i=0;i<valuearray.length;i++) { result[valuearray[i]] = i; }
		  return result;
	  };
	  
	  $scope.filter = function(entries, prop, value) {
		  return $filter('filter')(entries, function(entry) { return entry[prop] == value; });
	  };
	  
	  $scope.build = function(labelAxis, seriesAxis, info, entries) {
		  $scope.labels = info[labelAxis];
		  $scope.series = info[seriesAxis];
		  var labelMap = $scope.map($scope.labels);
		  var seriesMap = $scope.map($scope.series);
		  var d = $scope.data = [];
		  angular.forEach($scope.series, function() { d.push(new Array($scope.labels.length)); });
		  angular.forEach(entries, function(entry) {
			  d[seriesMap[entry[seriesAxis]]][labelMap[entry[labelAxis]]] = entry.value;
		  });
	  };
	  
	  $scope.prepare = function() {
		 var entries = $scope.extractData($scope.raw);
		 var info = $scope.buildAxes(entries);
			 
	  };
	  
	  $scope.update = function() {
	  };
	  
	  $scope.showBest = function(info) {
		 
		 if (info.hasMultipleDates) {
			 if (info.hasMultipleOwner) {
				 
			 }
			 
		 };
	  };
	  
	  
    }]);


   // Line Chart : label:time  series:owner  filter:context
   // Line chart: label:time series:context filter:owner
   // Bar chart: label:context, series:context newest:time 
   // Radar chart : label:context series:owner filter:time
   // 
