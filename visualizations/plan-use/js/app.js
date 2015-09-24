var planCreator = angular.module('planCreator', [ 'midata']);
planCreator.controller('TrainingCtrl', ['$scope', '$http', '$location', '$filter', 'midataServer', 'midataPortal',
	function($scope, $http, $location, $filter, midataServer, midataPortal) {

	    midataPortal.autoresize();
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
					midataServer.createRecord($scope.authToken, record.name, "Created using plan "+plan.name, record.content, "measurements", record.data)
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
		
		$scope.createRecord = function(date, content, name, value, unit) {
			console.log(date);
			var td = $filter('date')(date, "yyyy-MM-dd");
			
			var result = $filter('filter')($scope.records, function(rec){
			  return rec.name == name && rec.data[content] && rec.data[content][0].value && rec.data[content][0].dateTime == td; 
			});
			
			if (result && result.length > 1) {
				result = $filter('orderBy')(result, function(rec) { return rec.created; }, true);				
			}
			
			if (result && result.length > 0) {
				var rec = result[0];
				var res = { record : rec, data : rec.data[content][0] };
				return res;
			}
						
			var dp = { value : value, unit : unit, dateTime : td };
			var res = { created : date, content : content, format : "measurements", name : name, data : {} };
			res.data[content] = [ dp ];			
			$scope.records.push(res);
			var res2 = { record : res, data : dp };
			return res2;
		}
		
		$scope.process = function(plan) {
            console.log("process");									
			angular.forEach(plan.data.days, function(day) {
				if (!day.weight || !day.weight.name) day.weight = $scope.createRecord(day.date, "body/weight", "weight", null, "kg");
				if (!day.sleep || !day.sleep.name) day.sleep = $scope.createRecord(day.date, "sleep/time-in-bed", "sleep", null, "h");
				if (!day.pulse || !day.pulse.name) day.pulse = $scope.createRecord(day.date, "activities/heartrate", "heart rate", null, "bpm");
				var usedNames = {};
				angular.forEach(day.actions, function(action) {
					
					if (action.count) {
						var idx = usedNames[action.name] || 1;
					    usedNames[action.name] = (idx + 1);
					    var name = action.name + (idx > 1 ? " (" + idx+")" : "" );
						var unit = ("" + action.count).replace(/[0-9\.]+/g,"").trim();
						action.unit = unit;
						action.result = $scope.createRecord(day.date, "activities/minutes-active", name, null, unit);
					}
				});
			});
		};
		
		
						
		$scope.load = function() {	
			midataServer.getConfig($scope.authToken)
			.then(function(result) {
				if (!result.data || !result.data.readonly) $scope.readonly = false;
			});
			
			midataServer.getRecords($scope.authToken, { "format" : ["training-app", "measurements"] }, ["name", "owner", "ownerName", "format", "content", "description", "created", "data"])
			.then(function(results) {				
				
				angular.forEach(results.data, function(rec) {
                       if (rec.format == "training-app") {										
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
planCreator.controller('PreviewTrainingCtrl', ['$scope', '$http', '$location', '$filter', 'midataServer',
	function($scope, $http, $location, $filter, midataServer) {
		
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
			midataServer.getRecords($scope.authToken, { format : "training-app", content:"calendar/trainingplan" }, ["name", "content", "format", "description", "created", "data"])
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

