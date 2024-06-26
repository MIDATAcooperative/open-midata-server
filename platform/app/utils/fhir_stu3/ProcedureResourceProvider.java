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

package utils.fhir_stu3;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Procedure;
import org.hl7.fhir.dstu3.model.Reference;

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

public class ProcedureResourceProvider extends RecordBasedResourceProvider<Procedure> implements IResourceProvider {

	public ProcedureResourceProvider() {
						
		searchParamNameToPathMap.put("Procedure:based-on" ,"basedOn");
		searchParamNameToTypeMap.put("Procedure:based-on" , Sets.create("CarePlan", "ServiceRequest"));
		searchParamNameToPathMap.put("Procedure:context" , "context");
		searchParamNameToTypeMap.put("Procedure:context-on" , Sets.create("EpisodeOfCare", "Encounter"));
		searchParamNameToPathMap.put("Procedure:definition" , "definition");
		searchParamNameToTypeMap.put("Procedure:definition" , Sets.create("PlanDefinition", "HealthcareService", "ActivityDefinition"));
		searchParamNameToPathMap.put("Procedure:encounter" , "context");
		searchParamNameToTypeMap.put("Procedure:encounter" , Sets.create("Encounter"));
		searchParamNameToPathMap.put("Procedure:location" , "location");
		searchParamNameToTypeMap.put("Procedure:location" , Sets.create("Location"));
		searchParamNameToPathMap.put("Procedure:part-of" , "partOf");
		searchParamNameToTypeMap.put("Procedure:part-of" , Sets.create("Observation", "Procedure", "MedicationAdministration"));
		searchParamNameToPathMap.put("Procedure:patient" , "subject");
		searchParamNameToTypeMap.put("Procedure:patient" , Sets.create("Patient"));
		searchParamNameToPathMap.put("Procedure:performer" , "performer.actor");
		searchParamNameToTypeMap.put("Procedure:performer" , Sets.create("Practitioner", "Organization", "Device", "Patient", "RelatedPerson"));
		searchParamNameToPathMap.put("Procedure:subject" , "subject");
		searchParamNameToTypeMap.put("Procedure:subject" , Sets.create("Patient", "Group"));
		
		registerSearches("Procedure", getClass(), "getProcedure");
		
		FhirPseudonymizer.forSTU3()
		  .reset("Procedure")		
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
    
  			@Description(shortDefinition="The language of the resource")
  			@OptionalParam(name="_language")
  			StringAndListParam the_language, 
    
  			@Description(shortDefinition="A request for this procedure")
  			@OptionalParam(name="based-on", targetTypes={  } )
  			ReferenceAndListParam theBased_on, 
   
  			@Description(shortDefinition="Classification of the procedure")
  			@OptionalParam(name="category")
  			TokenAndListParam theCategory, 
    
  			@Description(shortDefinition="A code to identify a  procedure")
  			@OptionalParam(name="code")
  			TokenAndListParam theCode, 
    
  			@Description(shortDefinition="Encounter or episode associated with the procedure")
  			@OptionalParam(name="context", targetTypes={  } )
  			ReferenceAndListParam theContext, 
    
  			@Description(shortDefinition="Date/Period the procedure was performed")
  			@OptionalParam(name="date")
  			DateRangeParam theDate, 
    
  			@Description(shortDefinition="Instantiates protocol or definition")
  			@OptionalParam(name="definition", targetTypes={  } )
  			ReferenceAndListParam theDefinition, 
    
  			@Description(shortDefinition="Search by encounter")
 			@OptionalParam(name="encounter", targetTypes={  } )
  			ReferenceAndListParam theEncounter, 
    
  			@Description(shortDefinition="A unique identifier for a procedure")
 			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier, 
    
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
   
 			@Description(shortDefinition="preparation | in-progress | suspended | aborted | completed | entered-in-error | unknown")
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
				"Procedure:context" ,
				"Procedure:definition" ,
				"Procedure:encounter" ,
				"Procedure:location" ,
				"Procedure:part-of" ,
				"Procedure:patient" ,
				"Procedure:performer" ,
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
		paramMap.add("_language", the_language);
		paramMap.add("based-on", theBased_on);
		paramMap.add("category", theCategory);
		paramMap.add("code", theCode);
		paramMap.add("context", theContext);
		paramMap.add("date", theDate);
		paramMap.add("definition", theDefinition);
		paramMap.add("encounter", theEncounter);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("location", theLocation);
		paramMap.add("part-of", thePart_of);
		paramMap.add("patient", thePatient);
		paramMap.add("performer", thePerformer);
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
		AccessContext info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Procedure");

		builder.handleIdRestriction();
		
		builder.recordOwnerReference("patient", "Patient", "subject");
		
			

		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "code");
		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
	
		builder.restriction("based-on", true, null, "basedOn");
		builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");	
		
		builder.restriction("context", true, null, "context");
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "performed");
		builder.restriction("definition", true, null, "definition");	
		builder.restriction("encounter", true, "Encounter", "context");	
		builder.restriction("location", true, "Location", "location");
		builder.restriction("part-of", true, null, "partOf");	
		builder.restriction("performer", true, null, "performer.actor");
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "status");	
					
		return query.execute(info);
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
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
	

}