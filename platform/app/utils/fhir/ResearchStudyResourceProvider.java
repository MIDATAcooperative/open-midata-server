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
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.ResearchStudy.ResearchStudyArmComponent;
import org.hl7.fhir.r4.model.StringType;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import models.Info;
import models.MidataId;
import models.Record;
import models.Research;
import models.Study;
import models.StudyGroup;
import models.enums.InfoType;
import models.enums.StudyValidationStatus;
import utils.RuntimeConstants;
import utils.access.RecordManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class ResearchStudyResourceProvider extends RecordBasedResourceProvider<ResearchStudy> implements IResourceProvider {

	// Provide one default constructor
	public ResearchStudyResourceProvider() {
			
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
	
	// Return corresponding FHIR class
	@Override
	public Class<ResearchStudy> getResourceType() {
		return ResearchStudy.class;
	}

	// Main search function for resource. May have another name.
	// Copy method signature from happy fhir implementation.
	// Look at http://hapifhir.io/xref-jpaserver/ca/uhn/fhir/jpa/rp/dstu3/ObservationResourceProvider.html
	// Throw out unsupported search parameters (like "_has" and all starting with "PARAM_" )
	// Replace DateRangeParam with DateAndListParam everywhere except for _lastUpdated	
	// Add non FHIR _page parameter used for the pagination mechanism
	@Search()
	public Bundle getResearchStudy(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

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
		paramMap.add("_language", theResourceLanguage);
	
			
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

	// The actual search method implementation.
	// Basically this "maps" the FHIR query to a MIDATA query and executes it
	public Query buildQuery(SearchParameterMap params) throws AppException {
		
		// get execution context (which user, which app)
		info();

		// construct empty query and a builder for that query
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/ResearchStudy");

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
	
		
	@Override
	public String getRecordFormat() {	
		return "fhir/ResearchStudy";
	}
			
	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type "content" set. 
	// b) Each record must have a "name" that will be shown to the user in the record tree.
	//    The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR representation
	public void prepare(Record record, ResearchStudy theResearchStudy) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a fixed value or something else useful)
		String display = theResearchStudy.getTitle();	
		record.name = display;		
	    record.content = "ResearchStudy";
	    record.code = Collections.singleton("http://midata.coop ResearchStudy");
		record.owner = RuntimeConstants.instance.publicUser;	
		record._id = MidataId.from(theResearchStudy.getId());
		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta" section
		clean(theResearchStudy);
 
	}	
 
	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, ResearchStudy p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);
				
	}

	@Override
	protected void convertToR4(Object in) {
		
	}
	
	public static void updateFromStudy(AccessContext context, MidataId studyId) throws AppException {
		Study study = Study.getById(studyId, Study.ALL);
		updateFromStudy(context, study);
	}
	
	public static void deleteStudy(AccessContext context, MidataId studyId) throws AppException {        
		RecordManager.instance.deleteFromPublic(context, CMaps.map("_id",studyId).map("format","fhir/ResearchStudy").map("public","only").map("content","ResearchStudy"));		
	}
	
	public static void updateFromStudy(AccessContext context, Study study) throws AppException {
		if (study.validationStatus == StudyValidationStatus.DRAFT) return;
		try {
			info();
		} catch (AuthenticationException e) {
						
			ResearchStudyResourceProvider.setAccessContext(context);
		}
				
		ResearchStudyResourceProvider provider = ((ResearchStudyResourceProvider) FHIRServlet.myProviders.get("ResearchStudy")); 
		
		ResearchStudy researchStudy;
		
		boolean doupdate = false;
		Record oldRecord = null;
		
		List<Record> records = RecordManager.instance.list(info().getAccessorRole(), info(), CMaps.map("_id",study._id).map("format","fhir/ResearchStudy").map("public","only").map("content","ResearchStudy"), RecordManager.COMPLETE_DATA); 
		if (!records.isEmpty()) oldRecord = records.get(0);								  
				
		if (oldRecord != null) {
			researchStudy = provider.parse(oldRecord, ResearchStudy.class);
			doupdate = true;
		} else {
			researchStudy = new ResearchStudy();
		}
		
		researchStudy.setId(study._id.toString());
		if (!doupdate) researchStudy.addIdentifier().setSystem("http://midata.coop/codesystems/project-code").setValue(study.code);
		researchStudy.setTitle(study.name);
		researchStudy.setDescription(study.description);
		
		researchStudy.getDescriptionElement().removeExtension("http://hl7.org/fhir/StructureDefinition/translation");
		if (study.infos != null) {
			for (Info info : study.infos) {
				if (info.type == InfoType.SUMMARY) {
					for (Map.Entry<String,String> entry : info.value.entrySet()) {
						if (entry.getKey().equals("int")) {
							researchStudy.setDescription(entry.getValue());
						} else {
							Extension ext = researchStudy.getDescriptionElement().addExtension();
							ext.setUrl("http://hl7.org/fhir/StructureDefinition/translation");
							ext.addExtension("lang", new CodeType(entry.getKey()));
							ext.addExtension("content", new StringType(entry.getValue()));
						}
					}
				}
			}
		}
		
		switch(study.validationStatus) {	
		case DRAFT:
		case VALIDATION:researchStudy.setStatus(ResearchStudy.ResearchStudyStatus.INREVIEW);break;
		case REJECTED:researchStudy.setStatus(ResearchStudy.ResearchStudyStatus.DISAPPROVED);break;
		case VALIDATED:
			switch(study.participantSearchStatus) {
			case PRE:researchStudy.setStatus(ResearchStudy.ResearchStudyStatus.APPROVED);break;
			case SEARCHING:
				researchStudy.setStatus(ResearchStudy.ResearchStudyStatus.ACTIVE);break;				
			case CLOSED:
				switch(study.executionStatus) {
				  case PRE:
				  case RUNNING:
					  researchStudy.setStatus(ResearchStudy.ResearchStudyStatus.ACTIVE);break;
				  case ABORTED:
					  researchStudy.setStatus(ResearchStudy.ResearchStudyStatus.WITHDRAWN);break;
				  case FINISHED:
					  researchStudy.setStatus(ResearchStudy.ResearchStudyStatus.COMPLETED);break;
				}
			}
		}
		
		researchStudy.setArm(new ArrayList<ResearchStudyArmComponent>());
		for (StudyGroup group : study.groups) {
			researchStudy.addArm().setName(group.name).setDescription(group.description);
		}				
		
		Research org = Research.getById(study.owner, Research.ALL);
		if (org != null) {
			Reference sponsor = new Reference();
			sponsor.setDisplay(org.name);
			sponsor.setReference("Organization/"+org._id.toString());
			researchStudy.setSponsor(sponsor);
		}
						
		Period period = new Period();
		period.setStart(study.startDate);
		period.setEnd(study.endDate);
						
		researchStudy.setPeriod(period);		
		if (!doupdate) researchStudy.getMeta().addSecurity().setSystem("http://midata.coop/codesystems/security").setCode("public");
		
		if (doupdate) {
		  provider.updateRecord(oldRecord, researchStudy, provider.getAttachments(researchStudy));
		} else {
		  provider.createResource(researchStudy);
		}
		
	}
	
	

}