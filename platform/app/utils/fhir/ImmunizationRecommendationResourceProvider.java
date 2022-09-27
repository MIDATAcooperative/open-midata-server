package utils.fhir;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Reference;

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

public class ImmunizationRecommendationResourceProvider extends RecordBasedResourceProvider<ImmunizationRecommendation> implements IResourceProvider {

	public ImmunizationRecommendationResourceProvider() {
		searchParamNameToPathMap.put("ImmunizationRecommendation:information", "recommendation.supportingPatientInformation");
		
		searchParamNameToPathMap.put("ImmunizationRecommendation:support", "recommendation.supportingImmunization");
		searchParamNameToTypeMap.put("ImmunizationRecommendation:support", Sets.create("Immunization", "ImmunizationEvaluation"));

		searchParamNameToPathMap.put("ImmunizationRecommendation:patient", "patient");
		searchParamNameToTypeMap.put("ImmunizationRecommendation:patient", Sets.create("Patient"));
		
		registerSearches("ImmunizationRecommendation", getClass(), "getImmunizationRecommendation");

		FhirPseudonymizer.forR4().reset("ImmunizationRecommendation");
	}

	@Override
	public Class<ImmunizationRecommendation> getResourceType() {
		return ImmunizationRecommendation.class;
	}

	@Search()
	public Bundle getImmunizationRecommendation(@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,
			
			@Description(shortDefinition="Date recommendation(s) created")
 			@OptionalParam(name="date")
			DateAndListParam theDate, 

			@Description(shortDefinition="Business identifier")
 			@OptionalParam(name="identifier")
 			TokenAndListParam theIdentifier,
			
 			@Description(shortDefinition="Patient observations supporting recommendation")
 			@OptionalParam(name="information", targetTypes={  } )
 			ReferenceAndListParam theInformation, 	
			
 			@Description(shortDefinition="Who this profile is for")
 			@OptionalParam(name="patient", targetTypes={  } )
 			ReferenceAndListParam thePatient, 
			
 			@Description(shortDefinition="Vaccine recommendation status")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
			
 			@Description(shortDefinition="Past immunizations supporting recommendation")
 			@OptionalParam(name="support", targetTypes={  } )
 			ReferenceAndListParam theSupport, 
			
 			@Description(shortDefinition="Disease to be immunized against")
 			@OptionalParam(name="target-disease")
 			TokenAndListParam theTargetDisease,
 			
 			@Description(shortDefinition="Vaccine or vaccine group recommendation applies to")
 			@OptionalParam(name="vaccine-type")
 			TokenAndListParam theVaccineType,
 							
			@IncludeParam(reverse = true) Set<Include> theRevIncludes,
			@Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(name = "_lastUpdated") DateRangeParam theLastUpdated,

			@IncludeParam(allow = { "ImmunizationRecommendation:information", 
					                "ImmunizationRecommendation:patient", 
					                "ImmunizationRecommendation:support",
					                "*" }) Set<Include> theIncludes,
			@Sort SortSpec theSort,

			@ca.uhn.fhir.rest.annotation.Count Integer theCount,

			@OptionalParam(name = "_page") StringParam _page,

			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);		
				
		paramMap.add("date", theDate);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("information", theInformation);
		paramMap.add("patient", thePatient);	
		paramMap.add("status", theStatus);	
		paramMap.add("support", theSupport);
		paramMap.add("target-disease", theTargetDisease);
		paramMap.add("vaccine-type", theVaccineType);
						
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		paramMap.setFrom(_page != null ? _page.getValue() : null);

		return searchBundle(paramMap, theDetails);

	}

	public Query buildQuery(SearchParameterMap params) throws AppException {
		info();

		Query query = new Query();
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/ImmunizationRecommendation");

		builder.handleIdRestriction();

		// Add handling for search on the "owner" of the record.
		builder.recordOwnerReference("patient", "Patient", "patient");

		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");

		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "date");
        builder.restriction("information", true, null, "recommendation.supportingPatientInformation");
		builder.restriction("status", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "recommendation.forecastStatus");
        builder.restriction("support", true, null, "recommendation.supportingImmunization");
	    builder.restriction("target-disease", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "recommendation.targetDisease");
	    builder.restriction("vaccine-type", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "recommendation.vaccineCode");
		
		return query;
	}

	// This method is required if it is allowed to create the resource.
	// Just change the resource type
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam ImmunizationRecommendation theImmunizationRecommendation) {
		return super.createResource(theImmunizationRecommendation);
	}

	@Override
	public String getRecordFormat() {
		return "fhir/ImmunizationRecommendation";
	}

	// This method is required if it is allowed to update the resource.
	// Just change the resource type
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam ImmunizationRecommendation theImmunizationRecommendation) {
		return super.updateResource(theId, theImmunizationRecommendation);
	}

	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical
	// type
	// "content" set.
	// b) Each record must have a "name" that will be shown to the user in the
	// record tree.
	// The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the
	// FHIR
	// representation
	public void prepare(Record record, ImmunizationRecommendation theImmunizationRecommendation) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource
		// (or a
		// fixed value or something else useful)
		String display = setRecordCodeByCodeableConcept(record, null, "ImmunizationRecommendation");

		// Task b : Create record name
		String date = "No time";
		if (theImmunizationRecommendation.hasDate()) {
			try {
				date = FHIRTools.stringFromDateTime(theImmunizationRecommendation.getDateElement());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process date");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;

		// Task c : Set record owner based on subject and clean subject if this
		// was
		// possible
		Reference subjectRef = theImmunizationRecommendation.getPatient();
		if (cleanAndSetRecordOwner(record, subjectRef))
			theImmunizationRecommendation.setPatient(null);

		// Other cleaning tasks: Remove _id from FHIR representation and remove
		// "meta"
		// section
		clean(theImmunizationRecommendation);

	}

	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, ImmunizationRecommendation p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);

		// Add subject field from record owner field if it is not already there
		if (p.getPatient().isEmpty()) {
			p.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	protected void convertToR4(Object in) {

	}
}
