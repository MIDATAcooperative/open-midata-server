var withings = angular.module('withings', ['midata', 'pascalprecht.translate', 'withingsi18n']);
// Configuration
withings.config(['$translateProvider', 'i18nc', function ($translateProvider, i18nc) {

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
withings.factory('importer', ['$http', '$translate', 'midataServer', '$q', function ($http, $translate, midataServer, $q) {

	var importer = {};
	importer.autoimport = {};

	importer.error = {};
	importer.reimport = 7;
	importer.status = null;
	importer.requesting = 0;
	importer.requested = 0;
	importer.saving = false;
	importer.saved = 0;
	importer.totalImport = 0;
	importer.measure = null;
	importer.alldone = null;
	importer.repeat = false;
	importer.allMeasureTypes = [];

	var workouts_categories = [
		{ "id": 4, "name": "Staking" },
		{ "id": 5, "name": "BMX" },
		{ "id": 6, "name": "Bicycling" },
		{ "id": 7, "name": "Swim" },
		{ "id": 8, "name": "Surfing" },
		{ "id": 9, "name": "KiteSurfing" },
		{ "id": 10, "name": "Windsurfing" },
		{ "id": 11, "name": "Bodyboard" },
		{ "id": 12, "name": "Tennis" },
		{ "id": 13, "name": "Table Tennis" },
		{ "id": 14, "name": "Squash" },
		{ "id": 15, "name": "Badminton" },
		{ "id": 16, "name": "Lift Weights" },
		{ "id": 17, "name": "Calisthenics" },
		{ "id": 18, "name": "Elliptical" },
		{ "id": 19, "name": "Pilate" },
		{ "id": 20, "name": "Basketball" },
		{ "id": 21, "name": "Soccer" },
		{ "id": 22, "name": "Football" },
		{ "id": 23, "name": "Rugby" },
		{ "id": 24, "name": "VollyBall" },
		{ "id": 25, "name": "WaterPolo" },
		{ "id": 26, "name": "HorseRiding" },
		{ "id": 27, "name": "Golf" },
		{ "id": 28, "name": "Yoga" },
		{ "id": 29, "name": "Dancing" },
		{ "id": 30, "name": "Boxing" },
		{ "id": 31, "name": "Swim" },
		{ "id": 32, "name": "Fencing" },
		{ "id": 33, "name": "Wrestling" },
		{ "id": 34, "name": "Skiing" },
		{ "id": 35, "name": "SnowBoarding" },
		{ "id": 192, "name": "Handball" }
	];

	importer.measurementGroups = [
		{
			groupMeasureId: "activity_measures",
			actionType: "getactivity",
			measureTypes: [
				{ id: "activity_measures_steps", name: "Activity Measures - Steps", measureType: "steps", unit: "steps", system: "http://midata.coop", code: "activities/steps", factor: 1 },//, system: "http://loinc.org", code: "41950-7" },
				{ id: "activity_measures_distance", name: "Activity Measures - Distance", measureType: "distance", unit: "m", system: "http://midata.coop", code: "activities/distance", factor: 1 },//, system: "http://loinc.org", code: "41953-1" },
				//? //{ id: "activity_measures_calories", name: "Activity Measures - Calories", measureType: "calories", unit: "kcal", system: "http://midata.coop", code: "activities/calories", factor: 1 },//, system: "http://loinc.org", code: "41981-2" },
				{ id: "activity_measures_totalcalories", name: "Activity Measures - Total calories", measureType: "totalcalories", unit: "kcal", system: "http://midata.coop", code: "activities/calories", factor: 1 },//, system : "http://loinc.org", code : "41981-2"},
				{ id: "activity_measures_elevation", name: "Activity Measures - Elevation", measureType: "elevation", unit: "m", system: "http://midata.coop", code: "activities/elevation", factor: 1 },
				{ id: "activity_measures_soft", name: "Activity Measures - Soft Activities", measureType: "soft", unit: "min", system: "http://midata.coop", code: "activities/minutes-lightly-active", factor: 0.0166667 },//, system : "http://loinc.org", code : "55411-3"},
				{ id: "activity_measures_moderate", name: "Activity Measures - Moderate Activities", measureType: "moderate", unit: "min", system: "http://midata.coop", code: "activities/minutes-fairly-active", factor: 0.0166667 },//, system : "http://loinc.org", code : "55411-3"},
				{ id: "activity_measures_intense", name: "Activity Measures - Intense Activities", measureType: "intense", unit: "min", system: "http://midata.coop", code: "activities/minutes-very-active", factor: 0.0166667 }//, system : "http://loinc.org", code : "55411-3"},
			],
			getURL: function (userid) { // max 60 calls per minute  // http://oauth.withings.com/api/doc
				var _url = baseUrl + "/v2/measure?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdateymd=" + this.from.getFullYear() + "-" + twoDigit(this.from.getMonth() + 1) + "-" + twoDigit(this.from.getDate()));
				_url += ("&enddateymd=" + this.to.getFullYear() + "-" + twoDigit(this.to.getMonth() + 1) + "-" + twoDigit(this.to.getDate()));

				return _url;
			}
		},
		{
			groupMeasureId: "body_measures",
			actionType: "getmeas",
			measureTypes: [
				{ id: "body_measures_weigth", name: "Body Measures - Weight", measureType: 1, unit: "kg", system: "http://loinc.org", code: "3141-9" },
				{ id: "body_measures_height", name: "Body Measures - Height", measureType: 4, unit: "m", system: "http://loinc.org", code: "8302-2" },
				{ id: "body_measures_fat_free_mass", name: "Body Measures - Fat Free Mass", measureType: 5, unit: "kg", system: "http://midata.coop", code: "body/fat_free_mass" },
				{ id: "body_measures_fat_radio", name: "Body Measures - Fat Ratio", measureType: 6, unit: "%", system: "http://loinc.org", code: "41982-0" },
				{ id: "body_measures_fat_mass_weight", name: "Body Measures - Fat Mass Weight", measureType: 8, unit: "kg", system: "http://loinc.org", code: "73708-0" },
				{ id: "body_measures_diastolic_blood_pressure", name: "Body Measures - Diastolic Blood Pressure", measureType: 9, unit: "mmHg", system: "http://loinc.org", code: "8462-4" },
				{ id: "body_measures_systolic_blood_pressure", name: "Body Measures - Systolic Blood Pressure", measureType: 10, unit: "mmHg", system: "http://loinc.org", code: "8480-6" },
				{ id: "body_measures_heart_pulse", name: "Body Measures - Heart Pulse", measureType: 11, unit: "bpm", system: "http://loinc.org", code: "8867-4" },
				{ id: "body_measures_sp02", name: "Body Measures - SP02", measureType: 54, unit: "%", system: "http://loinc.org", code: "20564-1" },
				{ id: "body_measures_body_temperature", name: "Body Measures - Body Temperature", measureType: 71, unit: "?", system: "http://loinc.org", code: "8310-5" }, // TODO: unit not defined. Do Tests!
				{ id: "body_measures_muscle_mass", name: "Body Measures - Muscle Mass", measureType: 76, unit: "kg", system: "http://loinc.org", code: "73964-9" },// unit not defined. Do Tests! 
				{ id: "body_measures_hydration", name: "Body Measures - Hydration", measureType: 77, unit: "?", system: "http://midata.coop", code: "body/hydration" },// unit not defined. Do Tests!
				{ id: "body_measures_bone_mass", name: "Body Measures - Bone Mass", measureType: 88, unit: "kg", system: "http://midata.coop", code: "body/bone_mass" },// unit not defined. Do Tests!
				{ id: "body_measures_pulse_wave_velocity", name: "Body Measures - Pulse Wave Velocity", measureType: 91, unit: "?cm/s", system: "http://loinc.org", code: "77196-4" }// unit not defined. Do Tests!
			],
			getURL: function (userid) {
				var _url = baseUrl + "/measure?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdate=" + Math.round(this.from.getTime() / 1000));
				_url += ("&enddate=" + Math.round(this.to.getTime() / 1000));

				return _url;
			}
		},/*
		{
			groupMeasureId: "intraday_activity",
			actionType: "getintradayactivity",
			measureTypes: [
				{ id: "intraday_activity_calories", name: "Intraday Activity - Calories", measureType: "calories", unit: "kcal", system: "http://midata.coop", code: "activities/intraday/calories" },//, system: "http://loinc.org", code: "41981-2" },
				{ id: "intraday_activity_distance", name: "Intraday Activity - Distance", measureType: "distance", unit: "m", system: "http://midata.coop", code: "activities/intraday/distance" },//, system: "http://loinc.org", code: "41953-1" },
				{ id: "intraday_activity_duration", name: "Intraday Activity - Duration", measureType: "duration", unit: "s", system: "http://midata.coop", code: "activities/intraday/duration" },//, system: "http://loinc.org", code: "55411-3" },
				{ id: "intraday_activity_elevation", name: "Intraday Activity - Elevation", measureType: "elevation", unit: "m", system: "http://midata.coop", code: "activities/intraday/elevation" },//, system: "http://midata.coop", code: "activities/elevation" },
				{ id: "intraday_activity_steps", name: "Intraday Activity - Steps", measureType: "steps", unit: "{#}", system: "http://midata.coop", code: "activities/intraday/steps" },//, system: "http://loinc.org", code: "41950-7" },
				{ id: "intraday_activity_stroke", name: "Intraday Activity - Stroke", measureType: "stroke", unit: "{#}", system: "http://midata.coop", code: "activities/intraday/stroke" },
				{ id: "intraday_activity_pool_lap", name: "Intraday Activity - Pool lap", measureType: "pool_lap", unit: "{#}", system: "http://midata.coop", code: "activities/intraday/pool-lap" }
			],
			getURL: function (userid) { // max. 120 calls per minute
				var _url = baseUrl + "/v2/measure?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdate=" + Math.round(this.from.getTime()/1000) );
				_url += ("&enddate=" + Math.round(this.to.getTime()/1000) );

				return _url;
			}
		},*/
		/*{ //ABFRAGE KAMM LEER
			groupMeasureId: "sleep_measures",
			actionType: "get",
			measureTypes: [
				{ id: "sleep_measures_awake", name: "Sleep Measures - Awake", measureType: 0, unit: "min", system: "http://midata.coop", code: "sleep/minutes-awake" }//,
				//duplicated in summary//{ id: "sleep_measures_light_sleep", name: "Sleep Summary - Light sleep", measureType: 1, unit: "min", system: "http://midata.coop", code: "sleep/minutes-light-sleep" },
				//duplicated in summary//{ id: "sleep_measures_deep_sleep", name: "Sleep Summary - Deep Sleep", measureType: 2, unit: "min", system: "http://midata.coop", code: "sleep/minutes-asleep" },
				//duplicated in summary//{ id: "sleep_measures_rem_sleep", name: "Sleep Summary - REM Sleep", measureType: 3, unit: "min", system: "http://loinc.org", code: "sleep/rem" }
			],
			getURL: function (userid) { //  A single call can span up to 7 days maximum
				var _url = baseUrl + "/v2/sleep?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdate=" + Math.round(this.from.getTime()/1000) );
				_url += ("&enddate=" + Math.round(this.to.getTime()/1000) );

				return _url;
			}
		},*/
		{
			groupMeasureId: "sleep_summary",
			actionType: "getsummary",
			measureTypes: [
				{ id: "sleep_summary_wakeupduration", name: "Sleep Summary - Wake up duration", measureType: "wakeupduration", unit: "min", system: "http://loinc.org", code: "65554-8", factor: 0.0166667 },
				{ id: "sleep_summary_lightsleepduration", name: "Sleep Summary - Light sleep duration", measureType: "lightsleepduration", unit: "min", system: "http://midata.coop", code: "sleep/minutes-light-sleep", factor: 0.0166667 },
				{ id: "sleep_summary_deepsleepduration", name: "Sleep Summary - Deep sleep duration", measureType: "deepsleepduration", unit: "min", system: "http://midata.coop", code: "sleep/minutes-asleep", factor: 0.0166667 },
				{ id: "sleep_summary_remsleepduration", name: "Sleep Summary - REM sleep duration", measureType: "remsleepduration", unit: "min", system: "http://midata.coop", code: "sleep/rem", factor: 0.0166667 },
				{ id: "sleep_summary_wakeupcount", name: "Sleep Summary - Wake up count", measureType: "wakeupcount", unit: "times", system: "http://midata.coop", code: "sleep/wakeupcount", factor: 1 },
				{ id: "sleep_summary_durationtosleep", name: "Sleep Summary - Duration to sleep", measureType: "durationtosleep", unit: "min", system: "http://midata.coop", code: "sleep/minutes-to-fall-asleep", factor: 0.0166667 },
				{ id: "sleep_summary_durationtowakeup", name: "Sleep Summary - Duration to wake up", measureType: "durationtowakeup", unit: "min", system: "http://loinc.org", code: "65554-8", factor: 0.0166667 } // ATTENTION: Loinc-Unit "hh:mm". 
			],
			getURL: function (userid) { // A single call can span up to 200 days maximum.
				var _url = baseUrl + "/v2/sleep?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdateymd=" + this.from.getFullYear() + "-" + twoDigit(this.from.getMonth() + 1) + "-" + twoDigit(this.from.getDate()));
				_url += ("&enddateymd=" + this.to.getFullYear() + "-" + twoDigit(this.to.getMonth() + 1) + "-" + twoDigit(this.to.getDate()));

				return _url;
			}
		}/*,
		{
			groupMeasureId: "workouts",
			actionType: "getworkouts",
			measureTypes: [
				{ id: "workouts_calories", name: "Workouts - Calories", measureType: "calories", unit: "kcal", system: "http://loinc.org", code: "41981-2" },
				//not found //{ id: "workouts_strokes", name: "Workouts - Strokes", measureType: "strokes", unit: "{#}", system: "http://loinc.org", code: "" },
				//not found //{ id: "workouts_pool_length", name: "Workouts - Pool length", measureType: "pool_length", unit: "meters", system: "http://loinc.org", code: "" },
				//not found //{ id: "workouts_pool_laps", name: "Workouts - Pool laps", measureType: "pool_laps", unit: "{#}", system: "http://loinc.org", code: "" },
				//not found //{ id: "workouts_effduration", name: "Workouts - Effective duration", measureType: "effduration", unit: "seconds", system: "http://loinc.org", code: "" }
			],
			getURL: function (userid) {
				var _url = baseUrl + "/v2/measure?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdateymd=" + this.from.getFullYear() + "-" + twoDigit(this.from.getMonth() + 1) + "-" + twoDigit(this.from.getDate()) );
				_url += ("&enddateymd=" + this.to.getFullYear() + "-" + twoDigit(this.to.getMonth() + 1) + "-" + twoDigit(this.to.getDate()) );

				return _url;
			},
			categories: workouts_categories
		}*/
	];

	var codeToMidataCode = {};
	codeToMidataCode["http://midata.coop activities/steps"] = "activities/steps";
	codeToMidataCode["http://midata.coop activities/distance"] = "activities/distance";
	codeToMidataCode["http://midata.coop activities/calories"] = "activities/calories";
	codeToMidataCode["http://midata.coop activities/elevation"] = "activities/elevation";
	codeToMidataCode["http://midata.coop activities/minutes-lightly-active"] = "activities/minutes-lightly-active";
	codeToMidataCode["http://midata.coop activities/minutes-fairly-active"] = "activities/minutes-fairly-active";
	codeToMidataCode["http://midata.coop activities/minutes-very-active"] = "activities/minutes-very-active";
	codeToMidataCode["http://loinc.org 3141-9"] = "body/weight";
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
	//codeToMidataCode[""] = "";

	var baseUrl = "https://wbsapi.withings.net";
	var stored = {};

	// range: lastweek | lastmonth | lastyear | specified
	// date_from: type -> Date
	// date_to: type -> Date
	var setDateInMeasurements = function (range, date_from, date_to) {
		var _from = new Date();
		var _to = new Date();

		if (range !== undefined && range !== null) {

			if (range == "lastweek") {
				_from.setDate(_from.getDate() - 8);
				_from.setHours(1, 1, 1, 1);

				_to.setDate(_to.getDate() - 1);
				_to.setHours(1, 1, 1, 1);
			} else if (range == "lastmonth") {
				_from.setMonth(_from.getMonth() - 1);
				_from.setHours(1, 1, 1, 1);

				_to.setDate(_to.getDate() - 1);
				_to.setHours(1, 1, 1, 1);
			} else if (range == "lastyear") {
				_from.setFullYear(_from.getFullYear() - 1);
				_from.setHours(1, 1, 1, 1);

				_to.setDate(_to.getDate() - 1);
				_to.setHours(1, 1, 1, 1);
			} else if (range == "specified") {
				_from = date_from;
				_to = date_to;
			}
		}

		angular.forEach(importer.measurementGroups, function (measurement) {
			measurement.from = _from;
			measurement.to = _to;
		});
	};

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
				for (var i = 0; i < importer.measurementGroups.length; i++) {
					var measurementGroup = importer.measurementGroups[i];
					for (var j = 0; j < measurementGroup.measureTypes.length; j++) {
						// configurations to import
						measurementGroup.measureTypes[j].import = false;

						if (response.data && response.data.selected) {
							if (response.data.selected.indexOf(measurementGroup.measureTypes[j].id) >= 0) {
								measurementGroup.measureTypes[j].import = true;
							}
						}

						importer.allMeasureTypes.push(measurementGroup.measureTypes[j]);
					}
				}

				if (response.data && response.data.autoimport != null) {
					importer.autoimport = response.data.autoimport;
				} else {
					importer.autoimport = false;
				}

				reqDone();
			}, function () { reqDone(); })
			.then(function (response) {

				// get summary
				midataServer.getSummary(authToken, "content", { "format": "fhir/Observation", "subformat": "Quantity", "app": "withings" })
					.then(function (response) {
						var map = {};

						angular.forEach(importer.allMeasureTypes, function(measureType) {
							var midataCode = codeToMidataCode[measureType.system + " " + measureType.code];
							map[midataCode] = measureType;
						});

						for (var k = 0; k < response.data.length; k++) {
							var entry = response.data[k];
							var measurement = map[entry.contents[0]];

							if (measurement != null) {
								var newestDate = new Date(entry.newest);

								//newestDate.setHours(1, 1, 1, 1);
								//newestDate.setDate(newestDate.getDate() - $scope.reimport);
								measurement.imported = entry.count;
								if (measurement.from == null || measurement.from < newestDate) measurement.from = newestDate;
							}
						}

						reqDone();
					},
					function () { reqDone(); });
			});

		return deferred.promise;
	};

	importer.saveConfig = function (authToken){
			var config = { autoimport : importer.autoimport, selected:[] };
			angular.forEach(importer.allMeasureTypes, function(measurement) {
				if (measurement.import) config.selected.push(measurement.id);				
			});

			if(importer.autoimport === undefined || importer.autoimport === null)
				importer.autoimport = true;

			midataServer.setConfig(authToken, config, importer.autoimport);
	};

	// This is triggered from server
	importer.automatic = function (authToken, lang) {

		// set language to be used
		$translate.use(lang);

		return importer.initForm(authToken)
		.then(function(){
			importer.importNow(authToken);
		});
	};

	// Trigger the import. Must be runnable from webbrowser or from server
	importer.importNow = function (authToken) {

		console.log("import now started");

		// 1. Set Interval to import
		// "from" and "to" saved in measurements 
		if (importer.firstTime) {
			var from1970 = new Date();
			var toToday = new Date();
			from1970.setFullYear(1970);
			from1970.setHours(1, 1, 1, 1);
			toToday.setHours(1, 1, 1, 1);
			setDateInMeasurements("specified", from1970, toToday);
		} else {
			//setDateInMeasurements("lastweek");
			setDateInMeasurements("lastyear");
		}

		// 2. get user id
		midataServer.getOAuthParams(authToken)
			.then(function (response) {
				var _userid = response.data.userid;

				// Import all defined measurements groups
				importer.measurementGroups.forEach(function (measurementGroup) {

					// 3. request to withings 
					midataServer.oauth1Request(authToken, measurementGroup.getURL(_userid))
						.then(function (response) {console.log('ln 414');
							if (response.data.status == "0") {
console.log('ln 416');
								// get all prev records
								var _defPrevRecords = $q.defer();
								var _arrPrevRecords = [];
								measurementGroup.measureTypes.forEach(function (measurementType) {console.log('ln 420');
									_arrPrevRecords.push(
										getPrevRecords(authToken, measurementType.system + " " + measurementType.code, measurementGroup.from)
									);
console.log('ln 424');
									$translate(/*"titles." + */measurementType.id).then(function (t) { measurementType.title = t; });
									$translate(measurementType.id).then(function (t) { measurementType.name_translated = t; });
								});
console.log('ln 428');
								$q.all(_arrPrevRecords)
									.then(
									function (results) {
										_defPrevRecords.resolve(JSON.stringify(results));
									},
									function (errors) {
										_defPrevRecords.reject(errors);
									},
									function (updates) {
										_defPrevRecords.update(updates);
									});

								// all prev. Records loaded
								_defPrevRecords.promise.then(function () {
console.log('ln 443');
console.log(measurementGroup.groupMeasureId);
console.log('measurementtypes');
console.log(measurementGroup.measureTypes);
									//save records
									if (measurementGroup.groupMeasureId == 'activity_measures') {
										// save every measure
										measurementGroup.measureTypes.forEach(function (measurement) {
											if (measurement.import) {
												save_activities(authToken, response, measurement);	
											}
										});

									} else if (measurementGroup.groupMeasureId == 'body_measures') {
										save_bodyMeasures(authToken, response, measurementGroup);
									} else if (measurementGroup.groupMeasureId == 'sleep_summary') {
										save_sleepMeasures(authToken, response, measurementGroup);
									}
								});

							} else {
								console.log("Error: url " + measurementGroup.getURL(_userid));
							}
						});
				}, this);
			});
	};

	var getPrevRecords = function (authToken, code, from) {console.log('ln 471');
		return midataServer.getRecords(authToken, { "format": "fhir/Observation", "code": code, "index": { "effectiveDateTime": { "!!!ge": from } } }, ["version", "content", "data"])
			.then(function (results) {console.log('ln 473');
				angular.forEach(results.data, function (rec) {
					stored[rec.content + rec.data.effectiveDateTime] = rec;
				});
			}, function(reason) {
				console.log('Failed: ' + reason);
			});
	};


	// save a single record to the database
	var saveOrUpdateRecord = function (authToken, midataHeader, record) {//(title, content, formattedDate, record) {
		/*var _promise = {};
		var existing = stored[codeToMidataCode[midataHeader.code] + record.effectiveDateTime];
		if (existing) {
			if (existing.data.valueQuantity.value != record.valueQuantity.value) {
				_promise = updateRecord(authToken, existing._id, existing.version, record);
			} else {
				_promise = new Promise(function (resolve, reject) {
					importer.saved += 1;
				});
			}
		} else {
			_promise = saveRecord(authToken, midataHeader, record);
		}

		return _promise;*/

		var existing = stored[codeToMidataCode[midataHeader.code] + record.effectiveDateTime];
		if (existing) {
			if (existing.data.valueQuantity.value != record.valueQuantity.value) {
				return updateRecord(authToken, existing._id, existing.version, record);
			}
		} else {
			return saveRecord(authToken, midataHeader, record);
		}

		return null;
	};

	// save a single record to the database
	var saveRecord = function (authToken, midataHeader, record) {			
		/*return midataServer.createRecord(authToken, midataHeader, record)
			.then(function () {
				importer.log += ("\nRecord imported. Name: " + midataHeader.name + "; Code: " + midataHeader.code);
				console.log("Record imported. Name: " + midataHeader.name + "; Code: " + midataHeader.code);
				importer.saved += 1;
			})
			.catch(function (err) {
				importer.log += ("\nError! Message: " + err + "\nRecord not imported. Name: " + midataHeader.name + "; Code: " + midataHeader.code);
				console.log("Error! Message: " + err + "\nRecord not imported. Name: " + midataHeader.name + "; Code: " + midataHeader.code);
			});*/

			return {
				"resource" : record,
				"request" : {
					"method" : "POST",
					"url" : "Observation"
				}
			};
	};

	var updateRecord = function (authToken, id, version, record) {
		/*return midataServer.updateRecord(authToken, id, version, record)
			.then(function () {
				importer.saved += 1;
			})
			.catch(function (err) {
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

	var getMIDATAHeader = function (name, code) {
		var _midataHeader = {};
		_midataHeader.name = name;
		_midataHeader.format = "fhir/Observation";
		_midataHeader.subformat = "Quantity";
		_midataHeader.code = code;

		return _midataHeader;
	};

	// make a two digit string out of a given number
	var twoDigit = function (num) {
		return ("0" + num).slice(-2);
	};

	var save_activities = function (authToken, response, measurement) {
		var actions = [];
console.log('save activities');
console.log(response.data.body);
		// save every measure of every activity
		response.data.body.activities.forEach(function (activity) {

			var measure = activity[measurement.measureType];
			if (measure !== undefined && measure !== null && measure != "0") {

				var recordContent = {
					resourceType: "Observation",
					status: "preliminary",
					category: {
						coding: [{ system: "http://hl7.org/fhir/observation-category", code: "vital-signs", display: "Vital Signs" }]
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

				var action = saveOrUpdateRecord(authToken, getMIDATAHeader(measurement.name_translated, measurement.system + " " + measurement.code), recordContent);

				if(action !== null) actions.push(action);

				// Limit request size
				if (actions.length >= 200) {
					processTransaction(authToken,actions);
					actions = [];
				}
			}
		});

		if(actions.length > 0) { processTransaction(authToken, actions);}
	};


	var save_bodyMeasures = function (authToken, response, measurementGroup) {
		var actions = [];

		// save every measure
		response.data.body.measuregrps.forEach(function (mgroup) {
			var date = new Date(mgroup.date * 1000);

			mgroup.measures.forEach(function (measure) {
				for (var index = 0; index < measurementGroup.measureTypes.length; index++) {
					var measurementType = measurementGroup.measureTypes[index];

					if (!measurementType.import) { continue; }

					if (measurementType.measureType == measure.type) {

						var recordContent = {
							resourceType: "Observation",
							status: "preliminary",
							category: {
								coding: [{ system: "http://hl7.org/fhir/observation-category", code: "vital-signs", display: "Vital Signs" }]
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

						var action = saveOrUpdateRecord(authToken, getMIDATAHeader(measurementType.name_translated, measurementType.system + " " + measurementType.code), recordContent);

						if(action !== null) actions.push(action);

						// Limit request size
						if (actions.length >= 200) {
							processTransaction(authToken,actions);
							actions = [];
						}

						break;
					}
				}
			});
		});

		if(actions.length > 0) { processTransaction(authToken, actions);}
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
							coding: [{ system: "http://hl7.org/fhir/observation-category", code: "vital-signs", display: "Vital Signs" }]
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

					var action = saveOrUpdateRecord(authToken, getMIDATAHeader(measurementType.name_translated, measurementType.system + " " + measurementType.code), recordContent);

					if(action !== null) actions.push(action);

					// Limit request size
					if (actions.length >= 200) {
						processTransaction(authToken,actions);
						actions = [];
					}
				}
			}
		});

		if(actions.length > 0) { processTransaction(authToken, actions);}
	};

	var processTransaction = function(authToken, actions){
		console.log('esta entrando');
		var request = {
			"resourceType": "Bundle",
			"id": "bundle-transaction",
			"type": "transaction",
			"entry": actions
		};
		//$scope.requested++;
		midataServer.fhirTransaction(authToken, request)
		.then(function() {
			console.log("bundle saved");
			console.log(actions);
		});
		console.log('llega a guardarlo?');
	};

	return importer;
}]);

