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
	
	// the URL for the calls
	var baseUrl = "https://api.health.nokia.com";

	// there are the categories for the workouts (defined in Nokia Health)
	var workouts_categories = [
		{ "id": 1, "name": "Walk" },
		{ "id": 2, "name": "Run" },
		{ "id": 3, "name": "Hiking" },
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
		{ "id": 186, "name": "Base" },
		{ "id": 187, "name": "Rowing" },
		{ "id": 188, "name": "Zumba" },
		{ "id": 191, "name": "Baseball" },
		{ "id": 192, "name": "Handball" },
		{ "id": 193, "name": "Hockey" },
		{ "id": 194, "name": "Ice Hockey" },
		{ "id": 195, "name": "Climbing" },
		{ "id": 196, "name": "Ice Skating" }
	];
	importer.measurementGroups = [
		{
			groupMeasureId: "activity_measures",
			actionType: "getactivity",
			measureTypes: [
				{
					id: "activity_measures_steps",
					name: "Activity Measures - Steps",
					measureType: "steps",
					unit: "steps",
					system: "http://loinc.org",
					code: "41950-7",
					factor: 1,
					unitSystem: "http://unitsofmeasure.org",
					unitCode: "/d"
				},
				{
					id: "activity_measures_distance",
					name: "Activity Measures - Distance",
					measureType: "distance",
					unit: "m",
					system: "http://loinc.org",
					code: "41953-1",
					factor: 1,
					unitSystem: "http://unitsofmeasure.org",
					unitCode: "m"
				},
				////? //{ id: "activity_measures_calories", name: "Activity Measures - Calories", measureType: "calories", unit: "kcal", system: "http://midata.coop", code: "activities/calories", factor: 1, unitSystem: "http://unitsofmeasure.org", unitCode: "" },//, system: "http://loinc.org", code: "41981-2" },
				//{ id: "activity_measures_totalcalories", name: "Activity Measures - Total calories", measureType: "totalcalories", unit: "kcal", system: "http://midata.coop", code: "activities/calories", factor: 1, unitSystem: "http://unitsofmeasure.org", unitCode: "" },//, system : "http://loinc.org", code : "41981-2"},
				{
					id: "activity_measures_elevation",
					name: "Activity Measures - Elevation",
					measureType: "elevation",
					unit: "m",
					system: "http://midata.coop",
					code: "activities/elevation",
					factor: 1,
					unitSystem: "http://unitsofmeasure.org",
					unitCode: "m"
				},
				{
					id: "activity_measures_soft",
					name: "Activity Measures - Soft Activities", 
					measureType: "soft", 
					unit: "min", 
					system: "http://midata.coop", 
					code: "activities/minutes-lightly-active", 
					factor: 0.0166667, 
					unitSystem: "http://unitsofmeasure.org", 
					unitCode: "min"
				},//, system : "http://loinc.org", code : "55411-3"},
				{ 
					id: "activity_measures_moderate", 
					name: "Activity Measures - Moderate Activities", 
					measureType: "moderate", 
					unit: "min", 
					system: "http://midata.coop", 
					code: "activities/minutes-fairly-active", 
					factor: 0.0166667, 
					unitSystem: "http://unitsofmeasure.org", 
					unitCode: "min" 
				},//, system : "http://loinc.org", code : "55411-3"},
				{ 
					id: "activity_measures_intense", 
					name: "Activity Measures - Intense Activities", 
					measureType: "intense", 
					unit: "min", 
					system: "http://midata.coop", 
					code: "activities/minutes-very-active", 
					factor: 0.0166667, 
					unitSystem: "http://unitsofmeasure.org", 
					unitCode: "min" 
				}//, system : "http://loinc.org", code : "55411-3"},
			],
			getURL: function (userid, from, to) { // max 60 calls per minute  // https://developer.health.nokia.com/api/doc#api-Measure-get_activity
				var _url = baseUrl + "/v2/measure?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdateymd=" + from.getFullYear() + "-" + twoDigit(from.getMonth() + 1) + "-" + twoDigit(from.getDate()));
				_url += ("&enddateymd=" + to.getFullYear() + "-" + twoDigit(to.getMonth() + 1) + "-" + twoDigit(to.getDate()));
				/*
				oauth_consumer_key	String	Consumer key, provided by Nokia when registering as a partner.
				oauth_nonce	String	Random string (should be different for every request).
				oauth_signature	String	OAuth signature. Computed using hmac-sha1 on the oAuth base string, using consumer secret + '&' + oauth token secret (if available) as a secret. The result is then base64 & url-encoded. See home for detailed explanations.
				oauth_signature_method	String	OAuth signature method. Should always be equal to HMAC-SHA1
				oauth_timestamp	Number	Current date as unix epoch
				oauth_token	String	oAuth token (either request token for asking user authorization, or access token if querying data)
				oauth_version	Number	oAuth version. Should always be equal to 1.0
				*/
				return _url;
			}
		},
		{
			groupMeasureId: "body_measures",
			actionType: "getmeas",
			measureTypes: [
				{ 
					id: "body_measures_weigth", 
					name: "Body Measures - Weight", 
					measureType: 1, 
					unit: "kg", 
					system: "http://loinc.org", 
					code: "29463-7", 
					unitSystem: "http://unitsofmeasure.org", 
					unitCode: "kg" 
				},
				{ 
					id: "body_measures_height", 
					name: "Body Measures - Height", 
					measureType: 4, 
					unit: "m", 
					system: "http://loinc.org", 
					code: "8302-2", 
					unitSystem: "http://unitsofmeasure.org", 
					unitCode: "m" 
				},
				//{ id: "body_measures_fat_free_mass", name: "Body Measures - Fat Free Mass", measureType: 5, unit: "kg", system: "http://midata.coop", code: "body/fat_free_mass", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//{ id: "body_measures_fat_radio", name: "Body Measures - Fat Ratio", measureType: 6, unit: "%", system: "http://loinc.org", code: "41982-0", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//{ id: "body_measures_fat_mass_weight", name: "Body Measures - Fat Mass Weight", measureType: 8, unit: "kg", system: "http://loinc.org", code: "73708-0", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				{
					id: "body_measures_blood_pressure", 
					name: "Body Measures - Blood Pressure", 
					unit: "mm Hg", 
					unitSystem: "http://unitsofmeasure.org", 
					unitCode: "mm[Hg]", 
					system: "http://loinc.org", 
					code: "55417-0",
					diastolic: 
					{ 
						id: "body_measures_diastolic_blood_pressure", 
						measureType: 9, 
						system: "http://loinc.org", 
						code: "8462-4" 
					},
					systolic: 
					{ 
						id: "body_measures_systolic_blood_pressure", 
						measureType: 10, 
						system: "http://loinc.org", 
						code: "8480-6" 
					}
				},
				{ 
					id: "body_measures_heart_pulse", 
					name: "Body Measures - Heart Pulse", 
					measureType: 11, 
					unit: "bpm", 
					system: "http://loinc.org", 
					code: "8867-4", 
					unitSystem: "http://unitsofmeasure.org", 
					unitCode: "/min" 
				},
				//{ id: "body_measures_sp02", name: "Body Measures - SP02", measureType: 54, unit: "%", system: "http://loinc.org", code: "20564-1", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				{ 
					id: "body_measures_body_temperature", 
					name: "Body Measures - Body Temperature",
					measureType: 71, 
					unit: "C°", 
					system: "http://loinc.org", 
					code: "8310-5", 
					unitSystem: "http://unitsofmeasure.org", 
					unitCode: "Cel" 
				}
				//,
				//{ id: "body_measures_muscle_mass", name: "Body Measures - Muscle Mass", measureType: 76, unit: "kg", system: "http://loinc.org", code: "73964-9", unitSystem: "http://unitsofmeasure.org", unitCode: "" },// unit not defined. Do Tests!
				//{ id: "body_measures_hydration", name: "Body Measures - Hydration", measureType: 77, unit: "?", system: "http://midata.coop", code: "body/hydration", unitSystem: "http://unitsofmeasure.org", unitCode: "" },// unit not defined. Do Tests!
				//{ id: "body_measures_bone_mass", name: "Body Measures - Bone Mass", measureType: 88, unit: "kg", system: "http://midata.coop", code: "body/bone_mass", unitSystem: "http://unitsofmeasure.org", unitCode: "" },// unit not defined. Do Tests!
				//{ id: "body_measures_pulse_wave_velocity", name: "Body Measures - Pulse Wave Velocity", measureType: 91, unit: "?cm/s", system: "http://loinc.org", code: "77196-4", unitSystem: "http://unitsofmeasure.org", unitCode: "" }// unit not defined. Do Tests!
			],
			getURL: function (userid, from, to) {
				var _url = baseUrl + "/measure?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdate=" + Math.round(from.getTime() / 1000));
				_url += ("&enddate=" + Math.round(to.getTime() / 1000));
				/*
				oauth_consumer_key	String	Consumer key, provided by Nokia when registering as a partner.
				oauth_nonce	String	Random string (should be different for every request).
				oauth_signature	String	OAuth signature. Computed using hmac-sha1 on the oAuth base string, using consumer secret + '&' + oauth token secret (if available) as a secret. The result is then base64 & url-encoded. See home for detailed explanations.
				oauth_signature_method	String	OAuth signature method. Should always be equal to HMAC-SHA1
				oauth_timestamp	Number	Current date as unix epoch
				oauth_token	String	oAuth token (either request token for asking user authorization, or access token if querying data)
				oauth_version	Number	oAuth version. Should always be equal to 1.0
				*/
				return _url;
			}
		},/*
		{
			groupMeasureId: "intraday_activity",
			actionType: "getintradayactivity",
			measureTypes: [
				{ id: "intraday_activity_calories", name: "Intraday Activity - Calories", measureType: "calories", unit: "kcal", system: "http://midata.coop", code: "activities/intraday/calories", unitSystem: "http://unitsofmeasure.org", unitCode: "" },//, system: "http://loinc.org", code: "41981-2" },
				{ id: "intraday_activity_distance", name: "Intraday Activity - Distance", measureType: "distance", unit: "m", system: "http://midata.coop", code: "activities/intraday/distance", unitSystem: "http://unitsofmeasure.org", unitCode: "" },//, system: "http://loinc.org", code: "41953-1" },
				{ id: "intraday_activity_duration", name: "Intraday Activity - Duration", measureType: "duration", unit: "s", system: "http://midata.coop", code: "activities/intraday/duration", unitSystem: "http://unitsofmeasure.org", unitCode: "" },//, system: "http://loinc.org", code: "55411-3" },
				{ id: "intraday_activity_elevation", name: "Intraday Activity - Elevation", measureType: "elevation", unit: "m", system: "http://midata.coop", code: "activities/intraday/elevation", unitSystem: "http://unitsofmeasure.org", unitCode: "" },//, system: "http://midata.coop", code: "activities/elevation" },
				{ id: "intraday_activity_steps", name: "Intraday Activity - Steps", measureType: "steps", unit: "{#}", system: "http://midata.coop", code: "activities/intraday/steps" },//, system: "http://loinc.org", code: "41950-7", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				{ id: "intraday_activity_stroke", name: "Intraday Activity - Stroke", measureType: "stroke", unit: "{#}", system: "http://midata.coop", code: "activities/intraday/stroke", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				{ id: "intraday_activity_pool_lap", name: "Intraday Activity - Pool lap", measureType: "pool_lap", unit: "{#}", system: "http://midata.coop", code: "activities/intraday/pool-lap", unitSystem: "http://unitsofmeasure.org", unitCode: "" }
			],
			getURL: function (userid, from, to) { // max. 120 calls per minute
				var _url = baseUrl + "/v2/measure?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdate=" + Math.round(from.getTime()/1000) );
				_url += ("&enddate=" + Math.round(to.getTime()/1000) );
	
				return _url;
			}
		},*/
		/*{ //ABFRAGE KAMM LEER
			groupMeasureId: "sleep_measures",
			actionType: "get",
			measureTypes: [
				{ id: "sleep_measures_awake", name: "Sleep Measures - Awake", measureType: 0, unit: "min", system: "http://midata.coop", code: "sleep/minutes-awake", unitSystem: "http://unitsofmeasure.org", unitCode: "" }//,
				//duplicated in summary//{ id: "sleep_measures_light_sleep", name: "Sleep Summary - Light sleep", measureType: 1, unit: "min", system: "http://midata.coop", code: "sleep/minutes-light-sleep", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//duplicated in summary//{ id: "sleep_measures_deep_sleep", name: "Sleep Summary - Deep Sleep", measureType: 2, unit: "min", system: "http://midata.coop", code: "sleep/minutes-asleep", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//duplicated in summary//{ id: "sleep_measures_rem_sleep", name: "Sleep Summary - REM Sleep", measureType: 3, unit: "min", system: "http://loinc.org", code: "sleep/rem", unitSystem: "http://unitsofmeasure.org", unitCode: "" }
			],
			getURL: function (userid, from, to) { //  A single call can span up to 7 days maximum
				var _url = baseUrl + "/v2/sleep?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdate=" + Math.round(from.getTime()/1000) );
				_url += ("&enddate=" + Math.round(to.getTime()/1000) );
	
				return _url;
			}
		},*//*
		{
			groupMeasureId: "sleep_summary",
			actionType: "getsummary",
			measureTypes: [
				//{ id: "sleep_summary_wakeupduration", name: "Sleep Summary - Wake up duration", measureType: "wakeupduration", unit: "min", system: "http://loinc.org", code: "65554-8", factor: 0.0166667, unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//{ id: "sleep_summary_lightsleepduration", name: "Sleep Summary - Light sleep duration", measureType: "lightsleepduration", unit: "min", system: "http://midata.coop", code: "sleep/minutes-light-sleep", factor: 0.0166667, unitSystem: "http://unitsofmeasure.org", unitCode: ""},
				//{ id: "sleep_summary_deepsleepduration", name: "Sleep Summary - Deep sleep duration", measureType: "deepsleepduration", unit: "min", system: "http://midata.coop", code: "sleep/minutes-asleep", factor: 0.0166667 , unitSystem: "http://unitsofmeasure.org", unitCode: ""},
				//{ id: "sleep_summary_remsleepduration", name: "Sleep Summary - REM sleep duration", measureType: "remsleepduration", unit: "min", system: "http://midata.coop", code: "sleep/rem", factor: 0.0166667, unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//{ id: "sleep_summary_wakeupcount", name: "Sleep Summary - Wake up count", measureType: "wakeupcount", unit: "times", system: "http://midata.coop", code: "sleep/wakeupcount", factor: 1, unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//{ id: "sleep_summary_durationtosleep", name: "Sleep Summary - Duration to sleep", measureType: "durationtosleep", unit: "min", system: "http://midata.coop", code: "sleep/minutes-to-fall-asleep", factor: 0.0166667, unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//{ id: "sleep_summary_durationtowakeup", name: "Sleep Summary - Duration to wake up", measureType: "durationtowakeup", unit: "min", system: "http://loinc.org", code: "65554-8", factor: 0.0166667, unitSystem: "http://unitsofmeasure.org", unitCode: "" } // ATTENTION: Loinc-Unit "hh:mm".
			],
			getURL: function (userid, from, to) { // A single call can span up to 200 days maximum.
				var _url = baseUrl + "/v2/sleep?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdateymd=" + from.getFullYear() + "-" + twoDigit(from.getMonth() + 1) + "-" + twoDigit(from.getDate()));
				_url += ("&enddateymd=" + to.getFullYear() + "-" + twoDigit(to.getMonth() + 1) + "-" + twoDigit(to.getDate()));
	
				return _url;
			}
		}*//*,
		{
			groupMeasureId: "workouts",
			actionType: "getworkouts",
			measureTypes: [
				{ id: "workouts_calories", name: "Workouts - Calories", measureType: "calories", unit: "kcal", system: "http://loinc.org", code: "41981-2", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//not found //{ id: "workouts_strokes", name: "Workouts - Strokes", measureType: "strokes", unit: "{#}", system: "http://loinc.org", code: "", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//not found //{ id: "workouts_pool_length", name: "Workouts - Pool length", measureType: "pool_length", unit: "meters", system: "http://loinc.org", code: "", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//not found //{ id: "workouts_pool_laps", name: "Workouts - Pool laps", measureType: "pool_laps", unit: "{#}", system: "http://loinc.org", code: "", unitSystem: "http://unitsofmeasure.org", unitCode: "" },
				//not found //{ id: "workouts_effduration", name: "Workouts - Effective duration", measureType: "effduration", unit: "seconds", system: "http://loinc.org", code: "", unitSystem: "http://unitsofmeasure.org", unitCode: "" }
			],
			getURL: function (userid, from, to) {
				var _url = baseUrl + "/v2/measure?";
				_url += ("action=" + this.actionType);
				_url += ("&userid=" + userid);
				_url += ("&startdateymd=" + from.getFullYear() + "-" + twoDigit(from.getMonth() + 1) + "-" + twoDigit(from.getDate()) );
				_url += ("&enddateymd=" + to.getFullYear() + "-" + twoDigit(to.getMonth() + 1) + "-" + twoDigit(to.getDate()) );
	
				return _url;
			},
			categories: workouts_categories
		}*/
	];

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
									// initialize allMeasurementGroups with null
									importer.allMeasurementGroups[measurementGroup.groupMeasureId] = {from : null};
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
								measurement.hasSummary = true;

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
									importer.allMeasurementGroups[mapMeasureTypeToGroup[measurement.id]].from.getTime() - (1000 * 60 * 60 * 24 * daysRequest) > newestDate.getTime()) {
									importer.allMeasurementGroups[mapMeasureTypeToGroup[measurement.id]].from = newestDate;
								}
							}
						}

						reqDone();
					},
					function () { reqDone(); });
			});

		return deferred.promise
		.then(function(){
			var setDefaultDate = false;
			// set default initial date to import
			for(var key in importer.allMeasurementGroups){
				if (!importer.allMeasurementGroups[key].from) {
					setDefaultDate = true;
				} else { // proof if all types were checked
					for (var i = 0; i < importer.allMeasureTypes.length; i++) {
						var mt = importer.allMeasureTypes[i];
						if (!mt.hasSummary && mt.import && mapMeasureTypeToGroup[mt.id] == key) {
							setDefaultDate = true;
							break;
						}
					}
				}

				if (setDefaultDate) {
					var _from =new Date();
					_from.setFullYear(2008);
					_from.setHours(1, 1, 1, 1);
					_from.setMonth(1);
					importer.allMeasurementGroups[key].from = _from;
				}
			}
		});
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
							} else {
								console.log("Error! new measurement group not defined");
							}

						} else {
							console.log("Error: url " + measurementGroup.getURL(_userid, _fromBase, new Date(temp_to_milliseconds)));
							if (response.data.error) {
								console.log("Error Message from Nokia Health (status: " + response.data.status + "): " + response.data.error);
							}
						}

						return actionChain2;
					}, function (error) {
						console.log('Failed: ' + error);
					}).then(function(){
						importer.requesting--;
						console.log("requesting: " + importer.requesting);
					});
				};
				actionChain = actionChain.then(f);

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
			},
			function (reason) {
				importer.notSaved += actions.length;
			});
	};

	var finish = function () {
		if (importer.finished && importer.requesting <= 0 && importer.saved + importer.notSaved === importer.requested) {
			importer.status = "done";

			if (importer.notSaved !== 0) {
				importer.status = "with_errors";
			}
			importer.initForm(importer.authToken);
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
