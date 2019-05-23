package utils.fhir;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationAdministration;
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


public class MedicationAdministrationResourceProvider extends RecordBasedResourceProvider<MedicationAdministration>
		implements IResourceProvider {

	public MedicationAdministrationResourceProvider() {
		searchParamNameToPathMap.put("MedicationAdministration:context", "context");
		searchParamNameToTypeMap.put("MedicationAdministration:context", Sets.create("EpisodeOfCare", "Encounter"));

		searchParamNameToPathMap.put("MedicationAdministration:device", "device");
		searchParamNameToTypeMap.put("MedicationAdministration:device", Sets.create("Device"));

		searchParamNameToPathMap.put("MedicationAdministration:medication", "medication");
		searchParamNameToTypeMap.put("MedicationAdministration:medication", Sets.create("Medication"));

		searchParamNameToPathMap.put("MedicationAdministration:patient", "subject");
		searchParamNameToTypeMap.put("MedicationAdministration:patient", Sets.create("Patient"));

		searchParamNameToPathMap.put("MedicationAdministration:performer", "performer.action");
		searchParamNameToTypeMap.put("MedicationAdministration:performer",
				Sets.create("Practitioner", "Device", "Patient", "RelatedPerson"));

		searchParamNameToPathMap.put("MedicationAdministration:prescription", "prescription");
		searchParamNameToTypeMap.put("MedicationAdministration:prescription", Sets.create("MedicationRequest"));

		searchParamNameToPathMap.put("MedicationAdministration:subject", "subject");
		searchParamNameToTypeMap.put("MedicationAdministration:subject", Sets.create("Group", "Patient"));

		registerSearches("MedicationAdministration", getClass(), "getMedicationAdministration");
	}

	@Override
	public Class<MedicationAdministration> getResourceType() {
		return MedicationAdministration.class;
	}

	@Search()
	public Bundle getMedicationAdministration(
			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,
			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,
			@Description(shortDefinition = "Return administrations of this medication code") @OptionalParam(name = "code") TokenAndListParam theCode,
			@Description(shortDefinition = "Return administrations that share this encounter or episode of care") @OptionalParam(name = "context", targetTypes = {}) ReferenceAndListParam theContext,
			@Description(shortDefinition = "Return administrations with this administration device identity") @OptionalParam(name = "device", targetTypes = {}) ReferenceAndListParam theDevice,
			@Description(shortDefinition = "Date administration happened (or did not happen)") @OptionalParam(name = "effective-time") DateAndListParam theEffective_time,
			@Description(shortDefinition = "Return administrations with this external identifier") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier,
			@Description(shortDefinition = "Return administrations of this medication resource") @OptionalParam(name = "medication", targetTypes = {}) ReferenceAndListParam theMedication,
			@Description(shortDefinition = "Administrations that were not made") @OptionalParam(name = "not-given") TokenAndListParam theNot_given,
			@Description(shortDefinition = "The identity of a patient to list administrations  for") @OptionalParam(name = "patient", targetTypes = {}) ReferenceAndListParam thePatient,
			@Description(shortDefinition = "The identify of the individual who administered the medication") @OptionalParam(name = "performer", targetTypes = {}) ReferenceAndListParam thePerformer,

			@Description(shortDefinition = "The identity of a prescription to list administrations from") @OptionalParam(name = "prescription", targetTypes = {}) ReferenceAndListParam thePrescription,

			@Description(shortDefinition = "Reasons for administering the medication") @OptionalParam(name = "reason-given") TokenAndListParam theReason_given,

			@Description(shortDefinition = "Reasons for not administering the medication") @OptionalParam(name = "reason-not-given") TokenAndListParam theReason_not_given,

			@Description(shortDefinition = "MedicationAdministration event status (for example one of active/paused/completed/nullified)") @OptionalParam(name = "status") TokenAndListParam theStatus,

			@Description(shortDefinition = "The identify of the individual or group to list administrations for") @OptionalParam(name = "subject", targetTypes = {}) ReferenceAndListParam theSubject,

			@RawParam Map<String, List<String>> theAdditionalRawParams,

			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,
			@IncludeParam(allow = { "MedicationAdministration:context", "MedicationAdministration:device",
					"MedicationAdministration:medication", "MedicationAdministration:patient",
					"MedicationAdministration:performer", "MedicationAdministration:prescription",
					"MedicationAdministration:subject", "*" }) Set<Include> theIncludes,

			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,

			@OptionalParam(name = "_page") StringParam _page,

			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);
		paramMap.add("_language", the_language);
		paramMap.add("code", theCode);
		paramMap.add("context", theContext);
		paramMap.add("device", theDevice);
		paramMap.add("effective-time", theEffective_time);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("medication", theMedication);
		paramMap.add("not-given", theNot_given);
		paramMap.add("patient", thePatient);
		paramMap.add("performer", thePerformer);
		paramMap.add("prescription", thePrescription);
		paramMap.add("reason-given", theReason_given);
		paramMap.add("reason-not-given", theReason_not_given);
		paramMap.add("status", theStatus);
		paramMap.add("subject", theSubject);

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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/MedicationAdministration");

		builder.handleIdRestriction();

		// Add handling for search on the "owner" of the record.
		builder.recordOwnerReference("patient", "Patient", "subject");

		
		// On some resources there are searches for "patient" and "subject" which are
		// searches on the same field. patient is always the record owner
		// subject may be something different than a person and may not be the record
		// owner
		// in that case. Add a record owner search if possible and a normal search
		// otherwise:
		if (!builder.recordOwnerReference("subject", null, "subject"))
			builder.restriction("subject", true, null, "subject");

		builder.restriction("code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "medicationCodeableConcept");
		builder.restriction("context", true, null, "context");
		builder.restriction("device", true, "Device", "device");
		builder.restriction("effective-time", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "effective");
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("medication", true, "Medication", "medicationReference");
		builder.restriction("not-given", true, QueryBuilder.TYPE_BOOLEAN, "notGiven");
		builder.restriction("performer", true, null, "performer.actor");
		builder.restriction("prescription", true, "MedicationRequest", "prescription");
		builder.restriction("reason-given", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "reasonCode");
		builder.restriction("reason-not-given", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "reasonNotGiven");
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");

		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam MedicationAdministration theMedicationAdministration) {
		return super.createResource(theMedicationAdministration);
	}

	@Override
	public String getRecordFormat() {	
		return "fhir/MedicationAdministration";
	}
	

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId,
			@ResourceParam MedicationAdministration theMedicationAdministration) {
		return super.updateResource(theId, theMedicationAdministration);
	}

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type
	// "content" set.
	// b) Each record must have a "name" that will be shown to the user in the
	// record tree.
	// The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR
	// representation
	public void prepare(Record record, MedicationAdministration theMedicationAdministration) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a
		// fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, null, "MedicationAdministration"); 
																								

		// Task b : Create record name
		String date = "No time";
		if (theMedicationAdministration.hasEffective()) {
			try {
				// TODO Fails with DateTimePeriod
				date = FHIRTools.stringFromDateTime(theMedicationAdministration.getEffectiveDateTimeType() != null
						? theMedicationAdministration.getEffectiveDateTimeType()
						: theMedicationAdministration.getEffectivePeriod().getStartElement());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;

		// Task c : Set record owner based on subject and clean subject if this was
		// possible
		Reference subjectRef = theMedicationAdministration.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef))
			theMedicationAdministration.setSubject(null);

		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta"
		// section
		clean(theMedicationAdministration);

	}

	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, MedicationAdministration p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getSubject().isEmpty()) {
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
}
