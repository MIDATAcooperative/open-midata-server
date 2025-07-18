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

package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.BulkMail;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.enums.AuditEventType;
import models.enums.BulkMailStatus;
import models.enums.BulkMailType;
import models.enums.CommunicationChannelUseStatus;
import models.enums.ConsentStatus;
import models.enums.EMailStatus;
import models.enums.SubUserRole;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.AdminSecured;
import utils.auth.UnsubscribeToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.MailSenderType;
import utils.messaging.MailUtils;
import utils.stats.ActionRecorder;

public class BulkMails extends APIController {

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result get(Request request) throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "properties");
		
		// get news items
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
				
		ObjectIdConversion.convertMidataIds(properties, "_id", "creator", "studyId", "appId");		
		List<BulkMail> mailItems;
		
		mailItems = new ArrayList<BulkMail>(BulkMail.getAll(properties, BulkMail.ALL));
		long now = System.currentTimeMillis();
		for (BulkMail item : mailItems) {
			if (item.status == BulkMailStatus.STARTED || item.status == BulkMailStatus.IN_PROGRESS) {
				if (now - item.lastProgress > 1000l * 60l) item.status = BulkMailStatus.PAUSED;
			}
		}
		
		Collections.sort(mailItems);
		return ok(Json.toJson(mailItems));
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result add(Request request) throws JsonValidationException, AppException {
		// validate json
		requireSubUserRole(request, SubUserRole.NEWSWRITER);
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "name");
		
		MidataId creator = MidataId.from(request.attrs().get(play.mvc.Security.USERNAME));
        User creatorUser = User.getById(creator, Sets.create("email"));
		
		// create new news item
		BulkMail item = new BulkMail();
		item._id = new MidataId();		
		item.creator = creator;
		item.creatorName = creatorUser.email;
		item.created = new Date();
		item.name = JsonValidation.getString(json, "name");
		item.status = BulkMailStatus.DRAFT;
		
		updateMail(json, item);
			
		BulkMail.add(item);
		
		return ok(Json.toJson(item));
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result update(Request request) throws JsonValidationException, AppException {
		requireSubUserRole(request, SubUserRole.NEWSWRITER);
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "name");		
		
		BulkMail item = BulkMail.getById(JsonValidation.getMidataId(json, "_id"), BulkMail.ALL);
		
		updateMail(json, item);
						
		BulkMail.update(item);
		
		return ok();
	}

	private void updateMail(JsonNode json, BulkMail item) throws JsonValidationException, InternalServerException {
		item.title = JsonExtraction.extractStringMap(json.get("title"));
		item.content = JsonExtraction.extractStringMap(json.get("content"));		
		item.type = JsonValidation.getEnum(json, "type", BulkMailType.class);
		item.country = JsonValidation.getStringOrNull(json, "country");
		item.htmlFrame = JsonValidation.getUnboundString(json, "htmlFrame");
		item.studyGroup = JsonValidation.getStringOrNull(json, "studyGroup");
		item.studyId = json.has("studyId") ? JsonValidation.getMidataId(json, "studyId") : null;
		item.appId = json.has("appId") ? JsonValidation.getMidataId(json, "appId") : null;
		if (item.type == BulkMailType.PROJECT && item.studyId==null) throw new JsonValidationException("error.unknown.study", "studyId", "unknown", "Unknown project");
		if (item.type == BulkMailType.APP && item.appId==null) throw new JsonValidationException("error.unknown.app", "appId", "unknown", "Unknown App");
		if (item.studyId!=null) {
		  Study study = Study.getById(item.studyId, Sets.create("name", "code"));
		  if (study == null) throw new JsonValidationException("error.unknown.study", "studyId", "unknown", "Unknown project");
		  item.studyName = study.name;
		  item.studyCode = study.code;
		} else {
		  item.studyName = null;
		  item.studyCode = null;
		}
		if (item.appId != null) {
			Plugin plugin = Plugin.getById(item.appId);
			if (plugin == null) throw new JsonValidationException("error.unknown.app", "appId", "unknown", "Unknown application");
			item.appName = plugin.name;
		}
	}
	
	/**
	 * Delete a news item
	 * @param mailItemIdString id of news item to delete
	 * @return status 200
	 * @throws InternalServerException
	 */
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result delete(Request request, String mailItemIdString) throws AppException {
		requireSubUserRole(request, SubUserRole.NEWSWRITER);
		MidataId mailItemId = MidataId.from(mailItemIdString);
				
		BulkMail mailCampaign = BulkMail.getById(mailItemId, BulkMail.ALL);
		if (mailCampaign == null) throw new BadRequestException("error.unknown.bulkmail", "Mail not found");
		
		if (mailCampaign.status != BulkMailStatus.DRAFT &&
			(mailCampaign.status != BulkMailStatus.FINISHED || mailCampaign.progressCount>0)) throw new BadRequestException("error.invalid.bulkmail", "Wrong status for delete.");
		BulkMail.delete(mailItemId);
	
		return ok();
	}
	
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result send(Request request, String mailItemIdString) throws AppException {
		requireSubUserRole(request, SubUserRole.NEWSWRITER);
		MidataId mailItemId = MidataId.from(mailItemIdString);
		
		BulkMail mailCampaign = BulkMail.getById(mailItemId, BulkMail.ALL);
		if (mailCampaign == null) throw new BadRequestException("error.unknown.bulkmail", "Mail not found");
		if (mailCampaign.status == BulkMailStatus.FINISHED) throw new BadRequestException("error.invalid.bulkmail", "Mail not found");
		
		if (mailCampaign.status == BulkMailStatus.DRAFT) {
			mailCampaign.status = BulkMailStatus.STARTED;
			mailCampaign.started = new Date();
			mailCampaign.progressCount = 0;
			mailCampaign.progressId = null;
			mailCampaign.lastProgress = System.currentTimeMillis();
			mailCampaign.setProgress();
		}
				
		Runnable mySender =
			    new Runnable(){
			        public void run(){
			        	long st = ActionRecorder.start("BulkMails/send");
			        	try {
			                sendMails(mailCampaign);
			        	} catch (Exception e) {
			        		ErrorReporter.report("bulk mail sender", null, e);			        		
			        	} finally {
			        		ServerTools.endRequest();
			        		ActionRecorder.end("BulkMails/send", st);
			        	}
			        }
			    };
	    new Thread(mySender).start();
	    
		return ok();
	}
	
	@Security.Authenticated(AdminSecured.class)
	@APICall
    public Result test(Request request, String mailItemIdString) throws AppException {
		requireSubUserRole(request, SubUserRole.NEWSWRITER);
		MidataId mailItemId = MidataId.from(mailItemIdString);
		
		BulkMail mailCampaign = BulkMail.getById(mailItemId, BulkMail.ALL);
		if (mailCampaign == null) throw new BadRequestException("error.unknown.bulkmail", "Mail not found");
		//if (mailCampaign.status == BulkMailStatus.FINISHED) throw new BadRequestException("error.invalid.bulkmail", "Mail not found");
		
		MidataId executor = MidataId.from(request.attrs().get(play.mvc.Security.USERNAME));
		
		MidataId studyId = null;
		if (mailCampaign.type==BulkMailType.PROJECT) studyId = mailCampaign.studyId;
		
		sendMail(mailCampaign, executor, studyId);
		
		return ok();
    }
		
	private List<MidataId> getTargetUsers(BulkMail mailItem) throws AppException {
		if (mailItem.type == BulkMailType.PROJECT) {
			if (mailItem.studyGroup != null && mailItem.studyGroup.trim().length()==0) mailItem.studyGroup = null;
			Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(mailItem.studyId, mailItem.studyGroup, Sets.create("owner","projectEmails"));
			List<MidataId> ids = new ArrayList<MidataId>();
			if (mailItem.progressId != null) {
				for (StudyParticipation part : parts) if (part.projectEmails!=CommunicationChannelUseStatus.FORBIDDEN && part.owner.compareTo(mailItem.progressId) > 0) ids.add(part.owner);
			} else for (StudyParticipation part : parts) if (part.projectEmails!=CommunicationChannelUseStatus.FORBIDDEN) ids.add(part.owner);
	        Collections.sort(ids);		
			return ids;
		} else if (mailItem.type == BulkMailType.APP) {				
			Set<MobileAppInstance> parts = MobileAppInstance.getByApplication(mailItem.appId, Sets.create("owner"));
			Set<MidataId> ids = new HashSet<MidataId>();
			if (mailItem.progressId != null) {
				for (MobileAppInstance part : parts) if (part.owner.compareTo(mailItem.progressId) > 0) ids.add(part.owner);
			} else for (MobileAppInstance part : parts) ids.add(part.owner);
			List<MidataId> idsSorted = new ArrayList<MidataId>(ids);
		    Collections.sort(idsSorted);		
			return idsSorted;			
		} else {
			Set<User> users = User.getAllUser(CMaps.map("role",UserRole.MEMBER).map("status",User.NON_DELETED).mapNotEmpty("country", mailItem.country), Sets.create("_id","marketingEmail"));
			Set<MidataId> ids = new HashSet<MidataId>(users.size());
			if (mailItem.progressId != null) {
			  for (User user : users) if (user.marketingEmail!=CommunicationChannelUseStatus.FORBIDDEN && user._id.compareTo(mailItem.progressId) > 0) ids.add(user._id);
			} else for (User user : users) if (user.marketingEmail!=CommunicationChannelUseStatus.FORBIDDEN) ids.add(user._id);
			if (mailItem.studyId!=null) {
				if (mailItem.studyGroup != null && mailItem.studyGroup.trim().length()==0) mailItem.studyGroup = null;
				Set<StudyParticipation> parts = StudyParticipation.getParticipantsByStudy(mailItem.studyId, Sets.create("owner","projectEmails"));
				for (StudyParticipation part : parts) ids.remove(part.owner);
			}
			List<MidataId> result = new ArrayList<MidataId>(ids);
			Collections.sort(result);		
			return result;
		}
	}
	
	private void sendMails(BulkMail mailItem) throws AppException {
		List<MidataId> targets = getTargetUsers(mailItem);
		sendMails(mailItem, targets);
	}
	
	private void sendMails(BulkMail mailItem, List<MidataId> targets) throws AppException {
		MidataId studyId = null;
		if (mailItem.type==BulkMailType.PROJECT) studyId = mailItem.studyId;
		for (MidataId target : targets) {
			if (sendMail(mailItem, target, studyId)) {
				mailItem.progressCount++;
			}
			mailItem.status = BulkMailStatus.IN_PROGRESS;
			mailItem.progressId = target;
			mailItem.lastProgress = System.currentTimeMillis();
			mailItem.setProgress();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {}
		}
		mailItem.status = BulkMailStatus.FINISHED;
		mailItem.finished = new Date();
		mailItem.setProgress();
	}
	
	private boolean isEmptyMail(String content) {
		return content == null || content.trim().length()<10;
	}
	
	private boolean sendMail(BulkMail mailItem, MidataId targetUser, MidataId study) throws AppException {
		User user = User.getById(targetUser, Sets.create("status", "email", "emailLC", "firstname", "lastname", "language", "emailStatus", "role"));
		if (user != null && user.email != null && (user.emailStatus == EMailStatus.VALIDATED || user.emailStatus == EMailStatus.EXTERN_VALIDATED)) {
			String lang = user.language;
			if (lang == null) lang = InstanceConfig.getInstance().getDefaultLanguage();
			
			String content = mailItem.content.get(lang);
			String title = mailItem.title.get(lang);
			
			if (isEmptyMail(content)) {
				content = mailItem.content.get("int");
				title = mailItem.title.get("int");
			}
			
			if (isEmptyMail(content)) return false;
			if (title == null) return false;
			
			String link;
			if (study!=null && user.role != UserRole.ADMIN) {
				StudyParticipation sp = StudyParticipation.getByStudyAndMember(study, targetUser, Sets.create("_id","status"));
				if (sp == null || ! sp.isActive()) return false;
				
				link = "https://" + InstanceConfig.getInstance().getPortalServerDomain()+"/#/portal/unsubscribe?token="+UnsubscribeToken.consentToken(sp._id);
			} else link = "https://" + InstanceConfig.getInstance().getPortalServerDomain()+"/#/portal/unsubscribe?token="+UnsubscribeToken.userToken(targetUser);
			content = content.replaceAll("<unsubscribe>", link);
			boolean restricted = InstanceConfig.getInstance().getInstanceType().restrictBulkMails(); 
			//System.out.println(user.email+" "+user.firstname+" "+user.lastname+" "+title+" "+content);
			if (!restricted || (user.emailLC.endsWith("@midata.coop") || user.role==UserRole.ADMIN)) {
			  if (restricted) title="(Restricted Test): "+title;
			  try {
				AccessLog.log("send email to: "+user.email);
			    MailUtils.sendTextMail(MailSenderType.BULK, user.email, user.firstname+" "+user.lastname, title, content, mailItem.htmlFrame, mailItem.appId);
			  } catch (Exception e) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e2) {}
				try {
				    MailUtils.sendTextMail(MailSenderType.BULK, user.email, user.firstname+" "+user.lastname, title, content, mailItem.htmlFrame, mailItem.appId);
				} catch (Exception e3) {
				    mailItem.progressFailed++;
				}
			  }
			}
			return true;
		}
		return false;
	};
	
	@BodyParser.Of(BodyParser.Json.class)	
	@APICall
	public Result unsubscribe(Request request) throws AppException {
		JsonNode json = request.body().asJson();		
		if (!json.has("token")) throw new BadRequestException("error.missing.token", "No token");
		
		UnsubscribeToken tk = UnsubscribeToken.decrypt(JsonValidation.getString(json, "token"));
		if (tk != null) {
			if (tk.getUserId() != null) {
				User user = User.getById(tk.getUserId(), Sets.create(User.ALL_USER,"marketingEmail"));
				if (user!=null) {
					if (user.marketingEmail == CommunicationChannelUseStatus.FORBIDDEN) throw new BadRequestException("error.already_done.unsubscribed", "Already unsubscribed.");
					AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.COMMUNICATION_REJECTED).withActor(user).withMessage("email-link"));
					user.set("marketingEmail", CommunicationChannelUseStatus.FORBIDDEN);
				}
			}
			if (tk.getConsentId() != null) {
				StudyParticipation part = StudyParticipation.getById(tk.getConsentId(), Sets.create("_id","owner","study","projectEmails"));
				if (part!=null) {
					if (part.projectEmails == CommunicationChannelUseStatus.FORBIDDEN) throw new BadRequestException("error.already_done.unsubscribed", "Already unsubscribed.");
					AuditManager.instance.addAuditEvent(AuditEventBuilder.withType(AuditEventType.COMMUNICATION_REJECTED).withActor(null, part.owner).withStudy(part.study).withMessage("email-link"));
					part.set(part._id, "projectEmails", CommunicationChannelUseStatus.FORBIDDEN);
				}
			}
			AuditManager.instance.success();
		}
		
		return ok();
		
	}
}
