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

	importer.measurementGroups = measurementGroups;

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

	var stored = {};

	/**
	 * This function load the configs and update the view
	 * @param {string} authToken is the authentication token
	 */
	importer.initForm = function (authToken) {
		importer.authToken = authToken;
		importer.finished = false;

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
				importer.allMeasurementGroups = {};

				if (response.data && response.data.selected) {
					importer.countSelected = response.data.selected.length;
				}

				// pro Measurement-Group all measurement-Types
				importer.measurementGroups.forEach(function(measurementGroup){
					measurementGroup.measureTypes.forEach(function(measureType){
						// configurations to import
						measureType.import = false;

						if (response.data && response.data.selected) {
							if (response.data.selected.indexOf(measureType.id) >= 0) {
								measureType.import = true;

								// if not defined
								if (!importer.allMeasurementGroups[measurementGroup.groupMeasureId]) {
									// initialize allMeasurementGroups to import from today
									// TODO:posible solution = null?? and if null then set later 2008?
									var _from = new Date();
									_from.setFullYear(2008);
									_from.setHours(1, 1, 1, 1);
									_from.setMonth(1);
									importer.allMeasurementGroups[measurementGroup.groupMeasureId] = {from : _from};
								}
							}
						}

						importer.allMeasureTypes.push(measureType);

						mapMeasureTypeToGroup[measureType.id] = measurementGroup.groupMeasureId;

						// translations pro measureTyoe
						// it would be used to import the records... For that reason it is not in a chain...
						$translate(measureType.id).then(function (t) { measureType.name_translated = t; });
						$translate(importer.codeObservations.fitness.translate).then(function (t) { importer.codeObservations.fitness.name_translated = t; });
						$translate(importer.codeObservations.vitalSigns.translate).then(function (t) { importer.codeObservations.vitalSigns.name_translated = t; });

						if (measureType.diastolic) {
							$translate(measureType.diastolic.id).then(function (t) { measureType.diastolic.name_translated = t; });
						}
						if (measureType.systolic) {
							$translate(measureType.systolic.id).then(function (t) { measureType.systolic.name_translated = t; });
						}
					});
				});

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

								if (!importer.last || importer.last < newestDate) {
									importer.last = newestDate;
								}

								// update view
								measurement.imported = entry.count;
								if (measurement.from == null || measurement.from < newestDate) measurement.from = newestDate;

								// set value of "from" to import
								if (!importer.allMeasurementGroups[mapMeasureTypeToGroup[measurement.id]].from ||
									importer.allMeasurementGroups[mapMeasureTypeToGroup[measurement.id]].from.getTime() - (1000 * 60 * 60 * 24 * daysRequest) < newestDate.getTime()) {
									importer.allMeasurementGroups[mapMeasureTypeToGroup[measurement.id]].from = newestDate;
								} // TODO: es nimmt immer die neueste Wert! sollte der ältester in der Gruppe nehmmen um die Daten aktualisieren zu können!
								// das Problem ist dass per Default der Datum 2008 ist => muss das erstes Mal mit newestDate aktualisiert werden
							}
						}

						reqDone();
					},
					function () { reqDone(); });
			});

		return deferred.promise;
	};

	importer.saveConfig = function () {
		var config = { autoimport: importer.autoimport, selected: [] };
		angular.forEach(importer.allMeasureTypes, function (measurement) {
			if (measurement.import) config.selected.push(measurement.id);
		});

		if (importer.autoimport === undefined || importer.autoimport === null)
			importer.autoimport = true;

		return midataServer.setConfig(importer.authToken, config, importer.autoimport);
	};

	// This is triggered from server
	importer.automatic = function (authToken, lang) {

		// set language to be used
		$translate.use(lang);

		return importer.initForm(authToken)
			.then(function () {
				importer.importNow();
			});
	};

	// Trigger the import. Must be runnable from webbrowser or from server
	importer.importNow = function () {

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
				var mesGroupId = mapMeasureTypeToGroup[measureType.id];
				if (importer.allMeasurementGroups[mesGroupId] && importer.allMeasurementGroups[mesGroupId].from) {
					_from = new Date(importer.allMeasurementGroups[mesGroupId].from.getTime());
				} else {
					console.log("Error! allMeasurementGroups not defined for " + mesGroupId + ". It should be defined");
				}
				
				// get prev if newest is null
				var f = function () { return getPrevRecords(measureType.system + " " + measureType.code, _from); };
				_chainPrevRecords = _chainPrevRecords.then(f);
			}
		});

		// get user id
		_chainPrevRecords.then(function(){
			midataServer.getOAuthParams(importer.authToken)
			.then(function (response) {
				importer.userid = response.data.userid;
				importingRecords();
			});
		});
	};

	var importingRecords = function () {
		console.log("start importingRecords");
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

				var fromFormatted_forLog = _fromBase.getFullYear() + "-" + twoDigit(_fromBase.getMonth() + 1) + "-" + twoDigit(_fromBase.getDate());
				console.log("Prepering from " + fromFormatted_forLog + " to " + daysRequestInterval + " days later or today for " + measurementGroup.groupMeasureId);
				var f = function(){
				// 3. request to Nokia Health
					console.log("call url " + measurementGroup.getURL(importer.userid, _fromBase, new Date(temp_to_milliseconds)));
					return midataServer.oauth1Request(importer.authToken, measurementGroup.getURL(importer.userid, _fromBase, new Date(temp_to_milliseconds)))
					.then(function (response) {
						var actionDef2 = $q.defer();
						var actionChain2 = actionDef2.promise;
						actionDef2.resolve();
						if (response.data.status == "0") {
							//save records
							if (measurementGroup.groupMeasureId == 'activity_measures') {
								console.log("Response activity_measures");
								var f_s_a = function(){
									console.log("exec f_s_a save activities");
									return save_activities(response, measurementGroup);
								};
								actionChain2 = actionChain2.then(f_s_a);
							} else if (measurementGroup.groupMeasureId == 'body_measures') {
								console.log("Response body_measures");
								var f_s_bm = function(){
									console.log("exec f_s_bm save_bodyMeasures");
									return save_bodyMeasures(response, measurementGroup);
								};
								actionChain2 = actionChain2.then(f_s_bm);
							} else if (measurementGroup.groupMeasureId == 'sleep_summary') {
								console.log("Response sleep_summary");
								var f_s_sm = function(){
									console.log("exec f_s_sm save_sleepMeasures");
									return save_sleepMeasures(response, measurementGroup);
								};
								actionChain2 = actionChain2.then(f_s_sm);
								//updateRequesting();
							} else {
								console.log("Error! new measurement group not defined");
								//updateRequesting();
							}

						} else {
							console.log("Error: url " + measurementGroup.getURL(_userid, _fromBase, new Date(temp_to_milliseconds)));
							if (response.data.error) {
								console.log("Error Message from Nokia Health (status: " + response.data.status + "): " + response.data.error);
							}
							//updateRequesting();
						}

						return actionChain2;
					}, function (error) {
						console.log('Failed: ' + error);
						//updateRequesting();
					}).then(function(){
						importer.requesting--;
						console.log("requesting: " + importer.requesting);
					});
				};
				actionChain = actionChain.then(f);
				// // 3. request to Nokia Health
				// var f = function(){
				// 	console.log("call url " + measurementGroup.getURL(importer.userid, _fromBase, new Date(temp_to_milliseconds)));
				// 	return midataServer.oauth1Request(importer.authToken, measurementGroup.getURL(importer.userid, _fromBase, new Date(temp_to_milliseconds)))
				// 	.then(function (response) {
				// 		if (response.data.status == "0") {
				// 			//save records
				// 			if (measurementGroup.groupMeasureId == 'activity_measures') {
				// 				console.log("Response activity_measures");
				// 				// // save every measure
				// 				// measurementGroup.measureTypes.forEach(function (measurement) {
				// 				// 	if (measurement.import) {
				// 				// 		save_activities(response, measurement);
				// 				// 	}
				// 				// });
				// 				// //updateRequesting();
				// 				save_activities(response, measurementGroup);

				// 			} else if (measurementGroup.groupMeasureId == 'body_measures') {
				// 				console.log("Response body_measures");
				// 				save_bodyMeasures(response, measurementGroup);
				// 				//updateRequesting();
				// 			} else if (measurementGroup.groupMeasureId == 'sleep_summary') {
				// 				console.log("Response sleep_summary");
				// 				save_sleepMeasures(response, measurementGroup);
				// 				//updateRequesting();
				// 			} else {
				// 				console.log("Error! new measurement group not defined");
				// 				//updateRequesting();
				// 			}

				// 		} else {
				// 			console.log("Error: url " + measurementGroup.getURL(_userid, _fromBase, new Date(temp_to_milliseconds)));
				// 			if (response.data.error) {
				// 				console.log("Error Message from Nokia Health (status: " + response.data.status + "): " + response.data.error);
				// 			}
				// 			//updateRequesting();
				// 		}
				// 	}, function (error) {
				// 		console.log('Failed: ' + error);
				// 		//updateRequesting();
				// 	}).then(function(){
				// 		importer.requesting--;
				// 		console.log("requesting: " + importer.requesting);
				// 	});
				// };

				// actionChain = actionChain.then(f);
			} else {
				importer.requesting--;
				console.log("requesting: " + importer.requesting);
			}
		}, this);

		if (importFinished) {
			importer.finished = true;
		}

		return actionChain.then(function(){
			console.log("call function finish on actionChain");
			finish();
		},function (err) {
			console.log(err);
		});
	};

	var counterFromTest = 0;
	/**
	 * This function save the prev Records in cache
	 * @param {string} code the code used as key to save the record
	 * @param {Date} from The start date to get the previous records
	 */
	var getPrevRecords = function (code, from) {
		var fromFormatted = from.getFullYear() + "-" + twoDigit(from.getMonth() + 1) + "-" + twoDigit(from.getDate());
		console.log("from in getPrevRecords (#" + (counterFromTest++) + "): " + fromFormatted + ". code: " + code);
		return midataServer.getRecords(importer.authToken, { "format": "fhir/Observation", "code": code, "index": { "effectiveDateTime": { "!!!ge": from } } }, ["version", "content", "data"])
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

	var save_activities = function (response, measurementGroup) {
		console.log("save_activities");
		var transactionsDef = $q.defer();
		var transactionsChain = transactionsDef.promise;
		transactionsDef.resolve();
		// save every measure
		measurementGroup.measureTypes.forEach(function (measurement) {
			if (measurement.import) {


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
							var func_trans1 = function(){
								return processTransaction(actions);
							};
							transactionsChain = transactionsChain.then(func_trans1);
							actions = [];
						}
					}
				});
		
				if (actions.length > 0) { 
					var func_trans1 = function(){
						return processTransaction(actions); 
					};
					transactionsChain = transactionsChain.then(func_trans1);
				}



			}
		});

		return transactionsChain;
	};


	var save_bodyMeasures = function (response, measurementGroup) {
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
							processTransaction(actions);
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
					processTransaction(actions);
					actions = [];
				}
			}
		});

		if (actions.length > 0) { processTransaction(actions); }
	};

	var save_sleepMeasures = function (response, measurementGroup) {
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
						processTransaction(actions);
						actions = [];
					}
				}
			}
		});

		if (actions.length > 0) { processTransaction(actions); }
	};

	var processTransaction = function (actions) {
		console.log("process transaction");
		console.log(actions);
		var request = {
			"resourceType": "Bundle",
			"id": "bundle-transaction",
			"type": "transaction",
			"entry": actions
		};

		return midataServer.fhirTransaction(importer.authToken, request)
			.then(function () {
				importer.saved += actions.length;
				//finish();
			},
			function (reason) {
				importer.notSaved += actions.length;
				//importer.error.messages.push(reason);
				//finish();
			});
	};

	//var updateRequesting = function () {
	//	importer.requesting--;
	//	finish();
	//};

	var finish = function () {
		if (importer.finished && importer.requesting <= 0 && importer.saved + importer.notSaved === importer.requested) {
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
