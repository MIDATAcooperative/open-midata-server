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

import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Elements;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import models.Consent;
import models.ContentInfo;
import models.Record;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class ConsentResourceProvider extends HybridTypeResourceProvider<org.hl7.fhir.r4.model.Consent, Consent, Record> {

	public ConsentResourceProvider() {
		super(Consent.class, new MidataConsentResourceProvider(), Record.class, new RecordConsentResourceProvider());
		registerSearches("Consent", getClass(), "getConsent");
		searchParamNameToPathMap.put("Consent:patient", "patient");
		searchParamNameToTypeMap.put("Consent:patient", Sets.create("Patient"));
		searchParamNameToPathMap.put("Consent:actor", "provision.actor.reference");
		searchParamNameToPathMap.put("Consent:data", "provision.data.reference");
		searchParamNameToPathMap.put("Consent:consentor", "performer");
		searchParamNameToPathMap.put("Consent:organization", "organization");
		searchParamNameToTypeMap.put("Consent:organization", Sets.create("Organization"));
		searchParamNameToPathMap.put("Consent:source-reference", "source");			
	}
	
	 @Search()
	    public Bundle getConsent(
	    		@Description(shortDefinition="The resource identity")
	    		@OptionalParam(name="_id")
	    		StringAndListParam theId, 
	    		 
	    		@Description(shortDefinition="The resource language")
	    		@OptionalParam(name="_language")
	    		StringAndListParam theResourceLanguage, 
	    		   
	    		@Description(shortDefinition="Actions controlled by this consent")
	    		@OptionalParam(name="action")
	    		TokenAndListParam theAction, 
	    		   
	    		@Description(shortDefinition="Resource for the actor (or group, by role)")
	    		@OptionalParam(name="actor", targetTypes={  } )
	    		ReferenceAndListParam theActor, 
	    		   
	    		@Description(shortDefinition="Classification of the consent statement - for indexing/retrieval")
	    		@OptionalParam(name="category")
	    		TokenAndListParam theCategory, 
	    		   
	    		@Description(shortDefinition="Who is agreeing to the policy and exceptions")
	    		@OptionalParam(name="consentor", targetTypes={  } )
	    		ReferenceAndListParam theConsentor, 
	    		   
	    		@Description(shortDefinition="The actual data reference")
	    		@OptionalParam(name="data", targetTypes={  } )
	    		ReferenceAndListParam theData, 
	    		  
	    		@Description(shortDefinition="When this Consent was created or indexed")
	    		@OptionalParam(name="date")
	    		DateAndListParam theDate, 
	    		    
	    		@Description(shortDefinition="Identifier for this record (external references)")
	    		@OptionalParam(name="identifier")
	    		TokenAndListParam theIdentifier, 
	    		   
	    		@Description(shortDefinition="Custodian of the consent")
	    		@OptionalParam(name="organization", targetTypes={  } )
	    		ReferenceAndListParam theOrganization, 
	    		   
	    		@Description(shortDefinition="Who the consent applies to")
	    		@OptionalParam(name="patient", targetTypes={  } )
	    		ReferenceParam thePatient, 
	    		  
	    		@Description(shortDefinition="Period that this consent applies")
	    		@OptionalParam(name="period")
	    		DateRangeParam thePeriod, 
	    		   
	    		@Description(shortDefinition="Context of activities for which the agreement is made")
	    		@OptionalParam(name="purpose")
	    		TokenAndListParam thePurpose, 
	    		 
	    		@Description(shortDefinition="Security Labels that define affected resources")
	    		@OptionalParam(name="securitylabel")
	    		TokenAndListParam theSecuritylabel, 
	    		   
	    		@Description(shortDefinition="Source from which this consent is taken")
	    		@OptionalParam(name="source", targetTypes={  } )
	    		ReferenceAndListParam theSource, 
	    		  
	    		@Description(shortDefinition="draft | proposed | active | rejected | inactive | entered-in-error")
	    		@OptionalParam(name="status")
	    		TokenAndListParam theStatus, 
	    		
	    		@IncludeParam(reverse=true)
	    		Set<Include> theRevIncludes,
	    		@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
	    		@OptionalParam(name="_lastUpdated")
	    		DateRangeParam theLastUpdated, 
	    		 
	    		@IncludeParam(allow= {
	    				"Consent:actor" ,
	    				"Consent:consentor" ,
	    				"Consent:data" ,
	    				"Consent:organization" ,
	    				"Consent:patient" ,
	    				"Consent:source" , 
	    				"*"
	    		}) 
	    		Set<Include> theIncludes,
	    					
	    		@Sort 
	    		SortSpec theSort,
	    					
	    		@ca.uhn.fhir.rest.annotation.Count
	    		Integer theCount,
	    		
	    		SummaryEnum theSummary, // will receive the summary (no annotation required)
	    	    @Elements Set<String> theElements,
	    	    
	    	    @OptionalParam(name="_page")
				StringParam _page,
				
				RequestDetails theDetails
	    
	    		) throws AppException {
	    	
	    	SearchParameterMap paramMap = new SearchParameterMap();
	    	
	    	paramMap.add("_id", theId);
			paramMap.add("_language", theResourceLanguage);	   
			
			paramMap.add("action", theAction);
			paramMap.add("actor", theActor);
			paramMap.add("category", theCategory);
			paramMap.add("consentor", theConsentor);
			paramMap.add("data", theData);
			paramMap.add("date", theDate);
			paramMap.add("identifier", theIdentifier);
			paramMap.add("organization", theOrganization);
			paramMap.add("patient", thePatient);
			paramMap.add("period", thePeriod);
			paramMap.add("purpose", thePurpose);
			paramMap.add("securitylabel", theSecuritylabel);
			paramMap.add("source", theSource);
			paramMap.add("status", theStatus);
			
			paramMap.add("_lastUpdated", theLastUpdated);
			
	    	paramMap.setRevIncludes(theRevIncludes);
	    	paramMap.setLastUpdated(theLastUpdated);
	    	paramMap.setIncludes(theIncludes);
	    	paramMap.setSort(theSort);
	    	paramMap.setCount(theCount);
	    	paramMap.setElements(theElements);
	    	paramMap.setSummary(theSummary);
	    	    		    	
	    	return searchBundle(paramMap, theDetails);    	    	    	
	    }
	
	 @Create
	 @Override
	 public MethodOutcome createResource(@ResourceParam org.hl7.fhir.r4.model.Consent theConsent) {
		return super.createResource(theConsent);
	 }
		
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam org.hl7.fhir.r4.model.Consent theConsent) {
		return super.updateResource(theId, theConsent);
	}	

	@Override
	public boolean handleWithFirstProvider(org.hl7.fhir.r4.model.Consent resource) {
		for (CodeableConcept cat : resource.getCategory()) {
			for (Coding coding : cat.getCoding()) {
				if ("http://midata.coop/codesystems/consent-category".equals(coding.getSystem())) return true;
			}
		}
		for (Coding coding : resource.getProvision().getPurpose()) {			
			if ("http://midata.coop/codesystems/consent-type".equals(coding.getSystem())) return true;			
		}
        return false;
	}

	@Override
	public Class<org.hl7.fhir.r4.model.Consent> getResourceType() {
		return org.hl7.fhir.r4.model.Consent.class;
	}

	@Override
	protected void convertToR4(Object in) {
				
	}
}

class RecordConsentResourceProvider extends RecordBasedResourceProvider<org.hl7.fhir.r4.model.Consent> {
	
	@Override
	public String getRecordFormat() {
		return "fhir/Consent";
	}

	@Override
	public Class<org.hl7.fhir.r4.model.Consent> getResourceType() {
		return org.hl7.fhir.r4.model.Consent.class;
	}

	@Override
	protected void convertToR4(Object in) {
		
		
	}

	@Override
	public Query buildQuery(SearchParameterMap params) throws AppException {
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Consent");
		
		query.putAccount("content", "documents/patient-consent");
		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", null);
		
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("action", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "provision.action");
		builder.restriction("actor", true, null, "provision.actor.reference");
		builder.restriction("data", true, null, "provision.data.reference");
		builder.restriction("consentor", true, null, "performer");
		builder.restriction("organization", true, "Organization", "organization");
		builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");		
		builder.restriction("date", true, QueryBuilder.TYPE_DATETIME, "dateTime");
		builder.restriction("period", true, QueryBuilder.TYPE_PERIOD, "provision.period");
		builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");
		builder.restriction("purpose", true, QueryBuilder.TYPE_CODING, "provision.purpose");
		builder.restriction("source", true, null, "source");

		builder.restriction("securityLabel", true, QueryBuilder.TYPE_CODING, "provision.securityLabel");
		return query;
	}
	
	public void prepare(Record record, org.hl7.fhir.r4.model.Consent theConsent) throws AppException {
		// Set Record code and content
				
		ContentInfo.setRecordCodeAndContent(info().getUsedPlugin(), record, null, "documents/patient-consent");			
		record.name = "Consent";
				
		// Task c : Set record owner based on subject and clean subject if this was possible
		Reference subjectRef = theConsent.getPatient();
		if (cleanAndSetRecordOwner(record, subjectRef)) theConsent.setPatient(null);
								
		clean(theConsent);
	}

	@Override
	public void processResource(Record record, org.hl7.fhir.r4.model.Consent resource) throws AppException {		
		super.processResource(record, resource);
		
		if (resource.getPatient().isEmpty()) {			
			resource.setPatient(FHIRTools.getReferenceToUser(record.owner, record.ownerName));
		}
	}
	
	
}
