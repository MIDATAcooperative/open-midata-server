package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import models.BulkMail;
import models.MidataId;
import models.NewsItem;
import models.Study;
import models.StudyParticipation;
import models.User;
import models.enums.BulkMailStatus;
import models.enums.EMailStatus;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.auth.AdminSecured;
import utils.auth.AnyRoleSecured;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.messaging.MailUtils;

public class BulkMails extends Controller {

	@BodyParser.Of(BodyParser.Json.class)
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result get() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "properties");
		
		// get news items
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
				
		ObjectIdConversion.convertMidataIds(properties, "_id", "creator", "studyId");		
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
	public Result add() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name");
		
		MidataId creator = MidataId.from(request().attrs().get(play.mvc.Security.USERNAME));
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
	public Result update() throws JsonValidationException, InternalServerException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name");		
		
		BulkMail item = BulkMail.getById(JsonValidation.getMidataId(json, "_id"), BulkMail.ALL);
		
		updateMail(json, item);
						
		BulkMail.update(item);
		
		return ok();
	}

	private void updateMail(JsonNode json, BulkMail item) throws JsonValidationException, InternalServerException {
		item.title = JsonExtraction.extractStringMap(json.get("title"));
		item.content = JsonExtraction.extractStringMap(json.get("content"));
		
		item.studyGroup = JsonValidation.getStringOrNull(json, "studyGroup");
		item.studyId = JsonValidation.getMidataId(json, "studyId");
		
		Study study = Study.getById(item.studyId, Sets.create("name", "code"));
		if (study == null) throw new JsonValidationException("error.unknown.study", "studyId", "unknown", "Unknown project");
		
		item.studyName = study.name;
		item.studyCode = study.code;
	}
	
	/**
	 * Delete a news item
	 * @param mailItemIdString id of news item to delete
	 * @return status 200
	 * @throws InternalServerException
	 */
	@Security.Authenticated(AdminSecured.class)
	@APICall
	public Result delete(String mailItemIdString) throws AppException {
			
		MidataId mailItemId = MidataId.from(mailItemIdString);
				
		BulkMail mailCampaign = BulkMail.getById(mailItemId, BulkMail.ALL);
		if (mailCampaign == null) throw new BadRequestException("error.unknown.bulkmail", "Mail not found");
		
		if (mailCampaign.status != BulkMailStatus.DRAFT &&
			(mailCampaign.status != BulkMailStatus.FINISHED || mailCampaign.progressCount>0)) throw new BadRequestException("error.invalid.bulkmail", "Wrong status for delete.");
		BulkMail.delete(mailItemId);
	
		return ok();
	}
	
	public Result send(String mailItemIdString) throws AppException {
		
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
			        	try {
			                sendMails(mailCampaign);
			        	} catch (Exception e) {
			        		ErrorReporter.report("bulk mail sender", null, e);			        		
			        	} finally {
			        		ServerTools.endRequest();
			        	}
			        }
			    };
	    new Thread(mySender).start();
	    
		return ok();
	}
	
	private List<MidataId> getTargetUsers(BulkMail mailItem) throws AppException {
		if (mailItem.studyGroup != null && mailItem.studyGroup.trim().length()==0) mailItem.studyGroup = null;
		Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroup(mailItem.studyId, mailItem.studyGroup, Sets.create("owner"));
		List<MidataId> ids = new ArrayList<MidataId>();
		if (mailItem.progressId != null) {
			for (StudyParticipation part : parts) if (part.owner.compareTo(mailItem.progressId) > 0) ids.add(part.owner);
		} else for (StudyParticipation part : parts) ids.add(part.owner);
        Collections.sort(ids);		
		return ids;
	}
	
	private void sendMails(BulkMail mailItem) throws AppException {
		List<MidataId> targets = getTargetUsers(mailItem);
		sendMails(mailItem, targets);
	}
	
	private void sendMails(BulkMail mailItem, List<MidataId> targets) throws AppException {
		for (MidataId target : targets) {
			if (sendMail(mailItem, target)) {
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
	
	private boolean sendMail(BulkMail mailItem, MidataId targetUser) throws AppException {
		User user = User.getById(targetUser, Sets.create("status", "email", "firstname", "lastname", "language", "emailStatus"));
		if (user != null && user.email != null && (user.emailStatus == EMailStatus.VALIDATED || user.emailStatus == EMailStatus.EXTERN_VALIDATED)) {
			String lang = user.language;
			if (lang == null) lang = InstanceConfig.getInstance().getDefaultLanguage();
			
			String content = mailItem.content.get(lang);
			String title = mailItem.title.get(lang);
			if (content == null) {
				content = mailItem.content.get("int");
				title = mailItem.title.get("int");
			}
			
			if (content == null) return false;
			if (title == null) return false;
			
			MailUtils.sendTextMail(user.email, user.firstname+" "+user.lastname, title, content);
			return true;
		}
		return false;
	};
}
