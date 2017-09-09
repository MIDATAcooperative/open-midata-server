// the URL for the calls
var baseUrl = "https://api.health.nokia.com";

// make a two digit string out of a given number
var twoDigit = function (num) {
    return ("0" + num).slice(-2);
};

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

// The measurements defined in Nokia Health
var measurementGroups = [
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