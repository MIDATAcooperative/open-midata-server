package utils.fhir;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import models.HealthcareProvider;
import models.MidataId;
import models.Record;
import models.Research;
import models.enums.UserRole;
import utils.RuntimeConstants;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class OrganizationResourceProvider extends RecordBasedResourceProvider<Organization> implements IResourceProvider {

	// Provide one default constructor
	public OrganizationResourceProvider() {
			
		searchParamNameToPathMap.put("Organization:endpoint", "endpoint");
		searchParamNameToPathMap.put("Organization:part-of", "partOf");
		
		searchParamNameToTypeMap.put("Organization:endpoint", Sets.create("Endpoint", "PractitionerRole"));
		searchParamNameToTypeMap.put("Organization:part-of", Sets.create("Organization"));
				
		// Use name of @Search function as last parameter
		registerSearches("Organization", getClass(), "getOrganization");
	}
	
	// Return corresponding FHIR class
	@Override
	public Class<Organization> getResourceType() {
		return Organization.class;
	}

	// Main search function for resource. May have another name.
	// Copy method signature from happy fhir implementation.
	// Look at http://hapifhir.io/xref-jpaserver/ca/uhn/fhir/jpa/rp/dstu3/ObservationResourceProvider.html
	// Throw out unsupported search parameters (like "_has" and all starting with "PARAM_" )
	// Replace DateRangeParam with DateAndListParam everywhere except for _lastUpdated	
	// Add non FHIR _page parameter used for the pagination mechanism
	@Search()
	public Bundle getOrganization(
			@Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId,

			@Description(shortDefinition = "The resource language") @OptionalParam(name = "_language") StringAndListParam theResourceLanguage,

			@Description(shortDefinition="Is the Organization record active")
  			@OptionalParam(name="active")
  			TokenAndListParam theActive,
  			
  			@Description(shortDefinition = "A server defined search that may match any of the string fields in the Address, including line, city, district, state, country, postalCode, and/or text	Organization.address") @OptionalParam(name = "address") StringAndListParam theAddress,

			@Description(shortDefinition = "A city specified in an address") @OptionalParam(name = "address-city") StringAndListParam theAddress_city,

			@Description(shortDefinition = "A state specified in an address") @OptionalParam(name = "address-state") StringAndListParam theAddress_state,

			@Description(shortDefinition = "A postalCode specified in an address") @OptionalParam(name = "address-postalcode") StringAndListParam theAddress_postalcode,

			@Description(shortDefinition = "A country specified in an address") @OptionalParam(name = "address-country") StringAndListParam theAddress_country,

			@Description(shortDefinition = "A use code specified in an address") @OptionalParam(name = "address-use") TokenAndListParam theAddress_use,

			@Description(shortDefinition="Technical endpoints providing access to services operated for the organization")
  			@OptionalParam(name="endpoint", targetTypes={ Endpoint.class } )
  			ReferenceAndListParam theEndpoint, 
			
  			@Description(shortDefinition = "A portion of the organization's name or alias") 
			@OptionalParam(name = "name") 
			StringAndListParam theName,
  			
			@Description(shortDefinition="An organization of which this organization forms a part")
  			@OptionalParam(name="partof", targetTypes={ Organization.class } )
  			ReferenceAndListParam thePartof, 
			
  			@Description(shortDefinition = "A portion of the organization's name using some kind of phonetic matching algorithm") 
			@OptionalParam(name = "phonetic") 
			StringAndListParam thePhonetic,
  			
			@Description(shortDefinition="Any identifier for the organization (not the accreditation issuer's identifier)")
  			@OptionalParam(name="identifier")
  			TokenAndListParam theIdentifier,
											
			@Description(shortDefinition="A code for the type of organization")
  			@OptionalParam(name="type")
  			TokenAndListParam theType,
						
						  		
 			@IncludeParam(reverse=true)
 			Set<Include> theRevIncludes,
 			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
 			@OptionalParam(name="_lastUpdated")
 			DateRangeParam theLastUpdated, 
 
 			@IncludeParam(allow= {
 					"Organization:endpoint",
 					"Organization:part-of", 					  					
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
						
		paramMap.add("active", theActive);		
		paramMap.add("address", theAddress);		
		paramMap.add("address-city", theAddress_city);		
		paramMap.add("address-country", theAddress_country);		
		paramMap.add("address-postalcode", theAddress_postalcode);		
		paramMap.add("address-state", theAddress_state);		
		paramMap.add("address-use", theAddress_use);					
		paramMap.add("endpoint", theEndpoint);						
		paramMap.add("identifier", theIdentifier);				
		paramMap.add("name", theName);				
		paramMap.add("partof", thePartof);				
		paramMap.add("phonetic", thePhonetic);		
		
		
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
	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		
		// get execution context (which user, which app)
		ExecutionInfo info = info();

		// construct empty query and a builder for that query
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Organization");

		// Now all possible searches need to be handeled. For performance reasons it makes sense
		// to put searches that are very restrictive and frequently used first in order
	
		// Add default handling for the _id search parameter
		builder.handleIdRestriction();
					
        // Add handling for a multiTYPE search: "date" may be search on effectiveDateTime or effectivePeriod
        // Note that path = "effective" and type = TYPE_DATETIME_OR_PERIOD
        // If the search was only on effectiveDateTime then
        // type would be TYPE_DATETIME and path would be "effectiveDateTime" instead										
		
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restriction("name", true, QueryBuilder.TYPE_STRING, "name");
		builder.restriction("phonetic", true, QueryBuilder.TYPE_STRING, "name");
		builder.restriction("partof", true, "Organization", "partOf");				
		builder.restriction("type", true, QueryBuilder.TYPE_CODEABLE_CONCEPT, "type");
		builder.restriction("endpoint", true, "Endpoint", "endpoint");
		
		builder.restriction("address-city", true, QueryBuilder.TYPE_STRING, "address.city");
		builder.restriction("address-country", true, QueryBuilder.TYPE_STRING, "address.country");
		builder.restriction("address-postalcode", true, QueryBuilder.TYPE_STRING, "address.postalCode");
		builder.restriction("address-state", true, QueryBuilder.TYPE_STRING, "address.state");
		builder.restriction("address-use", false, QueryBuilder.TYPE_CODE, "address.use");
		builder.restrictionMany("address", true, QueryBuilder.TYPE_STRING, "address.text", "address.line", "address.city", "address.district", "address.state", "address.postalCode",
				"address.country");
		
																		
		builder.restriction("active", false, QueryBuilder.TYPE_BOOLEAN, "active");
		//query.putAccount("public", "only");
		// At last execute the constructed query
		return query.execute(info);
	}
	
		
	@Override
	public String getRecordFormat() {	
		return "fhir/Organization";
	}
			
	// Prepare a Midata record to be written into the database. Tasks:
	// a) Each record must have syntactical type "format" set and semantical type "content" set. 
	// b) Each record must have a "name" that will be shown to the user in the record tree.
	//    The name should describe the content, should not reveal secrets.
	// c) If the "subject" is the record owner he should be removed from the FHIR representation
	public void prepare(Record record, Organization theOrganization) throws AppException {
		// Task a : Set Record "content" field by using a code from the resource (or a fixed value or something else useful)
		String display = theOrganization.getName();	
		record.name = display;		
	    record.content = "Organization";
	    record.code = Collections.singleton("http://midata.coop Organization");
		record.owner = RuntimeConstants.instance.publicUser;	
		record._id = MidataId.from(theOrganization.getId());
		// Other cleaning tasks: Remove _id from FHIR representation and remove "meta" section
		clean(theOrganization);
 
	}	
 
	// Prepare a FHIR resource for output to the user
	// Basically re-add the stuff that was taken away by prepare
	@Override
	public void processResource(Record record, Organization p) throws AppException {
		// Add _id field and meta section
		super.processResource(record, p);
				
	}

	@Override
	protected void convertToR4(Object in) {
		
	}
	
	public static void updateFromResearch(MidataId executor, MidataId researchId) throws AppException {
		Research research = Research.getById(researchId, Research.ALL);
		updateFromResearch(executor, research);
	}
	
	public static void updateFromHP(MidataId executor, MidataId orgId) throws AppException {
		HealthcareProvider provider = HealthcareProvider.getById(orgId, HealthcareProvider.ALL);
		updateFromHP(executor, provider);
	}
	
	public static void deleteOrganization(MidataId executor, MidataId orgId) throws AppException {        
		RecordManager.instance.deleteFromPublic(executor, CMaps.map("_id",orgId).map("format","fhir/Organization").map("public","only").map("content","Organization"));		
	}
	
	public static void updateFromResearch(MidataId executor, Research research) throws AppException {
		
		try {
			info();
		} catch (AuthenticationException e) {
			ExecutionInfo inf = new ExecutionInfo(executor, UserRole.RESEARCH);
			
			OrganizationResourceProvider.setExecutionInfo(inf);
		}
				
		OrganizationResourceProvider provider = ((OrganizationResourceProvider) FHIRServlet.myProviders.get("Organization")); 
		
		Organization org;
		
		boolean doupdate = false;
		Record oldRecord = null;
		
		List<Record> records = RecordManager.instance.list(info().executorId, info().role, info().context, CMaps.map("_id",research._id).map("format","fhir/Organization").map("public","only").map("content","Organization"), RecordManager.COMPLETE_DATA); 
		if (!records.isEmpty()) oldRecord = records.get(0);								  
				
		if (oldRecord != null) {
			org = provider.parse(oldRecord, Organization.class);
			doupdate = true;
		} else {
			org = new Organization();
		}
		
		org.setId(research._id.toString());		
		org.setName(research.name);
		
		if (!doupdate) org.getMeta().addSecurity().setSystem("http://midata.coop/codesystems/security").setCode("public");
		
		if (doupdate) {
		  provider.updateRecord(oldRecord, org);
		} else {
		  provider.createResource(org);
		}
		
	}
	
   public static void updateFromHP(MidataId executor, HealthcareProvider healthProvider) throws AppException {
		
		try {
			info();
		} catch (AuthenticationException e) {
			ExecutionInfo inf = new ExecutionInfo(executor, UserRole.RESEARCH);
			
			OrganizationResourceProvider.setExecutionInfo(inf);
		}
				
		OrganizationResourceProvider provider = ((OrganizationResourceProvider) FHIRServlet.myProviders.get("Organization")); 
		
		Organization org;
		
		boolean doupdate = false;
		Record oldRecord = null;
		
		List<Record> records = RecordManager.instance.list(info().executorId, info().role, info().context, CMaps.map("_id",healthProvider._id).map("format","fhir/Organization").map("public","only").map("content","Organization"), RecordManager.COMPLETE_DATA); 
		if (!records.isEmpty()) oldRecord = records.get(0);								  
				
		if (oldRecord != null) {
			org = provider.parse(oldRecord, Organization.class);
			doupdate = true;
		} else {
			org = new Organization();
		}
		
		org.setId(healthProvider._id.toString());		
		org.setName(healthProvider.name);
		
		if (!doupdate) org.getMeta().addSecurity().setSystem("http://midata.coop/codesystems/security").setCode("public");
		
		if (doupdate) {
		  provider.updateRecord(oldRecord, org);
		} else {
		  provider.createResource(org);
		}
		
	}
		
}