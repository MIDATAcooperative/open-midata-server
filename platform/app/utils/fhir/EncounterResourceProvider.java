package utils.fhir;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RawParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberAndListParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

// TODO: Choose the correct super class and register in utils.fhir.FHIRServlet

public class EncounterResourceProvider extends RecordBasedResourceProvider<Encounter> implements IResourceProvider {

	public EncounterResourceProvider() {
		searchParamNameToPathMap.put("Encounter:appointment", "appointment");
		searchParamNameToTypeMap.put("Encounter:appointment", Sets.create("Appointment"));
		
		searchParamNameToPathMap.put("Encounter:diagnosis", "diagnosis.condition");
		searchParamNameToTypeMap.put("Encounter:diagnosis", Sets.create("Condition", "Procedure"));
		
		searchParamNameToPathMap.put("Encounter:episodeofcare", "episodeOfCare");
		searchParamNameToTypeMap.put("Encounter:episodeofcare", Sets.create("EpisodeOfCare"));
		
		searchParamNameToPathMap.put("Encounter:incomingreferral", "incomingReferral");
		searchParamNameToTypeMap.put("Encounter:incomingreferral", Sets.create("ReferralRequest"));
		
		searchParamNameToPathMap.put("Encounter:location", "location.location");
		searchParamNameToTypeMap.put("Encounter:location", Sets.create("Location"));
		
		searchParamNameToPathMap.put("Encounter:part-of", "partOf");
		searchParamNameToTypeMap.put("Encounter:part-of", Sets.create("Encounter"));
		
		searchParamNameToPathMap.put("Encounter:participant", "participant.individual");
		searchParamNameToTypeMap.put("Encounter:participant", Sets.create("Practitioner", "RelatedPerson"));
		
		searchParamNameToPathMap.put("Encounter:patient", "subject");
		searchParamNameToTypeMap.put("Encounter:patient", Sets.create("Patient"));
		
		searchParamNameToPathMap.put("Encounter:practitioner", "participant.individual");
		searchParamNameToTypeMap.put("Encounter:practitioner", Sets.create("Practitioner"));
		
		searchParamNameToPathMap.put("Encounter:service-provider", "serviceProvider");
		searchParamNameToTypeMap.put("Encounter:service-provider", Sets.create("Organization"));
		
		searchParamNameToPathMap.put("Encounter:subject", "subject");
		searchParamNameToTypeMap.put("Encounter:subject", Sets.create("Group", "Patient"));

		registerSearches("Encounter", getClass(), "getEncounter");
	}

	@Override
	public Class<Encounter> getResourceType() {
		return Encounter.class;
	}

	@Search()
	public Bundle getEncounter(
			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,

			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,

			@Description(shortDefinition = "The appointment that scheduled this encounter") @OptionalParam(name = "appointment", targetTypes = {}) ReferenceAndListParam theAppointment,

			@Description(shortDefinition = "inpatient | outpatient | ambulatory | emergency +") @OptionalParam(name = "class") TokenAndListParam theClass,

			@Description(shortDefinition = "A date within the period the Encounter lasted") @OptionalParam(name = "date") DateAndListParam theDate,

			@Description(shortDefinition = "Reason the encounter takes place (resource)") @OptionalParam(name = "diagnosis", targetTypes = {}) ReferenceAndListParam theDiagnosis,

			@Description(shortDefinition = "Episode(s) of care that this encounter should be recorded against") @OptionalParam(name = "episodeofcare", targetTypes = {}) ReferenceAndListParam theEpisodeofcare,

			@Description(shortDefinition = "Identifier(s) by which this encounter is known") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier,

			@Description(shortDefinition = "The ReferralRequest that initiated this encounter") @OptionalParam(name = "incomingreferral", targetTypes = {}) ReferenceAndListParam theIncomingreferral,

			@Description(shortDefinition = "Length of encounter in days") @OptionalParam(name = "length") NumberAndListParam theLength,

			@Description(shortDefinition = "Location the encounter takes place") @OptionalParam(name = "location", targetTypes = {}) ReferenceAndListParam theLocation,

			@Description(shortDefinition = "Time period during which the patient was present at the location") @OptionalParam(name = "location-period") DateAndListParam theLocation_period,

			@Description(shortDefinition = "Another Encounter this encounter is part of") @OptionalParam(name = "part-of", targetTypes = {}) ReferenceAndListParam thePart_of,

			@Description(shortDefinition = "Persons involved in the encounter other than the patient") @OptionalParam(name = "participant", targetTypes = {}) ReferenceAndListParam theParticipant,

			@Description(shortDefinition = "Role of participant in encounter") @OptionalParam(name = "participant-type") TokenAndListParam theParticipant_type,

			@Description(shortDefinition = "The patient ro group present at the encounter") @OptionalParam(name = "patient", targetTypes = {}) ReferenceAndListParam thePatient,

			@Description(shortDefinition = "Persons involved in the encounter other than the patient") @OptionalParam(name = "practitioner", targetTypes = {}) ReferenceAndListParam thePractitioner,

			@Description(shortDefinition = "Reason the encounter takes place (code)") @OptionalParam(name = "reason") TokenAndListParam theReason,

			@Description(shortDefinition = "The custodian organization of this Encounter record") @OptionalParam(name = "service-provider", targetTypes = {}) ReferenceAndListParam theService_provider,

			@Description(shortDefinition = "Wheelchair, translator, stretcher, etc.") @OptionalParam(name = "special-arrangement") TokenAndListParam theSpecial_arrangement,

			@Description(shortDefinition = "planned | arrived | triaged | in-progress | onleave | finished | cancelled +") @OptionalParam(name = "status") TokenAndListParam theStatus,

			@Description(shortDefinition = "The patient ro group present at the encounter") @OptionalParam(name = "subject", targetTypes = {}) ReferenceAndListParam theSubject,

			@Description(shortDefinition = "Specific type of encounter") @OptionalParam(name = "type") TokenAndListParam theType,

			@RawParam Map<String, List<String>> theAdditionalRawParams,

			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,

			@IncludeParam(allow = { "Encounter:appointment", "Encounter:diagnosis", "Encounter:episodeofcare",
					"Encounter:incomingreferral", "Encounter:location", "Encounter:part-of", "Encounter:participant",
					"Encounter:patient", "Encounter:practitioner", "Encounter:service-provider", "Encounter:subject",
					"*" }) Set<Include> theIncludes,

			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,

			@OptionalParam(name = "_page") StringParam _page,

			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);
		paramMap.add("_language", the_language);
		paramMap.add("appointment", theAppointment);
		paramMap.add("class", theClass);
		paramMap.add("date", theDate);
		paramMap.add("diagnosis", theDiagnosis);
		paramMap.add("episodeofcare", theEpisodeofcare);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("incomingreferral", theIncomingreferral);
		paramMap.add("length", theLength);
		paramMap.add("location", theLocation);
		paramMap.add("location-period", theLocation_period);
		paramMap.add("part-of", thePart_of);
		paramMap.add("participant", theParticipant);
		paramMap.add("participant-type", theParticipant_type);
		paramMap.add("patient", thePatient);
		paramMap.add("practitioner", thePractitioner);
		paramMap.add("reason", theReason);
		paramMap.add("service-provider", theService_provider);
		paramMap.add("special-arrangement", theSpecial_arrangement);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);
		paramMap.add("type", theType);
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		paramMap.setFrom(_page != null ? _page.getValue() : null);

		return searchBundle(paramMap, theDetails);

	}

	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();

		Query query = new Query();
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Encounter");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");

		// Add handling for search on the field that determines the MIDATA content type used.
        // TODO builder.recordCodeRestriction("code", "code");
		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");  // TODO not so sure what to do here with patient and subject
		
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		
		builder.restriction("appointment", true, "Appointment", "appointment");
		builder.restriction("class", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "class");
		builder.restriction("date", true, QueryBuilder.TYPE_PERIOD, "period");
		builder.restriction("type", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "type");		
		builder.restriction("diagnosis", true, null, "diagnosis.condition");
		builder.restriction("episodeofcare", true, "EpisodeOfCare", "episodeOfCare");
		
		builder.restriction("incomingreferral", true, "ReferralRequest", "incomingReferral");
		builder.restriction("length", true, QueryBuilder.TYPE_QUANTITY, "length"); 
		builder.restriction("location", true, "Location", "location.location");
		builder.restriction("location-period", true, QueryBuilder.TYPE_PERIOD, "location.period");
		builder.restriction("part-of", true, "Encounter", "partOf");
		builder.restriction("participant", true, null, "participant.individual");
		builder.restriction("participant-type", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "participant.type");
		builder.restriction("practitioner", true, null, "participant.individual");
		builder.restriction("reason", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "reason");
		builder.restriction("service-provider", true, "Organization", "serviceProvider");
		builder.restriction("special-arrangement", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "hospitalization.specialArrangement");
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");
		



		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Encounter theEncounter) {
		return super.createResource(theEncounter);
	}

	// Construct a new empty MIDATA record that is initialized with the correct
	// format.
	public Record init() {
		return newRecord("fhir/Encounter");
	}

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Encounter theEncounter) {
		return super.updateResource(theId, theEncounter);
	}

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type
	// "content" set.
	// b) Each record must have a "name" that will be shown to the user in the
	// record tree.
	// The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR
	// representation
	public void prepare(Record record, Encounter theEncounter) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a
		// fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, null, "Encounter"); 

		// Task b : Create record name
		String date = "No time";
		if (theEncounter.hasPeriod()) {
			try {
				date = FHIRTools.stringFromDateTime(theEncounter.getPeriod());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;

		// Task c : Set record owner based on subject and clean subject if this was
		// possible
		Reference subjectRef = theEncounter.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef))
			theEncounter.setSubject(null);

		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta"
		// section
		clean(theEncounter);

	}

	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, Encounter p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getSubject().isEmpty()) {
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
}
