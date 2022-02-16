/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import utils.access.pseudo.FhirPseudonymizer;
import utils.access.AccessContext;
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
		
		searchParamNameToPathMap.put("Immunization:performer", "performer.actor");
		searchParamNameToTypeMap.put("Immunization:performer", Sets.create("Practitioner", "Organization", "PractitionerRole"));
		
		searchParamNameToPathMap.put("Immunization:reaction", "reaction.detail");
		searchParamNameToTypeMap.put("Immunization:reaction", Sets.create("Observation"));
		
		searchParamNameToPathMap.put("Immunization:reason-reference", ".reasonReference");
		searchParamNameToTypeMap.put("Immunization:reason-reference", Sets.create("Condition", "Observation", "DiagnosticReport"));

		registerSearches("Immunization", getClass(), "getImmunization");
		
		FhirPseudonymizer.forR4()
		  .reset("Immunization")			  
		  .pseudonymizeReference("Immunization", "note", "authorReference");
	}

	@Override
	public Class<Immunization> getResourceType() {
		return Immunization.class;
	}

	@Search()
	public Bundle getImmunization(
			@Description(shortDefinition = "The ID of the resource") @OptionalParam(name = "_id") TokenAndListParam the_id,

			@Description(shortDefinition = "The language of the resource") @OptionalParam(name = "_language") StringAndListParam the_language,

  			@Description(shortDefinition="Vaccination  (non)-Administration Date")
  			@OptionalParam(name="date")
  			DateAndListParam theDate, 
    
  			@Description(shortDefinition="Business identifier")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="The service delivery location or facility in which the vaccine was / was to be administered")
  			@OptionalParam(name="location", targetTypes={  } )
  			ReferenceAndListParam theLocation, 
    
  			@Description(shortDefinition="Vaccine Lot Number")
  			@OptionalParam(name="lot-number")
  			StringAndListParam theLot_number, 
    
  			@Description(shortDefinition="Vaccine Manufacturer")
  			@OptionalParam(name="manufacturer", targetTypes={  } )
  			ReferenceAndListParam theManufacturer, 
    
  			@Description(shortDefinition="The patient for the vaccination record")
  			@OptionalParam(name="patient", targetTypes={  } )
  			ReferenceAndListParam thePatient, 
    
  			@Description(shortDefinition="The practitioner or organization who played a role in the vaccination")
  			@OptionalParam(name="performer", targetTypes={  } )
  			ReferenceAndListParam thePerformer, 
    
  			@Description(shortDefinition="Additional information on reaction")
  			@OptionalParam(name="reaction", targetTypes={  } )
  			ReferenceAndListParam theReaction, 
    
 			@Description(shortDefinition="When reaction started")
 			@OptionalParam(name="reaction-date")
			DateAndListParam theReaction_date, 
   
 			@Description(shortDefinition="Reason why the vaccine was administered")
 			@OptionalParam(name="reason-code")
 			TokenAndListParam theReason_code,
   
 			@Description(shortDefinition="Why immunization occurred")
 			@OptionalParam(name="reason-reference", targetTypes={  } )
 			ReferenceAndListParam theReason_reference, 
   
 			@Description(shortDefinition="The series being followed by the provider")
 			@OptionalParam(name="series")
 			StringAndListParam theSeries, 
   
 			@Description(shortDefinition="Immunization event status")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="Reason why the vaccine was not administered")
 			@OptionalParam(name="status-reason")
 			TokenAndListParam theStatus_reason,
   
 			@Description(shortDefinition="The target disease the dose is being administered against")
 			@OptionalParam(name="target-disease")
 			TokenAndListParam theTarget_disease,
   
 			@Description(shortDefinition="Vaccine Product Administered")
 			@OptionalParam(name="vaccine-code")
 			TokenAndListParam theVaccine_code,
  			
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"Immunization:location" ,
 					"Immunization:manufacturer" ,
 					"Immunization:patient" ,
 					"Immunization:performer" ,
 					"Immunization:reaction" ,
 					"Immunization:reason-reference" ,
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
		paramMap.add("date", theDate);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("location", theLocation);
		paramMap.add("lot-number", theLot_number);
		paramMap.add("manufacturer", theManufacturer);
		paramMap.add("patient", thePatient);
		paramMap.add("performer", thePerformer);
		paramMap.add("reaction", theReaction);
		paramMap.add("reaction-date", theReaction_date);
		paramMap.add("reason-code", theReason_code);
		paramMap.add("reason-reference", theReason_reference);
		paramMap.add("series", theSeries);
		paramMap.add("status", theStatus);
		paramMap.add("status-reason", theStatus_reason);
		paramMap.add("target-disease", theTarget_disease);
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
		AccessContext info = info();

		Query query = new Query();
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Immunization");

		builder.handleIdRestriction();

		// Add handling for search on the "owner" of the record.
		builder.recordOwnerReference("patient", "Patient", "patient"); 

		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "occurrenceDateTime");		
		
		builder.restriction("location", true, "Location", "location");
		builder.restriction("lot-number", true, QueryBuilder.TYPE_STRING, "lotNumber");
		builder.restriction("manufacturer", true, "Organization", "manufacturer");
		builder.restriction("performer", true, null, "performer.actor");				
		builder.restriction("practitioner", true, "Practitioner", "practitioner.actor");
		builder.restriction("reaction", true, "Observation", "reaction.detail");
		builder.restriction("reaction-date", true, QueryBuilder.TYPE_DATETIME, "reaction.date");
		builder.restriction("reason", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "explanation.reason");
		builder.restriction("reason-code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "reasonCode");
		builder.restriction("reason-reference", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "reasonReference");
		builder.restriction("series", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "protocolApplied.series");
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");
		builder.restriction("status-reason", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "statusReason");
		builder.restriction("target-disease", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "protocolApplied.targetDisease");
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

	@Override
	protected void convertToR4(Object in) {
		FHIRVersionConvert.rename(in, "date", "occurenceDateTime");
		
	}
}
