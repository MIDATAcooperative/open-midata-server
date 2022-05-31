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
import org.hl7.fhir.r4.model.MedicationStatement;
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

public class MedicationStatementResourceProvider extends RecordBasedResourceProvider<MedicationStatement> implements IResourceProvider {

	public MedicationStatementResourceProvider() {
		searchParamNameToPathMap.put("MedicationStatement:context", "context");
		searchParamNameToTypeMap.put("MedicationStatement:context", Sets.create("EpsiodeOfCare", "Encounter"));
		searchParamNameToPathMap.put("MedicationStatement:medication", "medicationReference");
		searchParamNameToTypeMap.put("MedicationStatement:medication", Sets.create("Medication"));
		searchParamNameToPathMap.put("MedicationStatement:part-of", "partOf");	
		searchParamNameToPathMap.put("MedicationStatement:patient", "subject");
		searchParamNameToTypeMap.put("MedicationStatement:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("MedicationStatement:source", "informationSource");		
		searchParamNameToPathMap.put("MedicationStatement:subject", "subject");				
						
		registerSearches("MedicationStatement", getClass(), "getMedicationStatement");
		
		FhirPseudonymizer.forR4()
		  .reset("MedicationStatement")		
		  .pseudonymizeReference("MedicationStatement", "informationSource")
		  .pseudonymizeReference("MedicationStatement", "note", "authorReference");		  
	}
	
	@Override
	public Class<MedicationStatement> getResourceType() {
		return MedicationStatement.class;
	}

	@Search()
	public Bundle getMedicationStatement(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

		
 			@Description(shortDefinition="Returns statements of this category of medicationstatement")
  			@OptionalParam(name="category")
  			TokenAndListParam theCategory,
    
  			@Description(shortDefinition="Return statements of this medication code")
  			@OptionalParam(name="code")
  			TokenAndListParam theCode,
    
  			@Description(shortDefinition="Returns statements for a specific context (episode or episode of Care).")
  			@OptionalParam(name="context", targetTypes={  } )
  			ReferenceAndListParam theContext, 
    
  			@Description(shortDefinition="Date when patient was taking (or not taking) the medication")
  			@OptionalParam(name="effective")
  			DateAndListParam theEffective, 
    
  			@Description(shortDefinition="Return statements with this external identifier")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="Return statements of this medication reference")
  			@OptionalParam(name="medication", targetTypes={  } )
  			ReferenceAndListParam theMedication, 
    
  			@Description(shortDefinition="Returns statements that are part of another event.")
  			@OptionalParam(name="part-of", targetTypes={  } )
  			ReferenceAndListParam thePart_of, 
    
  			@Description(shortDefinition="Returns statements for a specific patient.")
  			@OptionalParam(name="patient", targetTypes={  } )
  			ReferenceAndListParam thePatient, 
    
 			@Description(shortDefinition="Who or where the information in the statement came from")
 			@OptionalParam(name="source", targetTypes={  } )
 			ReferenceAndListParam theSource, 
   
 			@Description(shortDefinition="Return statements that match the given status")
 			@OptionalParam(name="status")
 			TokenAndListParam theStatus,
   
 			@Description(shortDefinition="The identity of a patient, animal or group to list statements for")
 			@OptionalParam(name="subject", targetTypes={  } )
 			ReferenceAndListParam theSubject,   		
 
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"MedicationStatement:context" ,
 					"MedicationStatement:medication" ,
 					"MedicationStatement:part-of" ,
 					"MedicationStatement:patient" ,
 					"MedicationStatement:source" ,
 					"MedicationStatement:subject" ,
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

		paramMap.add("_id", theId);
		paramMap.add("_language", theResourceLanguage);
	
		paramMap.add("category", theCategory);
		paramMap.add("code", theCode);
		paramMap.add("context", theContext);
		paramMap.add("effective", theEffective);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("medication", theMedication);
		paramMap.add("part-of", thePart_of);
		paramMap.add("patient", thePatient);
		paramMap.add("source", theSource);
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/MedicationStatement");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "subject");

		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "medicationCodeableConcept");
		builder.restriction("effective", true, QueryBuilder.TYPE_DATETIME_OR_PERIOD, "effective");
		
		if (!builder.recordOwnerReference("subject", null, "subject")) builder.restriction("subject", true, null, "subject");
		
		builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");			
		builder.restriction("context", true, null, "context");	
		builder.restriction("medication", true, null, "medicationReference");	
		builder.restriction("part-of", true, null, "partOf");
		builder.restriction("source", true, null, "informationSource");
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");	
				
		return query;
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam MedicationStatement theMedicationStatement) {
		return super.createResource(theMedicationStatement);
	}
	
	@Override
	public String getRecordFormat() {	
		return "fhir/MedicationStatement";
	}
	

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam MedicationStatement theMedicationStatement) {
		return super.updateResource(theId, theMedicationStatement);
	}		

	public void prepare(Record record, MedicationStatement theMedicationStatement) throws AppException {
		// Set Record code and content
		setRecordCodeByCodings(record, null, "MedicationStatement");		
		
		String date = "No time";		
		if (theMedicationStatement.hasEffective()) {
			try {
				date = FHIRTools.stringFromDateTime(theMedicationStatement.getEffective());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process effectiveDateTime");
			}
		} 
		record.name = date;		

		// clean
		Reference subjectRef = theMedicationStatement.getSubject();
		if (cleanAndSetRecordOwner(record, subjectRef)) theMedicationStatement.setSubject(null);
		
		clean(theMedicationStatement);
 
	}	
 
	@Override
	public void processResource(Record record, MedicationStatement p) throws AppException {
		super.processResource(record, p);
		
		if (p.getSubject().isEmpty()) {			
			p.setSubject(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	protected void convertToR4(Object in) {
		// No action
		
	}
	

}