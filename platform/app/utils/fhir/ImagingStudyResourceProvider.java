package utils.fhir;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ImagingStudy;
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
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;


public class ImagingStudyResourceProvider extends RecordBasedResourceProvider<ImagingStudy>
		implements IResourceProvider {

	public ImagingStudyResourceProvider() {
		searchParamNameToPathMap.put("ImagingStudy:basedon", "basedOn");
		searchParamNameToTypeMap.put("ImagingStudy:basedon", Sets.create("ReferralRequest", "CarePlan", "ProcedureRequest"));
		
		searchParamNameToPathMap.put("ImagingStudy:context", "context");
		searchParamNameToTypeMap.put("ImagingStudy:context", Sets.create("EpisodeOfCare", "Encounter"));
		
		searchParamNameToPathMap.put("ImagingStudy:endpoint", "endpoint"); // TODO ImagingStudy.endpoint | ImagingStudy.series.endpoint
		searchParamNameToTypeMap.put("ImagingStudy:endpoint", Sets.create("Endpoint"));
		
		searchParamNameToPathMap.put("ImagingStudy:patient", "patient");
		searchParamNameToTypeMap.put("ImagingStudy:patient", Sets.create("Patient"));
		
		searchParamNameToPathMap.put("ImagingStudy:performer", "series.performer");
		searchParamNameToTypeMap.put("ImagingStudy:performer", Sets.create("Practitioner"));

		registerSearches("ImagingStudy", getClass(), "getImagingStudy");
	}

	@Override
	public Class<ImagingStudy> getResourceType() {
		return ImagingStudy.class;
	}

	@Search()
	public Bundle getImagingStudy(
			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,

			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,

			@Description(shortDefinition = "The accession identifier for the study") @OptionalParam(name = "accession") TokenAndListParam theAccession,

			@Description(shortDefinition = "The order for the image") @OptionalParam(name = "basedon", targetTypes = {}) ReferenceAndListParam theBasedon,

			@Description(shortDefinition = "The body site studied") @OptionalParam(name = "bodysite") TokenAndListParam theBodysite,

			@Description(shortDefinition = "The context of the study") @OptionalParam(name = "context", targetTypes = {}) ReferenceAndListParam theContext,

			@Description(shortDefinition = "The type of the instance") @OptionalParam(name = "dicom-class") UriAndListParam theDicom_class,

			@Description(shortDefinition = "The endpoint for te study or series") @OptionalParam(name = "endpoint", targetTypes = {}) ReferenceAndListParam theEndpoint,

			@Description(shortDefinition = "Other identifiers for the Study") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier,

			@Description(shortDefinition = "The modality of the series") @OptionalParam(name = "modality") TokenAndListParam theModality,

			@Description(shortDefinition = "Who the study is about") @OptionalParam(name = "patient", targetTypes = {}) ReferenceAndListParam thePatient,

			@Description(shortDefinition = "The person who performed the study") @OptionalParam(name = "performer", targetTypes = {}) ReferenceAndListParam thePerformer,

			@Description(shortDefinition = "The reason for the study") @OptionalParam(name = "reason") TokenAndListParam theReason,

			@Description(shortDefinition = "The identifier of the series of images") @OptionalParam(name = "series") UriAndListParam theSeries,

			@Description(shortDefinition = "When the study was started") @OptionalParam(name = "started") DateAndListParam theStarted,

			@Description(shortDefinition = "The study identifier for the image") @OptionalParam(name = "study") UriAndListParam theStudy,

			@Description(shortDefinition = "The instance unique identifier") @OptionalParam(name = "uid") UriAndListParam theUid,

			@RawParam Map<String, List<String>> theAdditionalRawParams,

			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,

			@IncludeParam(allow = { "ImagingStudy:basedon", "ImagingStudy:context", "ImagingStudy:endpoint",
					"ImagingStudy:patient", "ImagingStudy:performer", "*" }) Set<Include> theIncludes,

			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,

			@OptionalParam(name = "_page") StringParam _page,

			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);
		paramMap.add("_language", the_language);
		paramMap.add("accession", theAccession);
		paramMap.add("basedon", theBasedon);
		paramMap.add("bodysite", theBodysite);
		paramMap.add("context", theContext);
		paramMap.add("dicom-class", theDicom_class);
		paramMap.add("endpoint", theEndpoint);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("modality", theModality);
		paramMap.add("patient", thePatient);
		paramMap.add("performer", thePerformer);
		paramMap.add("reason", theReason);
		paramMap.add("series", theSeries);
		paramMap.add("started", theStarted);
		paramMap.add("study", theStudy);
		paramMap.add("uid", theUid);

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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/ImagingStudy");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "patient");
	
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");

		builder.restriction("accession", true, QueryBuilder.TYPE_IDENTIFIER, "accession");
		builder.restriction("basedon", true, null, "basedOn");
		builder.restriction("bodysite", true, QueryBuilder.TYPE_CODING, "series.bodySite");
		builder.restriction("context", true, null, "context");
		builder.restriction("dicom-class", true, QueryBuilder.TYPE_URI, "series.instance.sopClass");
		builder.restriction("endpoint", true, "Endpoint", "series.endpoint"); 
		
		builder.restriction("modality", true, QueryBuilder.TYPE_CODING, "series.modality");		
		builder.restriction("performer", true, "Practitioner", "series.performer");
		builder.restriction("reason", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "reason");
		builder.restriction("series", true, QueryBuilder.TYPE_URI, "series.uid");
		builder.restriction("started", true, QueryBuilder.TYPE_DATETIME, "started");
		builder.restriction("study", true, QueryBuilder.TYPE_URI, "uid");
		builder.restriction("uid", true, QueryBuilder.TYPE_URI, "series.instance.uid");

		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam ImagingStudy theImagingStudy) {
		return super.createResource(theImagingStudy);
	}

	@Override
	public String getRecordFormat() {	
		return "fhir/ImagingStudy";
	}
	

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam ImagingStudy theImagingStudy) {
		return super.updateResource(theId, theImagingStudy);
	}

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type
	// "content" set.
	// b) Each record must have a "name" that will be shown to the user in the
	// record tree.
	// The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR
	// representation
	public void prepare(Record record, ImagingStudy theImagingStudy) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a
		// fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, null, "ImagingStudy");

		// Task b : Create record name
		String date = "No time";
		if (theImagingStudy.hasStarted()) {
			try {
				date = FHIRTools.stringFromDateTime(theImagingStudy.getStartedElement());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;

		// Task c : Set record owner based on subject and clean subject if this was
		// possible
		Reference subjectRef = theImagingStudy.getPatient(); // TODO correct to use patient?
		if (cleanAndSetRecordOwner(record, subjectRef))
			theImagingStudy.setPatient(null);

		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta"
		// section
		clean(theImagingStudy);

	}

	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, ImagingStudy p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getPatient().isEmpty()) {  // TODO correct to use patient?
			p.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
}
