package utils.fhir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.AuditEvent;
import org.hl7.fhir.dstu3.model.AuditEvent.AuditEventAgentComponent;
import org.hl7.fhir.dstu3.model.AuditEvent.AuditEventEntityComponent;
import org.hl7.fhir.dstu3.model.AuditEvent.AuditEventOutcome;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IIdType;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Elements;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
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
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import models.Consent;
import models.MidataAuditEvent;
import models.MidataId;
import models.Plugin;
import models.Study;
import models.User;
import models.UserGroupMember;
import models.enums.AuditEventType;
import models.enums.ConsentType;
import models.enums.UserRole;
import utils.AccessLog;
import utils.access.op.AndCondition;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;

public class AuditEventResourceProvider extends ResourceProvider<AuditEvent, MidataAuditEvent> implements IResourceProvider {
	

	public AuditEventResourceProvider() {
		registerSearches("AuditEvent", getClass(), "getAuditEvent");
	}
	
	@Override
	public Class<AuditEvent> getResourceType() {
		return AuditEvent.class;
	}

	/**
	 * Default implementation to retrieve a FHIR resource by id.
	 * @param theId ID of resource to be retrieved
	 * @return Resource read from database
	 * @throws AppException
	 */
	@Read()
	public AuditEvent getResourceById(@IdParam IIdType theId) throws AppException {
		MidataAuditEvent mae = MidataAuditEvent.getById(MidataId.from(theId.getIdPart()));	
		if (mae != null) return readAuditEventFromMidataAuditEvent(mae);
		throw new ResourceNotFoundException(theId);		
	}
	    
    
	/**
	 * Convert a MIDATA User object into a FHIR person object
	 * @param userToConvert user to be converted into a FHIR object
	 * @return FHIR person
	 * @throws AppException
	 */
	public AuditEvent readAuditEventFromMidataAuditEvent(MidataAuditEvent mae) throws AppException {
		
		IParser parser = ctx().newJsonParser();
		
		AuditEvent p = parser.parseResource(getResourceType(), mae.fhirAuditEvent.toString());
						
		switch (mae.status) {
		case 0:p.setOutcome(AuditEventOutcome._0);break;
		case 4:p.setOutcome(AuditEventOutcome._4);break;
		case 8:p.setOutcome(AuditEventOutcome._8);break;
		default:p.setOutcome(AuditEventOutcome._12);
		}
		p.setOutcomeDesc(mae.statusDescription);
		if (mae.statusKey != null) {
			p.addExtension().setUrl("http://midata.coop/extensions/outcome-localekey").setValue(new StringType(mae.statusKey));
		}
		
		//if (mae.event.equals(AuditEventType.STUDY_PARTICIPATION_APPROVED))
		
		return p;
	}
	
		
	   @Search()
	    public Bundle getAuditEvent(
	    		@Description(shortDefinition="The resource identity")
	    		@OptionalParam(name="_id")
	    		StringAndListParam theId, 
	    		 
	    		@Description(shortDefinition="Type of action performed during the event")
	    		@OptionalParam(name="action")
	    		TokenAndListParam theAction, 
	    		   
	    		@Description(shortDefinition="Identifier for the network access point of the user device")
	    		@OptionalParam(name="address")
	    		StringAndListParam theAddress, 
	    		   
	    		@Description(shortDefinition="Direct reference to resource")
	    		@OptionalParam(name="agent", targetTypes={  } )
	    		ReferenceAndListParam theAgent, 
	    		   
	    		@Description(shortDefinition="Human-meaningful name for the agent")
	    		@OptionalParam(name="agent-name")
	    		StringAndListParam theAgent_name, 
	    		    
	    		@Description(shortDefinition="Agent role in the event")
	    		@OptionalParam(name="agent-role")
	    		TokenAndListParam theAgent_role, 
	    		   
	    		@Description(shortDefinition="Alternative User id e.g. authentication")
	    		@OptionalParam(name="altid")
	    		TokenAndListParam theAltid, 
	    		   
	    		@Description(shortDefinition="Time when the event occurred on source")
	    		@OptionalParam(name="date")
	    		DateAndListParam theDate, 
	    		   
	    		@Description(shortDefinition="Specific instance of resource")
	    		@OptionalParam(name="entity", targetTypes={  } )
	    		ReferenceAndListParam theEntity, 
	    		   
	    		@Description(shortDefinition="Specific instance of object")
	    		@OptionalParam(name="entity-id")
	    		TokenAndListParam theEntity_id, 
	    		   
	    		@Description(shortDefinition="Descriptor for entity")
	    		@OptionalParam(name="entity-name")
	    		StringAndListParam theEntity_name, 
	    		  
	    		@Description(shortDefinition="What role the entity played")
	    		@OptionalParam(name="entity-role")
	    		TokenAndListParam theEntity_role, 
	    		   
	    		@Description(shortDefinition="Type of entity involved")
	    		@OptionalParam(name="entity-type")
	    		TokenAndListParam theEntity_type, 
	    		  
	    		@Description(shortDefinition="Whether the event succeeded or failed")
	    		@OptionalParam(name="outcome")
	    		TokenAndListParam theOutcome, 
	    		  
	    		@Description(shortDefinition="Direct reference to resource")
	    		@OptionalParam(name="patient", targetTypes={  } )
	    		ReferenceAndListParam thePatient, 
	    		  
	    		@Description(shortDefinition="Policy that authorized event")
	    		@OptionalParam(name="policy")
	    		UriAndListParam thePolicy, 
	    		  
	    		@Description(shortDefinition="Logical source location within the enterprise")
	    		@OptionalParam(name="site")
	    		TokenAndListParam theSite, 
	    		  
	    		@Description(shortDefinition="The identity of source detecting the event")
	    		@OptionalParam(name="source")
	    		TokenAndListParam theSource, 
	    		  
	    		@Description(shortDefinition="More specific type/id for the event")
	    		@OptionalParam(name="subtype")
	    		TokenAndListParam theSubtype, 
	    		  
	    		@Description(shortDefinition="Type/identifier of event")
	    		@OptionalParam(name="type")
	    		TokenAndListParam theType, 
	    		  
	    		@Description(shortDefinition="Unique identifier for the user")
	    		@OptionalParam(name="user")
	    		TokenAndListParam theUser, 
	    		
	    		@IncludeParam(reverse=true)
	    		Set<Include> theRevIncludes,
	    		@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
	    		@OptionalParam(name="_lastUpdated")
	    		DateRangeParam theLastUpdated, 
	    		 
	    		@IncludeParam(allow= {
	    					"AuditEvent:agent" ,
	    					"AuditEvent:entity" ,
	    					"AuditEvent:patient" ,
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
			
	    	paramMap.add("action", theAction);
	    	paramMap.add("address", theAddress);
	    	paramMap.add("agent", theAgent);
	    	paramMap.add("agent-name", theAgent_name);
	    	paramMap.add("agent-role", theAgent_role);
	    	paramMap.add("altid", theAltid);
	    	paramMap.add("date", theDate);
	    	paramMap.add("entity", theEntity);
	    	paramMap.add("entity-id", theEntity_id);
	    	paramMap.add("entity-name", theEntity_name);
	    	paramMap.add("entity-role", theEntity_role);
	    	paramMap.add("entity-type", theEntity_type);
	    	paramMap.add("outcome", theOutcome);
	    	paramMap.add("patient", thePatient);
	    	paramMap.add("policy", thePolicy);
	    	paramMap.add("site", theSite);
	    	paramMap.add("source", theSource);
	    	paramMap.add("subtype", theSubtype);
	    	paramMap.add("type", theType);
	    	paramMap.add("user", theUser);
	    	
	    	paramMap.setRevIncludes(theRevIncludes);
	    	paramMap.setLastUpdated(theLastUpdated);
	    	paramMap.setIncludes(theIncludes);
	    	paramMap.setSort(theSort);
	    	paramMap.setCount(theCount);
	    	paramMap.setElements(theElements);
	    	paramMap.setSummary(theSummary);	    	
			paramMap.setFrom(_page != null ? _page.getValue() : null);

			//return search(paramMap);
			return searchBundle(paramMap, theDetails);	    				
	    	    		    		    	  
	    }
	
	 	
			
	public static void updateMidataAuditEvent(MidataAuditEvent mae, MidataId appUsed, User actorUser, User modifiedUser, Consent affectedConsent, String message, Study study) throws AppException {
		AuditEvent ae = new AuditEvent();

		ae.setId(mae._id.toString());
		ae.setType(mae.event.getFhirType());
		ae.addSubtype(mae.event.getFhirSubType());
		ae.setAction(mae.event.getAction());
		ae.setRecorded(mae.timestamp);
		
		MidataId anonymize = null;
		if (affectedConsent != null && affectedConsent.type.equals(ConsentType.STUDYPARTICIPATION) && affectedConsent.getOwnerName() != null) {
			anonymize = affectedConsent.owner;			
		}
		
		if (actorUser != null) {
			AuditEventAgentComponent actor = ae.addAgent();
			actor.addRole().addCoding().setSystem("http://midata.coop/codesystems/user-role").setCode(actorUser.role.toString());
			actor.setRequestor(true);
			
			if (anonymize != null && actorUser._id.equals(anonymize)) {
				actor.setName(affectedConsent.getOwnerName());
				actor.setReference(new Reference("Patient/"+affectedConsent._id.toString()));
			} else {			
				if (actorUser.role.equals(UserRole.MEMBER)) {
					actor.setReference(new Reference("Patient/"+actorUser._id.toString()));
				} else if (actorUser.role.equals(UserRole.PROVIDER)) {
					actor.setReference(new Reference("Practitioner/"+actorUser._id.toString()));
				}
				actor.setName(actorUser.firstname+" "+actorUser.lastname);
				actor.setUserId(new Identifier().setValue(actorUser.getPublicIdentifier()));
			}
		}
		if (modifiedUser != null) {
			AuditEventEntityComponent aeec = ae.addEntity();
			aeec.setType(new Coding().setSystem("http://midata.coop/codesystems/user-role").setCode(modifiedUser.role.toString()));
			
			if (anonymize != null && modifiedUser._id.equals(anonymize)) {
				aeec.setName(affectedConsent.getOwnerName());
				aeec.setReference(new Reference("Patient/"+affectedConsent._id.toString()));
			} else {
				if (modifiedUser.role == UserRole.PROVIDER) {			   
				   aeec.setReference(new Reference("Practitioner/"+modifiedUser._id.toString()));
				} else {			   
				   aeec.setReference(new Reference("Patient/"+modifiedUser._id.toString()));
				}
			}
			
			aeec.setName(modifiedUser.firstname+" "+modifiedUser.lastname);
			aeec.setIdentifier(new Identifier().setValue(modifiedUser.getPublicIdentifier()));
		}
				
		if (affectedConsent != null) {
			AuditEventEntityComponent aeec = ae.addEntity();			
			aeec.setType(new Coding().setSystem("http://midata.coop/codesystems/consent-type").setCode(affectedConsent.type.toString()));			
			aeec.setReference(new Reference("Consent/"+affectedConsent._id.toString()));
			if (affectedConsent.type.equals(ConsentType.STUDYPARTICIPATION) && affectedConsent.getOwnerName() != null) {
			   aeec.setName(affectedConsent.getOwnerName());
			} else {
			   aeec.setName(affectedConsent.name);
			}
		}
		if (study != null) {
			AuditEventEntityComponent aeec = ae.addEntity();
			aeec.setType(new Coding().setSystem("http://hl7.org/fhir/resource-types").setCode("ResearchStudy"));
			aeec.setReference(new Reference("ResearchStudy/"+study._id.toString()));
			aeec.setName(study.name);
			aeec.setIdentifier(new Identifier().setValue(study.code));
			aeec.addExtension("http://midata.coop/extension/research-type", new CodeType(study.type.toString()));
		}
				
		
		if (message != null) {
			AuditEventEntityComponent aeec = ae.addEntity();			
			aeec.setName(message);
		}
		
		if (appUsed != null) {
			Plugin plugin = Plugin.getById(appUsed);
			if (plugin != null) {
			  AuditEventAgentComponent actor = ae.addAgent();
			  actor.addRole().addCoding().setSystem("http://dicom.nema.org/resources/ontology/DCM").setCode("110150");
			  actor.setName(plugin.name);
			  actor.setUserId(new Identifier().setValue(appUsed.toString()));
			}
		}
		
		String encoded = ctx.newJsonParser().encodeResourceToString(ae);		
		mae.fhirAuditEvent = BasicDBObject.parse(encoded);				
	}

	@Override
	public MidataAuditEvent fetchCurrent(IIdType theId) throws AppException {
		return MidataAuditEvent.getById(MidataId.from(theId.getIdPart()));
	}

	@Override
	public void processResource(MidataAuditEvent record, AuditEvent resource) throws AppException {				
	}

	@Override
	public List<MidataAuditEvent> searchRaw(SearchParameterMap params) throws AppException {
		ExecutionInfo info = info();			
		
		if (!info.context.mayAccess("AuditEvent", "fhir/AuditEvent")) return Collections.emptyList();
					
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, null);
		
		User current = info().cache.getUserById(info().ownerId);
		boolean authrestricted = false;
		if (!current.role.equals(UserRole.ADMIN)) {
		  Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByMember(info().executorId);
		  if (ugms.isEmpty()) {
		    query.putDataCondition(new AndCondition(CMaps.map("authorized", info.executorId)).optimize());
		  } else {
			Set<MidataId> allowedIds = new HashSet<MidataId>();
			allowedIds.add(info.executorId);
			for (UserGroupMember ugm : ugms) if (ugm.getRole().auditLogAccess()) allowedIds.add(ugm.userGroup);
			query.putDataCondition(new AndCondition(CMaps.map("authorized", CMaps.map("$in", allowedIds))).optimize());
			//query.putAccount("authorized", allowedIds);
		  }
		  authrestricted = true;
		}
		
		builder.handleIdRestriction();
		builder.restriction("action", false, QueryBuilder.TYPE_CODE, "fhirAuditEvent.action");
		builder.restriction("address", false, QueryBuilder.TYPE_STRING, "fhirAuditEvent.agent.network.address");
		builder.restriction("agent", false, "Patient", "fhirAuditEvent.agent.reference");
		
		if (params.containsKey("agent")) {
		  List<ReferenceParam> agents = builder.resolveReferences("agent", null);
		   if (agents != null && !authrestricted) {
			   query.putDataCondition(new AndCondition(CMaps.map("authorized", CMaps.map("$in", FHIRTools.referencesToIds(agents)))).optimize());			
		   }
		}
		
		builder.restriction("agent-name", false, QueryBuilder.TYPE_STRING, "fhirAuditEvent.agent.name");
		builder.restriction("agent-role", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "fhirAuditEvent.agent.role");	
		builder.restriction("altid", false, QueryBuilder.TYPE_CODE, "fhirAuditEvent.agent.altId");	
		builder.restriction("date", false, QueryBuilder.TYPE_DATETIME, "timestamp");	
		
		if (params.containsKey("entity")) {
			List<ReferenceParam> entities = builder.resolveReferences("entity", null);
			if (entities != null) {
				query.putAccount("about", ObjectIdConversion.toMidataIds(FHIRTools.referencesToIds(entities)));
			}
		}
		builder.restriction("entity", false, null, "fhirAuditEvent.entity.reference");
			
		builder.restriction("entity-id", false, QueryBuilder.TYPE_IDENTIFIER, "fhirAuditEvent.entity.identifier");
		builder.restriction("entity-name", false, QueryBuilder.TYPE_STRING, "fhirAuditEvent.entity.name");	
		builder.restriction("entity-role", false, QueryBuilder.TYPE_CODING, "fhirAuditEvent.entity.role");
		builder.restriction("entity-type", false, QueryBuilder.TYPE_CODING, "fhirAuditEvent.entity.type");
			
		//outcome	token	Whether the event succeeded or failed	AuditEvent.outcome	
		
		if (params.containsKey("patient")) {
			List<ReferenceParam> patients = builder.resolveReferences("patient", null);
			if (patients != null) {
				query.putDataCondition(new AndCondition(CMaps.map("authorized", CMaps.map("$in",ObjectIdConversion.toMidataIds(FHIRTools.referencesToIds(patients))))).optimize());
				//query.putAccount("authorized", FHIRTools.referencesToIds(patients));
			}
		}
		//patient	reference	Direct reference to resource	AuditEvent.agent.reference | AuditEvent.entity.reference
		//Patient)	
		builder.restriction("policy", false, QueryBuilder.TYPE_URI, "fhirAuditEvent.agent.policy");
		builder.restriction("site", false, QueryBuilder.TYPE_CODE, "fhirAuditEvent.source.site");
		builder.restriction("source", false, QueryBuilder.TYPE_IDENTIFIER, "fhirAuditEvent.source.identifier");
		
		//subtype	token	More specific type/id for the event	AuditEvent.subtype	
		//type	token	Type/identifier of event	AuditEvent.type	
		builder.restriction("type", false, QueryBuilder.TYPE_CODING, "fhirAuditEvent.type");
		
		builder.restriction("user", false, QueryBuilder.TYPE_IDENTIFIER, "fhirAuditEvent.agent.userId");
		
		Map<String, Object> properties = query.retrieveAsNormalMongoQuery();
		
		ObjectIdConversion.convertMidataIds(properties, "authorized", "about");
		
		//properties = CMaps.map("recorded", CMaps.map("$ge", 150));
		int limit = params.getCount() != null ? params.getCount() + 1 : 10000;	
		if (params.getFrom() != null) {
			properties.put("_id", CMaps.map("$lte", MidataId.from(params.getFrom()).toObjectId()));
		}
		
		List<MidataAuditEvent> events = MidataAuditEvent.getAll(properties, MidataAuditEvent.ALL, limit);
		AccessLog.log("RETURNED");
		AccessLog.log("#events="+events.size());
		return events;
	}

	@Override
	public List<AuditEvent> parse(List<MidataAuditEvent> events, Class<AuditEvent> resultClass) throws AppException {
		List<AuditEvent> result = new ArrayList<AuditEvent>();
		for (MidataAuditEvent mae : events) {
			result.add(readAuditEventFromMidataAuditEvent(mae));
		}								
		return result;
	}

 	
}