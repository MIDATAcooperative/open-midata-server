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
import org.hl7.fhir.r4.model.EpisodeOfCare;
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
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import models.Record;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class EpisodeOfCareResourceProvider extends RecordBasedResourceProvider<EpisodeOfCare> implements IResourceProvider {

	public EpisodeOfCareResourceProvider() {
		
		searchParamNameToPathMap.put("EpisodeOfCare:care-manager" , "careManager");
		searchParamNameToTypeMap.put("EpisodeOfCare:care-manager" , Sets.create("Practitioner"));
		searchParamNameToPathMap.put("EpisodeOfCare:condition", "diagnosis.condition");
		searchParamNameToTypeMap.put("EpisodeOfCare:condition" , Sets.create("Condition"));
		searchParamNameToPathMap.put("EpisodeOfCare:incoming-referral", "referralRequest");
		searchParamNameToTypeMap.put("EpisodeOfCare:incoming-referral" , Sets.create("ServiceRequest"));
		searchParamNameToPathMap.put("EpisodeOfCare:organization" , "managingOrganization");
		searchParamNameToTypeMap.put("EpisodeOfCare:organization" , Sets.create("Organization"));
		searchParamNameToPathMap.put("EpisodeOfCare:patient" , "patient");
		searchParamNameToTypeMap.put("EpisodeOfCare:patient" , Sets.create("Patient"));
		
		registerSearches("EpisodeOfCare", getClass(), "getEpisodeOfCare");
	}
	
	@Override
	public Class<EpisodeOfCare> getResourceType() {
		return EpisodeOfCare.class;
	}

	@Search()
	public Bundle getEpisodeOfCare(
			@Description(shortDefinition="The ID of the resource")
			@OptionalParam(name="_id")
			TokenAndListParam the_id, 
			   
			@Description(shortDefinition="The language of the resource")
			@OptionalParam(name="_language")
			StringAndListParam the_language, 
			    
			@Description(shortDefinition="Care manager/care coordinator for the patient")
  			@OptionalParam(name="care-manager", targetTypes={  } )
  			ReferenceAndListParam theCare_manager, 
    
  			@Description(shortDefinition="Conditions/problems/diagnoses this episode of care is for")
  			@OptionalParam(name="condition", targetTypes={  } )
  			ReferenceAndListParam theCondition, 
    
  			@Description(shortDefinition="The provided date search value falls within the episode of care's period")
  			@OptionalParam(name="date")
  			DateAndListParam theDate, 
    
  			@Description(shortDefinition="Business Identifier(s) relevant for this EpisodeOfCare")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
    
  			@Description(shortDefinition="Incoming Referral Request")
  			@OptionalParam(name="incoming-referral", targetTypes={  } )
  			ReferenceAndListParam theIncoming_referral, 
    
  			@Description(shortDefinition="The organization that has assumed the specific responsibilities of this EpisodeOfCare")
  			@OptionalParam(name="organization", targetTypes={  } )
  			ReferenceAndListParam theOrganization, 
    
  			@Description(shortDefinition="The patient who is the focus of this episode of care")
  			@OptionalParam(name="patient", targetTypes={  } )
  			ReferenceAndListParam thePatient, 
    
  			@Description(shortDefinition="The current status of the Episode of Care as provided (does not check the status history collection)")
  			@OptionalParam(name="status")
  			TokenAndListParam theStatus,
    
 			@Description(shortDefinition="Type/class  - e.g. specialist referral, disease management")
 			@OptionalParam(name="type")
 			TokenAndListParam theType,
  			
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"EpisodeOfCare:care-manager" ,
 					"EpisodeOfCare:condition" ,
 					"EpisodeOfCare:incoming-referral" ,
 					"EpisodeOfCare:organization" ,
 					"EpisodeOfCare:patient" ,
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
		paramMap.add("care-manager", theCare_manager);
		paramMap.add("condition", theCondition);
		paramMap.add("date", theDate);
		paramMap.add("identifier", theIdentifier);
		paramMap.add("incoming-referral", theIncoming_referral);
		paramMap.add("organization", theOrganization);
		paramMap.add("patient", thePatient);
		paramMap.add("status", theStatus);
		paramMap.add("type", theType);
		
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
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/EpisodeOfCare");

		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", "patient");
		builder.recordCodeRestriction("type", "type");
		
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");		
		builder.restriction("condition", true, "Condition", "diagnosis.condition");
		builder.restriction("date", true, QueryBuilder.TYPE_PERIOD, "period");
        builder.restriction("incoming-referral", true, "ServiceRequest", "referralRequest");		
        builder.restriction("organization", true, "Organization", "managingOrganization");
        builder.restriction("care-manager", true, "Practitioner", "careManager");	
						
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");	
				
		return query.execute(info);
	}

	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam EpisodeOfCare theEpisodeOfCare) {
		return super.createResource(theEpisodeOfCare);
	}
		
	@Override
	public String getRecordFormat() {	
		return "fhir/EpisodeOfCare";
	}	


	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam EpisodeOfCare theEpisodeOfCare) {
		return super.updateResource(theId, theEpisodeOfCare);
	}		

	public void prepare(Record record, EpisodeOfCare theEpisodeOfCare) throws AppException {
						
		String display  = setRecordCodeByCodeableConcept(record, theEpisodeOfCare.getTypeFirstRep(), "EpisodeOfCare");		
		String date = "";		
		if (theEpisodeOfCare.hasPeriod()) {
			try {
				date = FHIRTools.stringFromDateTime(theEpisodeOfCare.getPeriod());
			} catch (Exception e) {
				throw new UnprocessableEntityException("Cannot process period");
			}
		}
		record.name = display != null ? (display + " / " + date) : date;	

		// clean
		Reference subjectRef = theEpisodeOfCare.getPatient();
		if (cleanAndSetRecordOwner(record, subjectRef)) theEpisodeOfCare.setPatient(null);
		
		clean(theEpisodeOfCare);
 
	}	
 
	@Override
	public void processResource(Record record, EpisodeOfCare p) throws AppException {
		super.processResource(record, p);
		
		if (p.getPatient().isEmpty()) {			
			p.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}

	@Override
	protected void convertToR4(Object in) {
		// Nothing to do		
	}
	

}