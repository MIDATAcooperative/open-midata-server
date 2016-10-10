var fitbit = angular.module('fitbit', [ 'midata', 'pascalprecht.translate', 'fitbiti18n' ]);
fitbit.config(['$translateProvider', 'i18nc', function($translateProvider, i18nc) {	    
    
	$translateProvider
	.useSanitizeValueStrategy('escape')	   	    
	.registerAvailableLanguageKeys(['en', 'de', 'it', 'fr'], {
	  'en_*': 'en',
	  'de_*': 'de',
	  'fr_*': 'fr',
	  'it_*': 'it',
	})
	.translations('en', i18nc.en)
	.translations('de', i18nc.de)
	.translations('it', i18nc.it)
	.translations('fr', i18nc.fr)
	.fallbackLanguage('en');
}]);
fitbit.factory('importer', ['$http' , '$translate', 'midataServer', '$q', function($http, $translate, midataServer, $q) {
	var $scope = {};
	
	$scope.error = {};
	$scope.reimport = 7;
	$scope.status = null;
	$scope.requesting = 0;
	$scope.requested = 0;
	$scope.saving = false;
	$scope.saved = 0;
	$scope.totalImport = 0;
	$scope.measure = null;
	$scope.alldone = null;
	$scope.repeat = false;
	$scope.measurements = [
						
			{
				"id" : "food_calories_intake",
				"name": "Food - Calories Intake",				
				"endpoint": "/1/user/-/foods/log/caloriesIn/date/{date}/1d.json",
				"content" : "food/calories-in",
				"unit" : "kcal"
			},
			{
				"id" : "food_water_consumption",
				"name": "Food - Water Consumption",				
				"endpoint": "/1/user/-/foods/log/water/date/{date}/1d.json",
				"content" : "food/water",
				"unit" : "ml"
			},
			{
				"id" : "activities_calories_burned",
				"name": "Activities - Calories Burned",				
				"endpoint": "/1/user/-/activities/calories/date/{date}/1d.json",
				"content" : "activities/calories",
				"unit" : "kcal"
			},
			{
				"id" : "activities_steps",
				"name": "Activities - Steps",				
				"endpoint": "/1/user/-/activities/steps/date/{date}/1d.json",
				"content" : "activities/steps",
				"unit" : "steps"
			},
			{
				"id" : "activities_distance",
				"name": "Activities - Distance",				
				"endpoint": "/1/user/-/activities/distance/date/{date}/1d.json",
				"content" : "activities/distance",
				"unit" : "km"
			},
			{
				"id" : "activities_floors_climbed",
				"name": "Activities - Floors Climbed",				
				"endpoint": "/1/user/-/activities/floors/date/{date}/1d.json",
				"content" : "activities/floors",
				"unit" : "floors"
			},
			{
				"id" : "activities_elevation",
				"name": "Activities - Elevation",				
				"endpoint": "/1/user/-/activities/elevation/date/{date}/1d.json",
				"content" : "activities/elevation",
				"unit" : "m"
			},
			{
				"id" : "activities_minutes_sedentary",
				"name": "Activities - Minutes Sedentary",				
				"endpoint": "/1/user/-/activities/minutesSedentary/date/{date}/1d.json",
				"content" : "activities/minutes-sedentary",
				"unit" : "min"
			},
			{
				"id" : "activities_minutes_lightly_active",
				"name": "Activities - Minutes Lightly Active",				
				"endpoint": "/1/user/-/activities/minutesLightlyActive/date/{date}/1d.json",
				"content" : "activities/minutes-lightly-active",
				"unit" : "min"
			},
			{
				"id" : "activities_minutes_fairly_active",
				"name": "Activities - Minutes Fairly Active",				
				"endpoint": "/1/user/-/activities/minutesFairlyActive/date/{date}/1d.json",
				"content" : "activities/minutes-fairly-active",
				"unit" : "min"
			},
			{
				"id" : "activities_minutes_very_active",
				"name": "Activities - Minutes Very Active",				
				"endpoint": "/1/user/-/activities/minutesVeryActive/date/{date}/1d.json",
				"content" : "activities/minutes-very-active",
				"unit" : "min"
			},
			{
				"id" : "activities_calories_burned_in_activities",
				"name": "Activities - Calories Burned in Activities",				
				"endpoint": "/1/user/-/activities/activityCalories/date/{date}/1d.json",
				"content" : "activities/activity-calories",
				"unit" : "kcal"
			},
			{
				"id" : "sleep_time_in_bed",
				"name": "Sleep - Time in Bed",				
				"endpoint": "/1/user/-/sleep/timeInBed/date/{date}/1d.json",
				"content" : "sleep/time-in-bed",
				"unit" : "min"
			},
			{
				"id" : "sleep_minutes_asleep",
				"name": "Sleep - Minutes Asleep",				
				"endpoint": "/1/user/-/sleep/minutesAsleep/date/{date}/1d.json",
				"content" : "sleep/minutes-asleep",
				"unit" : "min"
			},
			{
				"id" : "sleep_minutes_awake",
				"name": "Sleep - Minutes Awake",				
				"endpoint": "/1/user/-/sleep/minutesAwake/date/{date}/1d.json",
				"content" : "sleep/minutes-awake",
				"unit" : "min"
			},
			{
				"id" : "sleep_minutes_to_fall_asleep",
				"name": "Sleep - Minutes to Fall Asleep",				
				"endpoint": "/1/user/-/sleep/minutesToFallAsleep/date/{date}/1d.json",
				"content" : "sleep/minutes-to-fall-asleep",
				"unit" : "min"
			},
			{
				"id" : "sleep_efficiency",
				"name": "Sleep - Efficiency",				
				"endpoint": "/1/user/-/sleep/efficiency/date/{date}/1d.json",
				"content" : "sleep/efficiency"
			},
			{
				"id" : "body_weight",
				"name": "Body - Weight",				
				"endpoint": "/1/user/-/body/weight/date/{date}/1d.json",
			    "content" : "body/weight",
			    "unit" : "kg"
			},
			{
				"id" : "body_bmi",
				"name": "Body - BMI",				
				"endpoint": "/1/user/-/body/bmi/date/{date}/1d.json",
				"content" : "body/bmi",
				"unit" : "kg/m2"
			},
			{
				"id" : "body_fat",
				"name": "Body - Fat",				
				"endpoint": "/1/user/-/body/fat/date/{date}/1d.json",
				"content" : "body/fat",	
				"unit" : "%"
			}						
			
	];
	var baseUrl = "https://api.fitbit.com";
	var stored = {};
	
	var setToDate = function(amendFrom) {
		 var yesterday = new Date();
		 yesterday.setDate(yesterday.getDate() - 1);
		 yesterday.setHours(1,1,1,1);
		 
		 angular.forEach($scope.measurements, function(measurement) {
			if (amendFrom) {
				measurement.from = new Date(measurement.to.getTime());
				measurement.from.setDate(measurement.from.getDate() + 1);
			}
			 
			measurement.to = yesterday;
				
			if (measurement.to.getTime() - measurement.from.getTime() > 1000 * 60 * 60 * 24 * 365) {
			  measurement.to = new Date(measurement.from.getTime() + 1000 * 60 * 60 * 24 * 365);
			  $scope.repeat = true;
			}
		 });		 
	};

	$scope.initForm = function(authToken, nofitbitquery) {
		var deferred = $q.defer();
		var done = 0;
		var reqDone = function() {
			done++;
			if (done == 3) deferred.resolve();
		};
		
		angular.forEach($scope.measurements, function(measurement) {
			measurement.from = null;
			measurement.to = null;		
		});
		
		$scope.authToken = authToken;
		if (!nofitbitquery && $scope.user == null) {
			midataServer.oauth2Request(authToken, baseUrl + "/1/user/-/profile.json")			
			.then(function(res) {
			  var response = res.data;
			  console.log(response);
			  $scope.user = response.user;
			  
			  if (response.user && response.user.memberSince) {
				  var since = new Date(response.user.memberSince);				  
				  				  				  
				  angular.forEach($scope.measurements, function(measurement) {
					if (measurement.from == null || measurement.from < since) measurement.from = since;					
				  });
				  
				  setToDate(false);
				  
				  //$("#toDate").datepicker("setDate", yesterday); 
			  }
			  reqDone();
			}, function(err, v2) {
				reqDone();
			});
		} else { reqDone(); }
		midataServer.getConfig(authToken)
		  .then(function(response) {			 
			 if (response.data && response.data.selected) {
				 $scope.countSelected = response.data.selected.length;
				 $scope.autoimport = response.data.autoimport;
				 angular.forEach($scope.measurements, function(measurement) {
					 if (response.data.selected.indexOf(measurement.name) >= 0) {
						 measurement.import = true;
					 }
				 });
			 } else {
				 angular.forEach($scope.measurements, function(measurement) {						
					measurement.import = true;
				 });
				 //$scope.countSelected = 0;
			 }
			 reqDone();
		  }, function() { reqDone(); });
		
		  midataServer.getSummary(authToken, "content" , { "format" : "fhir/Observation" , "subformat" : "Quantity", "app" : "fitbit" })
		  .then(function(response) {
			var map = {};
			angular.forEach($scope.measurements, function(measurement) {
			  map[measurement.content] = measurement;
			});
			angular.forEach(response.data, function(entry) {
				var measurement = map[entry.contents[0]];
				if (measurement != null) {
					var newestDate = new Date(entry.newest);
					
					if (!$scope.last) {
						$scope.last = newestDate;
					} else if ($scope.last < newestDate) {
						$scope.last = newestDate;
					}
					
					newestDate.setHours(1,1,1,1);
					newestDate.setDate(newestDate.getDate() - $scope.reimport);
					measurement.imported = entry.count;
					if (measurement.from == null || measurement.from < newestDate) measurement.from = newestDate;
				}
			});
			reqDone();
		  }, function() { reqDone(); });
			
		  return deferred.promise;
		};
		
		// start the importing of records
		$scope.startImport = function() {			
			$scope.error.message = null;
			$scope.error.messages = [];
			$scope.requested = 0;
			$scope.saved = 0;
			$scope.status = "importing";
			
			var actionDef = $q.defer();
			var actionChain = actionDef.promise;
			actionDef.resolve();
						
			angular.forEach($scope.measurements, function(measure) {
				if (measure.import) {
					var f = function() { return getPrevRecords(measure); };
					actionChain = actionChain.then(f);					
				}
			});			
			
			actionChain.then(function() {
			  angular.forEach($scope.measurements, function(measure) {
				if (measure.import) {					
					importRecords(measure);
				}
			  });
			});
		};
		
		var getPrevRecords = function(measure) {
		   return midataServer.getRecords($scope.authToken, { "format" :"fhir/Observation", "content" : measure.content, "index" : { "effectiveDateTime" : { "!!!ge" : measure.from }} }, ["version", "content", "data"])
		   .then(function(results) {
			   angular.forEach(results.data, function(rec) {
				 stored[rec.content+rec.data.effectiveDateTime] = rec;  
			   });
		   });
		};
				
		// import records, one main record and possibly a detailed record for each day
		var importRecords = function(measure) {
		
			var fromDate = measure.from;
			var toDate = measure.to;
			$translate("titles."+measure.id).then(function(t) { measure.title = t; });
			$translate(measure.id).then(function(t) { measure.name_translated = t; });
			$translate("fitness_data").then(function(t) { measure.category_name = t; });
			
			if (fromDate > toDate) return;
			
			$scope.status = "importing";			
			$scope.requesting++;			
			$scope.saving = true;
			
			
			var formattedFromDate = fromDate.getFullYear() + "-" + twoDigit(fromDate.getMonth() + 1) + "-" + twoDigit(fromDate.getDate());
			var formattedEndDate = toDate.getFullYear() + "-" + twoDigit(toDate.getMonth() + 1) + "-" + twoDigit(toDate.getDate());
												
			midataServer.oauth2Request($scope.authToken, baseUrl + measure.endpoint.replace("{date}", formattedFromDate).replace("1d", formattedEndDate))
			.success(function(response) {
					// check if an error was returned
				var actions = [];
				
				if (response.errors) {
					errorMessage("Failed to import data on " + formattedFromDate + ": " + response.errors[0].message + ".");
				} else {
					console.log(response);
					angular.forEach(response, function(v,dataName) {						
						
						angular.forEach(v, function(itm) {
						  var val = itm.value || itm.amount;
						  if (val != 0 ) { // jshint ignore:line
							  var recDate = itm.dateTime || itm.date;
							  if (measure.unit != null && itm.unit == null) itm.unit = measure.unit;
							  							  
							  var rec = {
								resourceType : "Observation",
								status : "preliminary",
								category : {
									  coding : [
									    {
									      system : "http://hl7.org/fhir/observation-category",
									      code : "fitness",
									      display : measure.category_name;
									   }
									  ]
								},
								code : { coding : [ { system : "http://midata.coop", code : measure.content, display : measure.name_translated } ] },
								effectiveDateTime : recDate,
								valueQuantity : {
									value : val,
									unit : itm.unit
								}
							  };
							  
							  var action = saveOrUpdateRecord(measure.title, measure.content, recDate, rec);
							  if (action !== null) actions.push(action);		
							  
							  // Limit request size
							  if (actions.length > 200) {
								  processTransaction(actions);
								  actions = [];
							  }
							  
						  }
						});						
												
					});				
				}
				if (actions.length > 0) {				  
				  processTransaction(actions);
				}
				
				$scope.requesting--;
				finish();
			}).
			error(function(err) {
					errorMessage("Failed to import data on " + formattedDate + ": " + err);
			});
						
			//$scope.requesting = false;
		};
		
		// make a two digit string out of a given number
		var twoDigit = function(num) {
			return ("0" + num).slice(-2);
		};

		// save a single record to the database
		var saveOrUpdateRecord = function(title, content, formattedDate, record) {
			var existing = stored[content+formattedDate];
			if (existing) {
				if (existing.data.valueQuantity.value != record.valueQuantity.value) {
					return updateRecord(existing._id, existing.version, record);
				} else { 
					return null; 
				}			
			} else {
				return saveRecord(title, content, formattedDate, record);
			} 
		};
			
		// save a single record to the database
		var saveRecord = function(title, content, formattedDate, record) {
			/*var name = title.replace("{date}", formattedDate);			
			midataServer.createRecord($scope.authToken, { "name" : name, "content" : content, "format" : "fhir/Observation", subformat : "Quantity" }, record)
			.then(function() {
					$scope.saved += 1;
					finish();
			})
			.catch(function(err) {
					errorMessage("Failed to save record '" + name + "' to database: " + err);
			});*/
			
			return {
				"resource" : record,
				"request" : {
					"method" : "POST",
					"url" : "Observation"
				}
			};
		};
		
		var updateRecord = function(id, version, record) {			
			/*midataServer.updateRecord($scope.authToken, id, version, record)
			.then(function() {
					$scope.saved += 1;
					finish();
			})
			.catch(function(err) {
					errorMessage("Failed to update record to database: " + err);
			});*/
			record.meta = { "version" : version };
			record.id = id;
			return {
				"resource" : record,
				"request" : {
					"method" : "PUT",
					"url" : "Observation/"+id
				}
			};
		};
		
		var processTransaction = function(actions) {
			var request = {
			   "resourceType": "Bundle",
			   "id": "bundle-transaction",
			   "type": "transaction",
			   "entry": actions
			};
			$scope.requested++;
			midataServer.fhirTransaction($scope.authToken, request)
			.then(function() {
				$scope.saved++;
				finish();
			});
		};
			
		// handle errors during import
		var errorMessage = function(errMsg) {
			$scope.error.messages.push(errMsg);
			finish();
		};

		// update application state at the end of an import
		var finish = function() {
			if ($scope.requesting === 0 && $scope.requested === $scope.saved + $scope.error.messages.length) {
				if ($scope.repeat) {
					$scope.repeat = false;
					$scope.totalImport += $scope.saved;
					setToDate(true);
					$scope.startImport();
				} else {							
					$scope.status = "done";
					if ($scope.error.messages.length > 0) {
						$scope.status = "with_errors"; 
					}
					$scope.saving = false;
					if ($scope.alldone != null) {
						$scope.alldone.resolve();
					} else {
					    $scope.initForm($scope.authToken);
					}
				}
			}
		};
		
		$scope.automatic = function(authToken, lang) {
			console.log("run automatic");
			$translate.use(lang);
			$scope.alldone = $q.defer();
			$scope.initForm(authToken)
			.then(function() {
				console.log("past init");
				$scope.startImport();
			});
			console.log("end automatic");
			return $scope.alldone.promise;
		};
		
		$scope.saveConfig = function() {
			var config = { autoimport : $scope.autoimport, selected:[] };
			angular.forEach($scope.measurements, function(measurement) {
				if (measurement.import) config.selected.push(measurement.name);				
			});
			midataServer.setConfig($scope.authToken, config, $scope.autoimport);
		};
	
	return $scope;	
}]);
fitbit.controller('ImportCtrl', ['$scope', '$http', '$location', '$translate', 'midataServer', 'midataPortal', 'importer',  
	function($scope, $http, $location, $translate, midataServer, midataPortal, importer) {
		// init
	    midataPortal.autoresize();
	    $translate.use(midataPortal.language);
        $scope.importer = importer;	    
					
		// get authorization token
		var authToken = $location.path().split("/")[1];
							
		$scope.progress = function() {
			var r = $scope.importer.requested > 0 ? $scope.importer.requested : 1;
			return { 'width' : ($scope.importer.saved * 100 / r)+"%" };
		};
		
		$scope.importer.initForm(authToken);
		
		$scope.submit = function() {
		   importer.saveConfig();
		   importer.startImport();
		};
	}
]);
fitbit.controller('PreviewCtrl', ['$scope', '$http', '$location', '$translate', 'midataServer', 'midataPortal', 'importer',  
	function($scope, $http, $location, $translate, midataServer, midataPortal, importer) {
	    $translate.use(midataPortal.language);
        $scope.importer = importer;	    
					
		// get authorization token
		var authToken = $location.path().split("/")[1];

		$scope.importer.initForm(authToken, true);
		
	}
]);
