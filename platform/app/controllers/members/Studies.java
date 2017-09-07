package controllers.members;

import java.util.ArrayList;
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
import models.Consent;
import models.History;
import models.Member;
import models.MidataId;
import models.ParticipationCode;
import models.Research;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.enums.ConsentStatus;
import models.enums.EventType;
import models.enums.InformationType;
import models.enums.InstanceType;
import models.enums.ParticipantSearchStatus;
import models.enums.ParticipationCodeStatus;
import models.enums.ParticipationStatus;
import models.enums.SubUserRole;
import models.enums.UserFeature;
import models.enums.WritePermissionType;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utils.InstanceConfig;
import utils.access.RecordManager;
import utils.auth.AnyRoleSecured;
import utils.auth.CodeGenerator;
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
	public static Result list() throws JsonValidationException, InternalServerException {
	   MidataId user = new MidataId(request().username());
	   
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
	public static Result search() throws JsonValidationException, InternalServerException, AuthException {
	   MidataId user = new MidataId(request().username());
	   
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
	public static Result enterCode() throws AppException {
        JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "code");
		String codestr = JsonValidation.getString(json, "code");
		MidataId userId = new MidataId(request().username());
		
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
		
		Study study = Study.getByIdFromMember(code.study, Sets.create("name", "participantSearchStatus", "owner", "createdBy", "recordQuery"));
				
		if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) return inputerror("code", "notsearching", "Study is not searching for participants.");
		
		StudyParticipation part = createStudyParticipation(study, user, code);
				
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
	public static Result updateParticipation(String id) throws JsonValidationException, AppException {
		JsonNode json = request().body().asJson();
		MidataId studyId = new MidataId(id);
		MidataId memberId = new MidataId(request().username());
		
		
		StudyParticipation part = StudyParticipation.getByStudyAndMember(studyId, memberId, Sets.create("history", "providers", "authorized"));
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
			RecordManager.instance.shareAPS(part._id, memberId, newProviders);
			
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
	
	public static StudyParticipation createStudyParticipation(Study study, Member member, ParticipationCode code) throws AppException {
		StudyParticipation part = new StudyParticipation();
		part._id = new MidataId();
		part.study = study._id;
		part.studyName = study.name;
		part.name = "Study: "+study.name;
		part.owner = member._id;
		part.dateOfCreation = new Date();
		
		String userName;
		
		if (study.requiredInformation == InformationType.DEMOGRAPHIC) {
			userName = null; //member.lastname+", "+member.firstname;	
		} else {
			do {
			  userName = "Part. " + CodeGenerator.nextUniqueCode();
			} while (StudyParticipation.existsByStudyAndMemberName(study._id, userName));
		}
				
		part.ownerName = userName;
		part.status = ConsentStatus.ACTIVE;
		part.writes = WritePermissionType.NONE;
		if (code != null) {
			part.group = code.group;
			part.recruiter = code.recruiter;		
			part.recruiterName = code.recruiterName;
			part.pstatus = ParticipationStatus.CODE;
		} else part.pstatus = ParticipationStatus.MATCH;
		
		//PatientResourceProvider.generatePatientForStudyParticipation(part, member);
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(member.birthday);
		part.yearOfBirth = cal.get(Calendar.YEAR);
		part.gender = member.gender;
		part.country = member.country;
		
		part.history = new ArrayList<History>();
		part.providers = new HashSet<MidataId>();
		part.authorized = new HashSet<MidataId>();		
		part.authorized.add(study.createdBy);
		
		RecordManager.instance.createAnonymizedAPS(member._id, study.createdBy, part._id, true);
		
		if (code != null) {
		  History codedentererd = new History(EventType.CODE_ENTERED, part, member, null); 
		  part.history.add(codedentererd);
		} 
		
		Circles.prepareConsent(part);
		StudyParticipation.add(part);
		Circles.setQuery(member._id, member._id, part._id, study.recordQuery);
		RecordManager.instance.applyQuery(member._id, study.recordQuery, member._id, part._id, study.requiredInformation.equals(InformationType.DEMOGRAPHIC));
		
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
	public static Result get(String id) throws JsonValidationException, InternalServerException {
	   MidataId userId = new MidataId(request().username());	
	   MidataId studyId = new MidataId(id);
	   	   
	   Set<String> studyFields = Sets.create("_id", "createdAt","createdBy","description","executionStatus","name","participantSearchStatus","validationStatus","history","infos","owner","participantRules","recordQuery","studyKeywords","requiredInformation","assistance");
	   Set<String> consentFields = Sets.create("_id", "pstatus", "history","providers");
	   Set<String> researchFields = Sets.create("_id", "name", "description");
	   
	   Study study = Study.getByIdFromMember(studyId, studyFields);
	   StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, consentFields);
	   Research research = Research.getById(study.owner, researchFields);
	   
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
	public static Result requestParticipation(String id) throws AppException {
		/*if (!InstanceConfig.getInstance().getInstanceType().equals(InstanceType.PERFTEST)) {
		  forbidSubUserRole(SubUserRole.TRIALUSER, SubUserRole.NONMEMBERUSER);
		  forbidSubUserRole(SubUserRole.STUDYPARTICIPANT, SubUserRole.NONMEMBERUSER);
		  forbidSubUserRole(SubUserRole.APPUSER, SubUserRole.NONMEMBERUSER);	
		}*/
		MidataId userId = new MidataId(request().username());		
		MidataId studyId = new MidataId(id);	
		
		User user = Member.getById(userId, Member.ALL_USER);
		
		Set<UserFeature> requirements = precheckRequestParticipation(userId, studyId);
		Set<UserFeature> notok = Application.loginHelperPreconditionsFailed(user, requirements);
		if (notok != null && !notok.isEmpty()) requireUserFeature(notok.iterator().next());
		
		requestParticipation(userId, studyId);		
		return ok();
	}
	
	public static void requestParticipation(MidataId userId, MidataId studyId) throws AppException {
		
		
		Member user = Member.getById(userId, Sets.create("firstname", "lastname", "birthday", "gender", "country", "history"));		
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("status", "pstatus", "history", "ownerName", "owner", "authorized", "sharingQuery", "validUntil", "createdBefore"));		
		Study study = Study.getByIdFromMember(studyId, Sets.create("executionStatus", "participantSearchStatus", "history", "owner", "createdBy", "name", "recordQuery", "requiredInformation", "termsOfUse"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
		if (participation == null) {
			if (study.participantSearchStatus != ParticipantSearchStatus.SEARCHING) throw new JsonValidationException("error.closed.study", "code", "notsearching", "Study is not searching for participants.");
			
			participation = createStudyParticipation(study, user, null);
										
		}
		
		if (participation.pstatus == ParticipationStatus.ACCEPTED || participation.pstatus == ParticipationStatus.REQUEST) return;				
		if (participation.pstatus != ParticipationStatus.CODE && participation.pstatus != ParticipationStatus.MATCH) throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.REQUEST);						
		participation.addHistory(new History(EventType.PARTICIPATION_REQUESTED, participation, user, null));
		if (study.termsOfUse != null) user.addHistoryOnce(new History(EventType.TERMS_OF_USE_AGREED, user, study.termsOfUse));		
		if (study.requiredInformation.equals(InformationType.RESTRICTED)) {
			PatientResourceProvider.createPatientForStudyParticipation(participation, user);
		} else {
			Circles.autosharePatientRecord(participation);
		}
		
		Circles.consentStatusChange(userId, participation, ConsentStatus.ACTIVE);				
				
	}
	
   public static Set<UserFeature> precheckRequestParticipation(MidataId userId, MidataId studyId) throws AppException {
				
		Member user = userId != null ? Member.getById(userId, Sets.create("firstname", "lastname", "birthday", "gender", "country")) : null;		
		StudyParticipation participation = userId != null ? StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create("status", "pstatus", "history", "ownerName", "owner", "authorized", "sharingQuery", "validUntil", "createdBefore")) : null;		
		Study study = Study.getByIdFromMember(studyId, Sets.create("executionStatus", "participantSearchStatus", "history", "owner", "createdBy", "name", "recordQuery", "requiredInformation", "requirements"));
		
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
	public static Result noParticipation(String id) throws JsonValidationException, AppException {
		MidataId userId = new MidataId(request().username());		
		MidataId studyId = new MidataId(id);
		
		Member user = Member.getById(userId, Sets.create("firstname", "lastname", "birthday", "gender", "country"));
		StudyParticipation participation = StudyParticipation.getByStudyAndMember(studyId, userId, Sets.create(Consent.ALL, "status", "pstatus", "history", "ownerName", "owner", "authorized"));		
		Study study = Study.getByIdFromMember(studyId, Sets.create("executionStatus", "participantSearchStatus", "history"));
		
		if (study == null) throw new BadRequestException("error.unknown.study", "Study does not exist.");
		if (participation == null) throw new BadRequestException("error.blocked.participation", "Member is not allowed to participate in study.");				
		if (participation.pstatus != ParticipationStatus.CODE && participation.pstatus != ParticipationStatus.MATCH && participation.pstatus != ParticipationStatus.REQUEST) throw new BadRequestException("error.invalid.status_transition", "Wrong participation status.");
		
		participation.setPStatus(ParticipationStatus.MEMBER_REJECTED);		
		participation.addHistory(new History(EventType.NO_PARTICIPATION, participation, user, null));
		Circles.consentStatusChange(userId, participation, ConsentStatus.REJECTED);
						
		return ok();
	}
		
}
