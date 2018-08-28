// Which file should be converted
var fileToRead = "study.json";

// Into which directory should the results been written? (Please ensure directory exists)
var outputDirectory = "out";

// How should the data be mapped to CSV?
var mapping = [
  {
	  file : "Patient.csv",
	  filter : {
	     resourceType : "Patient"
	  },
	  fields : [
		  { csv : "id", fhir : "id"},
		  { csv : "name", fhir : "name.text" },
		  { csv : "gender", fhir : "gender" },
		  { csv : "birthdate", fhir : "birthDate" },
		  { csv : "participant-id", fhir : "identifier.value", filter : { "identifier.system" : "http://midata.coop/identifier/participant-id" }},
		  { csv : "join-date", fhir : "meta.lastUpdated" }
	  ]
  },
  {
	  file : "Forecast.csv",
	  filter : {
		  resourceType : "Observation",
		  "code.coding.code" : "pollen-forecast"
	  },
	  forEach : "component",	  
	  fields : [
		  { csv : "id", fhir : "id" },		
		  { csv : "status", fhir : "status" },
		  { csv : "date", fhir : "effectiveDateTime" },
		  { csv : "specimen", fhir : "specimen.identifier.value" },
		  { csv : "location", fhir : "component.code.coding.code" },
		  { csv : "value", fhir : "component.valueQuantity.value" },
		  { csv : "ismain", fhir : "interpretation.coding.code", missing : "false" }
	  ]	  
  },
  {
	  file : "Symptoms.csv",
	  filter : {
		  resourceType : "Observation",
		  "code.coding.code" : "300910009"
	  },	
	  fields : [
		  { csv : "id", fhir : "id" },
		  { csv : "code", fhir : "code", onlyFirst:true },
		  { csv : "status", fhir : "status" },
		  { csv : "date", fhir : "effectiveDateTime" },
		  { csv : "person", fhir : "subject.reference" },
		  { csv : "pseudonym", fhir : "subject.display" },
		  { csv : "bodysite", fhir : "bodySite" },
		  { csv : "zipcode", fhir : "extension.extension.valueCode", filter : { "extension.url" : "http://midata.coop/Extensions/event-location", "extension.extension.url" : "zipcode" } },
		  { csv : "locality", fhir : "extension.extension.valueString", filter : { "extension.url" : "http://midata.coop/Extensions/event-location", "extension.extension.url" : "locality" } },
		  { csv : "treated", fhir : "extension.valueBoolean", filter : { "extension.url" : "http://midata.coop/Extensions/treated" } },
		  { csv : "quantity", fhir : "valueQuantity", missing : "none" }
	  ]	  
  },
  {
	  file : "Allergies.csv",
	  filter : {
		  resourceType : "AllergyIntolerance"
	  },	  
	  fields : [
		  { csv : "id", fhir : "id" },
		  { csv : "code", fhir : "code", onlyFirst : true },
		  { csv : "category", fhir : "category" },
		  { csv : "verified", fhir : "verificationStatus" },
		  { csv : "person", fhir : "patient.reference" },
		  { csv : "pseudonym", fhir : "patient.display" },
	  ]
  },
  {
	  file : "AirPurifier.csv",
	  filter : {
		  resourceType : "Observation",
		  "code.coding" : "http://midata.coop/codesystems/specific|air-purifier"
	  },	  
	  fields : [
		  { csv : "id", fhir : "id" },
		  { csv : "patient", fhir : "subject.reference" },
		  { csv : "pseudonym", fhir : "subject.display" },
		  { csv : "date", fhir : "effectiveDateTime" },
		  { csv : "answer", fhir : "valueCodeableConcept.coding.code" }
	  ]
  },
  {
	  file : "MainLocation.csv",
	  filter : {
		  resourceType : "Observation",
		  "code.coding" : "http://midata.coop|main-occupation-location"
	  },	
	  fields : [
		  { csv : "id", fhir : "id" },
		  { csv : "patient", fhir : "subject.reference" },
		  { csv : "pseudonym", fhir : "subject.display" },
		  { csv : "date", fhir : "effectiveDateTime" },
		  { csv : "answer", fhir : "valueCodeableConcept.coding.code" }
	  ]
  }/*,
  {
	  file : "Other.csv",
	  filter : {
		  resourceType : "Observation"
	  },
	  debug : true,
	  fields : [
		  { csv : "id", fhir : "id" },
		  { csv : "code", fhir : "code" },
		  { csv : "category", fhir : "category" }
	  ]
  }*/
];
		

var converter = require('./src/converter');
converter.run(fileToRead, outputDirectory, mapping);
