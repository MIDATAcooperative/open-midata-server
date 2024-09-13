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

package controllers.members;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.APICall;
import controllers.APIController;
import controllers.Application;
import controllers.Circles;
import controllers.research.AutoJoiner;
import models.Consent;
import models.Member;
import models.MidataId;
import models.ParticipationCode;
import models.Research;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.InformationType;
import models.enums.JoinMethod;
import models.enums.MessageReason;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.ParticipationStatus;
import models.enums.ProjectLeavePolicy;
import models.enums.RejoinPolicy;
import models.enums.UserFeature;
import models.enums.WritePermissionType;
import org.apache.commons.lang3.tuple.Pair;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.ApplicationTools;
import utils.access.Feature_Pseudonymization;
import utils.access.Feature_QueryRedirect;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.MemberSecured;
import utils.auth.Rights;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.fhir.PatientResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.Messager;

/**
 * functions about studies for members
 *
 */
public class Studies extends APIController {

	/**
	 * search for consents of a user related to studies
	 * @return list of consents (StudyParticipation)
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public Result list(Request request) throws JsonValidationException, InternalServerException {
	   MidataId user = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
	   
	   Set<String> fields = Sets.create("study","studyName", "pstatus");
	   Set<StudyParticipation> participation = StudyParticipation.getAllByMember(user, fields);
	   
	   return ok(JsonOutput.toJson(participation, "Consent", fields));
	}
	
	/**
	 * search for studies matching some criteria
	 * @return list of studies
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 * @throws AuthException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public Result search(Request request) throws JsonValidationException, InternalServerException, AuthException {
	   MidataId user = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
	   
	   JsonNode json = request.body().asJson();
	   JsonValidation.validate(json, "properties", "fields");
							   		
	   Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
	   ObjectIdConversion.convertMidataIds(properties, "_id", "owner", "createdBy", "studyKeywords");
	   Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
	   	   
	   Rights.chk("Studies.search", getRole(), properties, fields);
	   Set<Study> studies = Study.getAll(user, properties, fields);
	   
	   return ok(JsonOutput.toJson(studies, "Study", fields));
	}
	
	/**
	 * join study by participation code. NEEDS TO BE REWRITTEN. USES WRONG CONCEPT OF PARTICIPATION CODES
	 * @return
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public Result enterCode(Request request) throws AppException {
        JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "code");
		String codestr = JsonValidation.getString(json, "code");
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		
		Member user = Member.getById(userId, Sets.create("firstname","lastname","birthday","country","gender"));
				
		ParticipationCode code = ParticipationCode.getByCode(codestr);
		if (code == null) return inputerror("code", "notfound", "Unknown Participation Code.");
		
		StudyParticipation existing = StudyParticipation.getByStudyAndMember(code.study, userId, Sets.create("pstatus"));
		if (existing != null) {
			// Redirect to study page
			ObjectNode result = Json.newObject();
			result.put("study", code.study.toString());
			return ok(result);
		}
				
		if (code.status != ParticipationCodeStatus.UNUSED && 
			code.status != ParticipationCodeStatus.SHARED && 
			code.status != ParticipationCodeStatus.REUSEABLE) return inputerror("code","alreadyused","Participation code has expired.");
		
		Study study = Study.getById(code.study, Sets.create("name", "type", "participantSearchStatus", "owner", "createdBy", "recordQuery", "code", "leavePolicy"));
				
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return inputerror("code", "notsearching", "Study is not searching for participants.");
		
		StudyParticipation part = createStudyParticipation(context, study, user, code, null, JoinMethod.CODE);
				
		if (code.status != ParticipationCodeStatus.REUSEABLE) {
		   code.setStatus(ParticipationCodeStatus.USED);
		}
		
		ObjectNode result = Json.newObject();
		result.put("study", code.study.toString());
		return ok(result);
	}
		
	
	private static ParticipationCode checkCode(Study study, JoinMethod method, String codestr) throws AppException {
		if (method != JoinMethod.APP_CODE && method != JoinMethod.CODE) return null;
		if (codestr==null) throw new BadRequestException("error.missing.joincode", "Invalid participation code");
		
		ParticipationCode code = ParticipationCode.getByCode(codestr);
		if (code == null) throw new BadRequestException("error.invalid.joincode", "Invalid participation code");
		
		if (code.status != ParticipationCodeStatus.UNUSED && 
				code.status != ParticipationCodeStatus.SHARED && 
				code.status != ParticipationCodeStatus.REUSEABLE) throw new BadRequestException("error.invalid.joincode", "Invalid participation code");
		
		return code;		
	}
	
	private static void consumeCode(Study study, ParticipationCode code) throws AppException {
		if (code == null) return;
		if (code.status != ParticipationCodeStatus.REUSEABLE) {
			   code.setStatus(ParticipationCodeStatus.USED);
		}
	}
	
	/**
	 * change study participation of current member. add or remove a health professional to the study participation.
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public Result updateParticipation(Request request, String id) throws JsonValidationException, AppException {
		JsonNode json = request.body().asJson();
		MidataId studyId = new MidataId(id);
		MidataId memberId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		
		StudyParticipation part = StudyParticipation.getByStudyAndMember(studyId, memberId, StudyParticipation.STUDY_EXTRA);
		if (part == null) throw new BadRequestException("error.unknown.participation", "Study Participation not found.");
		
		JsonNode add = json.get("add");
		if (add != null) {
			JsonNode providers = add.get("providers");
			Set<MidataId> newProviders = JsonExtraction.extractMidataIdSet(providers);
			if (part.providers == null) part.providers = new HashSet<MidataId>();
			if (part.authorized == null) part.authorized = new HashSet<MidataId>();
			
			for (MidataId providerId : newProviders) {				
				part.providers.add(providerId);	
				part.authorized.add(providerId);
			}
			RecordManager.instance.shareAPS(context.forConsentReshare(part), newProviders);
			
			StudyParticipation.set(part._id, "providers", part.providers);
			StudyParticipation.set(part._id, "authorized", part.authorized);
		}
		
		JsonNode remove = json.get("remove");
		if (remove != null) {
			JsonNode providers = remove.get("providers");
			Set<MidataId> newProviders = JsonExtraction.extractMidataIdSet(providers);
			if (part.providers == null) part.providers = new HashSet<MidataId>();
			if (part.authorized == null) part.authorized = new HashSet<MidataId>();
			
			for (MidataId providerId : newProviders) {				
				part.providers.remove(providerId);	
				part.authorized.remove(providerId);
			}
			RecordManager.instance.unshareAPS(context.forConsentReshare(part), part._id, newProviders);
			
			StudyParticipation.set(part._id, "providers", part.providers);
			StudyParticipation.set(part._id, "authorized", part.authorized);
		}
		
		return ok();
	}
	
	/**
	 * helper function to create a study participation consent.
	 * @param study Study to participate
	 * @param member Member who may participate
	 * @param code ParticipationCode used (NEEDS REWRITE)
	 * @return StudyParticipation consent
	 * @throws InternalServerException
	 */
	
	public static StudyParticipation createStudyParticipation(AccessContext context, Study study, Member member, ParticipationCode code, Set<MidataId> observers, JoinMethod method) throws AppException {
		AccessLog.logBegin("start create project participation");
		StudyParticipation part = new StudyParticipation();
		part._id = new MidataId();
		part.study = study._id;
		part.studyName = study.name;
		part.name = "Study: "+study.name;
		part.owner = member._id;
		part.dateOfCreation = new Date();
		part.lastUpdated = part.dateOfCreation;
		part.creator = context.getActor();
		part.creatorApp = context.getUsedPlugin();
		part.dataupdate = System.currentTimeMillis();
		part.observers = observers;
		if (study.consentObserver != null) {
			if (part.observers == null) part.observers = study.consentObserver;
			else {
				Set<MidataId> allObservers = new HashSet<MidataId>();
				allObservers.addAll(study.consentObserver);
				allObservers.addAll(part.observers);
				part.observers = allObservers;
			}
		}
						
		if (study.requiredInformation != InformationType.DEMOGRAPHIC) {
			part.ownerName = "?";
		}
		
		part.status = ConsentStatus.UNCONFIRMED;
		part.writes = WritePermissionType.UPDATE_AND_CREATE;
		part.createdBefore = study.dataCreatedBefore;
		if (code != null) {
			part.group = code.group;
			part.recruiter = code.recruiter;		
			part.recruiterName = code.recruiterName;
			part.pstatus = ParticipationStatus.CODE;
			part.joinMethod = JoinMethod.CODE;
		} else {
			part.pstatus = ParticipationStatus.MATCH;
			part.joinMethod = method;
		}
		
		//PatientResourceProvider.generatePatientForStudyParticipation(part, member);
		
		//Calendar cal = Calendar.getInstance();
		//cal.setTime(member.birthday);
				
		part.providers = new HashSet<MidataId>();
		part.entityType = EntityType.USERGROUP;
		part.authorized = new HashSet<MidataId>();		
		part.authorized.add(study._id);
		part.sharingQuery = Feature_QueryRedirect.simplifyAccessFilter(null, study.recordQuery);
		
		RecordManager.instance.createAnonymizedAPS(context.getCache(), member._id, study._id, part._id, true);
		
		Set<MidataId> managers = context.getManagers();
		for (MidataId manager : managers) {
			if (!manager.equals(part.owner) && !part.authorized.contains(manager)) {
				part.managers = new HashSet<MidataId>();
				part.managers.add(manager);
			}
		}
		if (study.leavePolicy == ProjectLeavePolicy.FREEZE && part.managers != null) {
			context.getCache().getAPS(part._id).addAccess(part.managers, true);
		}
		
		
		Circles.consentStatusChange(context, part, null);
		
		// Query can only be applied if patient is doing it himself
		if (context.getAccessor().equals(member._id)) {
		  RecordManager.instance.applyQuery(context, part.sharingQuery, member._id, part, study.requiredInformation.equals(InformationType.DEMOGRAPHIC));
		}
		AccessLog.logEnd("end create project participation");
		return part;
		
	}
	
	/**
	 * retrieve information about a study, the organization that does the study and the participation consent (if exists) for the current user 
	 * @param id ID of study
	 * @return Study, Research Organization and Consent
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(AnyRoleSecured.class)
	public Result get(Request request, String id) throws AppException {
	   MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));	
	   AccessContext context = portalContext(request); 	
	   
	   Set<String> studyFields = Sets.create("_id", "type", "createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","infos","infosPart", "owner","participantRules","recordQuery","studyKeywords","requiredInformation","anonymous","assistance", "startDate", "endDate", "dataCreatedBefore", "termsOfUse", "joinMethods", "leavePolicy", "rejoinPolicy");
	   Set<String> consentFields = Sets.create("_id", "pstatus", "status", "providers", "ownerName");
	   Set<String> researchFields = Sets.create("_id", "name", "description");
	  
	   Study study;
	   MidataId studyId;
	   if (id.indexOf("-")>0) {
		   study = Study.getByCodeFromMember(id, studyFields);
		   if (study==null) return notFound();
		   studyId = study._id;
	   } else {
	       studyId = MidataId.from(id);
	       study = Study.getById(studyId, studyFields);
	       if (study==null) return notFound();
	   }
	 	   	   
	   StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, consentFields);
	   Research research = Research.getById(study.owner, researchFields);
	 
	   if (participation == null || participation.pstatus != ParticipationStatus.ACCEPTED) study.infosPart = null;
	   
	   if (participation != null) participation.ownerName = null;
	   if (participation != null && study.requiredInformation != InformationType.DEMOGRAPHIC) {
	       Pair<MidataId, String> ps = Feature_Pseudonymization.pseudonymizeUser(context, participation);
	       if (ps != null) participation.ownerName = ps.getRight();
	   }
	   
	   ObjectNode obj = Json.newObject();
	   obj.set("study", JsonOutput.toJsonNode(study, "Study", studyFields));
	   obj.set("participation", JsonOutput.toJsonNode(participation, "Consent", consentFields));
	   obj.set("research", JsonOutput.toJsonNode(research, "Research", researchFields));
	   
	   return ok(obj);
	}
	
	/**
	 * request study participation of current member for a given study
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws InternalServerException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	@BodyParser.Of(BodyParser.Json.class)
	public Result requestParticipation(Request request, String id) throws AppException {
		
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));		
		MidataId studyId = new MidataId(id);	
		JsonNode json = request.body().asJson();
		
		User user = Member.getById(userId, Member.ALL_USER_INTERNAL);
		
		Set<UserFeature> requirements = precheckRequestParticipation(userId, studyId);
		Set<UserFeature> notok = Application.loginHelperPreconditionsFailed(user, requirements);
		if (notok != null && !notok.isEmpty()) requireUserFeature(request, notok.iterator().next());
		StudyParticipation part = null;
		if (json.has("code")) {
		  part = requestParticipation(portalContext(request), userId, studyId, null, JoinMethod.CODE, JsonValidation.getString(json, "code"));
		} else {
		  part = requestParticipation(portalContext(request), userId, studyId, null, JoinMethod.PORTAL, null);		
		}
		
		return ok(JsonOutput.toJson(part, "Consent", Consent.ALL));
	}

	public static StudyParticipation requestParticipation(AccessContext context, MidataId userId, MidataId studyId, MidataId usingApp, JoinMethod joinMethod, String joinCode) throws AppException {
	   return requestParticipation(null, context, userId, studyId, usingApp, joinMethod, joinCode);
	}
	public static StudyParticipation requestParticipation(StudyParticipation participation, AccessContext context, MidataId userId, MidataId studyId, MidataId usingApp, JoinMethod joinMethod, String joinCode) throws AppException {
		AccessLog.logBegin("start request participation user="+userId+" project="+studyId);
		boolean successful = true;
		try {			
			Member user = Member.getById(userId, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country"));		
			if (participation == null) participation = StudyParticipation.getByStudyAndMember(studyId, userId, StudyParticipation.STUDY_EXTRA);		
			Study study = Study.getById(studyId, Sets.create("name", "joinMethods", "executionStatus", "participantSearchStatus", "owner", "createdBy", "name", "recordQuery", "requiredInformation", "termsOfUse", "code", "autoJoinGroup", "autoJoinTestGroup", "type", "consentObserver", "leavePolicy", "rejoinPolicy"));
			ParticipationCode code = null;
			if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
			        
			if (participation == null) {
				if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) throw new JsonValidationException("error.closed.study", "code", "notsearching", "Study is not searching for participants.");			
				if (study.joinMethods != null && !study.joinMethods.contains(joinMethod)) throw new JsonValidationException("error.blocked.joinmethod", "code", "joinmethod", "Study is not searching for participants using this channel.");
				code = checkCode(study, joinMethod, joinCode);
				Set<MidataId> observers = ApplicationTools.getObserversForApp(usingApp);
				participation = createStudyParticipation(context, study, user, code, observers, joinMethod);
			}
				
			if (participation.pstatus == ParticipationStatus.ACCEPTED || participation.pstatus == ParticipationStatus.REQUEST) return participation;
					
			if (participation.pstatus != ParticipationStatus.CODE && participation.pstatus != ParticipationStatus.MATCH) {
				if ((participation.pstatus == ParticipationStatus.MEMBER_RETREATED || participation.pstatus == ParticipationStatus.MEMBER_REJECTED) && study.rejoinPolicy == RejoinPolicy.DELETE_LAST) {
					if (participation.status != ConsentStatus.DELETED) {
						Circles.consentStatusChange(context, participation, ConsentStatus.DELETED);	
					}
					return requestParticipation(context, userId, studyId, usingApp, joinMethod, joinCode);
				} else throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");
			}
			
			AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_REQUESTED, userId, participation, study);
					
			participation.setPStatus(ParticipationStatus.REQUEST, joinMethod);	
			
			//participation.addHistory(new History(EventType.PARTICIPATION_REQUESTED, participation, user, null));
			if (study.termsOfUse != null) user.agreedToTerms(study.termsOfUse, usingApp, true);		
			
			successful = false;
			
			Circles.consentStatusChange(context, participation, ConsentStatus.ACTIVE);
			
			if (study.requiredInformation.equals(InformationType.RESTRICTED) || study.requiredInformation.equals(InformationType.NONE)) {						
				participation.ownerName = PatientResourceProvider.createPatientForStudyParticipation(context, study, participation, user);						
			} 
			consumeCode(study, code); 
			
			Messager.sendProjectMessage(context, participation, MessageReason.PROJECT_PARTICIPATION_REQUEST, null);
	
			AuditManager.instance.success();
			
			if (study.autoJoinGroup != null) {
				AutoJoiner.autoJoin(usingApp, userId, study._id);
			}
			successful = true;
			return participation;
		} finally {
			if (!successful && participation != null) {
				AccessLog.log("failure - removing participation");
				try {
			      RecordManager.instance.deleteAPS(context, participation._id);
				} catch (Exception e) {}
				try {
			      Consent.delete(participation.owner, participation._id);
				} catch (Exception e) {}
			}
			AccessLog.logEnd("end request participation");	
		}
	}
	
	
	
    public static StudyParticipation match(AccessContext context, MidataId userId, MidataId studyId, MidataId usingApp, JoinMethod joinMethod) throws AppException {
		
		
		Member user = Member.getById(userId, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country"));		
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, StudyParticipation.STUDY_EXTRA);

		Study study = Study.getById(studyId, Sets.create("name", "joinMethods", "executionStatus", "participantSearchStatus", "owner", "createdBy", "name", "recordQuery", "requiredInformation", "termsOfUse", "code", "autoJoinGroup", "autoJoinTestGroup", "type", "consentObserver", "leavePolicy", "rejoinPolicy"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
			
		if (participation == null) {
			if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) throw new JsonValidationException("error.closed.study", "code", "notsearching", "Study is not searching for participants.");			
			if (study.joinMethods != null && !study.joinMethods.contains(joinMethod)) throw new JsonValidationException("error.blocked.joinmethod", "code", "joinmethod", "Study is not searching for participants using this channel.");
			//code = checkCode(study, joinMethod, joinCode);
			Set<MidataId> observers = ApplicationTools.getObserversForApp(usingApp);
			participation = createStudyParticipation(context, study, user, null, observers, joinMethod);
		} else {
			if (participation.pstatus != ParticipationStatus.CODE && 
			    participation.pstatus != ParticipationStatus.MATCH && 
			    participation.pstatus != ParticipationStatus.ACCEPTED &&
			    participation.pstatus != ParticipationStatus.REQUEST) {
				if ((participation.pstatus == ParticipationStatus.MEMBER_RETREATED || participation.pstatus == ParticipationStatus.MEMBER_REJECTED) && study.rejoinPolicy == RejoinPolicy.DELETE_LAST) {
					if (participation.status != ConsentStatus.DELETED) {
						Circles.consentStatusChange(context, participation, ConsentStatus.DELETED);	
					}
					return match(context, userId, studyId, usingApp, joinMethod);
				} else throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");
			}
		}
							
		return participation;
		
	}
	
   public static Set<UserFeature> precheckRequestParticipation(MidataId userId, MidataId studyId) throws AppException {
	    AccessLog.logBegin("start precheck for project participation");
	    try {
			Member user = userId != null ? Member.getById(userId, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country")) : null;		
			StudyParticipation participation = userId != null ? StudyParticipation.getByStudyAndMember(studyId, userId, StudyParticipation.STUDY_EXTRA) : null;		
			Study study = Study.getById(studyId, Sets.create("type", "executionStatus", "participantSearchStatus", "owner", "createdBy", "name", "recordQuery", "requiredInformation", "anonymous", "requirements", "code", "rejoinPolicy"));
			
			if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
			if (participation == null) {
				if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) throw new JsonValidationException("error.closed.study", "code", "notsearching", "Study is not searching for participants.");
				return study.requirements;							
			}
			
			if (participation.pstatus == ParticipationStatus.ACCEPTED || participation.pstatus == ParticipationStatus.REQUEST) return study.requirements;			
			if (participation.pstatus != ParticipationStatus.CODE && participation.pstatus != ParticipationStatus.MATCH) {
				if ((participation.pstatus == ParticipationStatus.MEMBER_RETREATED || participation.pstatus == ParticipationStatus.MEMBER_REJECTED) && study.rejoinPolicy == RejoinPolicy.DELETE_LAST) {
				  return study.requirements;			  
				}
				throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");
			}
			
			return study.requirements;
	    } finally {
	    	AccessLog.logEnd("end precheck for project participation");
	    }
	}
	
	/**
	 * reject study participation (by member)
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public Result noParticipation(Request request, String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));		
		AccessContext context = portalContext(request);
		MidataId studyId = new MidataId(id);
		noParticipation(context, userId, studyId);
		return ok();
	}
		
	public static void noParticipation(AccessContext context, MidataId userId, MidataId studyId) throws AppException {
		Member user = Member.getById(userId, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country"));
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, StudyParticipation.STUDY_EXTRA);		
		Study study = Study.getById(studyId, Sets.create("name", "executionStatus", "participantSearchStatus", "createdBy", "code", "type"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
		if (participation == null) throw new BadRequestException("error.blocked.participation", "Member is not allowed to participate in study.");
		
		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_MEMBER_REJECTED, userId, participation, study);
		if (participation.pstatus != ParticipationStatus.CODE && participation.pstatus != ParticipationStatus.MATCH && participation.pstatus != ParticipationStatus.REQUEST) throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.MEMBER_REJECTED);		
		//participation.addHistory(new History(EventType.NO_PARTICIPATION, participation, user, null));
		Circles.consentStatusChange(context, participation, ConsentStatus.REJECTED);
		Circles.sendConsentNotifications(context, participation, ConsentStatus.REJECTED, false);
		controllers.research.Studies.leaveSharing(context, studyId, userId);
		AuditManager.instance.success();
		
	}
	
	/**
	 * reject study participation (by member)
	 * @param id ID of study
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@APICall
	@Security.Authenticated(MemberSecured.class)
	public Result retreatParticipation(Request request, String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
		MidataId studyId = new MidataId(id);
		
		retreatParticipation(context, userId, studyId, false);		
		
		AuditManager.instance.success();
		return ok();
	}
	
	public static void retreatParticipation(AccessContext context, MidataId userId, MidataId studyId, boolean isFakeAccount) throws JsonValidationException, AppException {
				
		Member user = Member.getById(userId, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country"));
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, StudyParticipation.STUDY_EXTRA);		
		Study study = Study.getById(studyId, Sets.create("name", "executionStatus", "participantSearchStatus", "createdBy", "code", "type", "leavePolicy"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
		if (participation == null) throw new BadRequestException("error.blocked.participation", "Member does not participate in study.");
		boolean wasActive = participation.isActive();
		participation.studyCode = study.code;
		if (participation.pstatus == ParticipationStatus.REQUEST || participation.pstatus == ParticipationStatus.MATCH || participation.pstatus == ParticipationStatus.CODE) {
			AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_MEMBER_REJECTED, userId, participation, study);			
			
			participation.setPStatus(ParticipationStatus.MEMBER_REJECTED);				
			Circles.consentStatusChange(context, participation, ConsentStatus.REJECTED);
			Circles.sendConsentNotifications(context, participation, ConsentStatus.REJECTED, wasActive);
		} else {
		   AuditEventBuilder bld = AuditEventBuilder.withType(AuditEventType.STUDY_PARTICIPATION_MEMBER_RETREAT).withActor(context, userId).withStudy(study).withConsent(participation);
		   if (isFakeAccount) bld = bld.withMessage("fake account");
		   AuditManager.instance.addAuditEvent(bld);		   
		   if (participation.pstatus != ParticipationStatus.ACCEPTED) throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");		   
		   Messager.sendProjectMessage(context, participation, MessageReason.PROJECT_PARTICIPATION_RETREAT, participation.group);
			
		   participation.setPStatus(ParticipationStatus.MEMBER_RETREATED);
		   if (isFakeAccount) {
			   Circles.consentStatusChange(context, participation, ConsentStatus.DELETED);
		   } else {
			   if (study.leavePolicy == null || study.leavePolicy == ProjectLeavePolicy.FREEZE) {		   
			     Circles.consentStatusChange(context, participation, ConsentStatus.FROZEN);
			   } else if (study.leavePolicy == ProjectLeavePolicy.REJECT) {			  
				 Circles.consentStatusChange(context, participation, ConsentStatus.REJECTED);
			   } else {
				 Circles.consentStatusChange(context, participation, ConsentStatus.DELETED);
			   }
			   Circles.sendConsentNotifications(context, participation, ConsentStatus.REJECTED, wasActive);
		   }
		}
		//participation.addHistory(new History(EventType.NO_PARTICIPATION, participation, user, null));
						
		controllers.research.Studies.leaveSharing(context, studyId, userId);
	}
		
}
