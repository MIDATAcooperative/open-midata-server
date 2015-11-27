var fitbit = angular.module('fitbit', [ 'midata' ]);
fitbit.controller('ImportCtrl', ['$scope', '$http', '$location', 'midataServer', 'midataPortal', 
	function($scope, $http, $location, midataServer, midataPortal) {
		// init
	    midataPortal.autoresize();
	    
		$scope.error = {};
		$scope.status = null;
		$scope.requesting = 0;
		$scope.requested = 0;
		$scope.saving = false;
		$scope.saved = 0;
		$scope.measure = null;
		$scope.measurements = [
							
				{
					"name": "Food - Calories Intake",
					"title": "Fitbit food (calories intake) {date}",
					"endpoint": "/1/user/-/foods/log/caloriesIn/date/{date}/1d.json",
					"content" : "food/calories-in",
					"unit" : "kcal"
				},
				{
					"name": "Food - Water Consumption",
					"title": "Fitbit food (water consumption) {date}",
					"endpoint": "/1/user/-/foods/log/water/date/{date}/1d.json",
					"content" : "food/water",
					"unit" : "ml"
				},
				{
					"name": "Activities - Calories Burned",
					"title": "Fitbit activities (calories burned) {date}",
					"endpoint": "/1/user/-/activities/calories/date/{date}/1d.json",
					"content" : "activities/calories",
					"unit" : "kcal"
				},
				{
					"name": "Activities - Steps",
					"title": "Fitbit activities (steps) {date}",
					"endpoint": "/1/user/-/activities/steps/date/{date}/1d.json",
					"content" : "activities/steps",
					"unit" : "steps"
				},
				{
					"name": "Activities - Distance",
					"title": "Fitbit activities (distance) {date}",
					"endpoint": "/1/user/-/activities/distance/date/{date}/1d.json",
					"content" : "activities/distance",
					"unit" : "km"
				},
				{
					"name": "Activities - Floors Climbed",
					"title": "Fitbit activities (floors climbed) {date}",
					"endpoint": "/1/user/-/activities/floors/date/{date}/1d.json",
					"content" : "activities/floors",
					"unit" : "floors"
				},
				{
					"name": "Activities - Elevation",
					"title": "Fitbit activities (elevation) {date}",
					"endpoint": "/1/user/-/activities/elevation/date/{date}/1d.json",
					"content" : "activities/elevation",
					"unit" : "m"
				},
				{
					"name": "Activities - Minutes Sedentary",
					"title": "Fitbit activities (minutes sedentary) {date}",
					"endpoint": "/1/user/-/activities/minutesSedentary/date/{date}/1d.json",
					"content" : "activities/minutes-sedentary",
					"unit" : "min"
				},
				{
					"name": "Activities - Minutes Lightly Active",
					"title": "Fitbit activities (minutes lightly active) {date}",
					"endpoint": "/1/user/-/activities/minutesLightlyActive/date/{date}/1d.json",
					"content" : "activities/minutes-lightly-active",
					"unit" : "min"
				},
				{
					"name": "Activities - Minutes Fairly Active",
					"title": "Fitbit activities (minutes fairly active) {date}",
					"endpoint": "/1/user/-/activities/minutesFairlyActive/date/{date}/1d.json",
					"content" : "activities/minutes-fairly-active",
					"unit" : "min"
				},
				{
					"name": "Activities - Minutes Very Active",
					"title": "Fitbit activities (minutes very active) {date}",
					"endpoint": "/1/user/-/activities/minutesVeryActive/date/{date}/1d.json",
					"content" : "activities/minutes-very-active",
					"unit" : "min"
				},
				{
					"name": "Activities - Calories Burned in Activities",
					"title": "Fitbit activities (calories burned in activities) {date}",
					"endpoint": "/1/user/-/activities/activityCalories/date/{date}/1d.json",
					"content" : "activities/activity-calories",
					"unit" : "kcal"
				},
				{
					"name": "Sleep - Time in Bed",
					"title": "Fitbit sleep (time in bed) {date}",
					"endpoint": "/1/user/-/sleep/timeInBed/date/{date}/1d.json",
					"content" : "sleep/time-in-bed",
					"unit" : "min"
				},
				{
					"name": "Sleep - Minutes Asleep",
					"title": "Fitbit sleep (minutes asleep) {date}",
					"endpoint": "/1/user/-/sleep/minutesAsleep/date/{date}/1d.json",
					"content" : "sleep/minutes-asleep",
					"unit" : "min"
				},
				{
					"name": "Sleep - Minutes Awake",
					"title": "Fitbit sleep (minutes awake) {date}",
					"endpoint": "/1/user/-/sleep/minutesAwake/date/{date}/1d.json",
					"content" : "sleep/minutes-awake",
					"unit" : "min"
				},
				{
					"name": "Sleep - Minutes to Fall Asleep",
					"title": "Fitbit sleep (minutes to fall asleep) {date}",
					"endpoint": "/1/user/-/sleep/minutesToFallAsleep/date/{date}/1d.json",
					"content" : "sleep/minutes-to-fall-asleep",
					"unit" : "min"
				},
				{
					"name": "Sleep - Efficiency",
					"title": "Fitbit sleep (efficiency) {date}",
					"endpoint": "/1/user/-/sleep/efficiency/date/{date}/1d.json",
					"content" : "sleep/efficiency"
				},
				{
					"name": "Body - Weight",
					"title": "Fitbit body (weight) {date}",
					"endpoint": "/1/user/-/body/weight/date/{date}/1d.json",
				    "content" : "body/weight",
				    "unit" : "kg"
				},
				{
					"name": "Body - BMI",
					"title": "Fitbit body (BMI) {date}",
					"endpoint": "/1/user/-/body/bmi/date/{date}/1d.json",
					"content" : "body/bmi",
					"unit" : "kg/m2"
				},
				{
					"name": "Body - Fat",
					"title": "Fitbit body (fat) {date}",
					"endpoint": "/1/user/-/body/fat/date/{date}/1d.json",
					"content" : "body/fat",	
					"unit" : "%"
				}						
				
		];
		var baseUrl = "https://api.fitbit.com";

		// init datepicker
		/*$("#datepicker").datepicker({
			"format": "M d, yyyy",
			"todayHighlight": true
		});*/

		// get authorization token
		var authToken = $location.path().split("/")[1];
		
		$scope.initForm = function() {
			
		if ($scope.user == null) {
			midataServer.oauth2Request(authToken, baseUrl + "/1/user/-/profile.json")			
			.success(function(response) {
			  console.log(response);
			  $scope.user = response.user;
			  if (response.user && response.user.memberSince) {
				  var since = new Date(response.user.memberSince);				  
				  
				  var yesterday = new Date();
				  yesterday.setDate(yesterday.getDate() - 1);
				  yesterday.setHours(1,1,1,1);
				  
				  angular.forEach($scope.measurements, function(measurement) {
					if (measurement.from == null || measurement.from < since) measurement.from = since;
					measurement.to = yesterday;
				  });
				  
				  //$("#toDate").datepicker("setDate", yesterday); 
			  }
			});
		}
			
		  midataServer.getSummary(authToken, "content" , { "format" : "measurements" , "app" : "fitbit" })
		  .then(function(response) {
			var map = {};
			angular.forEach($scope.measurements, function(measurement) {
			  map[measurement.content] = measurement;
			});
			angular.forEach(response.data, function(entry) {
				var measurement = map[entry.contents[0]];
				if (measurement != null) {
					var newestDate = new Date(entry.newest);
					newestDate.setHours(1,1,1,1);
					measurement.imported = entry.count;
					if (measurement.from == null || measurement.from < newestDate) measurement.from = newestDate;
				}
			});
		  });
			
		};

		// start the importing of records
		$scope.startImport = function() {			
			$scope.error.message = null;
			$scope.error.messages = [];
			angular.forEach($scope.measurements, function(measure) {
				if (measure.import) importRecords(measure);
			});			
		}

		// checks whether the given date is valid
		isValidDate = function(date) {
			return date instanceof Date && isFinite(date);
		}
		
		// import records, one main record and possibly a detailed record for each day
		importRecords = function(measure) {
		
			var fromDate = measure.from;
			var toDate = measure.to;
			
			if (fromDate > toDate) return;
			
			$scope.status = "Importing data from Fitbit...";			

			$scope.requesting++;
			$scope.requested = 0;
			$scope.saving = true;
			$scope.saved = 0;
			
			var formattedFromDate = fromDate.getFullYear() + "-" + twoDigit(fromDate.getMonth() + 1) + "-" + twoDigit(fromDate.getDate());
			var formattedEndDate = toDate.getFullYear() + "-" + twoDigit(toDate.getMonth() + 1) + "-" + twoDigit(toDate.getDate());
												
			midataServer.oauth2Request(authToken, baseUrl + measure.endpoint.replace("{date}", formattedFromDate).replace("1d", formattedEndDate))
			.success(function(response) {
					// check if an error was returned
				if (response.errors) {
					errorMessage("Failed to import data on " + formattedDate + ": " + response.errors[0].message + ".");
				} else {
					console.log(response);
					angular.forEach(response, function(v,dataName) {
						var grouped = {};
						
						angular.forEach(v, function(itm) {
						  var val = itm.value || itm.amount;
						  if (val != 0) {
							  var recDate = itm.dateTime || itm.date;
							  if (measure.unit != null && itm.unit == null) itm.unit = measure.unit;
							  if (grouped[recDate] == null) grouped[recDate] = [];
							  grouped[recDate].push(itm);
						  }
						});						
						
						angular.forEach(grouped, function(itms, date) {
							var rec = {};
							rec[dataName] = itms;
							$scope.requested += 1;
							saveRecord(measure.title, measure.content, date, rec);							
						});
					});				
				}
				
				$scope.requesting--;
				finish();
			}).
			error(function(err) {
					errorMessage("Failed to import data on " + formattedDate + ": " + err);
			});
						
			//$scope.requesting = false;
		}

		// make a two digit string out of a given number
		twoDigit = function(num) {
			return ("0" + num).slice(-2);
		}

		// save a single record to the database
		saveRecord = function(title, content, formattedDate, record) {
			var name = title.replace("{date}", formattedDate);			
			midataServer.createRecord(authToken, name, name, content, "measurements", record)
			.then(function() {
					$scope.saved += 1;
					finish();
			})
			.catch(function(err) {
					errorMessage("Failed to save record '" + name + "' to database: " + err);
			});
		}

		// handle errors during import
		errorMessage = function(errMsg) {
			$scope.error.messages.push(errMsg);
			finish();
		}

		// update application state at the end of an import
		finish = function() {
			if ($scope.requesting === 0 && $scope.requested === $scope.saved + $scope.error.messages.length) {
				$scope.status = "Imported " + $scope.saved + " records.";
				if ($scope.error.messages.length > 0) {
					$scope.status = "Imported " + $scope.saved + " of " + $scope.requested + " records. For failures see error messages.";
				}
				$scope.saving = false;
				$scope.initForm();
			}
		}
		
		$scope.progress = function() {
			var r = $scope.requested > 0 ? $scope.requested : 1;
			return { 'width' : ($scope.saved * 100 / r)+"%" };
		};
		
		$scope.initForm();
	}
]);
