angular.module('fhir')
.constant('profile', {
  publicResources : ["AllergyIntolerance","Appointment","AppointmentRespone","CarePlan","Claim","ClaimResponse","ClinicalImpression","Communication","CommunicationRequest","Condition","Contract","Coverage","DetectedIssue","DeviceUseRequest","DeviceUseStatement","DiagnosticOrder","DiagnosticReport","Encounter","EpisodeOfCare","FamilyMemberHistory","Flag","Goal","Immunization","ImmunizationRecommendation","Media","MedicationAdministration","MedicationDispense","MedicationOrder","MedicationStatement","NutritionOrder","Observation","Patient","Procedure","ProcedureRequest","Questionnaire","QuestionnaireResponse","ReferralRequest","RelatedPerson","Schedule","VisionPrescription","DocumentReference"],
  modify : {
	  "Patient.identifier" : { min : 1 },
	  "Patient.name" : { min : 1 },
	  "Patient.gender" : { min : 1 },
	  "AllergyIntolerance.sensitivityType" : { min : 1 },
	  "AllergyIntolerance.subject" : { min : 1},
	  "AllergyIntolerance.substance" : { min : 1}
	  "Immunization.subject" : { min : 1},
	  "Immunization.refusalIndicator" : { min : 1},
	  "Immunization.reported" : { min : 1},
	  "Immunization.vaccineType" : { min : 1}
	  "Condition.subject" : { min : 1}
	  "Condition.code" : { min : 1, system : "http://snomed.info/sct" },
	  "Condition.status" : { min : 1},
	  "Condition.onsetDate" : { min : 1}
      "Procedure.subject" : { min : 1},
      "Procedure.type" : { min : 1, system : "http://snomed.info/sct" },
      "Procedure.date.start" : { min : 1},
      "Procedure.date.end" : { min : 1},
	  "FamilyHitory.subject" : { min : 1},
	  "FamilyHitory.reation.relationship" : { min : 1}
	      
	/*  
	  Each MedicationPrescription must have:

	      1 patient in MedicationPrescription.patient
	      1 Medication object in MedicationPrescription.medication with system http://www.nlm.nih.gov/research/umls/rxnorm in Medication.code.coding.system
	      1 status of active in MedicationPrescription.status
	      1 object in MedicationPrescription.dosageInstruction.timingSchedule with 1 date in event.start and 0 or 1 date in event.end and 0 or 1 objects in repeat (with 1 value in repeat.frequency, 1 value in repeat.units, and 1 value in repeat.duration)
	      0 or 1 code in MedicationPrescription.dosageInstruction.doseQuantity with system of http://unitsofmeasure.org
	      0 or 1 objects in MedicationPrescription.dispense with 1 value in numberOfRepeatsAllowed, 1 code with system of http://unitsofmeasure.org in quantity, and 0 or 1 codes with system of http://unitsofmeasure.org in expectedSupplyDuration

	  Example: https://fhir-open-api.smarthealthit.org/MedicationPrescription/102
	  Medication Dispense

	  Each MedicationDispense must have:

	      1 patient in MedicationDispense.patient
	      1 reference to MedicationPrescription in MedicationDispense.authorizingPrescription
	      1 object in MedicationDispense.dispense with 1 extension of http://fhir-registry.smarthealthit.org/Profile/dispense#days-supply of type valueQuantity with system of http://unitsofmeasure.org with units of days and code of d
	      1 Medication object in MedicationDispense.dispense.medication with system http://www.nlm.nih.gov/research/umls/rxnorm in Medication.coding.system
	      1 status of completed in MedicationDispense.dispense.status
	      1 quantity with system http://unitsofmeasure.org and code of {tablets} and units of tablets in MedicationDispense.dispense.quantity
	      1 date in MedicationDispense.dispense.whenHandedOver

	  Example: https://fhir-open-api.smarthealthit.org/MedicationDispense/1229
	  Vital Signs

	  A set of Vital Signs is represented usng FHIR Observation resources. Each Observation must have:

	      1 patient in Observation.patient
	      1 LOINC-coded Vital Sign (see below) in Observation.name
	      1 indicator of ok (fixed value) in Observation.reliability
	      1 status indicator (see FHIR definitions) in Observation.status
	      1 quantity with system http://unitsofmeasure.org and a UCUM-coded value (see below) in Observation.valueQuantity
	      1 date indicating when the value was measured, in Observation.appliesDateTime

	  LOINC codes for vital signs

	  Top-level vital sign codes are all LOINC codes with system of http://loinc.org:
	  Vital Sign 	LOINC Code 	Units
	  Height 	8302-2 	cm, m,[in_us], [in_i]
	  Weight 	3141-9 	kg, g, lb_av, [oz_av]
	  Heart rate 	8867-4 	{beats}/min
	  Respiratory rate 	9279-1 	{breaths}/min
	  Temperature 	8310-5 	Cel, [degF]
	  Body Mass Index 	39156-5 	kg/m2
	  Oxygen saturation 	2710-2 	%{HemoglobinSaturation}
	  Head circumference 	8287-5 	cm, m, [in_us], [in_i]
	  Blood pressure (systolic and diastolic -- grouping structure) 	55284-4 	N/A
	  Systolic blood pressure 	8480-6 	mm[Hg]
	  Diastolic blood pressure 	8462-4 	mm[Hg]
	  Grouping blood pressures

	  The representation of a blood pressure measurement makes systolic/diastolic pairings explicit by using a "grouping observation" with LOINC code 55284-4 (see above). The grouping observation has no value itself, but refers to two individual observations for systolic and diastolic values. The grouping observation refers to its two individual components using Observation.related, where type is has-component and target is a resource reference to a systolic or diastolic blood pressure obsevation.
	  Example: blood pressure https://fhir-open-api.smarthealthit.org/Observation/691-bp
	  Grouping other vital signs

	  Any time a set of vital signs is measured together, as a set, it can be explicitly grouped using a "grouping" observation with LOINC code 8716-3.
	  Lab Results

	  An individual lab result is represented with the FHIR Observation resource. Each result must have:

	      1 patient in Observation.patient
	      1 LOINC code in Observation.name with system of http://loinc.org
	      1 indicator of ok (fixed value) in Observation.reliability
	      1 status indicator (see FHIR definitions) in Observation.status
	      1 date indicating when the sample was taken (or other "physiologically relevant" time), in Observation.appliesDateTime
	      1 value (details depend on whether the lab test is quantitative -- see below)
*/
  }
});