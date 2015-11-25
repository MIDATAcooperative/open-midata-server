var fitbit = angular.module('fitbit', [ 'midata' ]);
fitbit.controller('ImportCtrl', ['$scope', '$http', '$location', 'midataServer', 'midataPortal', 
	function($scope, $http, $location, midataServer, midataPortal) {
		// init
	    midataPortal.autoresize();
	    
		$scope.error = {};
		$scope.status = null;
		$scope.requesting = false;
		$scope.requested = 0;
		$scope.saving = false;
		$scope.saved = 0;
		$scope.measure = null;
		$scope.measurements = [
				{
					"name": "Water",
					"title": "Fitbit water consumption {date}",
					"endpoint": "/1/user/-/foods/log/water/date/{date}.json",
					"content" : "food/water-consumption"
				},
				{
					"name": "Body Weight",
					"title": "Fitbit body weight {date}",
					"endpoint": "/1/user/-/body/log/weight/date/{date}.json",
					"content" : "body/weight"
				},
				{
					"name": "Food - Calories Intake",
					"title": "Fitbit food (calories intake) {date}",
					"endpoint": "/1/user/-/foods/log/caloriesIn/date/{date}/1d.json",
					"content" : "food/calories-intake"
				},
				{
					"name": "Food - Water Consumption",
					"title": "Fitbit food (water consumption) {date}",
					"endpoint": "/1/user/-/foods/log/water/date/{date}/1d.json",
					"content" : "food/water-consumption"
				},
				{
					"name": "Activities - Calories Burned",
					"title": "Fitbit activities (calories burned) {date}",
					"endpoint": "/1/user/-/activities/calories/date/{date}/1d.json",
					"content" : "activities/calories-burned"
				},
				{
					"name": "Activities - Steps",
					"title": "Fitbit activities (steps) {date}",
					"endpoint": "/1/user/-/activities/steps/date/{date}/1d.json",
					"content" : "activities/steps"
				},
				{
					"name": "Activities - Distance",
					"title": "Fitbit activities (distance) {date}",
					"endpoint": "/1/user/-/activities/distance/date/{date}/1d.json",
					"content" : "activities/distance"
				},
				{
					"name": "Activities - Floors Climbed",
					"title": "Fitbit activities (floors climbed) {date}",
					"endpoint": "/1/user/-/activities/floors/date/{date}/1d.json",
					"content" : "activities/floors-climbed"
				},
				{
					"name": "Activities - Elevation",
					"title": "Fitbit activities (elevation) {date}",
					"endpoint": "/1/user/-/activities/elevation/date/{date}/1d.json",
					"content" : "activities/elevation"
				},
				{
					"name": "Activities - Minutes Sedentary",
					"title": "Fitbit activities (minutes sedentary) {date}",
					"endpoint": "/1/user/-/activities/minutesSedentary/date/{date}/1d.json",
					"content" : "activities/sedentary"
				},
				{
					"name": "Activities - Minutes Lightly Active",
					"title": "Fitbit activities (minutes lightly active) {date}",
					"endpoint": "/1/user/-/activities/minutesLightlyActive/date/{date}/1d.json",
					"content" : "activities/minutes-active"
				},
				{
					"name": "Activities - Minutes Fairly Active",
					"title": "Fitbit activities (minutes fairly active) {date}",
					"endpoint": "/1/user/-/activities/minutesFairlyActive/date/{date}/1d.json",
					"content" : "activities/minutes-active"
				},
				{
					"name": "Activities - Minutes Very Active",
					"title": "Fitbit activities (minutes very active) {date}",
					"endpoint": "/1/user/-/activities/minutesVeryActive/date/{date}/1d.json",
					"content" : "activities/minutes-active"
				},
				{
					"name": "Activities - Calories Burned in Activities",
					"title": "Fitbit activities (calories burned in activities) {date}",
					"endpoint": "/1/user/-/activities/activityCalories/date/{date}/1d.json",
					"content" : "activities/calories-burned"
				},
				{
					"name": "Sleep - Time in Bed",
					"title": "Fitbit sleep (time in bed) {date}",
					"endpoint": "/1/user/-/sleep/timeInBed/date/{date}/1d.json",
					"content" : "sleep/time-in-bed"
				},
				{
					"name": "Sleep - Minutes Asleep",
					"title": "Fitbit sleep (minutes asleep) {date}",
					"endpoint": "/1/user/-/sleep/minutesAsleep/date/{date}/1d.json",
					"content" : "sleep/minutes-asleep"
				},
				{
					"name": "Sleep - Minutes Awake",
					"title": "Fitbit sleep (minutes awake) {date}",
					"endpoint": "/1/user/-/sleep/minutesAwake/date/{date}/1d.json",
					"content" : "sleep/minutes-awake"
				},
				{
					"name": "Sleep - Minutes to Fall Asleep",
					"title": "Fitbit sleep (minutes to fall asleep) {date}",
					"endpoint": "/1/user/-/sleep/minutesToFallAsleep/date/{date}/1d.json",
					"content" : "sleep/minutes-to-fall-asleep"
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
				    "content" : "body/weight"
				},
				{
					"name": "Body - BMI",
					"title": "Fitbit body (BMI) {date}",
					"endpoint": "/1/user/-/body/bmi/date/{date}/1d.json",
					"content" : "body/bmi"
				},
				{
					"name": "Body - Fat",
					"title": "Fitbit body (fat) {date}",
					"endpoint": "/1/user/-/body/fat/date/{date}/1d.json",
					"content" : "body/fat"
				}
		];
		var baseUrl = "https://api.fitbit.com";

		// init datepicker
		$("#datepicker").datepicker({
			"format": "M d, yyyy",
			"todayHighlight": true
		});

		// get authorization token
		var authToken = $location.path().split("/")[1];
		
		$scope.initForm = function() {
			var data = {
				"authToken": authToken,
				"url": baseUrl + "/1/user/-/profile.json"
			};
									
			$http.post("https://" + window.location.hostname + ":9000/api/apps/oauth2", data).
			success(function(response) {
			  console.log(response);
			  if (response.user && response.user.memberSince) {
				  $("#fromDate").datepicker("setDate", new Date(response.user.memberSince));
				  
				  var yesterday = new Date();
				  yesterday.setDate(yesterday.getDate() - 1);
				  $("#toDate").datepicker("setDate", yesterday); 
			  }
			});
			
		};

		// start the importing of records
		$scope.startImport = function() {
			var fromDate = $("#fromDate").datepicker("getDate");
			var toDate = $("#toDate").datepicker("getDate");
			$scope.error.measure = !$scope.measure;
			$scope.error.date = (!isValidDate(fromDate) || !isValidDate(toDate));
			if ($scope.error.measure || $scope.error.date) {
				$scope.error.message = "Please fill in all fields.";
				return;
			} else if (fromDate > toDate) {
				$scope.error.date = true;
				$scope.error.message = "Start date must be before end date.";
				return;
			}
			$scope.error.message = null;
			importRecords(fromDate, toDate);
		}

		// checks whether the given date is valid
		isValidDate = function(date) {
			return date instanceof Date && isFinite(date);
		}
		
		// import records, one main record and possibly a detailed record for each day
		importRecords = function(fromDate, toDate) {
			$scope.error.messages = [];
			$scope.status = "Importing data from Fitbit...";
			$scope.requesting = true;
			$scope.requested = 0;
			$scope.saving = true;
			$scope.saved = 0;

			var formattedFromDate = fromDate.getFullYear() + "-" + twoDigit(fromDate.getMonth() + 1) + "-" + twoDigit(fromDate.getDate());
			var formattedEndDate = toDate.getFullYear() + "-" + twoDigit(toDate.getMonth() + 1) + "-" + twoDigit(toDate.getDate());
			
			var data = {
					"authToken": authToken,
					"url": baseUrl + $scope.measure.endpoint.replace("{date}", formattedFromDate).replace("1d", formattedEndDate)
			};
			
			
			$scope.requested += 1;
			$http.post("https://" + window.location.hostname + ":9000/api/apps/oauth2", data).
			success(function(response) {
					// check if an error was returned
				if (response.errors) {
					errorMessage("Failed to import data on " + formattedDate + ": " + response.errors[0].message + ".");
				} else {
					console.log(response);
					angular.forEach(response, function(v,dataName) {
						var grouped = {};
						
						angular.forEach(v, function(itm) {							
						  var recDate = itm.dateTime || itm.date;
						  if (grouped[recDate] == null) grouped[recDate] = [];
						  grouped[recDate].push(itm);						
						});						
						
						angular.forEach(grouped, function(itms, date) {
							var rec = {};
							rec[dataName] = itms;
							saveRecord($scope.measure.title, $scope.measure.content, date, rec);
						});
					});				
				}
			}).
			error(function(err) {
					errorMessage("Failed to import data on " + formattedDate + ": " + err);
			});
			
			// import records explicitly for each day (we want to store it in that granularity)
			/*
			for (var curDate = fromDate; curDate <= toDate; curDate.setDate(curDate.getDate() + 1)) {
				// capture loop variable 'curDate'
				(function(date) {
					var formattedDate = date.getFullYear() + "-" + twoDigit(date.getMonth() + 1) + "-" + twoDigit(date.getDate());
					var data = {
						"authToken": authToken,
						"url": baseUrl + $scope.measure.endpoint.replace("{date}", formattedDate)
					};
					$scope.requested += 1;
					$http.post("https://" + window.location.hostname + ":9000/api/apps/oauth2", data).
						success(function(response) {
							// check if an error was returned
							if (response.errors) {
								errorMessage("Failed to import data on " + formattedDate + ": " + response.errors[0].message + ".");
							} else {
								saveRecord($scope.measure.title, $scope.measure.content, formattedDate, response);
							}
						}).
						error(function(err) {
							errorMessage("Failed to import data on " + formattedDate + ": " + err);
						});
					})(curDate);
			}
			*/
			
			
			$scope.requesting = false;
		}

		// make a two digit string out of a given number
		twoDigit = function(num) {
			return ("0" + num).slice(-2);
		}

		// save a single record to the database
		saveRecord = function(title, content, formattedDate, record) {
			var name = title.replace("{date}", formattedDate);
			var data = {
					"authToken": authToken,
					"data": JSON.stringify(record),
					"name": name,
					"description": name,
					"format" : "measurements",
					"content" : content
			};
			$http.post("https://" + window.location.hostname + ":9000/api/apps/create", data).
				success(function() {
					$scope.saved += 1;
					finish();
				}).
				error(function(err) {
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
			if (!$scope.requesting && $scope.requested === $scope.saved + $scope.error.messages.length) {
				$scope.status = "Imported " + $scope.saved + " records.";
				if ($scope.error.messages.length > 0) {
					$scope.status = "Imported " + $scope.saved + " of " + $scope.requested + " records. For failures see error messages.";
				}
				$scope.saving = false;
			}
		}
		
		$scope.initForm();
	}
]);
