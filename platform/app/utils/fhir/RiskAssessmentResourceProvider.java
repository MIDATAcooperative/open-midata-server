package utils.fhir;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RiskAssessment;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
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
import utils.access.pseudo.FhirPseudonymizer;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class RiskAssessmentResourceProvider extends RecordBasedResourceProvider<RiskAssessment> implements IResourceProvider {

	// Provide one default constructor
	public RiskAssessmentResourceProvider() {
		
		// For each existing search parameter that has a "reference" type add one line:
		// searchParamNameToPathMap.put("Resource:search-name", "path from search specification");
		searchParamNameToPathMap.put("RiskAssessment:condition", "condition");
		searchParamNameToTypeMap.put("RiskAssessment:condition", Sets.create("Condition"));
		
		searchParamNameToPathMap.put("RiskAssessment:encounter", "encounter");
		searchParamNameToTypeMap.put("RiskAssessment:encounter", Sets.create("Encounter"));
				
		searchParamNameToPathMap.put("RiskAssessment:patient", "subject");				
		searchParamNameToTypeMap.put("RiskAssessment:patient", Sets.create("Patient"));				
		searchParamNameToPathMap.put("RiskAssessment:subject", "subject");				
		searchParamNameToTypeMap.put("RiskAssessment:subject", Sets.create("Patient", "Group"));
		searchParamNameToPathMap.put("RiskAssessment:performer", "performer");
		searchParamNameToTypeMap.put("RiskAssessment:performer", Sets.create("Practitioner", "Device", "PractitionerRole"));
				
		// Use name of @Search function as last parameter
		registerSearches("RiskAssessment", getClass(), "getRiskAssessment");
		
		FhirPseudonymizer.forR4()
		  .reset("RiskAssessment")		
		  .pseudonymizeReference("RiskAssessment", "performer")
		  .pseudonymizeReference("RiskAssessment", "note", "authorReference")
		  ;		  
	}
	
	// Return corresponding FHIR class
	@Override
	public Class<RiskAssessment> getResourceType() {
		return RiskAssessment.class;
	}

	// Main search function for resource. May have another name.
	// Copy method signature from happy fhir implementation.
	// Look at http://hapifhir.io/xref-jpaserver/ca/uhn/fhir/jpa/rp/dstu3/ObservationResourceProvider.html
	// Throw out unsupported search parameters (like "_has" and all starting with "PARAM_" )
	// Replace DateRangeParam with DateAndListParam everywhere except for _lastUpdated	
	// Add non FHIR _page parameter used for the pagination mechanism
	@Search()
	public Bundle getRiskAssessment(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

																		
			@Description(shortDefinition="Condition assessed")
  			@OptionalParam(name="condition", targetTypes={  } )
  			ReferenceAndListParam theCondition, 
  			
  			@Description(shortDefinition="When was assessment made?")
 			@OptionalParam(name="date")
			DateAndListParam theDate, 
			
			@Description(shortDefinition="Where was assessment performed?")
 			@OptionalParam(name="encounter", targetTypes={  } )
 			ReferenceAndListParam theEncounter, 
 			
 			@Description(shortDefinition="Unique identifier for the assessment")
 			@OptionalParam(name="identifier")
 			TokenAndListParam theIdentifier,
 			
 			@Description(shortDefinition="Evaluation mechanism")
 			@OptionalParam(name="method")
 			TokenAndListParam theMethod,
 			
 			@Description(shortDefinition="Who/what does assessment apply to?")
 			@OptionalParam(name="patient", targetTypes={  } )
 			ReferenceAndListParam thePatient, 
   
 			@Description(shortDefinition="Who did assessment?")
 			@OptionalParam(name="performer", targetTypes={  } )
 			ReferenceAndListParam thePerformer,
 			
 			@Description(shortDefinition="Likelihood of specified outcome")
 			@OptionalParam(name="probability")
 			NumberAndListParam theProbability,
 			
 			@Description(shortDefinition="Likelihood of specified outcome as a qualitative value")
 			@OptionalParam(name="risk")
 			TokenAndListParam theRisk,
 			
 			@Description(shortDefinition="Who/what does assessment apply to?")
 			@OptionalParam(name="subject", targetTypes={  } )
 			ReferenceAndListParam theSubject, 
      			  	
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"RiskAssessment:condition" ,
 					"RiskAssessment:encounter" ,
 					"RiskAssessment:patient" ,
 					"RiskAssessment:performer" ,
 					"RiskAssessment:subject" , 					
 					"*"
 			}) 
 			Set<Include> theIncludes,						
								
			@Sort SortSpec theSort,		
			
			@ca.uhn.fhir.rest.annotation.Count Integer theCount,
			
			// Non FHIR parameter used for pagination
			@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails

	) throws AppException {

		// The implementation of this method may also be copied from happy fhir except for the last lines
		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
	
		paramMap.add("condition", theCondition);
		paramMap.add("date", theDate);
		paramMap.add("encounter", theEncounter);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("method", theMethod);
		paramMap.add("patient", thePatient);
		paramMap.add("performer", thePerformer);
		paramMap.add("probability", theProbability);
		paramMap.add("risk", theRisk);
		paramMap.add("subject", theSubject);
							
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		
		// The last lines are different than the happy fhir version
		paramMap.setFrom(_page != null ? _page.getValue() : null);
		return searchBundle(paramMap, theDetails);
		
	}
	
	public Query buildQuery(SearchParameterMap params) throws AppException {
		
		// get execution context (which user, which app)
		info();

		// construct empty query and a builder for that query
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/RiskAssessment");

		// Now all possible searches need to be handeled. For performance reasons it makes sense
		// to put searches that are very restrictive and frequently used first in order
	
		// Add default handling for the _id search parameter
		builder.handleIdRestriction();
		
		// Add handling for search on the "owner" of the record. 
		builder.recordOwnerReference("patient", "Patient", "subject");
				
		// Add handling for search on the field that determines the MIDATA content type used.
        builder.recordCodeRestriction("code", "code");
			
                               	              	      
        // Add handling for a multiTYPE search: "date" may be search on effectiveDateTime or effectivePeriod
        // Note that path = "effective" and type = TYPE_DATETIME_OR_PERIOD
        // If the search was only on effectiveDateTime then
        // type would be TYPE_DATETIME and path would be "effectiveDateTime" instead
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "occurence");
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
        builder.restriction("encounter", true, "Encounter", "encounter");
        builder.restriction("performer", true, "Performer", "performer");
        builder.restriction("condition", true, "Condition", "condition");
        builder.restriction("method", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "method");
        builder.restriction("risk", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "prediction.qualitativeRisk");
        builder.restriction("probability", true, QueryBuilder.TYPE_DECIMAL_OR_RANGE, "prediction.probability");
        
		// On some resources there are searches for "patient" and "subject" which are 
		// searches on the same field. patient is always the record owner
		// subject may be something different than a person and may not be the record owner
		// in that case. Add a record owner search if possible and a normal search otherwise:		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
							
		return query;
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam RiskAssessment theRiskAssessment) {
		return super.createResource(theRiskAssessment);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/RiskAssessment";
	}
		

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam RiskAssessment theRiskAssessment) {
		return super.updateResource(theId, theRiskAssessment);
	}		

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type "content" set. 
	// b) Each record must have a "name" that will be shown to the user in the record tree.
	//    The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR representation
	public void prepare(Record record, RiskAssessment theRiskAssessment) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, theRiskAssessment.getCode(), "RiskAssessment");
		
		// Task b : Create record name
		String date = "No time";		
		if (theRiskAssessment.hasOccurrenceDateTimeType()) {
			try {
				date = FHIRTools.stringFromDateTime(theRiskAssessment.getOccurrenceDateTimeType());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process occurenceDateTime");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;		

		// Task c : Set record owner based on subject and clean subject if this was possible
		Reference subjectRef = theRiskAssessment.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theRiskAssessment.setSubject(null);
		
		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta" section
		clean(theRiskAssessment);
 
	}	
 
	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, RiskAssessment p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);
		
		// Add subject field from record owner field if it is not already there
		if (p.getSubject().isEmpty()) {			
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
	
	
	@Override
	protected void convertToR4(Object in) {
			
	}
	

}