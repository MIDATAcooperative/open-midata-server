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

import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
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
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import models.HealthcareProvider;
import models.MidataId;
import models.Record;
import models.Research;
import models.enums.AuditEventType;
import models.enums.EntityType;
import models.enums.ResearcherRole;
import models.enums.UserStatus;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.OrganizationTools;
import utils.QueryTagTools;
import utils.RuntimeConstants;
import utils.UserGroupTools;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
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

	public Query buildQuery(SearchParameterMap params) throws AppException {
		
		// get execution context (which user, which app)
		info();

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
		builder.restrictionMany("name", true, QueryBuilder.TYPE_STRING, "name", "alias");
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
		query.putAccount("public", "also");
		// At last execute the constructed query
		return query;
	}
	
	public List<Organization> search(AccessContext context, String identifier, String name, String city, boolean onlyActive) throws AppException {
		SearchParameterMap params = new SearchParameterMap();
		if (name != null) params.add("name", new StringParam(name));		
		if (city != null) params.add("address-city", new StringParam(city));
		if (identifier != null) {
			if (identifier.contains("|")) {
				String splitted[] = identifier.split("\\|");
				params.add("identifier", new TokenParam(splitted[0], splitted.length > 1 ? splitted[1] : null));
			} else params.add("identifier", new TokenParam(identifier));
		}
		if (onlyActive) params.add("active", new TokenParam("true"));
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, "fhir/Organization");
		builder.restriction("identifier", true, QueryBuilder.TYPE_IDENTIFIER, "identifier");
		builder.restrictionMany("name", true, QueryBuilder.TYPE_STRING, "name", "alias");
		builder.restriction("address-city", true, QueryBuilder.TYPE_STRING, "address.city");
		builder.restriction("active", false, QueryBuilder.TYPE_BOOLEAN, "active");		
		query.putAccount("public", "only");
		query.putAccount("content", "Organization/HP");
		List<Record> result = query.execute(context);
		return parse(result, getResourceType());
	}
	
		
	@Override
	public String getRecordFormat() {	
		return "fhir/Organization";
	}
	
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Organization theOrganization) {
		return super.createResource(theOrganization);
	}
		
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Organization theOrganization) {
		return super.updateResource(theId, theOrganization);
	}
	
	@Override
	public void updatePrepare(Record record, Organization theResource) throws AppException {
		if (theResource.getUserData("source")==null && record.content.equals("Organization/HP")) {
			AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.ORGANIZATION_CHANGED).withActor(info(), info().getActor()).withApp(info().getUsedPlugin()).withMessage(theResource.getName()));
			HealthcareProvider hp = HealthcareProvider.getByIdAlsoDeleted(record._id, HealthcareProvider.ALL);
			hp.name = theResource.getName();
			extractAddress(theResource, hp);
			MidataId oldParent = hp.parent;
			if (theResource.hasPartOf()) {
			   Reference parentRef = theResource.getPartOf();
			   hp.parent =  MidataId.parse(parentRef.getId());
			} else hp.parent = null;
			OrganizationTools.prepareModel(info(), hp, oldParent);
			record.mapped = hp;
			
		}
		if (!record.content.equals("Organization/HP") && record.tags != null && record.tags.contains("security:generated")) {
			if (theResource.getUserData("source") == null) throw new ForbiddenOperationException("Update not allowed on generated resources");
		}
		super.updatePrepare(record, theResource);		
	}
	
	
			
	@Override
	public void createPrepare(Record record, Organization theOrganization) throws AppException {
		
		Object source = theOrganization.getUserData("source");
		if (source != null && source.equals("HP")) {
		  record.content = "Organization/HP";			
		  record.code = Collections.singleton("http://midata.coop Organization/HP");
		  record._id = MidataId.from(theOrganization.getId());
		  addSecurityTag(record, theOrganization, QueryTagTools.SECURITY_PLATFORM_MAPPED);
		} else if (source != null && source.equals("Research")) {
		  record.content = "Organization/Research";
		  record.code = Collections.singleton("http://midata.coop Organization/Research");
		  record._id = MidataId.from(theOrganization.getId());
		  addSecurityTag(record, theOrganization, QueryTagTools.SECURITY_PLATFORM_MAPPED);
		} else {
		
		  if (theOrganization.getMeta().getSecurity("http://midata.coop/codesystems/security", "platform-mapped") != null) {
			  record.content = "Organization/HP";			
			  record.code = Collections.singleton("http://midata.coop Organization/HP");
			  HealthcareProvider hp = new HealthcareProvider();
			  hp.name = theOrganization.getName();
			  hp._id = record._id;
			  extractAddress(theOrganization, hp);
			  OrganizationTools.prepareModel(info(), hp, null);
			  record.mapped = hp;
		  } else {						
			  record.content = "Organization";
			  record.code = Collections.singleton("http://midata.coop Organization");
		  }
		}			   
		record.owner = RuntimeConstants.instance.publicUser;	
		
		super.createPrepare(record, theOrganization);
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
		
		for (Extension ext : theOrganization.getExtensionsByUrl("http://midata.coop/extensions/managed-by")) {
			Type t = ext.getValue();
			if (t instanceof Reference) t = FHIRTools.resolve((Reference) t);		
			ext.setValue(t);
		}
		
		for (Identifier id : theOrganization.getIdentifier()) {
			OrganizationTools.checkIdentifier(record, id.getSystem(), id.getValue());			
		}
		
		theOrganization.setPartOf(FHIRTools.resolve(theOrganization.getPartOf()));		
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
	public Organization createExecute(Record record, Organization theResource) throws AppException {	
		if (record.content.equals("Organization/HP") && theResource.getUserData("source")==null) {
			AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.ORGANIZATION_CREATED).withActor(info(), info().getActor()).withApp(info().getUsedPlugin()).withMessage(theResource.getName()));
			MidataId parent = null;
			if (theResource.hasPartOf()) {
			   Reference parentRef = theResource.getPartOf();
			   if (parentRef.hasReference()) {
			     parent =  MidataId.parse(parentRef.getReferenceElement().getIdPart());
			   }
			}
		   HealthcareProvider provider = (HealthcareProvider) record.mapped;
		   provider = UserGroupTools.createOrUpdateOrganizationUserGroup(info(), record._id, theResource.getName(), provider, parent, true, false);
		   try {
		       UserGroupTools.updateManagers(info(), record._id, new ArrayList<IBaseExtension>(theResource.getExtension()), true);
		       Organization result = super.createExecute(record, theResource);
			   AuditManager.instance.success();
		       return result;
		   } catch (AppException e) {
			   HealthcareProvider.delete(provider._id);			   
			   throw e;
		   }
		   
		}		
		Organization result = super.createExecute(record, theResource);

		return result;
		
	}

	@Override
	protected void convertToR4(Object in) {
		
	}
	
	public static void updateFromResearch(AccessContext context, MidataId researchId) throws AppException {
		Research research = Research.getById(researchId, Research.ALL);
		updateFromResearch(context, research);
	}
	
	public static void updateFromHP(AccessContext context, MidataId orgId) throws AppException {
		HealthcareProvider provider = HealthcareProvider.getByIdAlsoDeleted(orgId, HealthcareProvider.ALL);
		updateFromHP(context, provider);
	}
	
	public static void deleteOrganization(AccessContext context, MidataId orgId) throws AppException {        
		RecordManager.instance.deleteFromPublic(context, CMaps.map("_id",orgId).map("format","fhir/Organization").map("public","only").map("content","Organization"));		
	}
	
	public static void updateFromResearch(AccessContext context, Research research) throws AppException {
		
		try {
			info();
		} catch (AuthenticationException e) {						
			OrganizationResourceProvider.setAccessContext(context);
		}
				
		OrganizationResourceProvider provider = ((OrganizationResourceProvider) FHIRServlet.myProviders.get("Organization")); 
		
		Organization org;
		
		boolean doupdate = false;
		Record oldRecord = null;
		
		List<Record> records = RecordManager.instance.list(info().getAccessorRole(), info(), CMaps.map("_id",research._id).map("format","fhir/Organization").map("public","only").map("content","Organization/Research"), RecordManager.COMPLETE_DATA);
		if (!records.isEmpty()) oldRecord = records.get(0);								  
				
		if (oldRecord != null) {
			org = provider.parse(oldRecord, Organization.class);
			doupdate = true;
		} else {
			org = new Organization();
		}
		
		org.setId(research._id.toString());		
		org.setName(research.name);
		org.setUserData("source", "Research");
		
		if (!doupdate) {
			org.getMeta().addSecurity().setSystem("http://midata.coop/codesystems/security").setCode("public");
			org.getMeta().addSecurity().setSystem("http://midata.coop/codesystems/security").setCode("generated");			
		}				
		
		provider.addSecurityTag(oldRecord, org, QueryTagTools.SECURITY_PLATFORM_MAPPED);
		
		if (doupdate) {
		  provider.updatePrepare(oldRecord, org);
		  provider.updateRecord(oldRecord, org, provider.getAttachments(org));
		} else {
		  //RecordManager.instance.wipeFromPublic(executor, CMaps.map("_id", research._id).map("format","fhir/Organization"));		  
		  provider.createResource(org);
		}
		
	}
	
	public static List<String> getIdentifiers(AccessContext context, HealthcareProvider healthProvider) throws AppException {
		try {
			info();
		} catch (AuthenticationException e) {					
			OrganizationResourceProvider.setAccessContext(context);
		}
		
        OrganizationResourceProvider provider = ((OrganizationResourceProvider) FHIRServlet.myProviders.get("Organization")); 
				 
		List<Record> records = RecordManager.instance.list(info().getAccessorRole(), info(), CMaps.map("_id",healthProvider._id).map("format","fhir/Organization").map("public","only").map("content","Organization/HP"), RecordManager.COMPLETE_DATA); 
		if (!records.isEmpty()) {
			Organization org = provider.parse(records.get(0), Organization.class);
			List<String> result = new ArrayList<String>();
			for (Identifier id : org.getIdentifier()) {
				result.add(id.getSystem()+"|"+id.getValue());
			}
			return result;
		}
				
		return null;
	}
	
   public static void updateFromHP(AccessContext context, HealthcareProvider healthProvider) throws AppException {
		AccessLog.logBegin("begin update organization resource from model");
		try {
			info();
		} catch (AuthenticationException e) {					
			OrganizationResourceProvider.setAccessContext(context);
		}
				
		OrganizationResourceProvider provider = ((OrganizationResourceProvider) FHIRServlet.myProviders.get("Organization")); 
		
		Organization org;
		
		boolean doupdate = false;
		Record oldRecord = null;
		
		List<Record> records = RecordManager.instance.list(info().getAccessorRole(), info(), CMaps.map("_id",healthProvider._id).map("format","fhir/Organization").map("public","only").map("content","Organization/HP"), RecordManager.COMPLETE_DATA); 
		if (!records.isEmpty()) oldRecord = records.get(0);								  
				
		if (oldRecord != null) {
			org = provider.parse(oldRecord, Organization.class);
			doupdate = true;
		} else {
			org = new Organization();
		}
		
		org.setId(healthProvider._id.toString());		
		org.setName(healthProvider.name);
		org.setUserData("source", "HP");
		
		if (healthProvider.parent != null) {
			HealthcareProvider prov = HealthcareProvider.getByIdAlsoDeleted(healthProvider.parent, HealthcareProvider.ALL);
			if (prov != null) {
			  org.setPartOf(new Reference("Organization/"+healthProvider.parent).setDisplay(prov.name));
			}
		} else org.setPartOf(null);
		
		if (!doupdate) {
			org.getMeta().addSecurity().setSystem("http://midata.coop/codesystems/security").setCode("public");
			org.getMeta().addSecurity().setSystem("http://midata.coop/codesystems/security").setCode("generated");			
		}
		
		if (healthProvider.status != null && (healthProvider.status.isDeleted() || healthProvider.status == UserStatus.NEW)) {
			org.setActive(false);
		} else {
			org.setActive(true);
		}
		
		if (healthProvider.identifiers != null) {
			List<Identifier> ids = new ArrayList<Identifier>();
			for (String identifierStr : healthProvider.identifiers) {
				String parts[] = identifierStr.split("[\\s\\|]");
				Identifier identifier = new Identifier();				
				if (parts.length==1) identifier.setValue(parts[0]);
				else if (parts.length>=2) identifier.setSystem(parts[0]).setValue(parts[1]);
				for (Identifier oldId : org.getIdentifier()) {
					if (oldId.getValue().equals(identifier.getValue()) && oldId.getSystem() != null && oldId.getSystem().equals(identifier.getSystem())) identifier = oldId;
				}
				ids.add(identifier);
			}
			
			org.setIdentifier(ids);
		}
		
		Address adr = new Address();
		adr.setCity(healthProvider.city);
		adr.setPostalCode(healthProvider.zip);
		adr.setCountry(healthProvider.country);
		List<StringType> lines = new ArrayList<StringType>(2);
		if (healthProvider.address1 != null) lines.add(new StringType(healthProvider.address1));
		if (healthProvider.address2 != null) lines.add(new StringType(healthProvider.address2));
		adr.setLine(lines);
		org.getAddress().clear();
		org.addAddress(adr);
		
		if (healthProvider.phone != null && healthProvider.phone.length() > 0) {
			org.getTelecom().clear();
			org.addTelecom().setSystem(ContactPointSystem.PHONE).setValue(healthProvider.phone);
		}
		
		provider.addSecurityTag(oldRecord, org, QueryTagTools.SECURITY_PLATFORM_MAPPED);
		if (doupdate) {
		  provider.updateRecord(oldRecord, org, provider.getAttachments(org));
		} else {
 		  //RecordManager.instance.wipeFromPublic(executor, CMaps.map("_id", healthProvider._id).map("format","fhir/Organization"));
		  provider.createResource(org);
		}
		AccessLog.logEnd("end update organization from model");
	}
   
   private void extractAddress(Organization theResource, HealthcareProvider prov) {
	   if (theResource.hasAddress()) {
		   Address adr = theResource.getAddressFirstRep();		   
		   prov.city = adr.getCity();
		   prov.zip = adr.getPostalCode();
		   prov.country = adr.getCountry();
		   List<StringType> lines = adr.getLine();
		   if (lines != null && lines.size() > 0) prov.address1 = lines.get(0).getValue();
		   if (lines != null && lines.size() > 1) prov.address2 = lines.get(1).getValue();
		   
		   for (ContactPoint cp : theResource.getTelecom()) {
			  if (cp.getSystem() == ContactPointSystem.PHONE) {
				  prov.phone = cp.getValue();
			  }
		   }
	   } 
	   
   }
   
   

	@Override
	public void updateExecute(Record record, Organization theResource) throws AppException {
		
		super.updateExecute(record, theResource);
		if (theResource.getUserData("Source")==null) {	
			HealthcareProvider hp = (HealthcareProvider) record.mapped;
			if (!theResource.getActive() && (hp.status == null || !hp.status.isDeleted())) {
				hp.status = UserStatus.DELETED;
			}
			
			OrganizationTools.updateModel(info(), hp);   						
			UserGroupTools.updateManagers(info(), record._id, new ArrayList<IBaseExtension>(theResource.getExtension()), false);
		}
	}
	

	@Operation(name="$register-local", idempotent=false)
	public Organization registerLocal(				  
	   @OperationParam(name="org") Organization theResource	  
	   ) throws AppException {
		  AccessContext context = info();
		  if (context.getAccessorEntityType() != EntityType.SERVICES) throw new InvalidRequestException("Wrong application type.");
		  MidataId resourceId = new MidataId();
		  theResource.setId(resourceId.toString());
		  ApplicationTools.createDataBrokerGroup(info(), resourceId , theResource.getName());
		  return theResource;
	}
}