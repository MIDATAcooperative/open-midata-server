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
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Procedure;
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
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import utils.access.pseudo.FhirPseudonymizer;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class ProcedureResourceProvider extends RecordBasedResourceProvider<Procedure> implements IResourceProvider {

	public ProcedureResourceProvider() {
						
		searchParamNameToPathMap.put("Procedure:based-on" ,"basedOn");
		searchParamNameToTypeMap.put("Procedure:based-on" , Sets.create("CarePlan", "ServiceRequest"));
		searchParamNameToPathMap.put("Procedure:instantiates-canonical" , "instantiatesCanonical");
		searchParamNameToTypeMap.put("Procedure:instantiates-canonical" , Sets.create("Questionnaire", "Measure", "PlanDefinition", "OperationDefinition", "ActivityDefinition"));		
		searchParamNameToPathMap.put("Procedure:encounter" , "context");
		searchParamNameToTypeMap.put("Procedure:encounter" , Sets.create("Encounter"));
		searchParamNameToPathMap.put("Procedure:location" , "location");
		searchParamNameToTypeMap.put("Procedure:location" , Sets.create("Location"));
		searchParamNameToPathMap.put("Procedure:part-of" , "partOf");
		searchParamNameToTypeMap.put("Procedure:part-of" , Sets.create("Observation", "Procedure", "MedicationAdministration"));
		searchParamNameToPathMap.put("Procedure:patient" , "subject");
		searchParamNameToTypeMap.put("Procedure:patient" , Sets.create("Patient"));
		searchParamNameToPathMap.put("Procedure:performer" , "performer.actor");
		searchParamNameToTypeMap.put("Procedure:performer" , Sets.create("Practitioner", "Organization", "Device", "Patient", "RelatedPerson", "PractitionerRole"));
		searchParamNameToPathMap.put("Procedure:subject" , "subject");
		searchParamNameToTypeMap.put("Procedure:subject" , Sets.create("Patient", "Group"));
		
		registerSearches("Procedure", getClass(), "getProcedure");
		
		FhirPseudonymizer.forR4()
		  .reset("Procedure")	
		  .hideIfPseudonymized("Procedure", "text")
		  .pseudonymizeReference("Procedure", "recorder")
		  .pseudonymizeReference("Procedure", "asserter")
		  .pseudonymizeReference("Procedure", "performer", "actor")
		  .pseudonymizeReference("Procedure", "note", "authorReference");
	}
	
	@Override
	public Class<Procedure> getResourceType() {
		return Procedure.class;
	}

	@Search()
	public Bundle getProcedure(

  			@Description(shortDefinition="The ID of the resource")
  			@OptionalParam(name="_id")
  			TokenAndListParam the_id, 
      	   
  			@Description(shortDefinition="A request for this procedure")
  			@OptionalParam(name="based-on", targetTypes={  } )
  			ReferenceAndListParam theBased_on, 
    
  			@Description(shortDefinition="Classification of the procedure")
  			@OptionalParam(name="category")
  			TokenAndListParam theCategory,
    
  			@Description(shortDefinition="A code to identify a  procedure")
  			@OptionalParam(name="code")
  			TokenAndListParam theCode,
    
  			@Description(shortDefinition="When the procedure was performed")
  			@OptionalParam(name="date")
  			DateAndListParam theDate, 
    
  			@Description(shortDefinition="Encounter created as part of")
  			@OptionalParam(name="encounter", targetTypes={  } )
  			ReferenceAndListParam theEncounter, 
    
  			@Description(shortDefinition="A unique identifier for a procedure")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="Instantiates FHIR protocol or definition")
  			@OptionalParam(name="instantiates-canonical", targetTypes={  } )
  			ReferenceAndListParam theInstantiates_canonical, 
    
  			@Description(shortDefinition="Instantiates external protocol or definition")
  			@OptionalParam(name="instantiates-uri")
  			UriAndListParam theInstantiates_uri, 
    
 			@Description(shortDefinition="Where the procedure happened")
 			@OptionalParam(name="location", targetTypes={  } )
 			ReferenceAndListParam theLocation, 
   
 			@Description(shortDefinition="Part of referenced event")
 			@OptionalParam(name="part-of", targetTypes={  } )
 			ReferenceAndListParam thePart_of, 
   
 			@Description(shortDefinition="Search by subject - a patient")
 			@OptionalParam(name="patient", targetTypes={  } )
 			ReferenceAndListParam thePatient, 
   
 			@Description(shortDefinition="The reference to the practitioner")
 			@OptionalParam(name="performer", targetTypes={  } )
 			ReferenceAndListParam thePerformer, 
   
 			@Description(shortDefinition="Coded reason procedure performed")
 			@OptionalParam(name="reason-code")
 			TokenAndListParam theReason_code,
   
 			@Description(shortDefinition="The justification that the procedure was performed")
 			@OptionalParam(name="reason-reference", targetTypes={  } )
 			ReferenceAndListParam theReason_reference, 
   
 			@Description(shortDefinition="preparation | in-progress | not-done | suspended | aborted | completed | entered-in-error | unknown")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="Search by subject")
 			@OptionalParam(name="subject", targetTypes={  } )
 			ReferenceAndListParam theSubject,  		
 
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"Procedure:based-on" ,
 					"Procedure:encounter" ,
 					"Procedure:instantiates-canonical" ,
 					"Procedure:location" ,
 					"Procedure:part-of" ,
 					"Procedure:patient" ,
 					"Procedure:performer" ,
 					"Procedure:reason-reference" ,
 					"Procedure:subject" ,
 					"*"
 			}) 
 			Set<Include> theIncludes,
		
			@Sort 
			SortSpec theSort,
			
			@ca.uhn.fhir.rest.annotation.Count
			Integer theCount,
			
			@OptionalParam(name="_page")
			StringParam _page,
			
			RequestDetails theDetails

	) throws AppException {

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("_id", the_id);		
		paramMap.add("based-on", theBased_on);
		paramMap.add("category", theCategory);
		paramMap.add("code", theCode);
		paramMap.add("date", theDate);
		paramMap.add("encounter", theEncounter);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("instantiates-canonical", theInstantiates_canonical);
		paramMap.add("instantiates-uri", theInstantiates_uri);
		paramMap.add("location", theLocation);
		paramMap.add("part-of", thePart_of);
		paramMap.add("patient", thePatient);
		paramMap.add("performer", thePerformer);
		paramMap.add("reason-code", theReason_code);
		paramMap.add("reason-reference", theReason_reference);
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

	public Query buildQuery(SearchParameterMap params) throws AppException {
		info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Procedure");

		builder.handleIdRestriction();
		
		builder.recordOwnerReference("patient", "Patient", "subject");
		
			

		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "code");
		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
	
		builder.restriction("based-on", true, null, "basedOn");
		builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");	
				
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "performed");
		
		builder.restriction("encounter", true, "Encounter", "encounter");	
		builder.restriction("instantiates-canonical", true, null, "instantiatesCanonical");
		builder.restriction("instantiates-uri", true, QueryBuilder.TYPE_URI, "instantiatesUri");
		builder.restriction("location", true, "Location", "location");
		builder.restriction("part-of", true, null, "partOf");	
		builder.restriction("performer", true, null, "performer.actor");
		builder.restriction("reason-code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "reasonCode");
		builder.restriction("reason-reference", true, null, "reasonReference");
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");	
					
		return query;
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Procedure theProcedure) {
		return super.createResource(theProcedure);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/Procedure";
	}
	
	

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Procedure theProcedure) {
		return super.updateResource(theId, theProcedure);
	}		

	public void prepare(Record record, Procedure theProcedure) throws AppException {
		// Set Record code and content= setRecordCodeByCodeableConcept(record, theObservation.getCode(), null);		
		setRecordCodeByCodings(record, null, "Procedure");		
				
		String date = "No time";		
		if (theProcedure.hasPerformed()) {
			try {
				date = FHIRTools.stringFromDateTime(theProcedure.getPerformed());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process performedDateTime");
			}
		} 
		record.name = date;		

		// clean
		Reference subjectRef = theProcedure.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theProcedure.setSubject(null);
		
		clean(theProcedure);
 
	}	
 
	@Override
	public void processResource(Record record, Procedure p) throws AppException {
		super.processResource(record, p);
		
		if (p.getSubject().isEmpty()) {			
			p.setSubject(FHIRTools.getReferenceToOwner(record));
		}
	}

	@Override
	protected void convertToR4(Object in) {
		// No action
		
	}
	

}