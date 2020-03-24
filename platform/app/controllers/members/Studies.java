package controllers.members;

import java.util.Calendar;
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
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.ParticipationStatus;
import models.enums.UserFeature;
import models.enums.WritePermissionType;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
import utils.auth.ExecutionInfo;
import utils.auth.MemberSecured;
import utils.auth.Rights;
import utils.collections.Sets;
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
	public Result list() throws JsonValidationException, InternalServerException {
	   MidataId user = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
	   
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
	public Result search() throws JsonValidationException, InternalServerException, AuthException {
	   MidataId user = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
	   
	   JsonNode json = request().body().asJson();
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
	public Result enterCode() throws AppException {
        JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "code");
		String codestr = JsonValidation.getString(json, "code");
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
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
		
		Study study = Study.getById(code.study, Sets.create("name", "type", "participantSearchStatus", "owner", "createdBy", "recordQuery", "code"));
				
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return inputerror("code", "notsearching", "Study is not searching for participants.");
		
		StudyParticipation part = createStudyParticipation(userId, study, user, code);
				
		if (code.status != ParticipationCodeStatus.REUSEABLE) {
		   code.setStatus(ParticipationCodeStatus.USED);
		}
		
		ObjectNode result = Json.newObject();
		result.put("study", code.study.toString());
		return ok(result);
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
	public Result updateParticipation(String id) throws JsonValidationException, AppException {
		JsonNode json = request().body().asJson();
		MidataId studyId = new MidataId(id);
		MidataId memberId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));
		
		
		StudyParticipation part = StudyParticipation.getByStudyAndMember(studyId, memberId, Sets.create(Consent.SMALL, "providers", "authorized"));
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
			RecordManager.instance.shareAPS(part._id, RecordManager.instance.createContextFromConsent(memberId, part), memberId, newProviders);
			
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
			RecordManager.instance.unshareAPS(part._id, memberId, newProviders);
			
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
	
	public static StudyParticipation createStudyParticipation(MidataId executor, Study study, Member member, ParticipationCode code) throws AppException {
		StudyParticipation part = new StudyParticipation();
		part._id = new MidataId();
		part.study = study._id;
		part.studyName = study.name;
		part.name = "Study: "+study.name;
		part.owner = member._id;
		part.dateOfCreation = new Date();
		part.lastUpdated = part.dateOfCreation;
		part.dataupdate = System.currentTimeMillis();
		
		String userName;
		
		if (study.requiredInformation == InformationType.DEMOGRAPHIC) {
			userName = null; //member.lastname+", "+member.firstname;	
		} else {
			do {
			  userName = "P-" + CodeGenerator.nextUniqueCode();
			} while (StudyParticipation.existsByStudyAndMemberName(study._id, userName));
		}
				
		part.setOwnerName(userName);
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
			part.joinMethod = JoinMethod.ALGORITHM;
		}
		
		//PatientResourceProvider.generatePatientForStudyParticipation(part, member);
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(member.birthday);
		part.yearOfBirth = cal.get(Calendar.YEAR);
		part.gender = member.gender;
		part.country = member.country;
			
		part.providers = new HashSet<MidataId>();
		part.entityType = EntityType.USERGROUP;
		part.authorized = new HashSet<MidataId>();		
		part.authorized.add(study._id);
		
		RecordManager.instance.createAnonymizedAPS(member._id, study._id, part._id, true);
		
		/*if (code != null) {
		  History codedentererd = new History(EventType.CODE_ENTERED, part, member, null); 
		  part.history.add(codedentererd);
		} */
		
		Circles.prepareConsent(part, true);
		//StudyParticipation.add(part);		
		Circles.setQuery(executor, member._id, part._id, study.recordQuery);
		Circles.consentSettingChange(executor, part);
		
		// Query can only be applied if patient is doing it himself
		if (executor.equals(member._id)) {
		  RecordManager.instance.applyQuery(RecordManager.instance.createContextFromAccount(executor), study.recordQuery, member._id, part._id, study.requiredInformation.equals(InformationType.DEMOGRAPHIC));
		}
		
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
	public Result get(String id) throws JsonValidationException, InternalServerException {
	   MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));	
	   MidataId studyId = new MidataId(id);
	   	   
	   Set<String> studyFields = Sets.create("_id", "type", "createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","infos","infosPart", "owner","participantRules","recordQuery","studyKeywords","requiredInformation","anonymous","assistance", "startDate", "endDate", "dataCreatedBefore", "termsOfUse", "joinMethods");
	   Set<String> consentFields = Sets.create("_id", "pstatus", "providers");
	   Set<String> researchFields = Sets.create("_id", "name", "description");
	   
	   Study study = Study.getById(studyId, studyFields);
	   StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, consentFields);
	   Research research = Research.getById(study.owner, researchFields);
	 
	   if (participation == null || participation.pstatus != ParticipationStatus.ACCEPTED) study.infosPart = null;
	   ObjectNode obj = Json.newObject();
	   obj.put("study", JsonOutput.toJsonNode(study, "Study", studyFields));
	   obj.put("participation", JsonOutput.toJsonNode(participation, "Consent", consentFields));
	   obj.put("research", JsonOutput.toJsonNode(research, "Research", researchFields));
	   
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
	public Result requestParticipation(String id) throws AppException {
		/*if (!InstanceConfig.getInstance().getInstanceType().equals(InstanceType.PERFTEST)) {
		  forbidSubUserRole(SubUserRole.TRIALUSER, SubUserRole.NONMEMBERUSER);
		  forbidSubUserRole(SubUserRole.STUDYPARTICIPANT, SubUserRole.NONMEMBERUSER);
		  forbidSubUserRole(SubUserRole.APPUSER, SubUserRole.NONMEMBERUSER);	
		}*/
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));		
		MidataId studyId = new MidataId(id);	
		
		User user = Member.getById(userId, Member.ALL_USER_INTERNAL);
		
		Set<UserFeature> requirements = precheckRequestParticipation(userId, studyId);
		Set<UserFeature> notok = Application.loginHelperPreconditionsFailed(user, requirements);
		if (notok != null && !notok.isEmpty()) requireUserFeature(notok.iterator().next());
		
		requestParticipation(new ExecutionInfo(userId, getRole()), userId, studyId, null, JoinMethod.PORTAL);		
		return ok();
	}
	
	public static StudyParticipation requestParticipation(ExecutionInfo inf, MidataId userId, MidataId studyId, MidataId usingApp, JoinMethod joinMethod) throws AppException {
		
		
		Member user = Member.getById(userId, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country"));		
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("status", "pstatus", "ownerName", "owner", "authorized", "sharingQuery", "validUntil", "createdBefore"));		
		Study study = Study.getById(studyId, Sets.create("name", "joinMethods", "executionStatus", "participantSearchStatus", "owner", "createdBy", "name", "recordQuery", "requiredInformation", "termsOfUse", "code", "autoJoinGroup", "type"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
		
		
		if (participation == null) {
			if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) throw new JsonValidationException("error.closed.study", "code", "notsearching", "Study is not searching for participants.");
			
			if (study.joinMethods != null && !study.joinMethods.contains(joinMethod)) throw new JsonValidationException("error.closed.study", "code", "notsearching", "Study is not searching for participants using this channel."); 
			
			participation = createStudyParticipation(inf.executorId, study, user, null);
										
		}
				
		if (participation.pstatus == ParticipationStatus.ACCEPTED || participation.pstatus == ParticipationStatus.REQUEST) return participation;
		
		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_REQUESTED, userId, participation, study);
		
		if (participation.pstatus != ParticipationStatus.CODE && participation.pstatus != ParticipationStatus.MATCH) throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.REQUEST, joinMethod);	
		
		//participation.addHistory(new History(EventType.PARTICIPATION_REQUESTED, participation, user, null));
		if (study.termsOfUse != null) user.agreedToTerms(study.termsOfUse, usingApp);		
		if (study.requiredInformation.equals(InformationType.RESTRICTED)) {
			PatientResourceProvider.createPatientForStudyParticipation(inf, participation, user);
			Circles.autosharePatientRecord(inf.executorId, participation);
		} else {
			Circles.autosharePatientRecord(inf.executorId, participation);
		}
		
		Circles.consentStatusChange(inf.executorId, participation, ConsentStatus.ACTIVE);				

		AuditManager.instance.success();
		
		if (study.autoJoinGroup != null) {
			AutoJoiner.autoJoin(usingApp, userId, study._id);
		}
		
		return participation;
	}
	
	
	
    public static StudyParticipation match(MidataId executor, MidataId userId, MidataId studyId, MidataId usingApp) throws AppException {
		
		
		Member user = Member.getById(userId, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country"));		
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("status", "pstatus", "ownerName", "owner", "authorized", "sharingQuery", "validUntil", "createdBefore"));		
		Study study = Study.getById(studyId, Sets.create("name", "type", "executionStatus", "participantSearchStatus", "owner", "createdBy", "name", "recordQuery", "requiredInformation", "anonymous", "termsOfUse", "code"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
		
		
		if (participation == null) {
			if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) throw new JsonValidationException("error.closed.study", "code", "notsearching", "Study is not searching for participants.");
			
			participation = createStudyParticipation(executor, study, user, null);
										
		}		
		
		return participation;
		
	}
	
   public static Set<UserFeature> precheckRequestParticipation(MidataId userId, MidataId studyId) throws AppException {
				
		Member user = userId != null ? Member.getById(userId, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country")) : null;		
		StudyParticipation participation = userId != null ? StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("status", "pstatus", "ownerName", "owner", "authorized", "sharingQuery", "validUntil", "createdBefore")) : null;		
		Study study = Study.getById(studyId, Sets.create("type", "executionStatus", "participantSearchStatus", "owner", "createdBy", "name", "recordQuery", "requiredInformation", "anonymous", "requirements", "code"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
		if (participation == null) {
			if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) throw new JsonValidationException("error.closed.study", "code", "notsearching", "Study is not searching for participants.");
			return study.requirements;							
		}
		
		if (participation.pstatus == ParticipationStatus.ACCEPTED || participation.pstatus == ParticipationStatus.REQUEST) return study.requirements;			
		if (participation.pstatus != ParticipationStatus.CODE && participation.pstatus != ParticipationStatus.MATCH) throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");
		
		return study.requirements;
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
	public Result noParticipation(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));		
		MidataId studyId = new MidataId(id);
		
		Member user = Member.getById(userId, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country"));
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create(Consent.ALL, "status", "pstatus", "ownerName", "owner", "authorized"));		
		Study study = Study.getById(studyId, Sets.create("name", "executionStatus", "participantSearchStatus", "createdBy", "code", "type"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
		if (participation == null) throw new BadRequestException("error.blocked.participation", "Member is not allowed to participate in study.");
		
		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_MEMBER_REJECTED, userId, participation, study);
		if (participation.pstatus != ParticipationStatus.CODE && participation.pstatus != ParticipationStatus.MATCH && participation.pstatus != ParticipationStatus.REQUEST) throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.MEMBER_REJECTED);		
		//participation.addHistory(new History(EventType.NO_PARTICIPATION, participation, user, null));
		Circles.consentStatusChange(userId, participation, ConsentStatus.REJECTED);
		controllers.research.Studies.leaveSharing(userId, studyId, userId);
		AuditManager.instance.success();
		return ok();
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
	public Result retreatParticipation(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().attrs().get(play.mvc.Security.USERNAME));		
		MidataId studyId = new MidataId(id);
		
		retreatParticipation(userId, userId, studyId);		
		
		AuditManager.instance.success();
		return ok();
	}
	
	public static void retreatParticipation(MidataId executor, MidataId userId, MidataId studyId) throws JsonValidationException, AppException {
				
		Member user = Member.getById(userId, Sets.create("firstname", "lastname", "email", "birthday", "gender", "country"));
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create(Consent.ALL, "status", "pstatus", "ownerName", "owner", "authorized"));		
		Study study = Study.getById(studyId, Sets.create("name", "executionStatus", "participantSearchStatus", "createdBy", "code", "type"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
		if (participation == null) throw new BadRequestException("error.blocked.participation", "Member does not participate in study.");
		
		if (participation.pstatus == ParticipationStatus.REQUEST || participation.pstatus == ParticipationStatus.MATCH || participation.pstatus == ParticipationStatus.CODE) {
			AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_MEMBER_REJECTED, userId, participation, study);			
			
			participation.setPStatus(ParticipationStatus.MEMBER_REJECTED);				
			Circles.consentStatusChange(executor, participation, ConsentStatus.REJECTED);
		} else {
		
		AuditManager.instance.addAuditEvent(AuditEventType.STUDY_PARTICIPATION_MEMBER_RETREAT, userId, participation, study);
		   if (participation.pstatus != ParticipationStatus.ACCEPTED) throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");
		
		   participation.setPStatus(ParticipationStatus.MEMBER_RETREATED);
		}
		//participation.addHistory(new History(EventType.NO_PARTICIPATION, participation, user, null));
		Circles.consentStatusChange(executor, participation, ConsentStatus.FROZEN);				
		controllers.research.Studies.leaveSharing(executor, studyId, userId);
	}
		
}
