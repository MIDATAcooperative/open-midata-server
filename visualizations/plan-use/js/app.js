var planCreator = angular.module('planCreator', []);
planCreator.factory('server', [ '$http', function($http) {
	
	var service = {};
	
	service.createRecord = function(authToken, name, description, format, data) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"format" : format,
			"description": (description || "")
		};
		
		// submit to server
		return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/create", data);
	};
	
	service.getRecords = function(authToken, properties,fields) {
		 var data = { "authToken" : authToken, "properties" : properties, fields : fields };		
		 return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/records", data);
	};
	
	service.getConfig = function(authToken) {
		 var data = { "authToken" : authToken  };		
		 return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/getconfig", data);
	};
	
	service.setConfig = function(authToken, config) {
		 var data = { "authToken" : authToken, "config" : config  };		
		 return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/setconfig", data);
	};
	
	service.cloneAs = function(authToken, name, config) {
		 var data = { "authToken" : authToken, "name" : name, "config" : config };		
		 return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/clone", data);
	};
	
	return service;	
}]);
planCreator.controller('TrainingCtrl', ['$scope', '$http', '$location', '$filter', 'server',
	function($scope, $http, $location, $filter, server) {
		
		// init
		$scope.loading = true;
		$scope.error = null;
		
		$scope.authToken = $location.path().split("/")[1];
		$scope.records = [];
		$scope.skipped = { wrongFormat : 0, outdated : 0};
		
		$scope.activePlan = null;
		$scope.availablePlans = {};		
		$scope.saving = 0;
		$scope.readonly = true;
				
		// { date format name value [target] 
				
		$scope.save = function(plan) {							
			angular.forEach($scope.records, function(record) {
				if (record.changed) {
					$scope.saving++;
					record.changed = false;
					server.createRecord($scope.authToken, record.name, "Created using plan "+plan.name, record.format, record.data)
					.then(function() { $scope.saving--; });		
				}
			});
			
		};
		
		$scope.style = function(action) {
			var obj = { "font-weight" : "bold", "margin-top" : "15px" };
			if (action == $scope.selectedAction) return null;
			if (!action.count) return obj;
			return null;
		};
		
		$scope.mark = function(record) {			
			record.record.changed = true;
			console.log(record);
		};
		
		$scope.createRecord = function(date, format, name, value, unit) {
			console.log(date);
			var td = $filter('date')(date, "yyyy-MM-dd");
			
			var result = $filter('filter')($scope.records, function(rec){
			  return rec.name == name && rec.data[format] && rec.data[format][0].value && rec.data[format][0].dateTime == td; 
			});
			
			if (result && result.length > 1) {
				result = $filter('orderBy')(result, function(rec) { return rec.created; }, true);				
			}
			
			if (result && result.length > 0) {
				var rec = result[0];
				var res = { record : rec, data : rec.data[format][0] };
				return res;
			}
						
			var dp = { value : value, unit : unit, dateTime : td };
			var res = { created : date, format : format, name : name, data : {} };
			res.data[format] = [ dp ];			
			$scope.records.push(res);
			var res2 = { record : res, data : dp };
			return res2;
		}
		
		$scope.process = function(plan) {
            console.log("process");									
			angular.forEach(plan.data.days, function(day) {
				if (!day.weight || !day.weight.name) day.weight = $scope.createRecord(day.date, "body-weight", "weight", null, "kg");
				if (!day.sleep || !day.sleep.name) day.sleep = $scope.createRecord(day.date, "sleep-timeInBed", "sleep", null, "h");
				if (!day.pulse || !day.pulse.name) day.pulse = $scope.createRecord(day.date, "activities-heart", "heart rate", null, "bpm");
				var usedNames = {};
				angular.forEach(day.actions, function(action) {
					
					if (action.count) {
						var idx = usedNames[action.name] || 1;
					    usedNames[action.name] = (idx + 1);
					    var name = action.name + (idx > 1 ? " (" + idx+")" : "" );
						var unit = ("" + action.count).replace(/[0-9\.]+/g,"").trim();
						action.unit = unit;
						action.result = $scope.createRecord(day.date, "activities-minutesFairlyActive", name, null, unit);
					}
				});
			});
		};
		
		
						
		$scope.load = function() {	
			server.getConfig($scope.authToken)
			.then(function(result) {
				if (!result.data || !result.data.readonly) $scope.readonly = false;
			});
			
			server.getRecords($scope.authToken, { }, ["name", "owner", "ownerName", "format", "description", "created", "data"])
			.then(function(results) {				
				
				angular.forEach(results.data, function(rec) {
                       if (rec.format == "trainingplan") {										
							var old = $scope.availablePlans[rec.name];
							if (!old || old.created < rec.created) {
								$scope.availablePlans[rec.name] = rec;
								$scope.activePlan = rec;
							}
                       }
				
				});
				
				$scope.records = results.data;
                $scope.process($scope.activePlan);								
								
			});
		};
		

		$scope.load();
				
	}
]);
planCreator.controller('PreviewTrainingCtrl', ['$scope', '$http', '$location', '$filter', 'server',
	function($scope, $http, $location, $filter, server) {
		
		// init
		$scope.loading = true;
		$scope.error = null;
		
		$scope.authToken = $location.path().split("/")[1];
		$scope.records = [];
		$scope.skipped = { wrongFormat : 0, outdated : 0};
		
		$scope.activePlan = null;
		$scope.availablePlans = {};		
		$scope.saving = 0;
				
				
		$scope.style = function(action) {
			var obj = { "font-weight" : "bold" };
			if (action == $scope.selectedAction) return null;
			if (!action.count) return obj;
			return null;
		};
		
		$scope.process = function(plan) {
		   if (!plan) {
			   $scope.day = null;
			   return;
		   }
		   var today =  new Date().setHours(0, 0, 0, 0);
		   var days = $filter('filter')(plan.data.days, function(day) { return new Date(day.date).setHours(0,0,0,0) == today; });
		   if (days && days.length == 1) { $scope.day = days[0]; }
		   else $scope.day = null;			
		};
											
		$scope.load = function() {			
			server.getRecords($scope.authToken, { format : "trainingplan" }, ["name", "format", "description", "created", "data"])
			.then(function(results) {				
				
				angular.forEach(results.data, function(rec) {
                       										
							var old = $scope.availablePlans[rec.name];
							if (!old || old.created < rec.created) {
								$scope.availablePlans[rec.name] = rec;
								$scope.activePlan = rec;
							}
                      
				
				});
				
				$scope.process($scope.activePlan);								
								
			});
		};
		

		$scope.load();
				
	}
]);

