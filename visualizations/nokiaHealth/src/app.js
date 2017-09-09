var nokiaHealth = angular.module('nokiaHealth', ['midata', 'pascalprecht.translate', 'nokiaHealthi18n']);
// Configuration
nokiaHealth.config(['$translateProvider', 'i18nc', function ($translateProvider, i18nc) {

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
// The data importer
nokiaHealth.factory('importer', ['$http', '$translate', 'midataServer', '$q', function ($http, $translate, midataServer, $q) {

	midataServer.setSingleRequestMode(true);

	var importer = {};
	importer.userid = "";
	importer.autoimport = {};
	importer.error = {};
	importer.reimport = 7;
	importer.status = null;
	importer.requesting = 0;
	importer.requested = 0;
	importer.finished = false;
	importer.saving = false;
	importer.saved = 0;
	importer.notSaved = 0;
	importer.totalImport = 0;
	importer.measure = null;
	importer.alldone = null;
	importer.repeat = false;
	/** Save all MeasureTypes to show in the view. Used for the import too. */
	importer.allMeasureTypes = [];
	/** Save information to import */
	importer.allMeasurementGroups = {};
	var mapMeasureTypeToGroup = {};

	importer.codeObservations = {
		fitness: { code: "fitness", translate: "fitness_data", name_translated: "Fitness Data" },
		vitalSigns: { code: "vital-signs", translate: "vital_signs_data", name_translated: "Vital Signs" }
	};

	var daysRequestInterval = 150;
	var daysRequest = 7;

	importer.measurementGroups = measurementGroup;

	var codeToMidataCode = {};
	codeToMidataCode["http://loinc.org 41950-7"] = "activities/steps";
	codeToMidataCode["http://loinc.org 41953-1"] = "activities/distance-24h-calc";
	codeToMidataCode["http://midata.coop activities/calories"] = "activities/calories";
	codeToMidataCode["http://midata.coop activities/elevation"] = "activities/elevation";
	codeToMidataCode["http://midata.coop activities/minutes-lightly-active"] = "activities/minutes-lightly-active";
	codeToMidataCode["http://midata.coop activities/minutes-fairly-active"] = "activities/minutes-fairly-active";
	codeToMidataCode["http://midata.coop activities/minutes-very-active"] = "activities/minutes-very-active";
	codeToMidataCode["http://loinc.org 29463-7"] = "body/weight";
	codeToMidataCode["http://loinc.org 8302-2"] = "body/height";
	codeToMidataCode["http://midata.coop body/fat_free_mass"] = "body/fat_free_mass";
	codeToMidataCode["http://loinc.org 41982-0"] = "body/fat";
	codeToMidataCode["http://loinc.org 73708-0"] = "body/fat-total";
	codeToMidataCode["http://loinc.org 8462-4"] = "body/blood/diastolic";
	codeToMidataCode["http://loinc.org 8480-6"] = "body/blood/systolic";
	codeToMidataCode["http://loinc.org 8867-4"] = "activities/heartrate";
	codeToMidataCode["http://loinc.org 20564-1"] = "body/oxygen-saturation";
	codeToMidataCode["http://loinc.org 8310-5"] = "body/temperature";
	codeToMidataCode["http://loinc.org 73964-9"] = "body/muscle-total";
	codeToMidataCode["http://midata.coop body/hydration"] = "body/hydration";
	codeToMidataCode["http://midata.coop body/bone_mass"] = "body/bone_mass";
	codeToMidataCode["http://loinc.org 77196-4"] = "activities/pulse-wave-velocity";
	codeToMidataCode["http://midata.coop sleep/minutes-awake"] = "sleep/minutes-awake";
	codeToMidataCode["http://loinc.org 65554-8"] = "sleep/wakeup-duration";
	codeToMidataCode["http://midata.coop sleep/minutes-light-sleep"] = "sleep/minutes-light-sleep";
	codeToMidataCode["http://midata.coop sleep/minutes-asleep"] = "sleep/minutes-asleep";
	codeToMidataCode["http://midata.coop sleep/rem"] = "sleep/rem";
	codeToMidataCode["http://midata.coop sleep/wakeupcount"] = "sleep/wakeupcount";
	codeToMidataCode["http://midata.coop sleep/minutes-to-fall-asleep"] = "sleep/minutes-to-fall-asleep";
	codeToMidataCode["http://loinc.org 65554-8"] = "sleep/wakeup-duration";
	codeToMidataCode["http://loinc.org 55417-0"] = "body/bloodpressure";
	//codeToMidataCode[""] = "";

	var baseUrl = "https://api.health.nokia.com";
	var stored = {};

	// // range: lastweek | lastmonth | lastyear | specified
	// // date_from: type -> Date
	// // date_to: type -> Date
	// var setDateInMeasurements = function (range, date_from, date_to) {
	// 	var _from = new Date();
	// 	var _to = new Date();

	// 	if (range !== undefined && range !== null) {

	// 		if (range == "lastweek") {
	// 			_from.setDate(_from.getDate() - 8);
	// 			_from.setHours(1, 1, 1, 1);

	// 			_to.setDate(_to.getDate() - 1);
	// 			_to.setHours(1, 1, 1, 1);
	// 		} else if (range == "lastmonth") {
	// 			_from.setMonth(_from.getMonth() - 1);
	// 			_from.setHours(1, 1, 1, 1);

	// 			_to.setDate(_to.getDate() - 1);
	// 			_to.setHours(1, 1, 1, 1);
	// 		} else if (range == "lastyear") {
	// 			_from.setFullYear(_from.getFullYear() - 1);
	// 			_from.setHours(1, 1, 1, 1);

	// 			_to.setDate(_to.getDate() - 1);
	// 			_to.setHours(1, 1, 1, 1);
	// 		} else if (range == "specified") {
	// 			_from = date_from;
	// 			_to = date_to;
	// 		}
	// 	}

	// 	//angular.forEach(importer.measurementGroups, function (measurement) {
	// 	//	measurement.from = _from;
	// 	//	measurement.to = _to;
	// 	//});
	// };

	/**
	 * This function load the configs and update the view
	 * @param {string} authToken is the authentication token
	 */
	importer.initForm = function (authToken) {
		var deferred = $q.defer();
		var done = 0;
		var reqDone = function () {
			done++;
			if (done == 2) deferred.resolve();
		};

		// configs
		midataServer.getConfig(authToken)
			.then(function (response) {

				// initialize allMeasureTypes
				importer.allMeasureTypes = [];

				// pro Measurement-Group all measurement-Types
				importer.measurementGroups.forEach(function(measurementGroup){
					measurementGroup.measureTypes.forEach(function(measureType){
						// configurations to import
						measureType.import = false;

						if (response.data && response.data.selected) {
							if (response.data.selected.indexOf(measureType.id) >= 0) {
								measureType.import = true;
							}
						}

						importer.allMeasureTypes.push(measureType);

						mapMeasureTypeToGroup[measureType.id] = measurementGroup.groupMeasureId;

						// translations pro measureTyoe
						// it would be used to import the records... For that reason it is not in a chain...
						$translate(measurementType.id).then(function (t) { measurementType.name_translated = t; });
						$translate(importer.codeObservations.fitness.translate).then(function (t) { importer.codeObservations.fitness.name_translated = t; });
						$translate(importer.codeObservations.vitalSigns.translate).then(function (t) { importer.codeObservations.vitalSigns.name_translated = t; });

						if (measurementType.diastolic) {
							$translate(measurementType.diastolic.id).then(function (t) { measurementType.diastolic.name_translated = t; });
						}
						if (measurementType.systolic) {
							$translate(measurementType.systolic.id).then(function (t) { measurementType.systolic.name_translated = t; });
						}
					});
				});
				// for (var i = 0; i < importer.measurementGroups.length; i++) {
				// 	var measurementGroup = importer.measurementGroups[i];
				// 	for (var j = 0; j < measurementGroup.measureTypes.length; j++) {
				// 		// configurations to import
				// 		measurementGroup.measureTypes[j].import = false;

				// 		if (response.data && response.data.selected) {
				// 			if (response.data.selected.indexOf(measurementGroup.measureTypes[j].id) >= 0) {
				// 				measurementGroup.measureTypes[j].import = true;
				// 			}
				// 		}

				// 		importer.allMeasureTypes.push(measurementGroup.measureTypes[j]);

				// 		mapMeasureTypeToGroup[measurementGroup.measureTypes[j].id] = measurementGroup.groupMeasureId;

				// 		// translations pro measureTyoe
				// 		// it would be used to import the records... For that reason it is not in a chain...
				// 		$translate(measurementType.id).then(function (t) { measurementType.name_translated = t; });
				// 		$translate(importer.codeObservations.fitness.translate).then(function (t) { importer.codeObservations.fitness.name_translated = t; });
				// 		$translate(importer.codeObservations.vitalSigns.translate).then(function (t) { importer.codeObservations.vitalSigns.name_translated = t; });

				// 		if (measurementType.diastolic) {
				// 			$translate(measurementType.diastolic.id).then(function (t) { measurementType.diastolic.name_translated = t; });
				// 		}
				// 		if (measurementType.systolic) {
				// 			$translate(measurementType.systolic.id).then(function (t) { measurementType.systolic.name_translated = t; });
				// 		}
				// 	}
				// }

				if (response.data && response.data.autoimport != null) {
					importer.autoimport = response.data.autoimport;
				} else {
					importer.autoimport = false;
				}

				reqDone();
			}, function () { reqDone(); })
			.then(function (response) {

				// get summary
				midataServer.getSummary(authToken, "content", { "format": "fhir/Observation", "app": "nokiaHealth" })
					.then(function (response) {
						var map = {};
						importer.allMeasurementGroups = {};

						angular.forEach(importer.allMeasureTypes, function (measureType) {
							var midataCode = codeToMidataCode[measureType.system + " " + measureType.code];
							map[midataCode] = measureType;
						});

						for (var k = 0; k < response.data.length; k++) {
							var entry = response.data[k];
							var measurement = map[entry.contents[0]];

							if (measurement != null) {
								var newestDate = new Date(entry.newest);

								newestDate.setHours(1, 1, 1, 1);

								//if (!importer.last || importer.last < newestDate) {
								//	importer.last = newestDate;
								//}

								// update view
								measurement.imported = entry.count;
								if (measurement.from == null || measurement.from < newestDate) measurement.from = newestDate;

								// set value of "from" to import
								if (!importer.allMeasurementGroups[mapMeasureTypeToGroup[measuremente.id]].from ||
									importer.allMeasurementGroups[mapMeasureTypeToGroup[measuremente.id]].from.getTime() - (1000 * 60 * 60 * 24 * daysRequest) > newestDate.getTime()) {
									importer.allMeasurementGroups[mapMeasureTypeToGroup[measuremente.id]].from = newestDate;
								}
							}
						}

						reqDone();
					},
					function () { reqDone(); });
			});

		return deferred.promise;
	};

	importer.saveConfig = function (authToken) {
		var config = { autoimport: importer.autoimport, selected: [] };
		angular.forEach(importer.allMeasureTypes, function (measurement) {
			if (measurement.import) config.selected.push(measurement.id);
		});

		if (importer.autoimport === undefined || importer.autoimport === null)
			importer.autoimport = true;

		return midataServer.setConfig(authToken, config, importer.autoimport);
	};

	// This is triggered from server
	importer.automatic = function (authToken, lang) {

		// set language to be used
		$translate.use(lang);

		return importer.initForm(authToken)
			.then(function () {
				importer.importNow(authToken);
			});
	};

	// Trigger the import. Must be runnable from webbrowser or from server
	importer.importNow = function (authToken) {

		console.log("import started");
		importer.saving = true;
		importer.status = "importing";
		importer.requested = 0;
		importer.saved = 0;
		importer.notSaved = 0;
		importer.requesting = 0;
		importer.error.message = null;
		importer.error.messages = [];

		///** used to import from Server in days*/
		//var baseDateToStartImport = new Date();
		//baseDateToStartImport.setFullYear(2008);
		//baseDateToStartImport.setHours(1,1,1,1);

		// // if imported from portal
		// if (importer.firstTime) {
		// 	// // Withing founded: June 2008
		// } else { // if imported from Server
		// 	baseDateToStartImport.setDate(baseDateToStartImport.getDate() - daysRequest);
		// 	baseDateToStartImport.setHours(1,1,1,1);
		// }
		//console.log("importing from " + baseDateToStartImport.getFullYear() + "-" + twoDigit(baseDateToStartImport.getMonth() + 1) + "-" + twoDigit(baseDateToStartImport.getDate()));

		// load cache
		var _defPrevRecords = $q.defer();
		var _chainPrevRecords = _defPrevRecords.promise;
		_defPrevRecords.resolve();
		importer.allMeasureTypes.forEach(function (measureType) {
			if (measureType.import) {

				var _from = null;

				if (importer.allMeasurementGroups[measureType.id] && importer.allMeasurementGroups[measureType.id].from) {
					_from = new Date(importer.allMeasurementGroups[measureType.id].from.getTime());
				} else {
					// that means, first import
					_from = new Date();
					_from.setFullYear(2008);
					_from.setHours(1, 1, 1, 1);
				}
				//// get records from 7 days before last update of this measurementType
				//if (measureType.from) {
				//	_from = measureType.from.getTime() - (1000*60*60*24*daysRequest);	
				//} else {
				//	_from = new Date(baseDateToStartImport.getTime());
				//}
				// get prev if newest is null
				var f = function () { return getPrevRecords(authToken, measureType.system + " " + measureType.code, _from); };
				_chainPrevRecords = _chainPrevRecords.then(f);
			}
		});

		// get user id
		midataServer.getOAuthParams(authToken)
			.then(function (response) {
				importer.userid = response.data.userid;
				importingRecords();
			});
	};

	var importingRecords = function () {
		var actionDef = $q.defer();
		var actionChain = actionDef.promise;
		actionDef.resolve();

		importer.requesting += importer.measurementGroups.length;

		var importFinished = true;
		// Import all defined measurements groups
		importer.measurementGroups.forEach(function (measurementGroup) {

			if (importer.allMeasurementGroups[measurementGroup.groupMeasureId] && importer.allMeasurementGroups[measurementGroup.groupMeasureId].from) {
				importFinished = false;

				// Set Interval to import
				var _fromBase = new Date(importer.allMeasurementGroups[measurementGroup.groupMeasureId].from.getTime());

				// = milliseconds * seconds * minutes * hours * days
				var intervalDaysInMilliseconds = 1000 * 60 * 60 * 24 * daysRequestInterval;
				var today = new Date();
				today.setHours(1, 1, 1, 1);

				var temp_to_milliseconds = _fromBase.getTime() + intervalDaysInMilliseconds;
				if (temp_to_milliseconds > today.getTime()) {
					temp_to_milliseconds = today.getTime();

					// should not import again this measurType
					delete importer.allMeasurementGroups[measurementGroup.groupMeasureId];
				} else {
					// update value for the next iteration minus 1 day to avoid data loss
					importer.allMeasurementGroups[measurementGroup.groupMeasureId].from = new Date(temp_to_milliseconds - (1000*60*60*24));
				}

				// 3. request to Nokia Health
				var f = function(){
					return midataServer.oauth1Request(authToken, measurementGroup.getURL(importer.userid, _fromBase, new Date(temp_to_milliseconds)))
					.then(function (response) {
						if (response.data.status == "0") {
							//save records
							if (measurementGroup.groupMeasureId == 'activity_measures') {
								// save every measure
								measurementGroup.measureTypes.forEach(function (measurement) {
									if (measurement.import) {
										save_activities(authToken, response, measurement);
									}
								});
								updateRequesting();

							} else if (measurementGroup.groupMeasureId == 'body_measures') {
								save_bodyMeasures(authToken, response, measurementGroup);
								updateRequesting();
							} else if (measurementGroup.groupMeasureId == 'sleep_summary') {
								save_sleepMeasures(authToken, response, measurementGroup);
								updateRequesting();
							} else {
								console.log("Error! new measurement group not defined");
								updateRequesting();
							}

						} else {
							console.log("Error: url " + measurementGroup.getURL(_userid, _fromBase, new Date(temp_to_milliseconds)));
							if (response.data.error) {
								console.log("Error Message from Nokia Health (status: " + response.data.status + "): " + response.data.error);
							}
							updateRequesting();
						}
					}, function (error) {
						console.log('Failed: ' + error);
						updateRequesting();
					});
				};

				actionChain = actionChain.then(f);
			} else {
				importer.requesting--;
			}
		}, this);

		if (importFinished) {
			importer.finished = true;
		}

		//return actionChain;
	};

	var counterFromTest = 0;
	/**
	 * This function save the prev Records in cache
	 * @param {string} authToken The authentication token
	 * @param {string} code the code used as key to save the record
	 * @param {Date} from The start date to get the previous records
	 */
	var getPrevRecords = function (authToken, code, from) {
		var fromFormatted = from.getFullYear() + "-" + twoDigit(from.getMonth() + 1) + "-" + twoDigit(from.getDate());
		console.log("from in getPrevRecords (#" + (counterFromTest++) + "): " + fromFormatted + ". code: " + code);
		return midataServer.getRecords(authToken, { "format": "fhir/Observation", "code": code, "index": { "effectiveDateTime": { "!!!ge": from } } }, ["version", "content", "data"])
			.then(function (results) {
				angular.forEach(results.data, function (rec) {
					stored[rec.content + rec.data.effectiveDateTime] = rec;
				});
			}, function (reason) {
				console.log('Failed: ' + reason);
			});
	};

	// save a single record to the database
	var createRecordToSaveOrUpdate = function (code, record) {
		var existing = stored[codeToMidataCode[code] + record.effectiveDateTime];
		if (existing && existing.data) {
			if (codeToMidataCode[code] == 'body/bloodpressure') {
				// use 2 components
				if (existing.data.component &&
					record.component &&
					record.component[0].valueQuantity &&
					record.component[1].valueQuantity &&
					existing.data.component[0].valueQuantity &&
					existing.data.component[1].valueQuantity &&
					(existing.data.component[0].valueQuantity.value != record.component[0].valueQuantity.value ||
						existing.data.component[1].valueQuantity.value != record.component[1].valueQuantity.value)) {

					return createRecordToUpdate(existing._id, existing.version, record);
				}
			} else {
				if (existing.data.valueQuantity && existing.data.valueQuantity.value &&
					existing.data.valueQuantity.value != record.valueQuantity.value) {
					return createRecordToUpdate(existing._id, existing.version, record);
				}
			}
		} else {
			return createRecordToSave(record);
		}

		return null;
	};

	// save a single record to the database
	var createRecordToSave = function (record) {
		return {
			"resource": record,
			"request": {
				"method": "POST",
				"url": "Observation"
			}
		};
	};

	var createRecordToUpdate = function (id, version, record) {

		record.meta = { "versionId": version };
		record.id = id;
		return {
			"resource": record,
			"request": {
				"method": "PUT",
				"url": "Observation/" + id
			}
		};
	};

	// make a two digit string out of a given number
	var twoDigit = function (num) {
		return ("0" + num).slice(-2);
	};

	var save_activities = function (authToken, response, measurement) {
		var actions = [];
		// save every measure of every activity
		response.data.body.activities.forEach(function (activity) {

			var measure = activity[measurement.measureType];
			if (measure !== undefined && measure !== null && measure != "0") {

				var recordContent = {
					resourceType: "Observation",
					status: "preliminary",
					category: {
						coding: [{ system: "http://hl7.org/fhir/observation-category", code: importer.codeObservations.fitness.code, display: importer.codeObservations.fitness.name_translated }]
					},
					code: {
						coding: [{ system: measurement.system, code: measurement.code, display: measurement.name_translated }]
					},
					effectiveDateTime: activity.date, //yyyy-MM-ddThh:mm:ss.xxxx //-?[0-9]{4}(-(0[1-9]|1[0-2])(-(0[0-9]|[1-2][0-9]|3[0-1])(T([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\.[0-9]+)?(Z|(\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)))?)?)?
					valueQuantity: {
						value: Math.round(activity[measurement.measureType] * measurement.factor), // factor: to convert (ex: seconds to minutes)
						unit: measurement.unit
					}
				};

				if (measurement.unitCode) {
					recordContent.valueQuantity.system = measurement.unitSystem;
					recordContent.valueQuantity.code = measurement.unitCode;
				}

				var action = createRecordToSaveOrUpdate(measurement.system + " " + measurement.code, recordContent);

				if (action !== null) {
					importer.requested++;
					actions.push(action);
				}

				// Limit request size
				if (actions.length >= 200) {
					processTransaction(authToken, actions);
					actions = [];
				}
			}
		});

		if (actions.length > 0) { processTransaction(authToken, actions); }
	};


	var save_bodyMeasures = function (authToken, response, measurementGroup) {
		var actions = [];

		// save every measure
		response.data.body.measuregrps.forEach(function (mgroup) {
			var date = new Date(mgroup.date * 1000);

			var recordBPDiastolic = null;
			var recordBPSystolic = null;
			var _BPMeasurementType = null;
			mgroup.measures.forEach(function (measure) {

				for (var index = 0; index < measurementGroup.measureTypes.length; index++) {
					var measurementType = measurementGroup.measureTypes[index];

					if (!measurementType.import) { continue; }

					if (measurementType.id == "body_measures_blood_pressure" && measurementType.diastolic.measureType == measure.type) {
						_BPMeasurementType = measurementType;
						recordBPDiastolic = {
							"code": {
								"coding": [
									{
										"system": measurementType.diastolic.system,
										"code": measurementType.diastolic.code,
										"display": measurementType.diastolic.name_translated
									}
								]
							},
							"valueQuantity": {
								"value": Math.round10(Math.pow(10, measure.unit) * measure.value, -2),
								"unit": measurementType.unit
							}
						};

						if (measurementType.unitCode) {
							recordBPDiastolic.valueQuantity.system = measurementType.unitSystem;
							recordBPDiastolic.valueQuantity.code = measurementType.unitCode;
						}

					} else if (measurementType.id == "body_measures_blood_pressure" && measurementType.systolic.measureType == measure.type) {
						_BPMeasurementType = measurementType;
						recordBPSystolic = {
							"code": {
								"coding": [
									{
										"system": measurementType.systolic.system,
										"code": measurementType.systolic.code,
										"display": measurementType.systolic.name_translated
									}
								]
							},
							"valueQuantity": {
								"value": Math.round10(Math.pow(10, measure.unit) * measure.value, -2),
								"unit": measurementType.unit
							}
						};

						if (measurementType.unitCode) {
							recordBPSystolic.valueQuantity.system = measurementType.unitSystem;
							recordBPSystolic.valueQuantity.code = measurementType.unitCode;
						}
					}
					else if (measurementType.measureType == measure.type) {

						var recordContent = {
							resourceType: "Observation",
							status: "preliminary",
							category: {
								coding: [{ system: "http://hl7.org/fhir/observation-category", code: importer.codeObservations.vitalSigns.code, display: importer.codeObservations.vitalSigns.name_translated }]
							},
							code: {
								coding: [{ system: measurementType.system, code: measurementType.code, display: measurementType.name_translated }]
							},
							effectiveDateTime: date.toJSON(),
							valueQuantity: {
								value: Math.round10(Math.pow(10, measure.unit) * measure.value, -2), //* measurementType.factor, // factor: to convert (ex: seconds to minutes)
								unit: measurementType.unit
							}
						};

						if (measurementType.unitCode) {
							recordContent.valueQuantity.system = measurementType.unitSystem;
							recordContent.valueQuantity.code = measurementType.unitCode;
						}

						var action = createRecordToSaveOrUpdate(measurementType.system + " " + measurementType.code, recordContent);

						if (action !== null) {
							importer.requested++;
							actions.push(action);
						}

						// Limit request size
						if (actions.length >= 200) {
							processTransaction(authToken, actions);
							actions = [];
						}

						break;
					}
				} // end for
			});

			if (recordBPDiastolic || recordBPSystolic) {
				var recordBPContent = {
					resourceType: "Observation",
					status: "preliminary",
					category: {
						coding: [{ system: "http://hl7.org/fhir/observation-category", code: importer.codeObservations.vitalSigns.code, display: importer.codeObservations.vitalSigns.name_translated }]
					},
					code: {
						coding: [{ system: _BPMeasurementType.system, code: _BPMeasurementType.code, display: _BPMeasurementType.name_translated }]
					},
					effectiveDateTime: date.toJSON(),
					component: [recordBPDiastolic, recordBPSystolic]
				};

				var actionBP = createRecordToSaveOrUpdate(_BPMeasurementType.system + " " + _BPMeasurementType.code, recordBPContent);

				if (actionBP !== null) {
					importer.requested++;
					actions.push(actionBP);
				}

				// Limit request size
				if (actions.length >= 200) {
					processTransaction(authToken, actions);
					actions = [];
				}
			}
		});

		if (actions.length > 0) { processTransaction(authToken, actions); }
	};

	var save_sleepMeasures = function (authToken, response, measurementGroup) {
		var actions = [];
		// save every measure
		response.data.body.series.forEach(function (sleepInformation/*mgroup*/) {
			var startdate = new Date(sleepInformation.startdate * 1000);
			var enddate = new Date(sleepInformation.enddate * 1000);
			var date = sleepInformation.date; // yyyy-mm-dd

			for (var index = 0; index < measurementGroup.measureTypes.length; index++) {
				var measurementType = measurementGroup.measureTypes[index];

				if (!measurementType.import) { continue; }

				var valueForTheRecord = sleepInformation.data[measurementType.measureType];
				if (valueForTheRecord !== undefined && valueForTheRecord !== null && valueForTheRecord != "0") {

					var recordContent = {
						resourceType: "Observation",
						status: "preliminary",
						category: {
							coding: [{ system: "http://hl7.org/fhir/observation-category", code: importer.codeObservations.vitalSigns.code, display: importer.codeObservations.vitalSigns.name_translated }]
						},
						code: {
							coding: [{ system: measurementType.system, code: measurementType.code, display: measurementType.name_translated }]
						},
						effectiveDateTime: startdate.toJSON(),// ?? start or end??
						valueQuantity: {
							value: Math.round(valueForTheRecord * measurementType.factor), // factor: to convert (ex: seconds to minutes)
							unit: measurementType.unit
						}
					};

					if (measurementType.unitCode) {
						recordContent.valueQuantity.system = measurementType.unitSystem;
						recordContent.valueQuantity.code = measurementType.unitCode;
					}

					var action = createRecordToSaveOrUpdate(measurementType.system + " " + measurementType.code, recordContent);

					if (action !== null) {
						importer.requested++;
						actions.push(action);
					}

					// Limit request size
					if (actions.length >= 200) {
						processTransaction(authToken, actions);
						actions = [];
					}
				}
			}
		});

		if (actions.length > 0) { processTransaction(authToken, actions); }
	};

	var processTransaction = function (authToken, actions) {
		var request = {
			"resourceType": "Bundle",
			"id": "bundle-transaction",
			"type": "transaction",
			"entry": actions
		};

		midataServer.fhirTransaction(authToken, request)
			.then(function () {
				importer.saved += actions.length;
				finish();
			},
			function (reason) {
				importer.notSaved += actions.length;
				//importer.error.messages.push(reason);
				finish();
			});
	};

	var updateRequesting = function () {
		importer.requesting--;
		finish();
	};

	var finish = function () {
		if (importer.finished && importer.requesting === 0 && importer.saved + importer.notSaved === importer.requested) {
			importer.status = "done";

			if (importer.notSaved !== 0) {
				importer.status = "with_errors";
			}
		} else {
			importingRecords();
		}
	};

	return importer;
}]);

// HELPERS
(function () {
	/**
	 * Ajuste decimal de un número.
	 *
	 * @param {String}  tipo  El tipo de ajuste.
	 * @param {Number}  valor El numero.
	 * @param {Integer} exp   El exponente (el logaritmo 10 del ajuste base).
	 * @returns {Number} El valor ajustado.
	 */
	function decimalAdjust(type, value, exp) {
		// Si el exp no está definido o es cero...
		if (typeof exp === 'undefined' || +exp === 0) {
			return Math[type](value);
		}
		value = +value;
		exp = +exp;
		// Si el valor no es un número o el exp no es un entero...
		if (isNaN(value) || !(typeof exp === 'number' && exp % 1 === 0)) {
			return NaN;
		}
		// Shift
		value = value.toString().split('e');
		value = Math[type](+(value[0] + 'e' + (value[1] ? (+value[1] - exp) : -exp)));
		// Shift back
		value = value.toString().split('e');
		return +(value[0] + 'e' + (value[1] ? (+value[1] + exp) : exp));
	}

	// Decimal round
	if (!Math.round10) {
		Math.round10 = function (value, exp) {
			return decimalAdjust('round', value, exp);
		};
	}
	// Decimal floor
	if (!Math.floor10) {
		Math.floor10 = function (value, exp) {
			return decimalAdjust('floor', value, exp);
		};
	}
	// Decimal ceil
	if (!Math.ceil10) {
		Math.ceil10 = function (value, exp) {
			return decimalAdjust('ceil', value, exp);
		};
	}
})();
