/**
 * Example : calculate average body weight of study participants for each year of birth 
 */

/*
 * Include required libraries
 */
var midata = require('./src/midata-research');
var assert = require('assert');
 
/*
 * Variables required for calculation of results to be shared
 */
// patient id -> year of birth of patient
var mapPatientToYear = {};
// patient id -> newest record
var mapPatientToNewest = {};
// year of birth -> weight statistics
var mapYearToWeightStats = {};
// year of birth -> group reference
var mapYearToGroup = {};
// group -> aggregated observation resource
var mapGroupToObservation = {};

/*
 * App to login into MIDATA. Must be registered on the platform and must be linked to the correct study
 * parameters : name of app, secret of app, device code used for study link
 */
midata.app("research", "12345", "debug")

/*
 * Midata Server to connect to
 */
.useServer("https://localhost:9000")

/*
 * Research user to login. 
 * parameters : login of researcher user, password of researcher
 */
.loginResearcher("research@instant-mail.de", "Secret123")

/*
 * As example we get and remember the year of birth for all participants
 */
.forEachMatch("Patient", { _count : 1000 }, function(patient) {
	// Extract data
	var id = patient.id;
	var year = new Date(patient.birthDate).getFullYear();
	
	// Check data valid
	assert(id , "Missing patient ID");
	assert(year, "Missing year of birth: "+patient.birthDate);
	
	// Store mapping
	mapPatientToYear["Patient/"+id] = year;	
	
})
/*
 * We get all Observations which are body weight records
 * We keep the value of the newest record for each participant
 */
.forEachMatch("Observation", { code : "http://loinc.org|29463-7", _count : 1000 }, function(observation) {
	var patientRef = observation.subject.reference;
	
	// We skip observations that were entered in error
	if (observation.status == "entered-in-error" || observation.status == "cancelled") return;
	
	// We skip and remember the already stored aggregated Observations
	if (patientRef.startsWith("Group")) {
		mapGroupToObservation[patientRef] = observation;
		return;
	}
	
	// Check data valid
	assert(patientRef, "Missing patientRef");	
	assert(observation.valueQuantity, "Missing data in Observation");
	assert(observation.valueQuantity.unit == "kg", "Unsupported or missing unit");
	
	// We keep time and weight value of newest record for each patient
	var existingData = mapPatientToNewest[patientRef];	
	if (!existingData || existingData.date < observation.effectiveDateTime) {
	  mapPatientToNewest[patientRef] = { date : observation.effectiveDateTime, value : observation.valueQuantity.value }; 	
	}
})

/*
 * With the newest value for each participant we calculate average weight per year of birth
 */
.then(function() {

	// For each participant
	for (var patientRef in mapPatientToNewest) {
		
		// We retrieve the newest body weight value
		var data = mapPatientToNewest[patientRef];
		
		// We stored year of birth for all participants in the previous step 
		var year = mapPatientToYear[patientRef];	
		assert(year, "Missing year for patientRef: "+patientRef);
		
		// Average calculation
		var stats = mapYearToWeightStats[year];
		if (!stats) mapYearToWeightStats[year] = stats = { value : 0, count : 0};
		stats.count++;
		stats.value += data.value;
	}
		
})

/*
 * We need a Group for each year where we can link the results to.
 * First we check which groups have already been created
 */
.forEachMatch("Group", { "identifier" : "http://demostudy/year|" }, function(group) {
	
	// Store group reference for each year
	mapYearToGroup[group.identifier[0].value] = "Group/"+group.id;
})


/*
 * For each year where we have data but no existing Group we need to create one
 */
.modifyDB(function(db) {
	
	// For each year we have data...
	for (year in mapYearToWeightStats) {
		
		// If group not yet existing
		if (!mapYearToGroup[year]) {
			
			// Create it...
			db.create({
				resourceType : "Group",
				type : "person",
				actual : true,
				name : "Persons from year "+year,
				identifier : [ { system : "http://demostudy/year", value : year }]
				
			}).then(function(resource) {				
				// .. and store reference
				mapYearToGroup[resource.identifier[0].value] = "Group/"+resource.id;
			});
		}
	}
})

/*
 * Store or update aggregated average weights for each year 
 */
.modifyDBBundle(function(db) {	
	var today = new Date();
	// For each year where we have data
	for (year in mapYearToWeightStats) {
		
		// Calculate average weight
		var valueQuantity = { 
				value : mapYearToWeightStats[year].value / mapYearToWeightStats[year].count, 
				unit : "kg"
		}; 
			
		// If Observation already exists...
		if (mapGroupToObservation[mapYearToGroup[year]]) {
			var existingObservation = mapGroupToObservation[mapYearToGroup[year]];
			
			// ... update it
			existingObservation.effectiveDateTime = today;
			existingObservation.valueQuantity = valueQuantity;
			db.update(existingObservation);
			
		} else {	
			// Otherwise create a new one
			db.create({
				resourceType : "Observation",
				code : { coding : [{system : "http://loinc.org", code : "29463-7", display : "Body weigth" }] },
				subject : { reference : mapYearToGroup[year], display : "Group Year "+year },
				effectiveDateTime : today,
				valueQuantity : { value : mapYearToWeightStats[year].value / mapYearToWeightStats[year].count, unit : "kg" }
			});
		}
	}
})
/*
 * Log that the script has finished
 */
.then(function() {
	console.log("ALL FINISHED");
});
