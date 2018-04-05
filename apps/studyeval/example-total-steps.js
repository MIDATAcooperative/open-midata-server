/**
 * Example : calculate total number of steps walked on one day by study participants 
 */

/*
 * Include required libraries
 */
var midata = require('./src/midata-research');
var assert = require('assert');
 
/*
 * Variables required for calculation of results to be shared
 */
// Day to calculate 
var dayToCalculate = "2017-02-17";
// The total number of steps
var stepsTotal = 0;
// The reference to the FHIR group that the aggregated result will be assigned to
var groupForAggregation = null;
// result for selected day from a previous run
var existingAggregatedObservation = null;

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
 * We get all Observations of the requested day which are walked steps records
 * We count the total number of steps
 */
.forEachMatch("Observation", { code : "http://loinc.org|41950-7", date : dayToCalculate, _count : 1000 }, function(observation) {
	var patientRef = observation.subject.reference;
	
	// We skip observations that were entered in error
	if (observation.status == "entered-in-error" || observation.status == "cancelled") return;
	
	// We skip the already stored aggregated Observations
	if (patientRef.startsWith("Group")) {		
		return;
	}
		
	// Check data valid
	assert(patientRef, "Missing patientRef");	
	assert(observation.valueQuantity, "Missing data in Observation");
	
	// Add to total steps
	stepsTotal += observation.valueQuantity.value;			
})


/*
 * We need a Group where we assign the aggregated data to
 * First we check if the group has already been created
 */
.forEachMatch("Group", { "identifier" : "http://demostudy|totalsteps" }, function(group) {
	
	// Store group reference
	groupForAggregation = "Group/"+group.id;	
})

/*
 * If the group is not yet existing we create it
 */
.modifyDB(function(db) {
		
	// If group not yet existing
	if (!groupForAggregation) {
		
		// Create it...
		db.create({
			resourceType : "Group",
			type : "person",
			actual : true,
			name : "Study participants",
			identifier : [ { system : "http://demostudy", value : "totalsteps" }]
			
		}).then(function(resource) {				
			// .. and store reference
			groupForAggregation = "Group/"+resource.id;
		});
	}
	
})

/*
 * Have results already been stored by a previous run of the script?
 * Important: "groupForAggregation" is not set at the time the forEachMatch method is called (initialized). 
 * Therefore the criteria parameter is wrapped into " () => ( ... ) " which will delay the evaluation of the criteria expression until  
 * the query is actually executed. At that time groupForAggregation will have been set.
 */
.forEachMatch("Observation", () => ( { code : "http://loinc.org|41950-7", date : dayToCalculate, subject : groupForAggregation } ), function(observation) {
	existingAggregatedObservation = observation;
})

/*
 * Store sum of total steps
 */
.modifyDB(function(db) {	

	if (existingAggregatedObservation) {
		existingAggregatedObservation.valueQuantity.value = stepsTotal;
		db.update(existingAggregatedObservation);
	} else {
		db.create({
			resourceType : "Observation",
			code : { coding : [{system : "http://loinc.org", code : "41950-7", display : "Steps [24 hour]" }] },
			subject : { reference : groupForAggregation, display : "All study participants" },
			effectiveDateTime : dayToCalculate,
			valueQuantity : { value : stepsTotal, unit : "steps" }
		});
	}
	
})
.then(function() {
	console.log("Total steps: "+stepsTotal);
});
