package utils.fhir;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Reference;

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

public class ImmunizationResourceProvider extends RecordBasedResourceProvider<Immunization>
		implements IResourceProvider {

	public ImmunizationResourceProvider() {
		searchParamNameToPathMap.put("Immunization:location", "location");
		searchParamNameToTypeMap.put("Immunization:location", Sets.create("Location"));
		
		searchParamNameToPathMap.put("Immunization:manufacturer", "manufacturer");
		searchParamNameToTypeMap.put("Immunization:manufacturer", Sets.create("Organization"));
		
		searchParamNameToPathMap.put("Immunization:patient", "patient");
		searchParamNameToTypeMap.put("Immunization:patient", Sets.create("Patient"));
		
		searchParamNameToPathMap.put("Immunization:practitioner", "practitioner.actor");
		searchParamNameToTypeMap.put("Immunization:practitioner", Sets.create("Practitioner"));
		
		searchParamNameToPathMap.put("Immunization:reaction", "reaction.detail");
		searchParamNameToTypeMap.put("Immunization:reaction", Sets.create("Observation"));

		registerSearches("Immunization", getClass(), "getImmunization");
	}

	@Override
	public Class<Immunization> getResourceType() {
		return Immunization.class;
	}

	@Search()
	public Bundle getImmunization(
			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,

			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,

			@Description(shortDefinition = "Vaccination  (non)-Administration Date") @OptionalParam(name = "date") DateAndListParam theDate,

			@Description(shortDefinition = "Dose number within series") @OptionalParam(name = "dose-sequence") NumberAndListParam theDose_sequence,

			@Description(shortDefinition = "Business identifier") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier,

			@Description(shortDefinition = "The service delivery location or facility in which the vaccine was / was to be administered") @OptionalParam(name = "location", targetTypes = {}) ReferenceAndListParam theLocation,

			@Description(shortDefinition = "Vaccine Lot Number") @OptionalParam(name = "lot-number") StringAndListParam theLot_number,

			@Description(shortDefinition = "Vaccine Manufacturer") @OptionalParam(name = "manufacturer", targetTypes = {}) ReferenceAndListParam theManufacturer,

			@Description(shortDefinition = "Administrations which were not given") @OptionalParam(name = "notgiven") TokenAndListParam theNotgiven,

			@Description(shortDefinition = "The patient for the vaccination record") @OptionalParam(name = "patient", targetTypes = {}) ReferenceAndListParam thePatient,

			@Description(shortDefinition = "The practitioner who played a role in the vaccination") @OptionalParam(name = "practitioner", targetTypes = {}) ReferenceAndListParam thePractitioner,

			@Description(shortDefinition = "Additional information on reaction") @OptionalParam(name = "reaction", targetTypes = {}) ReferenceAndListParam theReaction,

			@Description(shortDefinition = "When reaction started") @OptionalParam(name = "reaction-date") DateAndListParam theReaction_date,

			@Description(shortDefinition = "Why immunization occurred") @OptionalParam(name = "reason") TokenAndListParam theReason,

			@Description(shortDefinition = "Explanation of reason vaccination was not administered") @OptionalParam(name = "reason-not-given") TokenAndListParam theReason_not_given,

			@Description(shortDefinition = "Immunization event status") @OptionalParam(name = "status") TokenAndListParam theStatus,

			@Description(shortDefinition = "Vaccine Product Administered") @OptionalParam(name = "vaccine-code") TokenAndListParam theVaccine_code,

			@RawParam Map<String, List<String>> theAdditionalRawParams,

			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,

			@IncludeParam(allow = { "Immunization:location", "Immunization:manufacturer", "Immunization:patient",
					"Immunization:practitioner", "Immunization:reaction", "*" }) Set<Include> theIncludes,
			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,

			@OptionalParam(name = "_page") StringParam _page,

			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);
		paramMap.add("_language", the_language);
		paramMap.add("date", theDate);
		paramMap.add("dose-sequence", theDose_sequence);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("location", theLocation);
		paramMap.add("lot-number", theLot_number);
		paramMap.add("manufacturer", theManufacturer);
		paramMap.add("notgiven", theNotgiven);
		paramMap.add("patient", thePatient);
		paramMap.add("practitioner", thePractitioner);
		paramMap.add("reaction", theReaction);
		paramMap.add("reaction-date", theReaction_date);
		paramMap.add("reason", theReason);
		paramMap.add("reason-not-given", theReason_not_given);
		paramMap.add("status", theStatus);
		paramMap.add("vaccine-code", theVaccine_code);

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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Immunization");

		builder.handleIdRestriction();

		// Add handling for search on the "owner" of the record.
		builder.recordOwnerReference("patient", "Patient", "patient"); // TODO patient -> patient?

		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "date");
		builder.restriction("dose-sequence", true, QueryBuilder.TYPE_INTEGER, "vaccinationProtocol.doseSequence");
		
		builder.restriction("location", true, "Location", "location");
		builder.restriction("lot-number", true, QueryBuilder.TYPE_STRING, "lotNumber");
		builder.restriction("manufacturer", true, "Organization", "manufacturer");
		builder.restriction("not-given", true, QueryBuilder.TYPE_BOOLEAN, "notGiven");			
		builder.restriction("practitioner", true, "Practitioner", "practitioner.actor");
		builder.restriction("reaction", true, "Observation", "reaction.detail");
		builder.restriction("reaction-date", true, QueryBuilder.TYPE_DATETIME, "reaction.date");
		builder.restriction("reason", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "explanation.reason");
		builder.restriction("reason-not-given", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "explanation.reasonNotGiven");
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");
		builder.restriction("vaccine-code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "vaccineCode");

		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Immunization theImmunization) {
		return super.createResource(theImmunization);
	}

	@Override
	public String getRecordFormat() {	
		return "fhir/Immunization";
	}
	
	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Immunization theImmunization) {
		return super.updateResource(theId, theImmunization);
	}

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type
	// "content" set.
	// b) Each record must have a "name" that will be shown to the user in the
	// record tree.
	// The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR
	// representation
	public void prepare(Record record, Immunization theImmunization) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a
		// fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, null, "Immunization");

		// Task b : Create record name
		String date = "No time";
		if (theImmunization.hasOccurrenceDateTimeType()) {
			try {
				date = FHIRTools.stringFromDateTime(theImmunization.getOccurrenceDateTimeType());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;

		// Task c : Set record owner based on subject and clean subject if this was
		// possible
		Reference subjectRef = theImmunization.getPatient(); // TODO is this correct, no subject field in this resource
		if (cleanAndSetRecordOwner(record, subjectRef))
			theImmunization.setPatient(null);

		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta"
		// section
		clean(theImmunization);

	}

	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, Immunization p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getPatient().isEmpty()) {
			p.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
}
