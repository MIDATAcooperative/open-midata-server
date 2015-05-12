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
			"description": description
		};
		
		// submit to server
		return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/create", data);
	};
	
	service.getRecords = function(authToken, properties,fields) {
		 var data = { "authToken" : authToken, "properties" : properties, fields : fields };		
		 return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/records", data);
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
			record.changed = true;
			console.log(record);
		};
		
		$scope.createRecord = function(date, format, name, value, unit) {
			console.log(date);
			var result = $filter('filter')($scope.records, function(rec){
			  return rec.name == name && rec.format == format && rec.data && rec.data.date == date
			});			
			if (result && result.length == 1) return result[0];
			if (result && result.length > 1) {
				result = $filter('orderBy')(result, function(rec) { return rec.created; }, true);
				return result[0];
			}			
			var res = { created : date, format : format, name : name, data : { date : date, value : value, unit : unit }};
			$scope.records.push(res);
			return res;
		}
		
		$scope.process = function(plan) {
            console.log("process");									
			angular.forEach(plan.data.days, function(day) {
				if (!day.weight || !day.weight.name) day.weight = $scope.createRecord(day.date, "weight", "weight", null, "kg");
				if (!day.sleep || !day.sleep.name) day.sleep = $scope.createRecord(day.date, "sleep", "sleep", null, "h");
				if (!day.pulse || !day.pulse.name) day.pulse = $scope.createRecord(day.date, "heart rate", "heart rate", null, "bpm");
				
				angular.forEach(day.actions, function(action) {
					if (action.count) {
						var unit = ("" + action.count).replace(/[0-9\.]+/g,"").trim();
						action.unit = unit;
						action.result = $scope.createRecord(day.date, "activity", action.name, null, unit);
					}
				});
			});
		};
		
		
						
		$scope.load = function() {			
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
				
				$scope.select($scope.activePlan);								
								
			});
		};
		

		$scope.load();
				
	}
]);

