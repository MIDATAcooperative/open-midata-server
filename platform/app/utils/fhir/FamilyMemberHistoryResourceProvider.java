package utils.fhir;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.FamilyMemberHistory;
import org.hl7.fhir.r4.model.IdType;
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
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;

// TODO: Choose the correct super class and register in utils.fhir.FHIRServlet

public class FamilyMemberHistoryResourceProvider extends RecordBasedResourceProvider<FamilyMemberHistory>
		implements IResourceProvider {

	public FamilyMemberHistoryResourceProvider() {
		searchParamNameToPathMap.put("FamilyMemberHistory:instantiates-canonical", "instantiatesCanonical");
				
		searchParamNameToPathMap.put("FamilyMemberHistory:patient", "patient");
		searchParamNameToTypeMap.put("FamilyMemberHistory:patient", Sets.create("Patient"));

		registerSearches("FamilyMemberHistory", getClass(), "getFamilyMemberHistory");
	}

	@Override
	public Class<FamilyMemberHistory> getResourceType() {
		return FamilyMemberHistory.class;
	}

	@Search()
	public Bundle getFamilyMemberHistory(
			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,

			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,

 			@Description(shortDefinition="A search by a condition code")
  			@OptionalParam(name="code")
  			TokenAndListParam theCode,
    
  			@Description(shortDefinition="When history was recorded or last updated")
  			@OptionalParam(name="date")
  			DateAndListParam theDate, 
    
  			@Description(shortDefinition="A search by a record identifier")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="Instantiates FHIR protocol or definition")
  			@OptionalParam(name="instantiates-canonical", targetTypes={  } )
  			ReferenceAndListParam theInstantiates_canonical, 
    
  			@Description(shortDefinition="Instantiates external protocol or definition")
  			@OptionalParam(name="instantiates-uri")
  			UriAndListParam theInstantiates_uri, 
    
  			@Description(shortDefinition="The identity of a subject to list family member history items for")
  			@OptionalParam(name="patient", targetTypes={  } )
  			ReferenceAndListParam thePatient, 
    
  			@Description(shortDefinition="A search by a relationship type")
  			@OptionalParam(name="relationship")
  			TokenAndListParam theRelationship,
    
  			@Description(shortDefinition="A search by a sex code of a family member")
  			@OptionalParam(name="sex")
  			TokenAndListParam theSex,
    
 			@Description(shortDefinition="partial | completed | entered-in-error | health-unknown")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
 
 			@RawParam
 			Map<String, List<String>> theAdditionalRawParams,
 
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"FamilyMemberHistory:instantiates-canonical" ,
 					"FamilyMemberHistory:patient" ,
 					"*"
 			}) 
 			Set<Include> theIncludes,

			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,

			@OptionalParam(name = "_page") StringParam _page,

			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);
		paramMap.add("_language", the_language);
		paramMap.add("code", theCode);
		paramMap.add("date", theDate);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("instantiates-canonical", theInstantiates_canonical);
		paramMap.add("instantiates-uri", theInstantiates_uri);
		paramMap.add("patient", thePatient);
		paramMap.add("relationship", theRelationship);
		paramMap.add("sex", theSex);
		paramMap.add("status", theStatus);
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/FamilyMemberHistory");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "patient");

		// Add handling for search on the field that determines the MIDATA content type
		// used.
		// TODO builder.recordCodeRestriction("code", "code");

		builder.restriction("code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "condition.code");
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "date");
		builder.restriction("instantiates-canonical", true, null, "instantiatesCcanonical");
		builder.restriction("sex", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "sex");
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("relationship", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "relationship");
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");

		return query.execute(info);
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam FamilyMemberHistory theFamilyMemberHistory) {
		return super.createResource(theFamilyMemberHistory);
	}

	@Override
	public String getRecordFormat() {	
		return "fhir/FamilyMemberHistory";
	}	
	

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId,
			@ResourceParam FamilyMemberHistory theFamilyMemberHistory) {
		return super.updateResource(theId, theFamilyMemberHistory);
	}

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type
	// "content" set.
	// b) Each record must have a "name" that will be shown to the user in the
	// record tree.
	// The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR
	// representation
	public void prepare(Record record, FamilyMemberHistory theFamilyMemberHistory) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a
		// fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, null, "FamilyMemberHistory"); // TODO check code line

		// Task b : Create record name
		String date = "No time";
		if (theFamilyMemberHistory.hasDateElement()) {
			try {
				date = FHIRTools.stringFromDateTime(theFamilyMemberHistory.getDateElement());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;

		// Task c : Set record owner based on subject and clean subject if this was
		// possible
		Reference subjectRef = theFamilyMemberHistory.getPatient(); // TODO is it currect to use patient?
		if (cleanAndSetRecordOwner(record, subjectRef))
			theFamilyMemberHistory.setPatient(null);

		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta"
		// section
		clean(theFamilyMemberHistory);

	}

	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, FamilyMemberHistory p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getPatient().isEmpty()) {
			p.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName)); // TODO is it currect to use patient?
		}
	}

	@Override
	protected void convertToR4(Object in) {
		// No action
		
	}
}
