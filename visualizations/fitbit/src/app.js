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
	midataServer.setSingleRequestMode(true);
	$scope.error = {};
	$scope.reimport = 7;
	$scope.status = null;
	$scope.measuresRequested = 0;
	$scope.measuresDone = 0;
	$scope.saving = false;
	$scope.saved = 0;
	$scope.totalImport = 0;
	$scope.measure = null;		
	$scope.measurements = [
						
			// {
			// 	"id" : "food_calories_intake",
			// 	"name": "Food - Calories Intake",				
			// 	"endpoint": "/1/user/-/foods/log/caloriesIn/date/{date}/1d.json",
			// 	"content" : "food/calories-in",
			// 	"unit" : "kcal",
			// 	"system" : "http://midata.coop", 
			//	"unitSystem" : "http://unitsofmeasure.org", 
			//	"unitCode" : ""
			// },
			{
			 	"id" : "food_water_consumption",
			 	"name": "Food - Water Consumption",				
			 	"endpoint": "/1/user/-/foods/log/water/date/{date}/1d.json",
			 	"content" : "food/water",
			 	"unit" : "ml",
			 	"system" : "http://midata.coop", 
			 	"code": "food/water",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : ""
			 },
			// {
			// 	"id" : "activities_calories_burned",
			// 	"name": "Activities - Calories Burned",				
			// 	"endpoint": "/1/user/-/activities/calories/date/{date}/1d.json",
			// 	"content" : "activities/calories",
			// 	"unit" : "kcal",
			// 	"system" : "http://midata.coop", 
			//	"unitSystem" : "http://unitsofmeasure.org", 
			//	"unitCode" : ""
			// },
			{
				"id" : "activities_steps",
				"name": "Activities - Steps",				
				"endpoint": "/1/user/-/activities/steps/date/{date}/1d.json",
				"content" : "activities/steps",
				"unit" : "steps",
				"system" : "http://loinc.org",
				"code" : "41950-7",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : "/d"
			},
			// Wrong LOINC code
			//{
			//	"id" : "activities_distance",
			//	"name": "Activities - Distance",				
			//	"endpoint": "/1/user/-/activities/distance/date/{date}/1d.json",
			//	"content" : "activities/distance",
			//	"unit" : "km",
			//	"system" : "http://loinc.org",
			//	"code" : "41953-1",
			//	"unitSystem" : "http://unitsofmeasure.org", 
			//	"unitCode" : "m"
			//},
			{
			 	"id" : "activities_floors_climbed",
				"name": "Activities - Floors Climbed",				
				"endpoint": "/1/user/-/activities/floors/date/{date}/1d.json",
				"content" : "activities/floors",
				"unit" : "floors",
				"system" : "http://midata.coop",
				"code" : "activities/floors",
			    "unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : ""
			 },
			 {
			 	"id" : "activities_elevation",
			 	"name": "Activities - Elevation",				
			 	"endpoint": "/1/user/-/activities/elevation/date/{date}/1d.json",
			 	"content" : "activities/elevation",
			 	"unit" : "m",
			 	"system" : "http://midata.coop", 
			 	"code" : "activities/elevation",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : "m"
			 },
			// {
			// 	"id" : "activities_minutes_sedentary",
			// 	"name": "Activities - Minutes Sedentary",				
			// 	"endpoint": "/1/user/-/activities/minutesSedentary/date/{date}/1d.json",
			// 	"content" : "activities/minutes-sedentary",
			// 	"unit" : "min",
			// 	"system" : "http://midata.coop", 
			//	"unitSystem" : "http://unitsofmeasure.org", 
			//	"unitCode" : ""
			// },
			 {
			 	"id" : "activities_minutes_lightly_active",
			 	"name": "Activities - Minutes Lightly Active",				
			 	"endpoint": "/1/user/-/activities/minutesLightlyActive/date/{date}/1d.json",
			 	"content" : "activities/minutes-lightly-active",
			 	"unit" : "min",
			 	"system" : "http://midata.coop", 
			 	"code" : "activities/minutes-lightly-active",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : "min"
			 },
			 {
			 	"id" : "activities_minutes_fairly_active",
			 	"name": "Activities - Minutes Fairly Active",				
			 	"endpoint": "/1/user/-/activities/minutesFairlyActive/date/{date}/1d.json",
			 	"content" : "activities/minutes-fairly-active",
			 	"unit" : "min",
			 	"system" : "http://midata.coop", 
			 	"code" : "activities/minutes-fairly-active",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : "min"
			 },
			 {
			 	"id" : "activities_minutes_very_active",
			 	"name": "Activities - Minutes Very Active",				
			 	"endpoint": "/1/user/-/activities/minutesVeryActive/date/{date}/1d.json",
			 	"content" : "activities/minutes-very-active",
			 	"unit" : "min",
			 	"system" : "http://midata.coop",
			 	"code" : "activities/minutes-very-active",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : "min"
			 },
			// {
			// 	"id" : "activities_calories_burned_in_activities",
			// 	"name": "Activities - Calories Burned in Activities",				
			// 	"endpoint": "/1/user/-/activities/activityCalories/date/{date}/1d.json",
			// 	"content" : "activities/activity-calories",
			// 	"unit" : "kcal",
			// 	"system" : "http://midata.coop", 
			//	"unitSystem" : "http://unitsofmeasure.org", 
			//	"unitCode" : ""
			// },
			{
				"id" : "sleep_time_in_bed",
				"name": "Sleep - Time in Bed",				
				"endpoint": "/1/user/-/sleep/timeInBed/date/{date}/1d.json",
				"content" : "sleep/time-in-bed",
				"unit" : "min",
				"system" : "http://midata.coop",
				"code" : "sleep/time-in-bed",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : ""
			},
			{
				"id" : "sleep_minutes_asleep",
				"name": "Sleep - Minutes Asleep",				
				"endpoint": "/1/user/-/sleep/minutesAsleep/date/{date}/1d.json",
			 	"content" : "sleep/minutes-asleep",
			 	"unit" : "min",
				"system" : "http://midata.coop",
				"code" : "sleep/minutes-asleep",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : ""
			 },
			 {
			 	"id" : "sleep_minutes_awake",
			 	"name": "Sleep - Minutes Awake",				
				"endpoint": "/1/user/-/sleep/minutesAwake/date/{date}/1d.json",
			 	"content" : "sleep/minutes-awake",
			 	"unit" : "min",
			 	"system" : "http://midata.coop",
			 	"code" : "sleep/minutes-awake",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : ""
			 },
			 {
			 	"id" : "sleep_minutes_to_fall_asleep",
			 	"name": "Sleep - Minutes to Fall Asleep",				
			 	"endpoint": "/1/user/-/sleep/minutesToFallAsleep/date/{date}/1d.json",
			 	"content" : "sleep/minutes-to-fall-asleep",
			 	"unit" : "min",
				"system" : "http://midata.coop", 
				"code" : "sleep/minutes-to-fall-asleep",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : ""
			 },
			// {
			// 	"id" : "sleep_efficiency",
			// 	"name": "Sleep - Efficiency",				
			// 	"endpoint": "/1/user/-/sleep/efficiency/date/{date}/1d.json",
			// 	"content" : "sleep/efficiency",
			// 	"system" : "http://midata.coop" // TODO: Achtung, "unit" nicht definiert, 
			//	"unitSystem" : "http://unitsofmeasure.org", 
			//	"unitCode" : ""
			// },
			{
				"id" : "body_weight",
				"name": "Body - Weight",				
				"endpoint": "/1/user/-/body/weight/date/{date}/1d.json",
			    "content" : "body/weight",
			    "unit" : "kg",
				"system" : "http://loinc.org",
				"code" : "29463-7",
				"unitSystem" : "http://unitsofmeasure.org", 
				"unitCode" : "kg"
			 }//,
			// {
			// 	"id" : "body_bmi",
			// 	"name": "Body - BMI",				
			// 	"endpoint": "/1/user/-/body/bmi/date/{date}/1d.json",
			// 	"content" : "body/bmi",
			// 	"unit" : "kg/m2",
			// 	"system" : "http://midata.coop", 
			//	"unitSystem" : "http://unitsofmeasure.org", 
			//	"unitCode" : ""
			// },
			// {
			// 	"id" : "body_fat",
			// 	"name": "Body - Fat",				
			// 	"endpoint": "/1/user/-/body/fat/date/{date}/1d.json",
			// 	"content" : "body/fat",	
			// 	"unit" : "%",
			// 	"system" : "http://midata.coop", 
			//	"unitSystem" : "http://unitsofmeasure.org", 
			//	"unitCode" : ""
			// }						
			
	];
	var baseUrl = "https://api.fitbit.com";
	var stored = {};
	
	/*
	var setToDate = function(amendFrom) {
		 var yesterday = new Date();
		 yesterday.setDate(yesterday.getDate() - 1);
		 yesterday.setHours(1,1,1,1);
		 
		 angular.forEach($scope.measurements, function(measurement) {
			if (amendFrom) {
				measurement.from = new Date(measurement.to.getTime());
				measurement.from.setDate(measurement.from.getDate() + 1);
				if (measurement.from.getTime() > yesterday.getTime()) {
				  measurement.skip = true;
				  console.log("skip");
				  console.log(measurement);
				} else  {
  				  measurement.skip = false;
				}
			} else measurement.skip = false;
			 
			measurement.to = yesterday;
				
			if (measurement.to.getTime() - measurement.from.getTime() > 1000 * 60 * 60 * 24 * 365) {
			  var years = Math.floor((measurement.to.getTime() - measurement.from.getTime()) / (1000 * 60 * 60 * 24 * 365));
			  if (years > $scope.totalYears) { $scope.totalYears = years; }
			  
			  measurement.to = new Date(measurement.from.getTime() + 1000 * 60 * 60 * 24 * 365);
			  console.log("do repeat");
			  measurement.repeat = true;
			  
			  
			} else measurement.repeat = false;
		 });		 
	};
	*/

	$scope.initForm = function(authToken, nofitbitquery) {
		var deferred = $q.defer();
		var done = 0;
		var reqDone = function() {
			done++;
			if (done == 3) deferred.resolve();
		};
		
		angular.forEach($scope.measurements, function(measurement) {
			measurement.from = measurement.fromDisplay = null;
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
					if (measurement.from == null || measurement.from < since) measurement.from = measurement.fromDisplay = since;					
				  });
				  
				  //setToDate(false);
				  
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
		
		  midataServer.getSummary(authToken, "content" , { "format" : "fhir/Observation" , "app" : "fitbit" })
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
					
					var displayNewest = new Date(newestDate.getTime());
					newestDate.setDate(newestDate.getDate() - $scope.reimport);
					measurement.imported = entry.count;
					
					if (!$scope.last) {
						$scope.last = displayNewest;
					} else if ($scope.last < displayNewest) {
						$scope.last = displayNewest;
					}
					
					
					if (measurement.from == null || measurement.from < newestDate) {
						measurement.from = newestDate;
						measurement.fromDisplay = displayNewest;
					}
				}
			});
			reqDone();
		  }, function() { reqDone(); });
			
		  return deferred.promise;
		};
		
		$scope.startImport = function() {
			$scope.error.message = null;
			$scope.error.messages = [];			
									
			$scope.saved = 0;			
			$scope.status = "importing";
			$scope.saving = true;
			
			var actionDef = $q.defer();
			var actionChain = actionDef.promise;
			actionDef.resolve();
						
			angular.forEach($scope.measurements, function(measure) {
				if (measure.import && !(measure.skip)) {
					$scope.measuresRequested++;
					var f = function() { return getPrevRecords(measure); };
					actionChain = actionChain.then(f);					
				}
			});			
			
			actionChain = actionChain.then(function() {
			  var work = [];
			  angular.forEach($scope.measurements, function(measure) {
				if (measure.import && !(measure.skip)) {					
					work.push(importRecords(measure));					
				}
			  });
			  return $q.all(work);
			});
			
			return actionChain.then(function() {
			  $scope.status = "done";			
			  if ($scope.error.messages.length > 0) {
				$scope.status = "with_errors"; 
			  }
			  $scope.saving = false;
			  console.log("Done import");
			});
						
		};
		
		var getPrevRecords = function(measure) {
			var fromDate = measure.from;
			var fromFormatted = fromDate.getFullYear() + "-" + twoDigit(fromDate.getMonth() + 1) + "-" + twoDigit(fromDate.getDate());
			
		   return midataServer.getRecords($scope.authToken, { "format" :"fhir/Observation", "content" : measure.content, "index" : { "effectiveDateTime" : { "!!!ge" : fromFormatted }} }, ["version", "content", "data"])
			   .then(function(results) {
				   angular.forEach(results.data, function(rec) {
					 stored[rec.content+rec.data.effectiveDateTime] = rec;  
				   });				  
			   });
		};
		
	    var importRange = function(measure, fromDate, toDate) {
			
			var formattedFromDate = fromDate.getFullYear() + "-" + twoDigit(fromDate.getMonth() + 1) + "-" + twoDigit(fromDate.getDate());
			var formattedEndDate = toDate.getFullYear() + "-" + twoDigit(toDate.getMonth() + 1) + "-" + twoDigit(toDate.getDate());
												
			return midataServer.oauth2Request($scope.authToken, baseUrl + measure.endpoint.replace("{date}", formattedFromDate).replace("1d", formattedEndDate))
			.then(function(response1) {
				var response = response1.data;
					// check if an error was returned
				var actions = [];
				var trans = [];
				
				if (response.errors) {
					var _error_message = response.errors[0].message;
					// show error message when no 
					if (!((_error_message.indexOf("/activities/elevation") !== -1 || _error_message.indexOf("/activities/floors") !== -1) && _error_message.indexOf("nvalid time series") !== -1)) {
							errorMessage("Failed to import data on " + formattedFromDate + ": " + _error_message + ".");	
					}
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
									      display : measure.category_name
									   }
									  ]
								},
								code : { coding : [ { system : measure.system, code : measure.code, display : measure.name_translated } ] },
								effectiveDateTime : recDate,
								valueQuantity : {
									value : Number(val),
									unit : itm.unit
								}
							  };
							  
							  if (measure.unitCode) {
								  rec.valueQuantity.system = measure.unitSystem;
								  rec.valueQuantity.code = measure.unitCode;
							  }

							  var action = saveOrUpdateRecord(measure.title, measure.content, recDate, rec);
							  if (action !== null) actions.push(action);		
							  
							  // Limit request size
							  if (actions.length > 200) {
								  trans.push(processTransaction(actions));
								  actions = [];
							  }
							  
						  }
						});						
												
					});				
				}
				if (actions.length > 0) {				  
				  trans.push(processTransaction(actions));
				}
				
				return $q.all(trans);
			});
				
		};
		
				
		// import records, one main record and possibly a detailed record for each day
		var importRecords = function(measure) {
		
			var fromDate = measure.from;
			var toDate = new Date(fromDate.getTime() + 1000 * 60 * 60 * 24 * 365);//measure.to;
			$translate("titles."+measure.id).then(function(t) { measure.title = t; });
			$translate(measure.id).then(function(t) { measure.name_translated = t; });
			$translate("fitness_data").then(function(t) { measure.category_name = t; });
			
			var yesterday = new Date();
			yesterday.setDate(yesterday.getDate() - 1);
			yesterday.setHours(1,1,1,1);
						
			var work = [];
			
			while (fromDate < yesterday) {
			  if (toDate > yesterday) toDate = yesterday;
			  work.push(importRange(measure, fromDate, toDate));			  
			  fromDate = new Date(toDate.getTime() + 1);
			  toDate = new Date(fromDate.getTime() + 1000 * 60 * 60 * 24 * 365);
			}
			
			return $q.all(work).then(function() { $scope.measuresDone++; });
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
			
			return {
				"resource" : record,
				"request" : {
					"method" : "POST",
					"url" : "Observation"
				}
			};
		};
		
		var updateRecord = function(id, version, record) {			
			
			record.meta = { "versionId" : version };
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
			return midataServer.fhirTransaction($scope.authToken, request)
			.then(function() {
				$scope.saved+=actions.length;				
			});
		};
			
		// handle errors during import
		var errorMessage = function(errMsg) {
			$scope.error.messages.push(errMsg);			
		};
	
		
		$scope.automatic = function(authToken, lang) {
			console.log("run automatic");
			$translate.use(lang);		
			return $scope.initForm(authToken)
			.then(function() {
				console.log("past init");
				return $scope.startImport();
			});			
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
		var authToken = $location.search().authToken;
							
		$scope.progress = function() {
			var r = $scope.importer.measuresRequested > 0 ? $scope.importer.measuresRequested : 1;			
			return { 'width' : Math.floor($scope.importer.measuresDone * 100 / r)+"%" };
		};
		
		$scope.importer.initForm(authToken);
		
		$scope.submit = function() {
		   importer.saveConfig();
		   importer.startImport().then(function() {
			   $scope.importer.initForm(authToken);			   
		   });
		};
	}
]);
fitbit.controller('PreviewCtrl', ['$scope', '$http', '$location', '$translate', 'midataServer', 'midataPortal', 'importer',  
	function($scope, $http, $location, $translate, midataServer, midataPortal, importer) {
	    $translate.use(midataPortal.language);
        $scope.importer = importer;	    
					
		// get authorization token
		var authToken = $location.search().authToken;

		$scope.importer.initForm(authToken, true);
		
	}
]);
