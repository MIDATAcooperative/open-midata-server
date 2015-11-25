var jawboneUp = angular.module('jawbone-up', []);
jawboneUp.controller('ImportCtrl', ['$scope', '$http', '$location', 
	function($scope, $http, $location) {
		// init
		$scope.error = {};
		$scope.status = null;
		$scope.requesting = false;
		$scope.requested = 0;
		$scope.saving = false;
		$scope.saved = 0;
		$scope.measure = null;
		$scope.measurements = [
				{
					"name": "Meals",
					"main": {
						"title": "Jawbone UP meals {date}", 
						"endpoint": "/nudge/api/v.1.1/users/@me/meals?date={date}",
						"content" : "food/meals",
						"format" : "jawbone/summary"
					}
				},
				{
					"name": "Moves",
					"main": {
						"title": "Jawbone UP moves {date}", 
						"endpoint": "/nudge/api/v.1.1/users/@me/moves?date={date}",
						"content" : "activities/move",
						"format" : "jawbone/summary"
					},
					"details": {
						"title": "Jawbone UP move ticks {date}", 
						"endpoint": "/nudge/api/v.1.1/moves/{xid}/ticks",
						"content" : "activities/move",
						"format" : "jawbone/ticks"
					}
				},
				{
					"name": "Sleep",
					"main": {
						"title": "Jawbone UP sleep {date}", 
						"endpoint": "/nudge/api/v.1.1/users/@me/sleeps?date={date}",
						"content" : "sleep",
						"format" : "jawbone/summary"
					},
					"details": {
						"title": "Jawbone UP sleep ticks {date}", 
						"endpoint": "/nudge/api/v.1.1/sleeps/{xid}/ticks",
						"content" : "sleep",
						"format" : "jawbone/ticks"
					}
				},
				{
					"name": "Workouts",
					"main": {
						"title": "Jawbone UP workouts {date}", 
						"endpoint": "/nudge/api/v.1.1/users/@me/workouts?date={date}",
						"content" : "activities/workouts",
						"format" : "jawbone/summary"
					}
				}
		];
		var baseUrl = "https://jawbone.com";

		// init datepicker
		$("#datepicker").datepicker({
			"format": "M d, yyyy",
			"todayHighlight": true
		});

		// get authorization token
		var authToken = $location.path().split("/")[1];

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
			$scope.status = "Importing data from Jawbone...";
			$scope.requesting = true;
			$scope.requested = 0;
			$scope.saving = true;
			$scope.saved = 0;

			// import records explicitly for each day (we want to store it in that granularity)
			for (var curDate = fromDate; curDate <= toDate; curDate.setDate(curDate.getDate() + 1)) {
				// capture loop variable 'curDate'
				(function(date) {
					var jawboneDate = date.getFullYear() + twoDigit(date.getMonth() + 1) + twoDigit(date.getDate());
					var formattedDate = date.getFullYear() + "-" + twoDigit(date.getMonth() + 1) + "-" + twoDigit(date.getDate());
					var data = {
						"authToken": authToken,
						"url": baseUrl + $scope.measure.main.endpoint.replace("{date}", jawboneDate)
					};
					$scope.requested += 1;
					$http.post("https://" + window.location.hostname + ":9000/api/apps/oauth2", data).
						success(function(response) {
							// check if an error was returned
							if (response.meta.code !== 200) {
								if (response.meta.error_detail) {
									errorMessage("Failed to import data on " + formattedDate + ": " + response.meta.error_detail);
								} else {
									errorMessage("Failed to import data on " + formattedDate + ": Unknown error.");
								}
							} else if (response.data.links && response.data.links.next) {
								data.url = baseUrl + response.data.links.next;
								delete response.data.links;
								fetchRemaining($scope.measure.main.title, $scope.measure.main.format, $scope.measure.main.content , formattedDate, response, data);
							} else {
								saveRecord($scope.measure.main.title, $scope.measure.main.format, $scope.measure.main.content, formattedDate, response);

								// also import record ticks if present
								if ($scope.measure.details && response.data.items.length > 0) {
									data.url = baseUrl + $scope.measure.details.endpoint.replace("{xid}", response.data.items[0].xid);
									$scope.requested += 1;
									$http.post("https://" + window.location.hostname + ":9000/api/apps/oauth2", data).
										success(function(response) {
											saveRecord($scope.measure.details.title, $scope.measure.details.format, $scope.measure.main.content, formattedDate, response);
										}).
										error(function(err) {
											errorMessage("Failed to import record ticks on " + formattedDate + ": " + err);
										});
								}
							}
						}).
						error(function(err) {
							errorMessage("Failed to import data on " + formattedDate + ": " + err);
						});
					})(curDate);
			}
			$scope.requesting = false;
		}

		// make a two digit string out of a given number
		twoDigit = function(num) {
			return ("0" + num).slice(-2);
		}

		// fetch the remaining data items of a record
		fetchRemaining = function(title, format, content, formattedDate, record, data) {
			$http.post("https://" + window.location.hostname + ":9000/api/apps/oauth2", data).
				success(function(response) {
					if (response.meta.code !== 200) {
						errorMessage("Failed to fetch remaining data items on " + formattedDate + ".");
					} else {
						record.data.items = record.data.items.concat(response.data.items);
						record.data.size += response.data.size;
						if (response.data.links && response.data.links.next) {
							data.url = baseUrl + response.data.links.next;
							fetchRemaining(title, format, content, formattedDate, record, data);
						} else {
							saveRecord(title, format, content, formattedDate, record);
						}
					}
				}).
				error(function(err) {
					errorMessage("Failed to fetch remaining data items on " + formattedDate + ": " + err);
				});
		}
		
		// save a single record to the database
		saveRecord = function(title, format, content, formattedDate, record) {
			var name = title.replace("{date}", formattedDate);
			var data = {
					"authToken": authToken,
					"data": JSON.stringify(record),
					"name": name,
					"description": name,
					"format" : format,
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
	}
]);
