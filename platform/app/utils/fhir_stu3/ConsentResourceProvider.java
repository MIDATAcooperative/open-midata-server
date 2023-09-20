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

package utils.fhir_stu3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Consent.ConsentActorComponent;
import org.hl7.fhir.dstu3.model.Consent.ConsentState;
import org.hl7.fhir.dstu3.model.Consent.ExceptComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import com.mongodb.BasicDBObject;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Elements;
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
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import controllers.Circles;
import controllers.members.HealthProvider;
import models.Consent;
import models.MidataId;
import models.Plugin;
import models.TypedMidataId;
import models.User;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EntityType;
import models.enums.WritePermissionType;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.access.Feature_FormatGroups;
import utils.audit.AuditManager;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

public class ConsentResourceProvider extends ReadWriteResourceProvider<org.hl7.fhir.dstu3.model.Consent, Consent> implements IResourceProvider {

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
		if (!checkAccessible()) throw new ResourceNotFoundException(theId);
		models.Consent consent = Circles.getConsentById(info(), MidataId.from(theId.getIdPart()), Consent.ALL);	
		if (consent == null) return null;
		return readConsentFromMidataConsent(consent, true);
	}
	    
    
	/**
	 * Convert a MIDATA User object into a FHIR person object
	 * @param userToConvert user to be converted into a FHIR object
	 * @return FHIR person
	 * @throws AppException
	 */
	public org.hl7.fhir.dstu3.model.Consent readConsentFromMidataConsent(models.Consent consentToConvert, boolean addMembers) throws AppException {
		if (consentToConvert.fhirConsent==null) {
			Circles.fillConsentFields(info(), Collections.singleton(consentToConvert), models.Consent.FHIR);
			updateMidataConsent(consentToConvert);
		}
		IParser parser = ctx().newJsonParser();
		AccessLog.log(consentToConvert.fhirConsent.toString());
		org.hl7.fhir.dstu3.model.Consent p = parser.parseResource(getResourceType(), consentToConvert.fhirConsent.toString());
		
		if (consentToConvert.sharingQuery == null) Circles.fillConsentFields(info(), Collections.singleton(consentToConvert), Sets.create("sharingQuery"));
		
		Map<String, Object> query = consentToConvert.sharingQuery;
		if (query != null) buildQuery(query, p);
		
		if (EntityType.USERGROUP.equals(consentToConvert.entityType)) {
			for (MidataId auth : consentToConvert.authorized) {
			   p.addActor().setReference(new Reference("Group/"+auth.toString()));
			}		
		} else if (EntityType.ORGANIZATION.equals(consentToConvert.entityType)) {
			for (MidataId auth : consentToConvert.authorized) {
			   p.addActor().setReference(new Reference("Organization/"+auth.toString()));
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
		case PRECONFIRMED:
		case ACTIVE:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.ACTIVE);break;
		case UNCONFIRMED:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.PROPOSED);break;
		case REJECTED:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.REJECTED);break;
		case INVALID:
		case FROZEN:
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
		if (consentToConvert.createdBefore != null || consentToConvert.createdAfter != null) {
		  c.setDataPeriod(new Period().setEnd(consentToConvert.createdBefore).setStart(consentToConvert.createdAfter));
		}
		if (consentToConvert.dateOfCreation != null) {
		  c.setDateTime(consentToConvert.dateOfCreation);
		}
		c.setPolicyRule("http://hl7.org/fhir/ConsentPolicy/opt-in");
		c.addPurpose(new Coding("http://midata.coop/codesystems/consent-type", consentToConvert.type.toString(), null));
		
		c.addExtension().setUrl("http://midata.coop/extensions/consent-name").setValue(new StringType(consentToConvert.name));
		
		String encoded = ctx.newJsonParser().encodeResourceToString(c);		
		consentToConvert.fhirConsent = BasicDBObject.parse(encoded);				
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
	    	    		    	
	    	return search(paramMap);    	    	    	
	    }
	
	@Override
	public List<Consent> searchRaw(SearchParameterMap params) throws AppException {		
		AccessContext info = info();
		
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, null);
			
		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", null);
		builder.restriction("_lastUpdated", false, QueryBuilder.TYPE_DATETIME, "lastUpdated");
		builder.restriction("category", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "fhirConsent.category");
		builder.setDateToString(true);
		builder.restriction("date", false, QueryBuilder.TYPE_DATETIME, "fhirConsent.dateTime");
		builder.restriction("period", false, QueryBuilder.TYPE_PERIOD, "fhirConsent.period");
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
		
		if (!info.mayAccess("Consent", "fhir/Consent")) properties.put("_id", info.getTargetAps());
		
		ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "authorized");
		
		Set<models.Consent> consents = new HashSet<models.Consent>();						
		if (authorized == null || authorized.contains(info().getLegacyOwner().toString())) consents.addAll(Consent.getAllByAuthorized(info().getLegacyOwner(), properties, Consent.FHIR));
		if (!properties.containsKey("owner") || utils.access.Query.getRestriction(properties.get("owner"), "owner").contains(info().getLegacyOwner().toString())) {
			consents.addAll(Consent.getAllByOwner(info().getLegacyOwner(), properties, Consent.FHIR, Circles.RETURNED_CONSENT_LIMIT));
		}
		return new ArrayList<Consent>(consents);
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
				
	
	private static void mayShare(MidataId pluginId, Map<String, Object> query) throws AppException {
		Plugin plugin = Plugin.getById(pluginId);
		if (plugin == null || !plugin.resharesData) throw new ForbiddenOperationException("Plugin is not allowed to share data.");
		if (!isSubQuery(plugin.defaultQuery, query)) throw new ForbiddenOperationException("Plugin is not allowed to share this type of data.");
				
	}
	
	private static boolean isSubQuery(Map<String, Object> masterQuery, Map<String, Object> subQuery) throws AppException {
		
		if (masterQuery.containsKey("$or")) {
			Collection<Map<String, Object>> parts = (Collection<Map<String, Object>>) masterQuery.get("$or");			
			for (Map<String, Object> part :parts) {
				boolean match = isSubQuery(part, subQuery);
				if (match) return true;
			}
		}
		
		masterQuery = new HashMap<String, Object>(masterQuery);
		subQuery = new HashMap<String, Object>(subQuery);
								
		Feature_FormatGroups.convertQueryToContents(masterQuery);		
		Feature_FormatGroups.convertQueryToContents(subQuery);
		
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
		

	@Override
	public void createPrepare(Consent consent, org.hl7.fhir.dstu3.model.Consent theResource) throws AppException {
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
		  consent.createdAfter = theResource.getPeriod().getStart();
		}
			
        consent.type = ConsentType.valueOf(theResource.getPurposeFirstRep().getCode());
		consent.owner = info().getLegacyOwner();
		consent.writes = WritePermissionType.UPDATE_AND_CREATE;
		
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
			mayShare(info().getUsedPlugin(), consent.sharingQuery);
			if (!info().getAccessor().equals(consent.owner)) throw new InvalidRequestException("Only consent owner may create active consents");
			consent.status = ConsentStatus.ACTIVE;
		} else if (theResource.getStatus() != ConsentState.PROPOSED) {
			throw new ForbiddenOperationException("consent status not supported for creation.");
		}
		
		if (consent.type == ConsentType.IMPLICIT) throw new ForbiddenOperationException("consent type not supported for creation.");
		
	}

	@Override
	public void createExecute(Consent consent, org.hl7.fhir.dstu3.model.Consent theResource) throws AppException {
        Circles.addConsent(info(), consent, true, null, false);        
		theResource.setDateTime(consent.dateOfCreation);		
	}

	@Override
	public Consent init() {
		Consent consent = new Consent();
		
		consent.creatorApp = info().getUsedPlugin();
		consent.creator = info().getActor();
		consent.status = ConsentStatus.UNCONFIRMED;
		consent.authorized = new HashSet<MidataId>();
		
		return consent;
	}

	@Override
	public void updatePrepare(Consent consent, org.hl7.fhir.dstu3.model.Consent theResource) throws AppException {
        		
		if ((theResource.getStatus() == ConsentState.ACTIVE || theResource.getStatus() == ConsentState.REJECTED) && consent.status == ConsentStatus.UNCONFIRMED) {
			mayShare(info().getUsedPlugin(), consent.sharingQuery);  
			if (theResource.getStatus() == ConsentState.ACTIVE && !info().getAccessor().equals(consent.owner)) throw new InvalidRequestException("Only consent owner may change consents to active consents");
		}
		
	}

	@Override
	public void updateExecute(Consent consent, org.hl7.fhir.dstu3.model.Consent theResource) throws AppException {
		if ((theResource.getStatus() == ConsentState.ACTIVE || theResource.getStatus() == ConsentState.REJECTED) && consent.status == ConsentStatus.UNCONFIRMED) {
		        					
			if (theResource.getStatus() == ConsentState.ACTIVE) {
				HealthProvider.confirmConsent(info(), consent._id);
			} else {
				HealthProvider.rejectConsent(info(), info().getAccessor(), consent._id);
			}
			
			AuditManager.instance.success();
		}
		
	}

	@Override
	public Consent fetchCurrent(IIdType theId) throws AppException {
		return Circles.getConsentById(info(), MidataId.from(theId.getIdPart()), Consent.ALL);	
	}

	@Override
	public void processResource(Consent record, org.hl7.fhir.dstu3.model.Consent resource) throws AppException {		
		
	}

	@Override
	public List<org.hl7.fhir.dstu3.model.Consent> parse(List<Consent> consents, Class<org.hl7.fhir.dstu3.model.Consent> resultClass) throws AppException {
		List<org.hl7.fhir.dstu3.model.Consent> result = new ArrayList<org.hl7.fhir.dstu3.model.Consent>();
		for (models.Consent consent : consents) {
			result.add(readConsentFromMidataConsent(consent, true));
		}
		
		return result;
	}

	@Override
	public String getVersion(Consent record) {
		return "";
	}

	@Override
	public Date getLastUpdated(Consent record) {
		return record.lastUpdated;
	}	
	
	
}