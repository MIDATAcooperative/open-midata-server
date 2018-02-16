var midata = require('./src/midata');
var assert = require('assert');
 
/*
 * Variables required for calculation of results to be shared
 */
var mapPatientToYear = {};
var weightPerYear = {};
var groupPerYear = {};
var ids = {};

/*
 * App to login into MIDATA. Must be registered on the platform and must be linked to the correct study
 */
midata.app("research", "12345", "debug")

/*
 * Midata Server to connect to
 */
.useServer("https://localhost:9000")

/*
 * Research user to login. 
 */
.loginResearcher("research@instant-mail.de", "Secret123")

/*
 * As example we get and remember the year of birth for all participants
 */
.forEachMatch("Patient", { _count : 1000 }, function(patient) {
	var id = patient.id;
	var year = new Date(patient.birthDate).getFullYear();
	if (ids[id]) assert(null, "Double id P:"+id);
	ids[id] = true;
	assert(id , "Missing Patient Id");
	assert(year, "Missing year: "+patient.birthDate);
	mapPatientToYear["Patient/"+id] = year;	
	
})
/*
 * We get all Observations which are body weight records
 * As example we calculate average weight per year of birth
 */
.forEachMatch("Observation", { code : "http://loinc.org|29463-7", _count : 1000 }, function(obs) {
	var patient = obs.subject.reference;
	if (ids[obs.id]) assert(null, "Double id "+obs.id);
	ids[obs.id] = true;
	if (patient.startsWith("Group")) return;
	// We stored year of birth for all participants in the previous step 
	var year = mapPatientToYear[patient];
	
	assert(patient, "Missing patient");
	assert(year, "Missing year for patient: "+patient);
	
	var stats = weightPerYear[year];
	if (!stats) weightPerYear[year] = stats = { value : 0, count : 0};
	stats.count++;
	stats.value += obs.valueQuantity.value;
		
})

/*
 * We need a Group for each year where we can link the results to.
 * First we check which groups have already been created
 */
.forEachMatch("Group", { "identifier" : "http://demostudy/year|" }, function(group) {
	groupPerYear[group.identifier[0].value] = "Group/"+group.id;
})

/*
 * For each year where we have data but no existing Group we need to create one
 */
.modifyDB(function(db) {
	for (year in weightPerYear) {
		if (!groupPerYear[year]) {
			db.create({
				resourceType : "Group",
				type : "person",
				actual : true,
				name : "Persons from year "+year,
				identifier : [ { system : "http://demostudy/year", value : year }]
				
			}).then(function(resource) {
				groupPerYear[resource.identifier[0].value] = "Group/"+resource.id;
			});
		}
	}
})
.modifyDBBundle(function(db) {	
	var today = new Date();
	for (year in weightPerYear) {
		db.create({
			resourceType : "Observation",
			code : { coding : [{system : "http://loinc.org", code : "29463-7", display : "Body weigth" }] },
			subject : { reference : groupPerYear[year], display : "Group Year "+year },
			effectiveDateTime : today,
			valueQuantity : { value : weightPerYear[year].value / weightPerYear[year].count, unit : "kg" }
		});
	}
})
.then(function() {
	console.log("ALL FINISHED");
});
