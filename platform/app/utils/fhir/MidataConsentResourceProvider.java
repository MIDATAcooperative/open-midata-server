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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Consent.ConsentDataMeaning;
import org.hl7.fhir.r4.model.Consent.ConsentState;
import org.hl7.fhir.r4.model.Consent.provisionActorComponent;
import org.hl7.fhir.r4.model.Consent.provisionComponent;
import org.hl7.fhir.r4.model.Consent.provisionDataComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;

import com.mongodb.BasicDBObject;

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
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateAndListParam;
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
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import controllers.Circles;
import controllers.members.HealthProvider;
import controllers.members.Studies;
import models.Consent;
import models.ConsentVersion;
import models.ContentCode;
import models.MidataId;
import models.Plugin;
import models.Research;
import models.Study;
import models.StudyParticipation;
import models.TypedMidataId;
import models.User;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.EntityType;
import models.enums.JoinMethod;
import models.enums.WritePermissionType;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.ErrorReporter;
import utils.PluginLoginCache;
import utils.access.Feature_FormatGroups;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.messaging.SubscriptionManager;

public class MidataConsentResourceProvider extends ReadWriteResourceProvider<org.hl7.fhir.r4.model.Consent, Consent> implements IResourceProvider {

	public MidataConsentResourceProvider() {
						
	}
	
	@Override
	public Class<org.hl7.fhir.r4.model.Consent> getResourceType() {
		return org.hl7.fhir.r4.model.Consent.class;
	}

	/**
	 * Default implementation to retrieve a FHIR resource by id.
	 * @param theId ID of resource to be retrieved
	 * @return Resource read from database
	 * @throws AppException
	 */
	@Read(version=true)	
	public org.hl7.fhir.r4.model.Consent getResourceById(@IdParam IIdType theId) throws AppException {
		if (!checkAccessible()) throw new ResourceNotFoundException(theId);
		models.Consent consent = Circles.getConsentById(info(), MidataId.from(theId.getIdPart()), info().getUsedPlugin(), Consent.FHIR);	
		if (consent == null) return null;
		
		if (theId.hasVersionIdPart()) {
			String versionStr = theId.getVersionIdPart();			
			if (versionStr.equals(getVersion(consent))) {
				return readConsentFromMidataConsent(info(), consent, true);
			}
			ConsentVersion version = ConsentVersion.getByIdAndVersion(consent._id, versionStr, ConsentVersion.ALL);
			 IParser parser = ctx().newJsonParser();
			 return parser.parseResource(getResourceType(), version.fhirConsent.toString());
		} else return readConsentFromMidataConsent(info(), consent, true);
	}
	
	@History()
	public List<org.hl7.fhir.r4.model.Consent> getHistory(@IdParam IIdType theId) throws AppException {
	  if (!checkAccessible()) throw new ResourceNotFoundException(theId);
	  models.Consent consent = Circles.getConsentById(info(), MidataId.from(theId.getIdPart()), info().getUsedPlugin(), Consent.FHIR);
	  if (consent==null) throw new ResourceNotFoundException(theId);
	  
	  List<ConsentVersion> versions = ConsentVersion.getAllById(consent._id, ConsentVersion.ALL);	   	  
	  List<org.hl7.fhir.r4.model.Consent> result = new ArrayList<org.hl7.fhir.r4.model.Consent>(versions.size()+1);
	  
	   IParser parser = ctx().newJsonParser();
	   for (ConsentVersion record : versions) {			    
		    Object data = record.fhirConsent;			
			org.hl7.fhir.r4.model.Consent p = parser.parseResource(getResourceType(), data.toString());
			//processResource(record, p);
			p.setId(new IdType("Consent", record._id.toString(), record.version));
			result.add(p);
	   }
	   result.add(readConsentFromMidataConsent(info(), consent, true));
	   
	   return result;
	}
	    
    
	/**
	 * Convert a MIDATA User object into a FHIR person object
	 * @param userToConvert user to be converted into a FHIR object
	 * @return FHIR person
	 * @throws AppException
	 */
	public org.hl7.fhir.r4.model.Consent readConsentFromMidataConsent(AccessContext context, models.Consent consentToConvert, boolean addMembers) throws AppException {
		if (consentToConvert.fhirConsent==null) {
			Circles.fillConsentFields(context, Collections.singleton(consentToConvert), models.Consent.FHIR);
			updateMidataConsent(consentToConvert, null);
		} else {
			convertToR4(consentToConvert, consentToConvert.fhirConsent);
		}
		IParser parser = ctx().newJsonParser();
		//AccessLog.log(consentToConvert.fhirConsent.toString());
		org.hl7.fhir.r4.model.Consent p = parser.parseResource(getResourceType(), consentToConvert.fhirConsent.toString());		
		
		if (consentToConvert.sharingQuery == null) Circles.fillConsentFields(context, Collections.singleton(consentToConvert), Sets.create("sharingQuery"));
		
		processResource(consentToConvert, p);
		addQueryToConsent(consentToConvert, p);			
		addActorsToConsent(consentToConvert, p);
						
		return p;
	}
	
	public void addQueryToConsent(Consent consentToConvert, org.hl7.fhir.r4.model.Consent p) throws AppException {
		Map<String, Object> query = consentToConvert.sharingQuery;
		if (query != null) buildQuery(query, p.getProvision());
	}
	
	public void addActorsToConsent(Consent consentToConvert, org.hl7.fhir.r4.model.Consent p) throws AppException {
		p.getProvision().getActor().removeIf(actor -> { return actor.hasRole() && ("GRANTEE".equals(actor.getRole().getCodingFirstRep().getCode())); });
		if (EntityType.USERGROUP.equals(consentToConvert.entityType)) {
			for (MidataId auth : consentToConvert.authorized) {
			   p.getProvision().addActor().setRole(new CodeableConcept().addCoding(new Coding().setSystem("http://hl7.org/fhir/v3/RoleCode").setCode("GRANTEE"))).setReference(new Reference("Group/"+auth.toString()));
			}			
		} else {
			for (MidataId auth : consentToConvert.authorized) {
				try {
			      p.getProvision().addActor().setRole(new CodeableConcept().addCoding(new Coding().setSystem("http://hl7.org/fhir/v3/RoleCode").setCode("GRANTEE"))).setReference(FHIRTools.getReferenceToUser(auth, null));
				} catch (InternalServerException e) {}
			}
			if (consentToConvert.externalAuthorized != null) {
				for (String auth : consentToConvert.externalAuthorized) {
				  p.getProvision().addActor().setRole(new CodeableConcept().addCoding(new Coding().setSystem("http://hl7.org/fhir/v3/RoleCode").setCode("GRANTEE"))).setReference(new Reference().setIdentifier(new Identifier().setSystem("http://midata.coop/identifier/patient-login-or-invitation").setValue(auth)));
				}
			}
		}
	}
	
	private void buildQuery(Map<String, Object> query, provisionComponent p) throws AppException {
		p.getClass_().clear();
		p.getProvision().clear();
		if (query.containsKey("code")) {
			Set<String> vals = utils.access.Query.getRestriction(query.get("code"), "code");
		  	for (String s : vals) {
		  		int i = s.indexOf(' ');
		  		if (i >= 0) {
		  			String system = s.substring(0, i);
		  			String code = s.substring(i+1);
		  			p.addCode().addCoding().setSystem(system).setCode(code);
		  		}
		  		
		  	}
		}		
        if (query.containsKey("content")) {		  	
		  	Set<String> vals = utils.access.Query.getRestriction(query.get("content"), "content");
		  	for (String s : vals) p.addClass_().setSystem("http://midata.coop/codesystems/content").setCode(s);
		}
        if (query.containsKey("group")) {		  	
		  	Set<String> vals = utils.access.Query.getRestriction(query.get("group"), "group");
		  	for (String s : vals) p.addClass_().setSystem("http://midata.coop/codesystems/group").setCode(s);
		}
        if (query.containsKey("app")) {		  	
		  	Set<String> vals = utils.access.Query.getRestriction(query.get("app"), "app");
		  	for (String s : vals) p.addClass_().setSystem("http://midata.coop/codesystems/app").setCode(s);
		}
        if (query.containsKey("format")) {		  	
		  	Set<String> vals = utils.access.Query.getRestriction(query.get("format"), "format");
		  	for (String s : vals) {
		  		if (s.startsWith("fhir/")) {
		  			p.addClass_().setSystem("http://hl7.org/fhir/resource-types").setCode(s.substring("fhir/".length()));		  					  		
		  		} else {
		  		   p.addClass_().setSystem("http://midata.coop/codesystems/format").setCode(s);
		  		}
		  	}
		}
        if (query.containsKey("$or")) {
        	Collection<Map<String, Object>> col = (Collection<Map<String, Object>>) query.get("$or");
        	for (Map<String, Object> part : col) {
        	  provisionComponent sub = p.addProvision();
        	  buildQuery(part, sub);
        	}        	
        }
	}
	
	private void createQuery(provisionComponent p, Map<String, Object> query) {
		if (p.getType() != org.hl7.fhir.r4.model.Consent.ConsentProvisionType.DENY) {
			List<Coding> coding = p.getClass_();
			Set<String> contents = null;
			Set<String> formats = null;
			Set<String> groups = null;
			Set<String> codes = null;
			Set<String> apps = null;
			
			for (Coding code : coding) {			
				if (code.getSystem().equals("http://midata.coop/codesystems/content")) {
					if (contents == null) contents = new HashSet<String>();
					contents.add(code.getCode());
				} else if (code.getSystem().equals("http://midata.coop/codesystems/group")) {
					if (groups == null) groups = new HashSet<String>();
					groups.add(code.getCode());				
				} else if (code.getSystem().equals("http://hl7.org/fhir/resource-types")) {
					if (formats == null) formats = new HashSet<String>();
					formats.add("fhir/"+code.getCode());
				} else if (code.getSystem().equals("http://midata.coop/codesystems/format")) {
					if (formats == null) formats = new HashSet<String>();
					formats.add(code.getCode());
				} else if (code.getSystem().equals("http://midata.coop/codesystems/app")) {
					if (apps == null) apps = new HashSet<String>();
					apps.add(code.getCode());
				}
			}
			for (CodeableConcept cc : p.getCode()) {
				for (Coding code : cc.getCoding()) {
					try {
						if (codes == null) codes = new HashSet<String>();
						if (contents == null) contents = new HashSet<String>();
						String theCode = code.getSystem()+" "+code.getCode();
						String content = ContentCode.getContentForSystemCode(theCode);
						if (content == null) throw new UnprocessableEntityException("Unknown code used system="+code.getSystem()+" code="+code.getCode());
						contents.add(content);
						codes.add(theCode);
					} catch (AppException e) {
						throw new UnprocessableEntityException("Unknown code used system="+code.getSystem()+" code="+code.getCode());
					}
					
				}
			}
			if (contents != null) query.put("content", contents);
			if (formats != null) query.put("format", formats);
			if (groups != null) query.put("group", groups);
			if (codes != null) query.put("code", codes);
			if (apps != null) query.put("app", apps);
			
			if (p.hasProvision()) {
				ArrayList<Map<String, Object>> subqueries = new ArrayList<Map<String, Object>>();
				query.put("$or", subqueries);
				for (provisionComponent prov : p.getProvision()) {
					Map<String, Object> subquery = new HashMap<String, Object>();
					createQuery(prov, subquery);
					if (subquery.containsKey("format") || subquery.containsKey("content") || subquery.containsKey("group")) {
					  subqueries.add(subquery);
					}
				}
			}
		}
	}
	
	private void processDataSharing(Consent consent, org.hl7.fhir.r4.model.Consent p) throws AppException {
		if (consent.status != ConsentStatus.ACTIVE) return;
		if (info().getAccessor().equals(consent.owner)) processDataSharing(consent, p.getProvision());
	}
	
	private void processDataSharing(Consent consent, provisionComponent prov) throws AppException {		
		if (prov.getType() != org.hl7.fhir.r4.model.Consent.ConsentProvisionType.DENY) {
			Set<MidataId> share = new HashSet<MidataId>();
			if (prov.hasData()) {
				for (provisionDataComponent entry : prov.getData()) {
					if (entry.hasMeaning() && !entry.getMeaning().equals(ConsentDataMeaning.INSTANCE)) {
						throw new UnprocessableEntityException("Unsupported value for 'meaning'");
					}
					if (entry.hasReference()) {
						Reference ref = entry.getReference();
						if (ref.hasReference()) {
							if (!ref.getReferenceElement().isAbsolute()) {
								String id = ref.getReferenceElement().getIdPart();
								share.add(MidataId.from(id));
							}
						}
					}
				}
			}
			if (!share.isEmpty()) {
				RecordManager.instance.share(info(), info().getTargetAps(), consent._id, share, false);
			}
		}
		if (prov.hasProvision()) {
			for (provisionComponent sub : prov.getProvision()) {
				processDataSharing(consent, sub);
			}
		}
	}
	
	public static void storeVersion(models.Consent consent, String version) throws InternalServerException {
		ConsentVersion old = new ConsentVersion();
		old._id = consent._id;
		old.version = version;
		old.fhirConsent = consent.fhirConsent;		
		ConsentVersion.add(old);		
	}
	
	public static void updateMidataConsent(models.Consent consentToConvert, org.hl7.fhir.r4.model.Consent newversion) throws AppException {
		
		StudyParticipation part = null;
		Study study = null;
		Research org = null;
		if (consentToConvert.type == ConsentType.STUDYPARTICIPATION) {
			if (consentToConvert instanceof StudyParticipation) {
				part = (StudyParticipation) consentToConvert;
			} else {
				part = StudyParticipation.getById(consentToConvert._id, StudyParticipation.STUDY_EXTRA);
			}
			study = Study.getById(part.study, Sets.create("code","name","owner"));
			if (study!=null) {
				org = Research.getById(study.owner, Sets.create("name"));
			}
		}
		
		org.hl7.fhir.r4.model.Consent c = newversion;
		if (consentToConvert.fhirConsent != null) {
		    IParser parser = ctx().newJsonParser();		
		    org.hl7.fhir.r4.model.Consent parsed = parser.parseResource(org.hl7.fhir.r4.model.Consent.class, consentToConvert.fhirConsent.toString());
		    String version = parsed.getMeta().getVersionId();
		    if (version == null || version.length()==0) version = "0";
		    if (!consentToConvert.dateOfCreation.equals(consentToConvert.lastUpdated)) storeVersion(consentToConvert, version);
		    if (c == null) c = parsed;
		} else if (c==null) {
			c = new org.hl7.fhir.r4.model.Consent();			
			
		}
		if (!c.hasExtension("http://midata.coop/extensions/consent-name")) {
			c.addExtension().setUrl("http://midata.coop/extensions/consent-name").setValue(new StringType(consentToConvert.name));
		}
		if (part != null) {
			if (!c.hasExtension("http://midata.coop/extensions/project-join-method")) c.addExtension().setUrl("http://midata.coop/extensions/project-join-method").setValue(new StringType(part.joinMethod.toString()));
			if (part.projectEmails != null) {
			  if (c.hasExtension("http://midata.coop/extensions/communication-channel-use")) {
			    c.getExtensionByUrl("http://midata.coop/extensions/communication-channel-use").setValue(new StringType(part.projectEmails.toString()));
			  } else {
			    c.addExtension().setUrl("http://midata.coop/extensions/communication-channel-use").setValue(new StringType(part.projectEmails.toString()));
			  }
			}
		}
		 
		c.setId(consentToConvert._id.toString());
		c.getProvision().getActor().clear();
		
		switch (consentToConvert.status) {
		case PRECONFIRMED:
		case ACTIVE:
			c.setStatus(ConsentState.ACTIVE);break;
		case UNCONFIRMED:c.setStatus(ConsentState.PROPOSED);break;
		case REJECTED:c.setStatus(ConsentState.REJECTED);break;
		case EXPIRED:c.setStatus(ConsentState.INACTIVE);break;
		case FROZEN:c.setStatus(ConsentState.INACTIVE);break;
		case INVALID:c.setStatus(ConsentState.INACTIVE);break;
		}
		
		String categoryCode = consentToConvert.categoryCode;
		if (categoryCode == null) categoryCode = "default";
		c.setCategory(new ArrayList<CodeableConcept>());
		c.addCategory().addCoding().setCode(categoryCode).setSystem("http://midata.coop/codesystems/consent-category");

		if (study != null) {
			c.addCategory().addCoding().setCode(study.code).setSystem("http://midata.coop/codesystems/project-code").setDisplay(study.name);
			if (org != null) {
			   c.getProvision().addActor().setRole(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType").setCode("IRCP"))).setReference(new Reference("Organization/"+study.owner.toString()).setDisplay(org.name));
			}
		}
		
		if (consentToConvert.owner != null) {
		  c.setPatient(FHIRTools.getReferenceToUser(User.getByIdAlsoDeleted(consentToConvert.owner, Sets.create("role", "firstname", "lastname", "email", "status"))));
		} else if (consentToConvert.externalOwner != null) {
		  c.setPatient(new Reference().setIdentifier(new Identifier().setSystem("http://midata.coop/identifier/patient-login-or-invitation").setValue(consentToConvert.externalOwner)));
		}
		
		if (consentToConvert.validUntil != null) {
		  c.getProvision().setPeriod(new Period().setEnd(consentToConvert.validUntil));	
		}
		if (consentToConvert.createdBefore != null) {
		  c.getProvision().setDataPeriod(new Period().setEnd(consentToConvert.createdBefore));
		}
		if (consentToConvert.dateOfCreation != null) {
		  c.setDateTime(consentToConvert.dateOfCreation);
		}
	    c.getPolicy().clear();
		c.addPolicy().setUri("http://hl7.org/fhir/ConsentPolicy/opt-in");
		c.getProvision().setPurpose(new ArrayList<Coding>());
		c.getProvision().addPurpose(new Coding("http://midata.coop/codesystems/consent-type", consentToConvert.type.toString(), null));
		
		c.getMeta().setVersionId(""+System.currentTimeMillis());
		consentToConvert.lastUpdated = new Date();
		c.getMeta().setLastUpdated(consentToConvert.lastUpdated);
		
		if (part != null) {
			Extension ext = c.getExtensionByUrl("http://midata.coop/extensions/project-status");
			if (ext == null) ext = c.addExtension().setUrl("http://midata.coop/extensions/project-status");
			ext.setValue(new CodeType(part.pstatus.toString()));
		}
		
		
		String encoded = ctx.newJsonParser().encodeResourceToString(c);		
		consentToConvert.fhirConsent = BasicDBObject.parse(encoded);				
	}
			
	  
	
	@Override
	public List<Consent> searchRaw(SearchParameterMap params) throws AppException {		
		AccessContext info = info();
		
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, null);
			
		builder.handleIdRestriction();
		builder.recordOwnerReference("patient", "Patient", null);
		builder.restriction("_lastUpdated", false, QueryBuilder.TYPE_DATETIME, "lastUpdated");
		builder.restriction("identifier", false, QueryBuilder.TYPE_IDENTIFIER, "fhirConsent.identifier");
		builder.restriction("action", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "fhirConsent.provision.action");
		builder.restriction("consentor", false, null, "fhirConsent.performer");
		builder.restriction("organization", false, "Organization", "fhirConsent.organization");
		builder.restriction("category", false, QueryBuilder.TYPE_CODEABLE_CONCEPT, "fhirConsent.category");		
		builder.restriction("date", false, QueryBuilder.TYPE_DATETIME, "dateOfCreation");
		builder.restriction("period", false, QueryBuilder.TYPE_PERIOD_ENDONLY, "validUntil");
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "fhirConsent.status");
		builder.restriction("purpose", false, QueryBuilder.TYPE_CODING, "fhirConsent.provision.purpose");
		builder.restriction("data", false, null, "fhirConsent.provision.data.reference");					
		Set<String> authorized = null;
		if (params.containsKey("actor")) {
			List<ReferenceParam> actors = builder.resolveReferences("actor", null);
			if (actors != null) {
				if (FHIRTools.areAllOfType(actors, Sets.create("Patient","Group"))) {
				  authorized = FHIRTools.referencesToIds(actors);				
				} else builder.restriction("actor", false, null, "fhirConsent.provision.actor.reference");
			}
		}
								
		Map<String, Object> properties = query.retrieveAsNormalMongoQuery();
			
		if (authorized != null) {
			properties.put("authorized", authorized);
		}
		
		if (!info.mayAccess("Consent", "fhir/Consent")) properties.put("_id", info.getTargetAps());
		
		Object category = info.getAccessRestriction("Consent", "fhir/Consent", "category");
		if (category != null) properties.put("categoryCode", category);
		Object observer = info.getAccessRestriction("Consent", "fhir/Consent", "observer");
		if (observer != null) {
			
			 properties.put("observers", resolveObserver(observer));
		}
		AccessContext context = info();
		boolean ownerQuery = true;//!properties.containsKey("owner") || utils.access.Query.getRestriction(properties.get("owner"), "owner").contains(info().getLegacyOwner().toString());
		if (properties.containsKey("owner")) {
			Object ow = properties.get("owner");
			if (ow instanceof Collection) ow = ((Collection) ow).iterator().next();
			MidataId targetOwner = MidataId.from(ow);
			context = ApplicationTools.actAsRepresentative(context, targetOwner, false);
			if (context == null) {				
				ownerQuery = false;		
			}
		}
		
		ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "authorized", "observers");
		AccessLog.log(properties.toString());
		Set<models.Consent> consents = new HashSet<models.Consent>();						
		if (context != null && (authorized == null || authorized.contains(context.getOwner().toString()))) {
			AccessLog.log("read by auth");
			consents.addAll(Consent.getAllByAuthorized(context.getOwner(), properties, Consent.FHIR));
		}
		if (ownerQuery) {
			AccessLog.log("read by owner");
			
			consents.addAll(Consent.getAllByOwner(context.getOwner(), properties, Consent.FHIR, Circles.RETURNED_CONSENT_LIMIT));
		}
		MidataId pluginId = info().getUsedPlugin();
		if (pluginId != null) {
		  Plugin plugin = Plugin.getById(pluginId);
		  if (plugin.consentObserving) {
			  AccessLog.log("read by observer");
			  consents.addAll(Consent.getAllByObserver(pluginId, properties, Consent.FHIR, Circles.RETURNED_CONSENT_LIMIT));
		  }
		}		
		return new ArrayList<Consent>(consents);
	} 	
	
	public static Set<MidataId> resolveObserver(Object observer) throws AppException {
		 Set<Object> apps;
		 if (observer instanceof Collection) apps = new HashSet<Object>((Collection) observer);
		 else apps = Collections.singleton(observer);
		 Set<MidataId> resolved = new HashSet<MidataId>();
		 for (Object app : apps) {
			 if (!MidataId.isValid(app.toString())) {
				 Plugin p = PluginLoginCache.getByFilename(app.toString());					 
				 if (p!=null) resolved.add(p._id);
				 else throw new BadRequestException("error.internal", "Queried for unknown app as observer with internal name '"+app.toString()+"'.");
			 } else resolved.add(MidataId.from(app));
		 }
		 return resolved;
	}
	
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam org.hl7.fhir.r4.model.Consent theConsent) {
		return super.createResource(theConsent);
	}
	
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam org.hl7.fhir.r4.model.Consent theConsent) {		
		try {			
			return update(theId, theConsent);
		} catch (BaseServerResponseException e) {
			throw e;
		} catch (BadRequestException e2) {
			throw new InvalidRequestException(e2.getMessage());
		} catch (InternalServerException e3) {
			ErrorReporter.report("FHIR (update resource)", null, e3);
			throw new InternalErrorException(e3.getMessage());
		} catch (Exception e4) {
			ErrorReporter.report("FHIR (update resource)", null, e4);
			throw new InternalErrorException("internal error during resource update");
		}	
	}
				
	
	private static void mayShare(MidataId pluginId, Map<String, Object> query) throws AppException {
		Plugin plugin = Plugin.getById(pluginId);
		if (plugin == null || !plugin.resharesData) throw new ForbiddenOperationException("Plugin is not allowed to share data.");
		//AccessLog.log("may share="+query.toString()+" sub of "+plugin.defaultQuery.toString());
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
		
		if (subQuery.containsKey("$or")) {
			Collection<Map<String, Object>> parts = (Collection<Map<String, Object>>) subQuery.get("$or");
			for (Map<String, Object> part :parts) {
				boolean match = isSubQuery(masterQuery, part);
				if (match) return true;
			}
			return false;
		}
		
		masterQuery = new HashMap<String, Object>(masterQuery);
		subQuery = new HashMap<String, Object>(subQuery);
								
		Feature_FormatGroups.convertQueryToContents(masterQuery);		
		Feature_FormatGroups.convertQueryToContents(subQuery);
		
		masterQuery.remove("group-system");
		subQuery.remove("group-system");
		
	    for (Map.Entry<String, Object> entry : masterQuery.entrySet()) {
	    	if (entry.getKey().equals("owner")) continue;
	    	if (!subQuery.containsKey(entry.getKey())) return false;
	    	Set<String> master = utils.access.Query.getRestriction(entry.getValue(), entry.getKey());
	    	Set<String> sub = utils.access.Query.getRestriction(subQuery.get(entry.getKey()), entry.getKey());
	    	if (!master.containsAll(sub)) return false;	    	
	    }
		return true;
	}
		

	@Override
	public void createPrepare(Consent consent, org.hl7.fhir.r4.model.Consent theResource) throws AppException {
		commonPrepare(consent, theResource);
		
		if (!info().mayAccess("Patient", "fhir/Patient")) throw new ForbiddenOperationException("Consent creation requires access to patient resource.");
		
		Set<MidataId> observers = ApplicationTools.getObserversForApp(info().getUsedPlugin());
		consent.observers = observers;		
		
		if (theResource.getProvision().getPeriod() != null) {
		  consent.validUntil = theResource.getProvision().getPeriod().getEnd();
		}
			
		if (theResource.getProvision().getDataPeriod() != null) {
		  consent.createdBefore = theResource.getProvision().getDataPeriod().getEnd();
		}
			
		ConsentType type = null;
		if (theResource.hasScope()) {
			if ("research".equalsIgnoreCase(theResource.getScope().getCodingFirstRep().getCode())) type = ConsentType.STUDYPARTICIPATION;					
			else if (! ("patient-privacy".equalsIgnoreCase(theResource.getScope().getCodingFirstRep().getCode()))) throw new NotImplementedOperationException("Value '"+theResource.getScope().getCodingFirstRep().getCode()+"' for Consent.scope not supported.");
		}
		if (theResource.getProvision().hasPurpose()) {
			String typeCode = theResource.getProvision().getPurposeFirstRep().getCode();
			if (typeCode != null && typeCode.length() > 0) type = ConsentType.valueOf(typeCode);
		}
		
        
		consent.owner = info().getLegacyOwner();
		consent.creatorApp = info().getUsedPlugin();
		consent.creator = info().getActor();
		consent.writes = WritePermissionType.UPDATE_AND_CREATE;
		
		for (provisionActorComponent cac : theResource.getProvision().getActor()) {
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
				  if (mid.getType().equals("Practitioner")) {
				     if (type == null) type = ConsentType.HEALTHCARE;
				  } else if (mid.getType().equals("Patient")) {
					 if (type == null) type = ConsentType.CIRCLE;
				  }
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
			  if (mid.getType().equals("Practitioner")) {
				  if (type == ConsentType.CIRCLE) type = ConsentType.HCRELATED;				 
			  }
			} else {
				String login = FHIRTools.getMidataLoginFromReference(patient);
				if (login != null) {
					consent.externalOwner = login;
					consent.owner = null;
				}
			}
		}
		
		if (type == null) throw new UnprocessableEntityException("No MIDATA consent type may be derived from provided FHIR consent.");
		if (type != ConsentType.CIRCLE && type != ConsentType.HCRELATED && type != ConsentType.HEALTHCARE && type != ConsentType.REPRESENTATIVE && type != ConsentType.STUDYPARTICIPATION) throw new UnprocessableEntityException("MIDATA consent type not supported in API");
		consent.type = type;
		
		Map<String, Object> query = new HashMap<String, Object>();
		createQuery(theResource.getProvision(), query);
		Feature_FormatGroups.convertQueryToContents(query);	
		
		consent.sharingQuery = query;
        
		if (theResource.getStatus() == ConsentState.ACTIVE) {			
			if (ApplicationTools.actAsRepresentative(info(), consent.owner, false)==null) throw new InvalidRequestException("Only consent owner or representative may create active consents");
			
			if (consent.type == ConsentType.STUDYPARTICIPATION) {
				Study study = getStudyForConsent(theResource);
				if (study == null) throw new InvalidRequestException("Unknown project reference");
				mayShare(info().getUsedPlugin(), study.recordQuery);
			} else {
				mayShare(info().getUsedPlugin(), consent.sharingQuery);
			}
			
			consent.status = ConsentStatus.ACTIVE;
		} else if (theResource.getStatus() != ConsentState.PROPOSED) {
			throw new ForbiddenOperationException("consent status not supported for creation.");
		}
		
		if (consent.type == ConsentType.IMPLICIT) throw new ForbiddenOperationException("consent type not supported for creation.");
		
	}
		
	public void commonPrepare(Consent consent, org.hl7.fhir.r4.model.Consent theResource) throws AppException {
        consent.categoryCode = theResource.getCategoryFirstRep().getCodingFirstRep().getCode();
		
		consent.name = theResource.getCategoryFirstRep().getText();
		for (Extension ext : theResource.getExtensionsByUrl("http://midata.coop/extensions/consent-name")) {
		  consent.name = ext.getValue().toString();	
		}
		if (consent.name == null) consent.name = "Unnamed";
									
	}

	@Override
	public org.hl7.fhir.r4.model.Consent createExecute(Consent consent, org.hl7.fhir.r4.model.Consent theResource) throws AppException {
		if (consent.type == ConsentType.STUDYPARTICIPATION) {
			Study study = getStudyForConsent(theResource);
			MidataId studyId = study._id;
			String joinCode = null;
			StudyParticipation part;
			
			part = controllers.members.Studies.match(info(), consent.owner, studyId, info().getUsedPlugin(), JoinMethod.API);				
			
			theResource.setId(part._id.toString());
			MidataConsentResourceProvider.updateMidataConsent(part, theResource);
			Consent.set(part._id, "fhirConsent", part.fhirConsent);
			if (consent.status == ConsentStatus.ACTIVE) {
			   part = controllers.members.Studies.requestParticipation(part, info(), consent.owner, studyId, info().getUsedPlugin(), JoinMethod.API, joinCode);
			   theResource.setStatus(ConsentState.ACTIVE);
			} else {						
			   SubscriptionManager.resourceChange(info(), part);
			}
			processDataSharing(part, theResource);
			return readConsentFromMidataConsent(info(), part, true);			
		} else {		    		   
			String encoded = ctx.newJsonParser().encodeResourceToString(theResource);		
			consent.fhirConsent = BasicDBObject.parse(encoded);		 
	        Circles.addConsent(info(), consent, true, null, false);        
			theResource.setDateTime(consent.dateOfCreation);
			theResource.setId(consent._id.toString());
			processDataSharing(consent, theResource);
			addQueryToConsent(consent, theResource);			
			addActorsToConsent(consent, theResource);
			return theResource;
		}
		
		
	}
	
	private Study getStudyForConsent(org.hl7.fhir.r4.model.Consent theResource) throws AppException {
		for (CodeableConcept category : theResource.getCategory()) {
			if (category.hasCoding()) {
				for (Coding coding : category.getCoding()) {
					if (coding.hasCode() && coding.hasSystem() && coding.getSystem().equals("http://midata.coop/codesystems/project-code")) {
						String studyCode = coding.getCode();
						Study study = Study.getByCodeFromMember(studyCode, Study.ALL);
						return study;
					}
				}
			}
		}
		return null;
	}

	@Override
	public Consent init(org.hl7.fhir.r4.model.Consent theResource) {
		Consent consent = new Consent();
		
		consent.creatorApp = info().getUsedPlugin();
		consent.status = ConsentStatus.UNCONFIRMED;
		consent.authorized = new HashSet<MidataId>();
		
		return consent;
	}

	@Override
	public void updatePrepare(Consent consent, org.hl7.fhir.r4.model.Consent theResource) throws AppException {
        
		switch(consent.status) {
		case UNCONFIRMED:
		case INVALID:
			if (theResource.getStatus()==ConsentState.ACTIVE) {
				if (!info().getAccessor().equals(consent.owner)) throw new InvalidRequestException("Only consent owner may change consents to active consents");
				mayShare(info().getUsedPlugin(), consent.sharingQuery);
			} 
			break;
		case ACTIVE:
			break;
		case REJECTED:
			if (theResource.getStatus()!=ConsentState.REJECTED) throw new InvalidRequestException("Status of rejected consents may not be changed.");
			break;
		case FROZEN:
			if (theResource.getStatus()!=ConsentState.INACTIVE) throw new InvalidRequestException("Status of inactive consents may not be changed.");
			break;
		}
						
	}

	@Override
	public void updateExecute(Consent consent, org.hl7.fhir.r4.model.Consent theResource) throws AppException {
		AccessContext context = ApplicationTools.actAsRepresentative(info(), consent.owner, true);
		ConsentState state = theResource.getStatus();
		MidataConsentResourceProvider.updateMidataConsent(consent, theResource);				
		Consent.set(consent._id, "fhirConsent", consent.fhirConsent);		
		SubscriptionManager.resourceChange(info(), consent);
		theResource.setStatus(state);
						
		switch(consent.status) {		
		case UNCONFIRMED:
			if (theResource.getStatus()==ConsentState.ACTIVE) {
				if (consent.type == ConsentType.STUDYPARTICIPATION) {
					   StudyParticipation part = StudyParticipation.getById(consent._id, Sets.create("study"));
					   Studies.requestParticipation(info(), consent.owner, part.study, info().getUsedPlugin(), JoinMethod.API, null);
				} else {
				    HealthProvider.confirmConsent(context, consent._id);
				}
			}
			if (theResource.getStatus()==ConsentState.REJECTED) {
				if (consent.type == ConsentType.STUDYPARTICIPATION) {
				   StudyParticipation part = StudyParticipation.getById(consent._id, Sets.create("study"));
				   Studies.noParticipation(info(), consent.owner, part.study);
				} else {
				   HealthProvider.rejectConsent(context, consent.owner, consent._id);
				}
			}
			break;
		case ACTIVE:
			if (theResource.getStatus()==ConsentState.REJECTED || theResource.getStatus()==ConsentState.INACTIVE) {
				if (consent.type == ConsentType.STUDYPARTICIPATION) {
					   StudyParticipation part = StudyParticipation.getById(consent._id, Sets.create("study"));
					   Studies.retreatParticipation(context, consent.owner, part.study);
				} else {
				       HealthProvider.rejectConsent(context, consent.owner, consent._id);
				}
			}
			break;
		case REJECTED:
		case FROZEN:	
		case INVALID:
			break;
		}
						
		AuditManager.instance.success();		
		
	}

	@Override
	public Consent fetchCurrent(IIdType theId, org.hl7.fhir.r4.model.Consent resource) throws AppException {
		return Circles.getConsentById(info(), MidataId.from(theId.getIdPart()), info().getUsedPlugin(), Consent.FHIR);	
	}

	@Override
	public void processResource(Consent record, org.hl7.fhir.r4.model.Consent resource) throws AppException {		
		// Leave empty. Think about history consents (ConsentVersion)
		
        Extension meta = null;
        if (resource.getMeta().hasExtension("http://midata.coop/extensions/metadata")) {
        	meta = resource.getMeta().getExtensionByUrl("http://midata.coop/extensions/metadata");
        } else {
        	meta = new Extension("http://midata.coop/extensions/metadata"); 
        	resource.getMeta().addExtension(meta);	
        }
		
		if (record.creatorApp != null) {
		  Plugin creatorApp = Plugin.getById(record.creatorApp);		
		  if (creatorApp != null && !meta.hasExtension("app")) meta.addExtension("app", new Coding("http://midata.coop/codesystems/app", creatorApp.filename, creatorApp.name));
		}
		if (record.creator != null && !meta.hasExtension("creator")) meta.addExtension("creator", FHIRTools.getReferenceToUser(record.creator, null));
				
		
	}

	@Override
	public List<org.hl7.fhir.r4.model.Consent> parse(List<Consent> consents, Class<org.hl7.fhir.r4.model.Consent> resultClass) throws AppException {
		List<org.hl7.fhir.r4.model.Consent> result = new ArrayList<org.hl7.fhir.r4.model.Consent>();
		for (models.Consent consent : consents) {
			result.add(readConsentFromMidataConsent(info(), consent, true));
		}
		
		return result;
	}

	@Override
	public String getVersion(Consent record) {
		if (record.fhirConsent != null) {
			BasicBSONObject obj = ((BasicBSONObject) record.fhirConsent.get("meta"));
			return obj != null ? obj.getString("versionId","0") : "0";
		} else return "0";		
	}

	@Override
	public Date getLastUpdated(Consent record) {
		return record.lastUpdated;
	}

	@Override
	protected void convertToR4(Object in) {
		// Leave empty. Think about history consents (ConsentVersion)		
	}	
	
	public static MidataConsentResourceProvider getInstance() {
		return (MidataConsentResourceProvider) (((HybridTypeResourceProvider) FHIRServlet.myProviders.get("Consent")).getFirstProvider());
	}
	
	
}