var googleFit = angular.module('googleFit', ['midata', 'pascalprecht.translate', 'googleFiti18n']);

//Config the translate used for make the translations
googleFit.config(['$translateProvider', 'i18nc', function ($translateProvider, i18nc) {

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

//the service used to import the google fit datas to midata server
googleFit.factory('importer', ['$http', '$translate', 'midataServer', '$q', function ($http, $translate, midataServer, $q) {

	//Class variables

	var $scope = {}; //Contain the variable who has to be exported
	$scope.typeOfRequest = 'hourly';
	$scope.saving = false; //If the importation is beeing imported
	$scope.measuresDone = 0; //How many measures has been imported at the moment
	$scope.measurements = [ //Contain all the measures who has to be imported in midata
		{
			"name": "Activities - Steps",
			"dataTypeName": "com.google.step_count.delta",
			"content": "activities/steps",
			"unit": "steps",
			"system": "http://loinc.org",
			"code": "41950-7",
			"unitSystem": "http://unitsofmeasure.org",
			"unitCode": "/d"
		}
	];
	var authToken; //The authToken from midadta oauth2

	/**
	 * A request to google Fit can give only values from last 90 days. Also for obtaining values from first value we will need
	 * to do multiples request until the last value is get. 
	 * TODO: At this stage, we set an arbitrary number of requests. it wiil be good, if we can found the first date value and 
	 * then find the number of requests needed until this value.
	 */
	var nbOfRequests = 10;
	var daysSinceLastImport;
	var googleFitMeasures; //All the measures get from google fit.
	var midataRecords = []; //All the values get from midata

	//Functions variables

	/**
	* Init the service
	*/
	$scope.init = function (authTokenImport) {
		authToken = authTokenImport;
		$scope.measuresRequested = $scope.measurements.length; //number of measure to import
		var promises = [];
		//Get the config from the midata server
		promises.push(
			midataServer.getConfig(authToken)
				.then(function (response) {
					if (response.data && response.data.selected) {
						$scope.countSelected = response.data.selected.length;
						$scope.autoimport = response.data.autoimport; //If auto import has to be do
						$scope.typeOfRequest = response.data.typeOfRequest;
						//For each measure detect if this value has to be imported or not
						angular.forEach($scope.measurements, function (measurement) {
							if (response.data.selected.indexOf(measurement.name) >= 0) {
								measurement.import = true;
							}
						});
					} else {
						//For each measure detect if the values has to be imported or not
						angular.forEach($scope.measurements, function (measurement) {
							measurement.import = true;
						});
					}
				})
		);

		//For each measure get the date of the last value, of the oldest value and the number of values present in MIDATA
		angular.forEach($scope.measurements, function (measurement) {

			measurement.import = false;
			promises.push(
				midataServer.getSummary(authToken, "content", { "format": "fhir/Observation", "content": "activities/steps", "app": "googleFitPlugin" })
					.then(function (response) {
						if (typeof response.data[0] !== 'undefined') {
							measurement.newest = response.data[0].newest;
							var oneDayInMillis = 24 * 60 * 60 * 1000;
							daysSinceLastImport = Math.round(Math.abs((new Date(measurement.newest).getTime() - new Date().getTime()) / (oneDayInMillis)));
							$scope.last = measurement.newest;
							measurement.oldest = response.data[0].oldest;
							measurement.nbOfValues = response.data[0].count;
						}
					})
			);

		});
		return promises;
	};

	/**
	* Import datas from Google Fit to MIDATA
	*/
	$scope.doImportation = function (authTokenImport) {
		if (typeof authTokenImport !== 'undefined') {
			authToken = authTokenImport;
		}

		angular.forEach($scope.measurements, function (measurement) {
			$scope.measuresDone++;
			if (measurement.import) {
				if (typeof daysSinceLastImport === 'undefined' || daysSinceLastImport > 0) {
					$scope.saving = true;

					//Get the steps values from midata
					getMidataRecords(measurement).then(function (values) {
						midataRecords = values.data;
						var googleFitRequests = createMeasurementRequests(measurement, $scope.typeOfRequest); //Array containing all the requests to Google Fit
						
						googleFitResponse = sendGoogleFitRequests(googleFitRequests); //Send all the requests to Google Fit and get the answer

						//We need a promise to be sure that all the values have been obtained before continuing
						Promise.all(googleFitResponse.promise).then(function () {
							googleFitMeasures = googleFitResponse.value; //All the values obtained from Google Fit
							saveOrUpdateRecord(measurement, googleFitMeasures, midataRecords); //Compare the values obtained with google fit, with the values in midata. If the value don't exist, create a new value, if not update the value.

						});
					});
				} else {
					$scope.saving = false;
				}

			}
		});
	};

	/**
	* Create an array containing the requests to obtain from Google Fit the steps for 90 days divided by hours.
	* @param nbOfRequests How many requests containing blocks of 90 days in the past we want to have
	* @return The array containing the requests
	*/
	var createMeasurementRequests = function (measurement, typeOfRequest) {
		var startDateInMillis;
		var endDateInMillis;
		var googleFitMeasurementRequests = [];
		var requests = [];
		var ninetyDaysInMilli = 1000 * 60 * 60 * 24 * 90;
		if (typeof $scope.last !== 'undefined') {
			if (daysSinceLastImport < 90) {
				requests.push({
					start: $scope.last,
					end: new Date().getTime()
				});
			} else {
				nbOfRequests = daysSinceLastImport / 90;
				for (var i = 0; i < nbOfRequests; i++) {
					requests.push({
						start: new Date().getTime() - ninetyDaysInMilli * (i + 1),
						end: new Date().getTime() - ninetyDaysInMilli * i
					});
				}
			}
		} else {
			for (var j = 0; j < nbOfRequests; j++) {
				if (nbOfRequests === j + 1) {
					requests.push({
						start: $scope.last,
						end: new Date().getTime() - ninetyDaysInMilli * j
					});
				} else {
					requests.push({
						start: new Date().getTime() - ninetyDaysInMilli * (j + 1),
						end: new Date().getTime() - ninetyDaysInMilli * j
					});
				}

			}
		}
		if (typeOfRequest === 'hourly') {
			for (var k = 0; k < requests.length; k++) {
				googleFitMeasurementRequests.push(
					{
						"aggregateBy": [{
							"dataTypeName": measurement.dataTypeName
						}],
						"bucketByTime": {
							"durationMillis": 1000 * 60 * 60,
						},
						"startTimeMillis": requests[k].start,
						"endTimeMillis": requests[k].end
					}
				);
			}
		} else if (typeOfRequest === 'daily') {
			for (var l = 0; l < requests.length; l++) {
				googleFitMeasurementRequests.push(
					{
						"aggregateBy": [{
							"dataTypeName": measurement.dataTypeName
						}],
						"bucketByTime": {
							"durationMillis": 1000 * 60 * 60 * 24,
						},
						"startTimeMillis": requests[l].start,
						"endTimeMillis": requests[l].end
					}
				);
			}
		} else {
			for (var m = 0; m < requests.length; m++) {
				googleFitMeasurementRequests.push(
					{
						"aggregateBy": [{
							"dataTypeName": measurement.dataTypeName
						}],
						"bucketByActivitySegment": {},
						"startTimeMillis": requests[m].start,
						"endTimeMillis": requests[m].end
					}
				);
			}
		}
		return googleFitMeasurementRequests;
	};

	/**
	* Send a request to the google server and get back the datas.
	* @param requests An array containing all the requests
	* @return The promise and the values
	*/
	var sendGoogleFitRequests = function (googleFitRequests) {
		var values = []; //Array containing all the steps values from Google Fit
		var promises = []; //Promises resolving when all the values are returned from Google Fit
		angular.forEach(googleFitRequests, function (value, key) { //For each request
			promises.push(
				midataServer.oauth2Request(authToken, 'https://www.googleapis.com/fitness/v1/users/me/dataset:aggregate', 'POST', googleFitRequests[key]).then(function (googleFitResponse) {
					angular.forEach(googleFitResponse.data.bucket, function (value) {
						if (value.dataset[0].point.length > 0) {
							values.push({
								startTimeMillis: value.startTimeMillis,
								endTimeMillis: value.endTimeMillis,
								value: value.dataset[0].point[0].value[0].intVal
							});
						}
					});
				})
			);
		});
		return { promise: promises, value: values };
	};
	/**
	* Create a FHIR observation steps object
	* @param effectiveDateTime EffectiveDateTime of the observation
	* @param value Value of the observation
	* @return The fhir object
	*/
	var midataRecord = function (measurement, startDateTime, endDateTime, value) {
		return {
			"resourceType": "Observation",
			"status": "final",
			"category": [
				{
					"coding": [
						{
							"system": "http://hl7.org/fhir/observation-category",
							"code": "fitness",
							"display": "Fitness Data"
						}
					],
				}
			],
			"code": {
				"coding": [
					{
						"system": measurement.system,
						"code": measurement.code,
						"display": measurement.name
					}
				]
			},
			"effectivePeriod": {
				start: startDateTime,
				end: endDateTime
			},
			"valueQuantity": {
				"value": value,
				"unit": measurement.unit,
				"system": measurement.unitSystem
			}
		};
	};

	var getMidataRecords = function (measurement) {
		return midataServer.getRecords(authToken, { "format": "fhir/Observation", "content": measurement.content, "index": { "effectiveDateTime": { "!!!ge": '1970-01-01' } } }, ["version", "content", "data"]);
	};

	/**
	* Algorithm to define if the value has to be created or updated in midata
	* @param googleRecords An array containing all the Google Fit records
	* @param midataRecords An array containing all the Midata records
	*/
	var saveOrUpdateRecord = function (measurement, googleRecords, midataRecords) {
		var bodyRequests = []; //Array containing all the requests to save or update the values in Midata

		angular.forEach(googleRecords, function (googleRecord) {
			var isPresent = false;
			var value;
			for (var i = 0; i < midataRecords.length; i++) {
				if (new Date(midataRecords[i].data.effectiveDateTime).getTime() === parseInt(googleRecord.startTimeMillis)) { //If the date of Google Fit steps and midata steps are the same

					isPresent = true;
					value = midataRecords[i];
				}
			}
			if (!isPresent) {
				bodyRequests.push(createRecord(midataRecord(measurement, new Date(parseInt(googleRecord.startTimeMillis)), new Date(parseInt(googleRecord.endTimeMillis)), googleRecord.value)));
			} else {
				if (value.data.valueQuantity.value !== googleRecord.value) {
					bodyRequests.push(updateRecord(value._id, value.version, value.data));
				}
			}
		});

		processTransaction(bodyRequests); //Send the bundle containing all the requests to midata
	};

	/**
	* Add information to request in order to have the correct request to midata to save a value
	*/
	var saveRecord = function (record) {
		return {
			"resource": record,
			"request": {
				"method": "POST",
				"url": "Observation"
			}
		};
	};
	/** 
	* Add information to request in order to have the correct request to midata to save a value
	*/
	var createRecord = function (record) {
		return {
			"resource": record,
			"request": {
				"method": "POST",
				"url": "Observation"
			}
		};
	};
	/**
	* Add information to request in order to have the correct request to midata to update a value
	*/
	var updateRecord = function (id, version, record) {
		record.id = id;
		record.meta = { "versionId": version };
		return {
			"resource": record,
			"request": {
				"method": "PUT",
				"url": "Observation/" + id
			}
		};
	};

	/**
	* Create a bundle to save all the requests in midata
	* @param requests An array containing all the request to save or update in midata
	*/
	var processTransaction = function (requests) {
		if (requests.length > 0) {
			var bundleRequest = {
				"resourceType": "Bundle",
				"id": "bundle-transaction",
				"type": "transaction",
				"entry": requests
			};

			midataServer.fhirTransaction(authToken, bundleRequest)
				.then(function () {
					console.log('bundle saved');
					$scope.saving = false;
					daysSinceLastImport = 0;
					$scope.last = new Date().getTime();
					
				});
		} else {
			$scope.saving = false;
		}
	};
	/**
	* Save the configuration of the plugin in midata
	*/
	$scope.saveConfig = function () {
		var config = { autoimport: $scope.autoimport, selected: [], typeOfRequest: $scope.typeOfRequest };
		angular.forEach($scope.measurements, function (measurement) {
			if (measurement.import) config.selected.push(measurement.name);
		});
		midataServer.setConfig(authToken, config, $scope.autoimport);
	};
	/**
	* Function called by the server to do the automatic importation
	* @param authToken Authtoken of the midata platform
	*/
	$scope.automatic = function (authToken) {
		console.log("run automatic");
		var init = $scope.init(authToken);
		Promise.all(init).then(function () {
			$scope.doImportation(authToken);
		}
		);
	};
	return $scope;
}]);



//Controller of the main import page
googleFit.controller('GoogleFitCtrl', ['$q', '$scope', '$filter', '$location', '$translate', 'midataServer', 'midataPortal', 'importer',
	function ($q, $scope, $filter, $location, $translate, midataServer, midataPortal, importer) {
		var authToken = $location.search().authToken;
		$scope.progress = function () {
			var r = $scope.importer.measuresRequested > 0 ? $scope.importer.measuresRequested : 1;
			return { 'width': Math.floor($scope.importer.measuresDone * 100 / r) + "%" };
		};
		var init = importer.init(authToken);
		Promise.all(init).then();
		$scope.importer = importer;
		midataPortal.autoresize();
		$translate.use(midataPortal.language);
		$scope.submit = function () {
			importer.saveConfig();
			importer.doImportation();
		};
	}
]);

//Preview of the plugin on midata portal
googleFit.controller('GoogleFitPreviewCtrl', ['$scope', '$filter', '$location', '$translate', 'midataServer', 'midataPortal', 'importer',
	function ($scope, $filter, $location, $translate, midataServer, midataPortal, importer) {
		var authToken = $location.search().authToken;
		$translate.use(midataPortal.language);
		importer.init(authToken);
		$scope.importer = importer;
	}
]);
