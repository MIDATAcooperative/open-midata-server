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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResearchStudy;

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
import models.Consent;
import models.ContentInfo;
import models.MidataId;
import models.Record;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class ResearchStudyResourceProvider extends HybridTypeResourceProvider<ResearchStudy, Record, Record> {

	public ResearchStudyResourceProvider() {
		super(Record.class, new RecordResearchStudyResourceProvider(), Record.class, new MidataResearchStudyResourceProvider(), false);
		searchParamNameToPathMap.put("ResearchStudy:principalinvestigator", "principalinvestigator");
		searchParamNameToPathMap.put("ResearchStudy:part-of", "partOf");
		searchParamNameToPathMap.put("ResearchStudy:protocol", "protocol");
		searchParamNameToPathMap.put("ResearchStudy:site", "site");
		searchParamNameToPathMap.put("ResearchStudy:sponsor", "sponsor");
		
		searchParamNameToTypeMap.put("ResearchStudy:principalinvestigator", Sets.create("Practitioner", "PractitionerRole"));
		searchParamNameToTypeMap.put("ResearchStudy:part-of", Sets.create("ResearchStudy"));
		searchParamNameToTypeMap.put("ResearchStudy:protocol", Sets.create("PlanDefinition"));
		searchParamNameToTypeMap.put("ResearchStudy:site", Sets.create("Location"));
		searchParamNameToTypeMap.put("ResearchStudy:sponsor", Sets.create("Organization"));
		
			
		// Use name of @Search function as last parameter
		registerSearches("ResearchStudy", getClass(), "getResearchStudy");
	}
	
	@Search()
	public Bundle getResearchStudy(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,
		
			@Description(shortDefinition="Classifications for the study")
  			@OptionalParam(name="category")
  			TokenAndListParam theCategory,
			
			@Description(shortDefinition="When the study began and ended")
 			@OptionalParam(name="date")
			DateAndListParam theDate, 
						
			@Description(shortDefinition="Drugs, devices, etc. under study")
  			@OptionalParam(name="focus")
  			TokenAndListParam theFocus,
			
						
			@Description(shortDefinition="Business Identifier for study")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
									
			@Description(shortDefinition="Used to search for the study")
  			@OptionalParam(name="keyword")
  			TokenAndListParam theKeyword,
									
			@Description(shortDefinition="Geographic region(s) for study")
  			@OptionalParam(name="location")
  			TokenAndListParam theLocation,
			
					
			@Description(shortDefinition="Part of larger study")
  			@OptionalParam(name="partof", targetTypes={ ResearchStudy.class } )
  			ReferenceAndListParam thePartof, 
								
			@Description(shortDefinition="Researcher who oversees multiple aspects of the study")
  			@OptionalParam(name="principalinvestigator", targetTypes={ Practitioner.class, PractitionerRole.class } )
  			ReferenceAndListParam thePrincipalinvestigator, 
						
			@Description(shortDefinition="Steps followed in executing study")
  			@OptionalParam(name="protocol", targetTypes={ PlanDefinition.class } )
  			ReferenceAndListParam theProtocol, 
			
			@Description(shortDefinition="Facility where study activities are conducted")
  			@OptionalParam(name="site", targetTypes={ Location.class} )
  			ReferenceAndListParam theSite, 
			
			@Description(shortDefinition="Organization that initiates and is legally responsible for the study")
  			@OptionalParam(name="sponsor", targetTypes={ Organization.class} )
  			ReferenceAndListParam theSponsor, 
									
			@Description(shortDefinition="active | administratively-completed | approved | closed-to-accrual | closed-to-accrual-and-intervention | completed | disapproved | in-review | temporarily-closed-to-accrual | temporarily-closed-to-accrual-and-intervention | withdrawn")
  			@OptionalParam(name="status")
  			TokenAndListParam theStatus,
						
			@Description(shortDefinition="Name for this study")
  			@OptionalParam(name="title")
  			StringAndListParam theTitle,
			
						  		
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"ResearchStudy:principalinvestigator",
 					"ResearchStudy:part-of",
 					"ResearchStudy:protocol",
 					"ResearchStudy:site",
 					"ResearchStudy:sponsor",  					 
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
			
		paramMap.add("category", theCategory);		
		paramMap.add("date", theDate);		
		paramMap.add("focus", theFocus);		
		paramMap.add("identifier", theIdentifier);		
		paramMap.add("keyword", theKeyword);		
		paramMap.add("location", theLocation);		
		paramMap.add("partof", thePartof);					
		paramMap.add("principalinvestigator", thePrincipalinvestigator);						
		paramMap.add("protocol", theProtocol);				
		paramMap.add("site", theSite);				
		paramMap.add("sponsor", theSponsor);				
		paramMap.add("status", theStatus);		
		paramMap.add("title", theTitle);
		
		paramMap.setRevIncludes(theRevIncludes);
		paramMap.setLastUpdated(theLastUpdated);
		paramMap.setIncludes(theIncludes);
		paramMap.setSort(theSort);
		paramMap.setCount(theCount);
		
		// The last lines are different than the happy fhir version
		paramMap.setFrom(_page != null ? _page.getValue() : null);
		return searchBundle(paramMap, theDetails);
		
	}
	
	 @Create
	 @Override
	 public MethodOutcome createResource(@ResourceParam ResearchStudy theResearchStudy) {
		return super.createResource(theResearchStudy);
	 }
		
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam ResearchStudy theResearchStudy) {
		return super.updateResource(theId, theResearchStudy);
	}	

	@Override
	public boolean handleWithFirstProvider(ResearchStudy resource) {
		AccessLog.log("first prov = "+resource.toString());
		for (Identifier ids : resource.getIdentifier()) {
			AccessLog.log("first prov id system="+ids.getSystem());
			if (ids.getSystem().equals("http://midata.coop/codesystems/project-code")) return false;
			if (ids.getSystem().equals("http://midata.coop/codesystems/study-code")) return false;
		}
		AccessLog.log("first prov true");
        return true;
	}

	@Override
	public Class<ResearchStudy> getResourceType() {
		return ResearchStudy.class;
	}

	@Override
	protected void convertToR4(Object in) {
				
	}
}

class RecordResearchStudyResourceProvider extends RecordBasedResourceProvider<ResearchStudy> {
	
	@Override
	public String getRecordFormat() {
		return "fhir/ResearchStudy";
	}

	@Override
	public Class<ResearchStudy> getResourceType() {
		return ResearchStudy.class;
	}

	@Override
	protected void convertToR4(Object in) {
		
		
	}

	@Override
	public Query buildQuery(SearchParameterMap params) throws AppException {
		// get execution context (which user, which app)
			info();

			// construct empty query and a builder for that query
			Query query = new Query();		
			QueryBuilder builder = new QueryBuilder(params, query, "fhir/ResearchStudy");
			query.putAccount("content", "ExternalResearchStudy");
			// Now all possible searches need to be handeled. For performance reasons it makes sense
			// to put searches that are very restrictive and frequently used first in order
		
			// Add default handling for the _id search parameter
			builder.handleIdRestriction();
			
				
	        // Add handling for a multiTYPE search: "date" may be search on effectiveDateTime or effectivePeriod
	        // Note that path = "effective" and type = TYPE_DATETIME_OR_PERIOD
	        // If the search was only on effectiveDateTime then
	        // type would be TYPE_DATETIME and path would be "effectiveDateTime" instead		
			builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
			builder.restriction("keyword", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "keyword");
			builder.restriction("date", true, QueryBuilder.TYPE_PERIOD, "period");
			builder.restriction("category", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "category");
			builder.restriction("focus", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "focus");
			builder.restriction("location", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "location");
			
													
			builder.restriction("partof", true, "ResearchStudy", "partOf");
			builder.restriction("principalinvestigator", true, null, "principalInvestigator");
			builder.restriction("protocol", true, "PlanDefinition", "protocol");
			builder.restriction("site", true, "Location", "site");
			builder.restriction("sponsor", true, "Organization", "sponsor");
									
			builder.restriction("status", true, QueryBuilder.TYPE_CODE, "status");
			builder.restriction("title", true, QueryBuilder.TYPE_STRING, "title");								
				
			return query;
	}
	
	public void prepare(Record record, ResearchStudy theResearchStudy) throws AppException {
		// Set Record code and content
										
		String display = theResearchStudy.getTitle();	
		record.name = display;		
	    record.content = "ExternalResearchStudy";
	    record.code = Collections.singleton("http://midata.coop ExternalResearchStudy");			
												
		clean(theResearchStudy);
	}

	@Override
	public void processResource(Record record, ResearchStudy resource) throws AppException {		
		super.processResource(record, resource);
				
	}
	
	
}
