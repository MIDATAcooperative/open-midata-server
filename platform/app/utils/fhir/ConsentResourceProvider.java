package utils.fhir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Consent.ConsentActorComponent;
import org.hl7.fhir.dstu3.model.Consent.ConsentState;
import org.hl7.fhir.dstu3.model.Consent.ExceptComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.codesystems.ConsentExceptType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Group.GroupMemberComponent;
import org.hl7.fhir.dstu3.model.Group.GroupType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Elements;
import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.param.CompositeAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import controllers.Circles;
import controllers.UserGroups;
import controllers.members.HealthProvider;
import models.Consent;
import models.MidataId;
import models.Plugin;
import models.Record;
import models.TypedMidataId;
import models.User;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EntityType;
import models.enums.UserStatus;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.access.Feature_FormatGroups;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

public class ConsentResourceProvider extends ResourceProvider<org.hl7.fhir.dstu3.model.Consent> implements IResourceProvider {

	public ConsentResourceProvider() {
		registerSearches("Consent", getClass(), "getConsent");
	}
	
	@Override
	public Class<org.hl7.fhir.dstu3.model.Consent> getResourceType() {
		return org.hl7.fhir.dstu3.model.Consent.class;
	}

	/**
	 * Default implementation to retrieve a FHIR resource by id.
	 * @param theId ID of resource to be retrieved
	 * @return Resource read from database
	 * @throws AppException
	 */
	@Read()
	public org.hl7.fhir.dstu3.model.Consent getResourceById(@IdParam IIdType theId) throws AppException {
		models.Consent consent = Circles.getConsentById(info().executorId, MidataId.from(theId.getIdPart()), Consent.ALL);	
		if (consent == null) return null;
		return readConsentFromMidataConsent(consent, true);
	}
	
    @History()
    @Override
	public List<org.hl7.fhir.dstu3.model.Consent> getHistory(@IdParam IIdType theId) throws AppException {
    	throw new ResourceNotFoundException("No history kept for Consent resource"); 
    }
    
	/**
	 * Convert a MIDATA User object into a FHIR person object
	 * @param userToConvert user to be converted into a FHIR object
	 * @return FHIR person
	 * @throws AppException
	 */
	public org.hl7.fhir.dstu3.model.Consent readConsentFromMidataConsent(models.Consent consentToConvert, boolean addMembers) throws AppException {
		if (consentToConvert.fhirConsent==null) {
			Circles.fillConsentFields(info().executorId, Collections.singleton(consentToConvert), models.Consent.FHIR);
			updateMidataConsent(consentToConvert);
		}
		IParser parser = ctx().newJsonParser();
		AccessLog.log(consentToConvert.fhirConsent.toString());
		org.hl7.fhir.dstu3.model.Consent p = parser.parseResource(getResourceType(), consentToConvert.fhirConsent.toString());
		
		if (consentToConvert.sharingQuery == null) Circles.fillConsentFields(info().executorId, Collections.singleton(consentToConvert), Sets.create("sharingQuery"));
		
		Map<String, Object> query = consentToConvert.sharingQuery;
		if (query != null) buildQuery(query, p);
		
		if (EntityType.USERGROUP.equals(consentToConvert.entityType)) {
			for (MidataId auth : consentToConvert.authorized) {
			   p.addActor().setReference(new Reference("Group/"+auth.toString()));
			}			
		} else {
			for (MidataId auth : consentToConvert.authorized) {
				try {
			      p.addActor().setRole(new CodeableConcept().addCoding(new Coding().setSystem("http://hl7.org/fhir/v3/RoleCode").setCode("GRANTEE"))).setReference(FHIRTools.getReferenceToUser(auth, null));
				} catch (InternalServerException e) {}
			}
		}
		
		return p;
	}
	
	private void buildQuery(Map<String, Object> query, org.hl7.fhir.dstu3.model.Consent p) throws AppException {
        if (query.containsKey("content")) {
		  	ExceptComponent ex = p.addExcept();
		  	Set<String> vals = utils.access.Query.getRestriction(query.get("content"), "content");
		  	for (String s : vals) ex.addClass_().setSystem("http://midata.coop/codesystems/content").setCode(s);
		}
        if (query.containsKey("group")) {
		  	ExceptComponent ex = p.addExcept();
		  	Set<String> vals = utils.access.Query.getRestriction(query.get("group"), "group");
		  	for (String s : vals) ex.addClass_().setSystem("http://midata.coop/codesystems/group").setCode(s);
		}
        if (query.containsKey("format")) {
		  	ExceptComponent ex = p.addExcept();
		  	Set<String> vals = utils.access.Query.getRestriction(query.get("format"), "format");
		  	for (String s : vals) ex.addClass_().setSystem("http://midata.coop/codesystems/format").setCode(s);
		}
	}
	
	public static void updateMidataConsent(models.Consent consentToConvert) throws AppException {
		org.hl7.fhir.dstu3.model.Consent c = new org.hl7.fhir.dstu3.model.Consent();

		c.setId(consentToConvert._id.toString());
		
		switch (consentToConvert.status) {
		case ACTIVE:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.ACTIVE);break;
		case UNCONFIRMED:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.PROPOSED);break;
		case REJECTED:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.REJECTED);break;
		case EXPIRED:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.INACTIVE);break;
		}
		
		String categoryCode = consentToConvert.categoryCode;
		if (categoryCode == null) categoryCode = "default";
		c.addCategory().addCoding().setCode(categoryCode).setSystem("http://midata.coop/codesystems/consent-category");

		if (consentToConvert.owner != null) {
		  c.setPatient(FHIRTools.getReferenceToUser(User.getById(consentToConvert.owner, Sets.create("role", "firstname", "lastname", "email"))));
		} else if (consentToConvert.externalOwner != null) {
		  c.setPatient(new Reference().setIdentifier(new Identifier().setSystem("http://midata.coop/identifier/patient-login-or-invitation").setValue(consentToConvert.externalOwner)));
		}
		
		if (consentToConvert.validUntil != null) {
		  c.setPeriod(new Period().setEnd(consentToConvert.validUntil));	
		}
		if (consentToConvert.createdBefore != null) {
		  c.setDataPeriod(new Period().setEnd(consentToConvert.createdBefore));
		}
		if (consentToConvert.dateOfCreation != null) {
		  c.setDateTime(consentToConvert.dateOfCreation);
		}
		c.setPolicyRule("http://hl7.org/fhir/ConsentPolicy/opt-in");
		c.addPurpose(new Coding("http://midata.coop/codesystems/consent-type", consentToConvert.type.toString(), null));
		
		c.addExtension().setUrl("http://midata.coop/extensions/consent-name").setValue(new StringType(consentToConvert.name));
		
		String encoded = ctx.newJsonParser().encodeResourceToString(c);		
		consentToConvert.fhirConsent = (DBObject) JSON.parse(encoded);				
	}
			
	   @Search()
	    public List<IBaseResource> getConsent(
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
	    		DateRangeParam theDate, 
	    		    
	    		@Description(shortDefinition="Identifier for this record (external references)")
	    		@OptionalParam(name="identifier")
	    		TokenAndListParam theIdentifier, 
	    		   
	    		@Description(shortDefinition="Custodian of the consent")
	    		@OptionalParam(name="organization", targetTypes={  } )
	    		ReferenceAndListParam theOrganization, 
	    		   
	    		@Description(shortDefinition="Who the consent applies to")
	    		@OptionalParam(name="patient", targetTypes={  } )
	    		ReferenceAndListParam thePatient, 
	    		  
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
	    	    @Elements Set<String> theElements
	    
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
			
	    	paramMap.setRevIncludes(theRevIncludes);
	    	paramMap.setLastUpdated(theLastUpdated);
	    	paramMap.setIncludes(theIncludes);
	    	paramMap.setSort(theSort);
	    	paramMap.setCount(theCount);
	    	paramMap.setElements(theElements);
	    	paramMap.setSummary(theSummary);
	    	    		    	
	    	return search(paramMap);    	    	    	
	    }
	
	@Override
	public List<Record> searchRaw(SearchParameterMap params) throws AppException {		
		return null;
	}
 
	@Override
	public List<IBaseResource> search(SearchParameterMap params) {
		try {
					
			//ExecutionInfo info = info();
	
			Query query = new Query();		
			QueryBuilder builder = new QueryBuilder(params, query, null);
				
			builder.handleIdRestriction();
			builder.recordOwnerReference("patient", "Patient", null);
			builder.restriction("category", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "fhirConsent.category");
			builder.restriction("date", false, QueryBuilder.TYPE_DATETIME, "fhirConsent.dateTime");
			builder.restriction("period", false, QueryBuilder.TYPE_DATETIME, "fhirConsent.period");
			builder.restriction("status", false, QueryBuilder.TYPE_CODE, "fhirConsent.status");
			
			Set<String> authorized = null;
			if (params.containsKey("actor")) {
				List<ReferenceParam> actors = builder.resolveReferences("actor", null);
				if (actors != null) {
					authorized = FHIRTools.referencesToIds(actors);				
				}
			}
									
			Map<String, Object> properties = query.retrieveAsNormalMongoQuery();
				
			if (authorized != null) {
				properties.put("authorized", authorized);
			}
			ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "authorized");
			
			Set<models.Consent> consents = new HashSet<models.Consent>();						
			if (authorized == null || authorized.contains(info().ownerId.toString())) consents.addAll(Consent.getAllByAuthorized(info().ownerId, properties, Consent.FHIR));
			if (!properties.containsKey("owner") || utils.access.Query.getRestriction(properties.get("owner"), "owner").contains(info().ownerId.toString())) consents.addAll(Consent.getAllByOwner(info().ownerId, properties, Consent.FHIR));
			List<IBaseResource> result = new ArrayList<IBaseResource>();
			for (models.Consent consent : consents) {
				result.add(readConsentFromMidataConsent(consent, true));
			}
			
			return result;
			
		 } catch (AppException e) {
		       ErrorReporter.report("FHIR (search)", null, e);	       
			   return null;
		 } catch (NullPointerException e2) {
		   	    ErrorReporter.report("FHIR (search)", null, e2);	 
				throw new InternalErrorException(e2);
		}
		
	}
	
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam org.hl7.fhir.dstu3.model.Consent theConsent) {
		return super.createResource(theConsent);
	}
	
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam org.hl7.fhir.dstu3.model.Consent theConsent) {		
		try {			
			return update(theId, theConsent);
		} catch (BaseServerResponseException e) {
			throw e;
		} catch (BadRequestException e2) {
			throw new InvalidRequestException(e2.getMessage());
		} catch (InternalServerException e3) {
			ErrorReporter.report("FHIR (update resource)", null, e3);
			throw new InternalErrorException(e3);
		} catch (Exception e4) {
			ErrorReporter.report("FHIR (update resource)", null, e4);
			throw new InternalErrorException(e4);
		}	
	}
	
	protected MethodOutcome update(IdType theId, org.hl7.fhir.dstu3.model.Consent theResource) throws AppException {
		Consent consent = Circles.getConsentById(info().executorId, MidataId.from(theId.getIdPart()), Consent.FHIR);
		if (consent == null) throw new ResourceNotFoundException(theId);
		
		if ((theResource.getStatus() == ConsentState.ACTIVE || theResource.getStatus() == ConsentState.REJECTED) && consent.status == ConsentStatus.UNCONFIRMED) {
			mayShare(info().pluginId, consent.sharingQuery);
						
			if (theResource.getStatus() == ConsentState.ACTIVE) {
				HealthProvider.confirmConsent(info().executorId, consent._id);
			} else {
				HealthProvider.rejectConsent(info().executorId, consent._id);
			}
		}
		
		MethodOutcome retVal = new MethodOutcome(new IdType("Consent", consent._id.toString(), null), true);	
        retVal.setResource(theResource);
        
		return retVal;		
	}
	
	private static void mayShare(MidataId pluginId, Map<String, Object> query) throws AppException {
		Plugin plugin = Plugin.getById(pluginId);
		if (!plugin.resharesData) throw new ForbiddenOperationException("Plugin is not allowed to share data.");
		if (!isSubQuery(plugin.defaultQuery, query)) throw new ForbiddenOperationException("Plugin is not allowed to share this type of data.");
				
	}
	
	private static boolean isSubQuery(Map<String, Object> masterQuery, Map<String, Object> subQuery) throws AppException {
		
		masterQuery = new HashMap<String, Object>(masterQuery);
		subQuery = new HashMap<String, Object>(subQuery);
		
		String groupSystem = null;
		
		if (masterQuery.containsKey("group-system")) {
		  groupSystem = masterQuery.get("group-system").toString();
		} else {
		  groupSystem = "v1";
		}
		Feature_FormatGroups.convertQueryToContents(groupSystem, masterQuery);
		if (subQuery.containsKey("group-system")) {
		  groupSystem = subQuery.get("group-system").toString();
		} else {
		  groupSystem = "v1";
		}
		Feature_FormatGroups.convertQueryToContents(groupSystem, subQuery);
		
		masterQuery.remove("group-system");
		subQuery.remove("group-system");
		
	    for (Map.Entry<String, Object> entry : masterQuery.entrySet()) {
	    	if (!subQuery.containsKey(entry.getKey())) return false;
	    	Set<String> master = utils.access.Query.getRestriction(entry.getValue(), entry.getKey());
	    	Set<String> sub = utils.access.Query.getRestriction(subQuery.get(entry.getKey()), entry.getKey());
	    	if (!master.containsAll(sub)) return false;	    	
	    }
		return true;
	}
	
	/**
	 * Implementation for create
	 * @param theResource
	 * @return
	 * @throws AppException
	 */
	protected MethodOutcome create(org.hl7.fhir.dstu3.model.Consent theResource) throws AppException {
		models.Consent consent = new Consent();
		
		consent.creatorApp = info().pluginId;
		consent.status = ConsentStatus.UNCONFIRMED;
		consent.authorized = new HashSet<MidataId>();
		consent.categoryCode = theResource.getCategoryFirstRep().getCodingFirstRep().getCode();
		
		consent.name = theResource.getCategoryFirstRep().getText();
		for (Extension ext : theResource.getExtensionsByUrl("http://midata.coop/extensions/consent-name")) {
		  consent.name = ext.getValue().toString();	
		}
		if (consent.name == null) consent.name = "Unnamed";
		
		
		if (theResource.getPeriod() != null) {
		  consent.validUntil = theResource.getPeriod().getEnd();
		}
			
		if (theResource.getDataPeriod() != null) {
		  consent.createdBefore = theResource.getPeriod().getEnd();
		}
			
        consent.type = ConsentType.valueOf(theResource.getPurposeFirstRep().getCode());
		consent.owner = info().ownerId;
		
		for (ConsentActorComponent cac : theResource.getActor()) {
			Reference ref = FHIRTools.resolve(cac.getReference());
			TypedMidataId mid = FHIRTools.getMidataIdFromReference(ref.getReferenceElement());
			if (mid == null) {
			  String login = FHIRTools.getMidataLoginFromReference(ref);
			  if (login != null) {
				  if (consent.externalAuthorized == null) {
					  consent.externalAuthorized = new HashSet<String>();
				  }
				  consent.externalAuthorized.add(login);
			  }
			} else {
			  if (mid.getType().equals("Group")) {
				  if (consent.entityType != null && !consent.entityType.equals(EntityType.USERGROUP)) throw new NotImplementedOperationException("Consent actors need to be all people or all groups. Mixed actors are not supported.");
				  consent.entityType = EntityType.USERGROUP;
			  } else {
				  if (consent.entityType != null && !consent.entityType.equals(EntityType.USER)) throw new NotImplementedOperationException("Consent actors need to be all people or all groups. Mixed actors are not supported.");				  
				  consent.entityType = EntityType.USER;
			  }
			  consent.authorized.add(mid.getMidataId());
			}
		}
		
		Reference patient = theResource.getPatient();
		if (patient != null) {
			patient = FHIRTools.resolve(patient);
			TypedMidataId mid = FHIRTools.getMidataIdFromReference(patient.getReferenceElement());
			if (mid != null) {
			  consent.owner = mid.getMidataId();
			} else {
				String login = FHIRTools.getMidataLoginFromReference(patient);
				if (login != null) {
					consent.externalOwner = login;
					consent.owner = null;
				}
			}
		}
		
		Map<String, Object> query = new HashMap<String, Object>();
		Set<String> contents = new HashSet<String>();
		for (ExceptComponent ec : theResource.getExcept()) {
			if (ec.getType() == org.hl7.fhir.dstu3.model.Consent.ConsentExceptType.PERMIT) {
				for (Coding coding : ec.getClass_()) {
					String system = coding.getSystem();
					if (system.equals("http://midata.coop/codesystems/content")) {
					  contents.add(coding.getCode());	
					}
				}
			}
		}
		if (!contents.isEmpty()) query.put("content", contents);
		consent.sharingQuery = query;
        
		if (theResource.getStatus() == ConsentState.ACTIVE) {
			mayShare(info().pluginId, consent.sharingQuery);
			consent.status = ConsentStatus.ACTIVE;
		} else if (theResource.getStatus() != ConsentState.PROPOSED) {
			throw new ForbiddenOperationException("consent status not supported for creation.");
		}
		
		if (consent.type == ConsentType.IMPLICIT) throw new ForbiddenOperationException("consent type not supported for creation.");
		Circles.addConsent(info().executorId, consent, true, null);
        
		theResource.setDateTime(consent.dateOfCreation);
		
		MethodOutcome retVal = new MethodOutcome(new IdType("Consent", consent._id.toString(), null), true);	
        retVal.setResource(theResource);
        
		return retVal;			
	}
}