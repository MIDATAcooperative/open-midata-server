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

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
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
import models.Record;
import utils.access.pseudo.FhirPseudonymizer;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class AllergyIntoleranceResourceProvider extends RecordBasedResourceProvider<AllergyIntolerance> implements IResourceProvider {

	public AllergyIntoleranceResourceProvider() {
		searchParamNameToPathMap.put("AllergyIntolerance:asserter", "asserter");
		searchParamNameToTypeMap.put("AllergyIntolerance:asserter", Sets.create("Practitioner", "Patient", "RelatedPerson"));
		searchParamNameToPathMap.put("AllergyIntolerance:recorder", "recorder");
		searchParamNameToTypeMap.put("AllergyIntolerance:recorder", Sets.create("Practitioner", "Patient", "RelatedPerson"));		
		searchParamNameToPathMap.put("AllergyIntolerance:patient", "patient");
		searchParamNameToTypeMap.put("AllergyIntolerance:patient", Sets.create("Patient"));
		
		registerSearches("AllergyIntolerance", getClass(), "getAllergyIntolerance");
		
		FhirPseudonymizer.forR4()
		  .reset("AllergyIntolerance")
		  .pseudonymizeReference("AllergyIntolerance", "asserter")
		  .pseudonymizeReference("AllergyIntolerance", "recorder")
		  .pseudonymizeReference("AllergyIntolerance", "reaction", "note", "authorReference");
	}
	
	@Override
	public Class<AllergyIntolerance> getResourceType() {
		return AllergyIntolerance.class;
	}

	@Search()
	public Bundle getAllergyIntolerance(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

		
  			@Description(shortDefinition="Source of the information about the allergy")
  			@OptionalParam(name="asserter", targetTypes={  } )
  			ReferenceAndListParam theAsserter, 
    
  			@Description(shortDefinition="food | medication | environment | biologic")
  			@OptionalParam(name="category")
  			TokenAndListParam theCategory, 
    
  			@Description(shortDefinition="active | inactive | resolved")
  			@OptionalParam(name="clinical-status")
  			TokenAndListParam theClinical_status, 
    
  			@Description(shortDefinition="Code that identifies the allergy or intolerance")
  			@OptionalParam(name="code")
  			TokenAndListParam theCode, 
    
  			@Description(shortDefinition="low | high | unable-to-assess")
  			@OptionalParam(name="criticality")
  			TokenAndListParam theCriticality, 
    
  			@Description(shortDefinition="Date record was believed accurate")
  			@OptionalParam(name="date")
			DateAndListParam theDate, 
    
  			@Description(shortDefinition="External ids for this item")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier, 
    
  			@Description(shortDefinition="Date(/time) of last known occurrence of a reaction")
  			@OptionalParam(name="last-date")
			DateAndListParam theLast_date, 
    
  			@Description(shortDefinition="Clinical symptoms/signs associated with the Event")
  			@OptionalParam(name="manifestation")
 			TokenAndListParam theManifestation, 
   
 			@Description(shortDefinition="Date(/time) when manifestations showed")
 			@OptionalParam(name="onset")
			DateAndListParam theOnset, 
   
 			@Description(shortDefinition="Who the sensitivity is for")
 			@OptionalParam(name="patient", targetTypes={  } )
 			ReferenceAndListParam thePatient, 
   
 			@Description(shortDefinition="Who recorded the sensitivity")
 			@OptionalParam(name="recorder", targetTypes={  } )
 			ReferenceAndListParam theRecorder, 
   
 			@Description(shortDefinition="How the subject was exposed to the substance")
 			@OptionalParam(name="route")
 			TokenAndListParam theRoute, 
   
 			@Description(shortDefinition="mild | moderate | severe (of event as a whole)")
 			@OptionalParam(name="severity")
 			TokenAndListParam theSeverity, 
   
 			@Description(shortDefinition="allergy | intolerance - Underlying mechanism (if known)")
 			@OptionalParam(name="type")
 			TokenAndListParam theType, 
   
 			@Description(shortDefinition="unconfirmed | confirmed | refuted | entered-in-error")
 			@OptionalParam(name="verification-status")
 			TokenAndListParam theVerification_status,   		
 
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"AllergyIntolerance:asserter" ,
 					"AllergyIntolerance:patient" ,
 					"AllergyIntolerance:recorder" ,
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
	
		paramMap.add("asserter", theAsserter);
		paramMap.add("category", theCategory);
		paramMap.add("clinical-status", theClinical_status);
		paramMap.add("code", theCode);
		paramMap.add("criticality", theCriticality);
		paramMap.add("date", theDate);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("last-date", theLast_date);
		paramMap.add("manifestation", theManifestation);
		paramMap.add("onset", theOnset);
		paramMap.add("patient", thePatient);
		paramMap.add("recorder", theRecorder);
		paramMap.add("route", theRoute);
		paramMap.add("severity", theSeverity);
		paramMap.add("type", theType);
		paramMap.add("verification-status", theVerification_status);		
		
		
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		paramMap.setFrom(_page != null ? _page.getValue() : null);

		return searchBundle(paramMap, theDetails);
		
	}

	public Query buildQuery(SearchParameterMap params) throws AppException {
		AccessContext info = info();

		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/AllergyIntolerance");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "patient");

		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("code", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "code", QueryBuilder.TYPE_CODEABLE_CONCEPT, "reaction.substance");
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "recordedDate|assertedDate");
		
		builder.restriction("asserter", true, null, "asserter");				        
		builder.restriction("last-date", true, QueryBuilder.TYPE_DATETIME, "lastOccurrence");			
		builder.restriction("manifestation", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "reaction.manifestation");
		builder.restriction("onset", true, QueryBuilder.TYPE_DATETIME, "reaction.onset");	
		builder.restriction("recorder", true, null, "recorder");	
		builder.restriction("route", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "reaction.exposureRoute");	
		builder.restriction("severity", false, QueryBuilder.TYPE_CODE, "reaction.severity");
		builder.restriction("type", false, QueryBuilder.TYPE_CODE, "type");
		builder.restriction("criticality", false, QueryBuilder.TYPE_CODE, "criticality");
		builder.restriction("category", false, QueryBuilder.TYPE_CODE, "category");	
		builder.restriction("clinical-status", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "clinicalStatus");			
		builder.restriction("verification-status", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "verificationStatus");	
								
		return query;
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam AllergyIntolerance theAllergyIntolerance) {
		return super.createResource(theAllergyIntolerance);
	}
		
	
	@Override
	public String getRecordFormat() {	
		return "fhir/AllergyIntolerance";
	}	

	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam AllergyIntolerance theAllergyIntolerance) {
		return super.updateResource(theId, theAllergyIntolerance);
	}		

	public void prepare(Record record, AllergyIntolerance theAllergyIntolerance) throws AppException {
		// Set Record code and content
		record.name = setRecordCodeByCodings(record, null, "AllergyIntolerance");		
						
		if (record.name==null) record.name = "Allergy Intolerance";		

		// clean
		Reference subjectRef = theAllergyIntolerance.getPatient();
		if (cleanAndSetRecordOwner(record, subjectRef)) theAllergyIntolerance.setPatient(null);
		
		clean(theAllergyIntolerance);
 
	}	
 
	@Override
	public void processResource(Record record, AllergyIntolerance p) throws AppException {
		super.processResource(record, p);
		
		if (p.getPatient().isEmpty()) {			
			p.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	protected void convertToR4(Object in) {
		FHIRVersionConvert.convertCodeToCodesystem(in, "clinicalStatus", "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical");
		FHIRVersionConvert.convertCodeToCodesystem(in, "verificationStatus", "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification");
		FHIRVersionConvert.rename(in, "assertedDate", "recordedDate");
	}
	

}