angular.module('fhirObservation')
.controller('ObservationCtrl', ['$scope', '$filter', '$state', 'midataServer', 'midataPortal', 'configuration', 'data', 'fhirinfo',
 	function($scope, $filter, $state, midataServer, midataPortal, configuration, data, fhirinfo) {
 		
	var measure = $scope.measure = $state.params.measure;
	
	$scope.data = data;
	
	$scope.timing = { dateFrom : null, dateTo : null, mode : '1M' };
	
	$scope.shift = function(factor) {		
		switch($scope.timing.mode) {
		case '1M': 
			$scope.timing.dateTo.setMonth($scope.timing.dateTo.getMonth() + factor);
			$scope.timing.dateFrom.setTime($scope.timing.dateTo.getTime());
			$scope.timing.dateFrom.setMonth($scope.timing.dateFrom.getMonth() - 1);
			break;
		case '3M': $scope.timing.dateTo.setMonth($scope.timing.dateTo.getMonth() + 3*factor);
		    $scope.timing.dateFrom.setTime($scope.timing.dateTo.getTime());
		    $scope.timing.dateFrom.setMonth($scope.timing.dateFrom.getMonth()  - 3);
		    break;
		case '1Y': $scope.timing.dateRo.setFullYear($scope.timing.dateTo.getFullYear() + factor);
		    $scope.timing.dateFrom.setTime($scope.timing.dateTo.getTime());
		    $scope.timing.dateFrom.setFullYear($scope.timing.dateFrom.getMonth() - 1);
		    break;
		}		
		
	};
	
	$scope.init = function() {
	   fhirinfo.getInfos(midataPortal.language, measure)
	   .then(function() {
		   if (!configuration.owner) configuration.owner = "self";
	
		   midataServer.getSummary(midataServer.authToken, "SINGLE", { format : ["fhir/Observation"], content : measure,  owner : "self" }) 			
		   .then(function(sumResult) {
			  if (!sumResult.data || sumResult.data.length === 0) return;
			  var entry = sumResult.data[0];
			  if (entry.count < 30) {
				$scope.timing.dateFrom = $scope.timing.dateTo = null;  
			  } else {
				$scope.timing.dateTo = new Date(entry.newest);
				$scope.timing.dateFrom = new Date();
				$scope.shift(0);
			  }
			  $scope.reload();   
		   });	   
	   });
	};	  
	
	$scope.reload = function() {
		var criteria = { content : measure, owner : configuration.owner };
		if ($scope.timing.dateFrom) criteria.after = $scope.timing.dateFrom;
		if ($scope.timing.dateTo) criteria.before = $scope.timing.dateTo;
		data.getRecords(criteria)
		   .then(function(records) {
			  $scope.records = records; 
			  if (records.length > 0) configuration.owner = records[0].owner.$oid;
			  console.log(configuration.owner);
			  var entries = $scope.extractData(records);
			  var info = $scope.buildAxes(entries);
			  $scope.build(info, entries);
		   });	
	};
 		 		 		
 	$scope.showSingle = function(record) {
 		$state.go("^.record", { id : record._id.$oid });
 	};
 		 	 		 		 	
 	$scope.changePerson = function() {
 		$scope.changeperson = true;
 	};
 		
 		
 	$scope.getLabel = fhirinfo.getLabel;
 	$scope.configuration = configuration; 	
 	$scope.getCodeableConcept = data.getCodeableConcept;
           
    $scope.extractData = function(records) {
        var entries = [];
        var idx = 0;
        
        var addEntry = function(record,cmp,cdate,measure) {
      	  var q = cmp.valueQuantity || { value : 1 };
      	  var cnt = "";
      	  if (measure) cnt = measure; 
      	  else if (cmp.code && cmp.code.coding && cmp.code.coding[0].code) {
      		  cnt = cmp.code.coding[0].code;
      		  fhirinfo.codeToLabel[cnt] = fhirinfo.codeToLabel[cnt] || cmp.code.coding[0].display;
      	  }
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
          		  addEntry(record, record.data, cdate, measure);
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
	   
	   $scope.labels = dates(info[labelAxis]);
	   $scope.series = shorten(info[seriesAxis]);
	   
	   var labelMap = $scope.map(info[labelAxis]);
	   var seriesMap = $scope.map(info[seriesAxis]);
	   var d = $scope.chart = [];
	   var h = [];
	  
	   angular.forEach($scope.series, function() { d.push(new Array($scope.labels.length).fill(0)); });
	   angular.forEach(entries, function(entry) {
	       d[seriesMap[entry[seriesAxis]]][labelMap[entry[labelAxis]]] = entry.value;
	   });
	      
	   console.log($scope.labels);
	   console.log($scope.series);
	   console.log($scope.data);
	};
	  	
		
	$scope.onClick = function(a,b,c) {
		 console.log(a);
		 console.log(b);
		 console.log(c);
	};
	    
    
    
    
    
    $scope.init();
 		
}])
.controller('PersonChangeCtrl', ['$scope', 'midataServer', 'configuration', 'data', function($scope, midataServer, configuration, data) {
	    $scope.data = data;
	
	    var createOwnerList = function(info) {
	    	  var aOwner = {};
	    	  angular.forEach(info, function(entry) {	              
	              data.owners[entry.owners[0]] = entry.ownerNames[0];	              
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
	    	    
	    
	    $scope.reloadSummary();	    	
}]);