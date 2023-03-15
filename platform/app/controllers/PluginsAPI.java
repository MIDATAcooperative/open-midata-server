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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.BSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSONParseException;

import actions.MobileCall;
import actions.VisualizationCall;
import models.Consent;
import models.ContentInfo;
import models.MidataId;
import models.Plugin;
import models.Record;
import models.RecordsInfo;
import models.Space;
import models.SubscriptionData;
import models.UserGroupMember;
import models.enums.AggregationType;
import models.enums.UsageAction;
import models.enums.UserRole;
import play.libs.Json;
import play.libs.oauth.OAuth.ConsumerKey;
import play.libs.oauth.OAuth.OAuthCalculator;
import play.libs.oauth.OAuth.RequestToken;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Http.Request;
import play.mvc.Result;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.QueryTagTools;
import utils.RuntimeConstants;
import utils.ServerTools;
import utils.access.DBRecord;
import utils.access.EncryptedFileHandle;
import utils.access.RecordConversion;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.auth.MobileAppSessionToken;
import utils.auth.Rights;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.AccountCreationAccessContext;
import utils.context.AccountReuseAccessContext;
import utils.context.ConsentAccessContext;
import utils.context.ContextManager;
import utils.context.CreateParticipantContext;
import utils.context.PublicAccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.fhir.SubscriptionResourceProvider;
import utils.json.JsonExtraction;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.largerequests.HugeBodyParser;
import utils.messaging.ServiceHandler;
import utils.messaging.SubscriptionManager;
import utils.stats.Stats;
import utils.stats.UsageStatsRecorder;

/**
 * API functions to be used by MIDATA plugins
 *
 */
public class PluginsAPI extends APIController {

	
	static WSClient ws;
	
	public static void init(WSClient ws1) {
		ws = ws1;
	}
	
	/**
	 * handle OPTIONS requests. 
	 * @return status ok
	 */
	@VisualizationCall
	public Result checkPreflight() {		
		return ok();
	}
 
	/**
	 * return IDs of all records current plugin has access to. Deprecated. Use getRecords instead.
	 * @return list of object IDs
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result getIds() throws AppException, JsonValidationException  {		
		return ok();
		
	}

	/**
	 * retrieve configuration json for a space. 
	 * @return json previously stored with setConfig
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result getConfig(Request request) throws JsonValidationException, AppException {
		
		// validate json
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "authToken");
		
		// decrypt authToken and check whether space with corresponding owner exists
		AccessContext inf = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());
		
		if (inf.getSingleReadableRecord() != null)  {
		   ObjectNode result = Json.newObject();
		   result.put("readonly", true);
		   return ok(result);
		}
				
		BSONObject meta = RecordManager.instance.getMeta(inf, inf.getTargetAps(), "_config");
		
		if (meta != null) return ok(Json.toJson(meta.toMap()));
		
		return ok();
	}
	
	/**
	 * retrieve oauth extra json for a space. 
	 * @return json previously stored with setConfig
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result getOAuthParams(Request request) throws JsonValidationException, AppException {
		
		// validate json
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "authToken");
				
		AccessContext inf = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());
				
		BSONObject meta = RecordManager.instance.getMeta(inf, inf.getTargetAps(), "_oauthParams");
		
		if (meta != null) return ok(Json.toJson(meta.toMap()));
		
		return ok();
	}
	
	/**
	 * store configuration json for a space
	 * @return status ok
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result setConfig(Request request) throws JsonValidationException, AppException {
		Stats.startRequest(request);
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "authToken");
		
		// decrypt authToken 
		AccessContext inf = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());
		
		Stats.setPlugin(inf.getUsedPlugin());
		
		if (json.has("config")) {
		   Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));		
		   RecordManager.instance.setMeta(inf, inf.getTargetAps(), "_config", config);
		}
		if (json.has("autoimport")) {
			boolean auto = JsonValidation.getBoolean(json, "autoimport");
			Space space = Space.getByIdAndOwner(inf.getTargetAps(), inf.getLegacyOwner(), Sets.create("autoImport", "owner", "visualization"));
			if (space==null) throw new InternalServerException("error.internal", "Space not found.");
			
			// Disable old style import
			Space.set(space._id, "autoImport", false);
			
			List<SubscriptionData> entries = SubscriptionData.getByOwnerAndFormatAndInstance(space.owner, "time", space._id, SubscriptionData.ALL);
			SubscriptionData data = null;
			for (SubscriptionData d : entries) data = d;
			if (auto) {		
				
				if (data == null) {
					data = new SubscriptionData();
					data._id = new MidataId();
				}				
				data.active = true;
				data.app = space.visualization;
				data.instance = space._id;
				data.format = "time";
				data.content = null;
				data.lastUpdated = System.currentTimeMillis();
				data.owner = space.owner;
				data.session = ServiceHandler.encrypt(KeyManager.instance.currentHandle(space.owner));
				SubscriptionResourceProvider.fillInFhirForAutorun(data);
				data.add();
				SubscriptionManager.subscriptionChange(data);
				
				
				/*User autoRunner = Admin.getByEmail("autorun-service", Sets.create("_id"));
				RecordManager.instance.shareAPS(space._id, spaceToken.executorId, Collections.singleton(autoRunner._id));
				RecordManager.instance.materialize(spaceToken.executorId, space._id);
				Space.set(space._id, "autoImport", auto);*/
			} else {
				for (SubscriptionData data1 : entries) data1.setOff(data._id);
				/*User autoRunner = Admin.getByEmail("autorun-service", Sets.create("_id"));
				RecordManager.instance.unshareAPS(space._id, spaceToken.executorId, Collections.singleton(autoRunner._id));
				Space.set(space._id, "autoImport", auto);*/
			}
		}
				
		Stats.finishRequest(request, "200");
		return ok();
	}
	
	/**
	 * clone current space with a different confuguration
	 * @return
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result cloneAs(Request request) throws JsonValidationException, AppException {
		
		// validate json
		JsonNode json = request.body().asJson();
		
		JsonValidation.validate(json, "authToken", "name", "config");
		
		// decrypt authToken 
		AccessContext inf = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());
		
		Map<String, Object> config = JsonExtraction.extractMap(json.get("config"));
		String name = JsonValidation.getString(json, "name");
		Space current = Space.getByIdAndOwner(inf.getTargetAps(), inf.getLegacyOwner(), Sets.create("context", "visualization", "app", "licence"));
		if (current == null) throw new BadRequestException("error.unknown.space", "The current space does no longer exist.");
		
		Space space = Spaces.add(inf.getLegacyOwner(), name, current.visualization, current.type, current.context, current.licence);		
		BSONObject bquery = RecordManager.instance.getMeta(inf, inf.getTargetAps(), "_query");		
		Map<String, Object> query;
		if (bquery != null) {
			query = bquery.toMap();
						
			RecordManager.instance.shareByQuery(ContextManager.instance.createSharingContext(inf, inf.getCache().getAccountOwner()), space._id, query);
		}		
		RecordManager.instance.setMeta(inf, space._id, "_config", config);
						
		return ok();
	}

	/**
	 * retrieve records current space has access to matching some criteria
	 * @return list of Records
	 * @throws JsonValidationException
	 * @throws AppException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result getRecords(Request request) throws JsonValidationException, AppException {
		Stats.startRequest(request);
		// validate json
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "authToken", "properties", "fields");
		
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
						
		AccessContext inf = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());
		Stats.setPlugin(inf.getUsedPlugin());
		UsageStatsRecorder.protokoll(inf.getUsedPlugin(), UsageAction.GET);
		
		Collection<Record> records = getRecords(inf, properties, fields);
				
		Stats.finishRequest(request, "200", properties.keySet());
		return ok(JsonOutput.toJson(records, "Record", fields)).as("application/json");
	}
	
	/**
	 * helper function to retrieve records matching some criteria
	 * @param inf execution info manages access rights
	 * @param properties key-value map containing restrictions
	 * @param fields set of field names to return
	 * @return collection of records matching given criteria where executor has access to 
	 * @throws AppException
	 */
	public static Collection<Record> getRecords(AccessContext inf, Map<String,Object> properties, Set<String> fields) throws AppException {
		// Do not check for fields that query for parts of the data.
		Set<String> chkFields = new HashSet<String>();
		for (String f : fields) {
			  if (!f.startsWith("data.")) chkFields.add(f);
		}
						
		Rights.chk("getRecords", UserRole.ANY, chkFields);
					
		if (inf.getSingleReadableRecord() != null) properties.put("_id", inf.getSingleReadableRecord());

		// get record data
		Collection<Record> records = null;
				
		AccessLog.log("NEW QUERY");		
		records = RecordManager.instance.list(inf.getAccessorRole(), inf, properties, fields);		  
						
		ReferenceTool.resolveOwners(records, fields.contains("ownerName"), fields.contains("creatorName"));
		
		return records;
	}
	
	/**
	 * retrieve aggregated information about records matching some criteria
	 * @return record info json
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result getInfo(Request request) throws AppException, JsonValidationException {
		Stats.startRequest(request);
		
		// check whether the request is complete
		JsonNode json = request.body().asJson();				
		JsonValidation.validate(json, "authToken", "properties", "summarize");
		
		// decrypt authToken 
		AccessContext authToken = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());
		//SpaceToken authToken = SpaceToken.decryptAndSession(request, json.get("authToken").asText());
		if (authToken == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		Stats.setPlugin(authToken.getUsedPlugin());		
		MidataId targetAps = authToken.getTargetAps();
		
		Collection<RecordsInfo> result;

		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = json.has("fields") ? JsonExtraction.extractStringSet(json.get("fields")) : Sets.create();
		
		if (authToken.getSingleReadableRecord() != null) {
			Collection<Record> record = RecordManager.instance.list(authToken.getAccessorRole(), authToken, CMaps.map("_id", authToken.getSingleReadableRecord()), Sets.create("owner", "content", "format", "group"));
			result = new ArrayList<RecordsInfo>();
			for (Record r : record) result.add(new RecordsInfo(r));			
		} else {
							
			AggregationType aggrType = JsonValidation.getEnum(json, "summarize", AggregationType.class);		
		    result = RecordManager.instance.info(authToken.getAccessorRole(), targetAps, authToken, properties, aggrType);

		    

		}
	    if (fields.contains("ownerName")) ReferenceTool.resolveOwnersForRecordsInfo(result, true);
	    
	    Stats.finishRequest(request, "200", properties.keySet());
		return ok(JsonOutput.toJson(result, "Record", Record.ALL_PUBLIC)).as("application/json");
	}
	
	/**
	 * retrieve a file record current space has access to
	 * @return file
	 * @throws AppException
	 * @throws JsonValidationException
	 */	
	@VisualizationCall	
	public Result getFile(Request request) throws AppException, JsonValidationException {
		Stats.startRequest(request);
	
		AccessContext info = null;
		Optional<String> param = request.header("Authorization");
		String param2 = request.queryString("authToken").orElse(null);
		
		if (param.isPresent() && param.get().startsWith("Bearer ")) {
          info = ExecutionInfo.checkToken(request, param.get().substring("Bearer ".length()), false, true);                  	
		} else if (param2 != null) {
		  info = ExecutionInfo.checkToken(request, param2, false, true);
		} else throw new BadRequestException("error.auth", "Please provide authorization token as 'Authorization' header or 'authToken' request parameter.");
				
		String id = request.queryString("id").orElse(null);
					
		if (info == null) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
		if (id == null) {
			throw new BadRequestException("error.missing.input_field", "Missing id");
		}
		Stats.setPlugin(info.getUsedPlugin());
		Pair<String,Integer> recordId = RecordManager.instance.parseFileId(id);
			
		return MobileAPI.getFile(request, info, MidataId.from(recordId.getLeft()), recordId.getRight(), true);		
	}
	
	/**
	 * create a new record. The current space will automatically have access to it.
	 * @return
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result createRecord(Request request) throws AppException, JsonValidationException {
		Stats.startRequest(request);		
		
		// check whether the request is complete
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "name", "format");
		if (!json.has("content") && !json.has("code")) throw new JsonValidationException("error.validation.fieldmissing", "Request parameter 'content' or 'code' not found.");
		
		
		AccessContext authToken = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());
		Stats.setPlugin(authToken.getUsedPlugin());
		if (authToken.getSingleReadableRecord() != null) throw new BadRequestException("error.internal", "This view is readonly.");
		UsageStatsRecorder.protokoll(authToken.getUsedPlugin(), UsageAction.POST);	
		
		String data = JsonValidation.getJsonString(json, "data");
		String name = JsonValidation.getString(json, "name");
		String description = JsonValidation.getString(json, "description");
		String format = JsonValidation.getString(json, "format");
		
		String content = JsonValidation.getStringOrNull(json, "content");
		Set<String> code = JsonExtraction.extractStringSet(json.get("code"));
		
		
		Record record = new Record();
		if (json.has("_id")) {
			record._id = JsonValidation.getMidataId(json, "_id");			
		} else {
		    record._id = new MidataId();
		}
		record.app = authToken.getUsedPlugin();
		record.owner = authToken.getLegacyOwner();
		record.creator = authToken.getActor();
		record.modifiedBy = record.creator;
		record.created = record._id.getCreationDate();
		
		/*if (json.has("created-override")) {
			record.created = JsonValidation.getDate(json, "created-override");
		}*/
		
		record.format = format;
		
		
		ContentInfo.setRecordCodeAndContent(authToken.getUsedPlugin(), record, code, content);		
					
		try {
			record.data = BasicDBObject.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
		
		createRecord(authToken, record);
		
		Stats.finishRequest(request, "200", Collections.EMPTY_SET);
		return ok();
	}
	
	/**
	 * Helper function to create a record
	 * @param inf execution context
	 * @param record record to add to the database
	 * @throws AppException
	 */
	public static void createRecord(AccessContext inf, Record record) throws AppException  {
       createRecord(inf, record, null);		
	}
			
	public static void createRecord(AccessContext inf, Record record, List<EncryptedFileHandle> fileData) throws AppException  {
		AccessContext context = inf;
		if (record.format==null) record.format = "application/json";
		if (record.content==null) record.content = "other";
		if (record.owner==null) record.owner = inf.getLegacyOwner();
		if (record.name==null) record.name="unnamed";
		
		if (record.tags != null && record.tags.contains(QueryTagTools.SECURITY_PUBLIC)) {
			record.owner = RuntimeConstants.instance.publicUser;
			context = context.forPublic();
		}
		
		DBRecord dbrecord = RecordConversion.instance.toDB(record);
        				
		if (!record.owner.equals(inf.getAccessor()) && !inf.getAccessor().equals(RuntimeConstants.instance.autorunService) && !(context instanceof ConsentAccessContext) && !(context instanceof CreateParticipantContext) && !(context instanceof AccountCreationAccessContext) && !(context instanceof PublicAccessContext)) {
			BSONObject query = RecordManager.instance.getMeta(inf, inf.getTargetAps(), "_query");
			Set<Consent> consent = null;
			if (query != null && query.containsField("link-study")) {
				
				MidataId groupId = MidataId.from(query.get("link-study"));
                UserGroupMember ugm = UserGroupMember.getByGroupAndActiveMember(groupId, inf.getAccessor());
                if (ugm != null) context = context.forUserGroup(ugm);
				consent = Consent.getHealthcareOrResearchActiveByAuthorizedAndOwner(groupId, record.owner);
				
			} else {
				
			    consent = Consent.getHealthcareOrResearchActiveByAuthorizedAndOwner(inf.getAccessor(), record.owner);
			}
									
			if (consent == null || consent.isEmpty()) {
				if (InstanceConfig.getInstance().getInstanceType().doExtendedDeveloperReports()) {
				   throw new PluginException(inf.getUsedPlugin(), "error.noconsent", "No active consent that allows to add data for target person.");					
				} else {
				   throw new BadRequestException("error.noconsent", "No active consent that allows to add data for target person.");
				}
			}
			AccessContext contextWithConsent = null;
			AccessContext lastTried = null;
			for (Consent c : consent) {
				ConsentAccessContext cac = new ConsentAccessContext(c, context);
				
				if (cac.mayCreateRecord(dbrecord)) {				
					contextWithConsent = cac;
					break;
				} else lastTried = cac;
			}
			if (contextWithConsent == null) {
				if (InstanceConfig.getInstance().getInstanceType().doExtendedDeveloperReports()) {
				   throw new PluginException(inf.getUsedPlugin(), "error.noconsent", "None of the "+consent.size()+" possible consents allow to write "+dbrecord.getErrorInfo()+" for target person.\n\nLast tried create permission chain:\n==================================\n"+lastTried.getMayCreateRecordReport(dbrecord));
				} else {
				   throw new InternalServerException("error.internal", "Record may not be created!");
				}
			}
			context = contextWithConsent;
		} else if (!context.mayCreateRecord(dbrecord)) {
			throw new PluginException(inf.getUsedPlugin(), "error.plugin", dbrecord.getErrorInfo()+" may not be created. Please check access filter and permissions in developer portal.\n\nCreate permission chain:\n========================\n"+context.getMayCreateRecordReport(dbrecord));					
		}
		if (context.mustPseudonymize()) throw new PluginException(inf.getUsedPlugin(), "error.plugin", dbrecord.getErrorInfo()+" may not be created. Access is pseudonymized! \n\nCreate permisssion chain:\n========================\n"+context.getMayCreateRecordReport(dbrecord));
		
		//MidataId targetAPS = targetConsent != null ? targetConsent : inf.targetAPS;
		if (record.tags != null && record.tags.contains(QueryTagTools.SECURITY_LOCALCOPY)) {
			  RecordManager.instance.addLocalRecord(context, record);
		} else if (fileData != null) {			 
			  RecordManager.instance.addRecord(context, record, context.getTargetAps(), fileData);
		} else {
			  RecordManager.instance.addRecord(context, record, context.getTargetAps());
		}
		
		Set<MidataId> records = Collections.singleton(record._id);
								    				
		AccessContext myContext = context;		
		
		if (inf.getAccessor().equals(inf.getLegacyOwner())) {
			while (myContext != null) {
				if (!myContext.isIncluded(dbrecord)) {
					RecordManager.instance.share(inf, inf.getLegacyOwner(), myContext.getTargetAps(), records, !myContext.getOwner().equals(dbrecord.owner));
				}
				myContext = myContext.getParent();
			}									
		} else {
			
			myContext = myContext.getParent();			
			while (myContext != null) {
				if (!myContext.isIncluded(dbrecord)) {
					RecordManager.instance.share(inf, context.getTargetAps(), myContext.getTargetAps(), records, !myContext.getOwner().equals(dbrecord.owner));
				}
				myContext = myContext.getParent();
			}					
		}
		
		/*
		if (inf.space != null && inf.space.autoShare != null && !inf.space.autoShare.isEmpty()) {
			for (MidataId autoshareAps : inf.space.autoShare) {
				Consent consent = Consent.getByIdAndOwner(autoshareAps, inf.ownerId, Sets.create("type"));
				if (consent != null) { 
				  RecordManager.instance.share(inf.executorId, inf.space._id, autoshareAps, records, true);
				}
			}
		}
		*/
		
		/* Publication of study results */ 
		if (record.owner.equals(inf.getAccessor())) {
			BSONObject query = RecordManager.instance.getMeta(inf, inf.getTargetAps(), "_query");
			if (query != null && query.containsField("target-study")) {				
				inf.getRequestCache().getStudyPublishBuffer().add(inf, record);						
			} else if (query != null && query.containsField("target-study-private")) {				
				inf.getRequestCache().getStudyPublishBuffer().addPrivate(inf, record);
			}
		}
		
		SubscriptionManager.resourceChange(record);
	}
	
	/**
	 * Helper method for OAuth 1.0 apps: Need to compute signature based on consumer secret, which should stay in the backend.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public CompletionStage<Result> oAuth1Call(Request request) throws AppException {
	
		// check whether the request is complete
		JsonNode json = request.body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "url");
		} catch (JsonValidationException e) {
			return CompletableFuture.completedFuture(badRequest(e.getMessage()));
		}

		// decrypt authToken and check whether a user exists who has the app installed
		AccessContext inf = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());
				
		BSONObject oauthMeta = RecordManager.instance.getMeta(inf, inf.getTargetAps(), "_oauth");
    	if (oauthMeta == null) throw new BadRequestException("error.notauthorized.action", "No valid oauth credentials.");
		Map<String, Object> tokens = oauthMeta.toMap();	
						
		String oauthToken, oauthTokenSecret;
		MidataId appId;
		
		oauthToken = (String) tokens.get("oauthToken");
		oauthTokenSecret = (String) tokens.get("oauthTokenSecret");
		appId = new MidataId(tokens.get("appId").toString());
		

		// also get the consumer key and secret
				
		Plugin app;
		try {			
				app = Plugin.getById(appId, Sets.create("consumerKey", "consumerSecret", "apiUrl"));
				if (app == null) return CompletableFuture.completedFuture(badRequest("Invalid authToken"));			
		} catch (InternalServerException e) {
			return CompletableFuture.completedFuture(badRequest(e.getMessage()));
		}

		String method = json.has("method") ? json.get("method").asText() : "GET";
		JsonNode body = json.has("body") ? json.get("body") : null;
		// At the moment only GET is supported
		
		// perform the api call
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		RequestToken token = new RequestToken(oauthToken, oauthTokenSecret);
		AccessLog.log(app.consumerKey);
		AccessLog.log(app.consumerSecret);
		AccessLog.log(oauthToken);
		AccessLog.log(oauthTokenSecret);
		//OAuthCalculator calc = new OAuthCalculator(key, token);

		
		String url = json.get("url").asText();
		if (app.apiUrl == null || !url.startsWith(app.apiUrl)) throw new BadRequestException("error.invalid.url", "API URL does not match URL in app definition.");
		
		return ws.url(json.get("url").asText()).sign(new OAuthCalculator(key, token)).get().thenApplyAsync(
				result -> { 
					return status(result.getStatus(),result.getBodyAsBytes()).as(result.getContentType());					
				});			
	
	}
	
	/**
	 * update a record. 
	 * @return
	 * @throws AppException
	 * @throws JsonValidationException
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result updateRecord(Request request) throws AppException, JsonValidationException {
        Stats.startRequest(request);				
		// check whether the request is complete
		JsonNode json = request.body().asJson();		
		JsonValidation.validate(json, "authToken", "data", "_id");
		
		AccessContext authToken = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());
				
		if (authToken.getSingleReadableRecord() != null) throw new BadRequestException("error.internal", "This view is readonly.");
		UsageStatsRecorder.protokoll(authToken.getUsedPlugin(), UsageAction.PUT);				
		
		String data = JsonValidation.getJsonString(json, "data");
		
		//String name = JsonValidation.getString(json, "name");
		//String description = JsonValidation.getString(json, "description");
		//String format = JsonValidation.getString(json, "format");
		
		//String content = JsonValidation.getString(json, "content");
				
		Record record = new Record();
		
		record._id = JsonValidation.getMidataId(json, "_id");	
		record.version = JsonValidation.getStringOrNull(json, "version");
		 		
		//record.app = authToken.pluginId;
		//record.owner = authToken.ownerId;
		record.modifiedBy = authToken.getActor();
		record.lastUpdated = new Date();		
							
		try {
			record.data = BasicDBObject.parse(data);
		} catch (JSONParseException e) {
			throw new BadRequestException("error.invalid.json", "Record data is invalid JSON.");
		}
				
		updateRecord(authToken, record);
			
		Stats.finishRequest(request, "200", Collections.EMPTY_SET);
		return ok();
	}
	
	/**
	 * Helper function to update a record
	 * @param inf execution context
	 * @param record record to add to the database
	 * @throws AppException
	 * @return the new version string of the record
	 */
	public static String updateRecord(AccessContext inf, Record record) throws AppException  {
		return RecordManager.instance.updateRecord(inf.getAccessor(), inf.getUsedPlugin(), inf, record, Collections.emptyList());				
	}
	
	/**
	 * Helper method for OAuth 2.0 apps: API calls can sometimes only be done from the backend. Uses the
	 * "Authorization: Bearer [accessToken]" header.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public CompletionStage<Result> oAuth2Call(Request request) throws AppException {
		
		// check whether the request is complete
		JsonNode json = request.body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "url");
		} catch (JsonValidationException e) {
			return badRequestPromise(e.getMessage());
		}

		AccessContext inf = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());
		String method = json.has("method") ? json.get("method").asText() : "get";
		JsonNode body = json.has("body") ? json.get("body") : null;
	
		String url = json.get("url").asText();
		Plugin app = Plugin.getById(inf.getUsedPlugin());
		if (app.apiUrl == null || !url.startsWith(app.apiUrl)) throw new BadRequestException("error.invalid.url", "API URL does not match URL in app definition.");
		
		CompletionStage<WSResponse> response = oAuth2Call(inf, url, method, body);	
		
		CompletionStage<Result> promise = response.thenApply(response1 -> {			
				Optional<String> ct = response1.getSingleHeader("Content-Type");
				if (ct.isPresent()) return ok(response1.asJson()).as(ct.get());
				else return ok(response1.asJson());			
		});
		return promise;
	}
	
	/**
	 * Helper method for OAuth 2.0 apps
	 * @param inf execution context
	 * @param url URL to call
	 * @return result of oauth request
	 * @throws AppException
	 */
    public static CompletionStage<WSResponse> oAuth2Call(AccessContext inf, String url, String method, JsonNode body) throws AppException {
				
    	BSONObject oauthMeta = RecordManager.instance.getMeta(inf, inf.getTargetAps(), "_oauth");
    	if (oauthMeta == null) throw new BadRequestException("error.notauthorized.action", "No valid oauth credentials.");
		Map<String, String> tokens = oauthMeta.toMap();				
		String accessToken;
		accessToken = tokens.get("accessToken");
		try {
		  url = url.replace("=<accessToken>", "="+URLEncoder.encode(accessToken,"UTF-8"));
		} catch (UnsupportedEncodingException e) {}
		// perform OAuth API call on behalf of the app
		WSRequest holder = ws.url(url);
		holder.addHeader("Authorization", "Bearer " + accessToken);
		
		if (method == null || method.equalsIgnoreCase("get")) return holder.get();
		if (method.equalsIgnoreCase("post")) {
			if (body == null) throw new BadRequestException("error.missing.body", "Missing request body for POST request.");
			return holder.post(body);
		}
		if (method.equalsIgnoreCase("put")) {
			if (body == null) throw new BadRequestException("error.missing.body", "Missing request body for PUT request.");
			return holder.put(body);
		}
		if (method.equalsIgnoreCase("delete")) return holder.delete();
		
		throw new BadRequestException("error.internal", "Unknown request type");
				
	}
	
	/**
	 * Accepts and stores files up to any size
	 */
    @BodyParser.Of(HugeBodyParser.class)
	public Result uploadFile(Request request) throws AppException {
    	EncryptedFileHandle handle = null;
    	
		Stats.startRequest(request);
		System.out.println("Start handle request");
		try {
							
		// check meta data
		MultipartFormData<EncryptedFileHandle> formData = request.body().asMultipartFormData();
		FilePart<EncryptedFileHandle> fileData = formData.getFile("file");
		if (fileData == null) {
			throw new BadRequestException("error.internal", "No file found.");
		}
		handle = fileData.getRef();			
		String filename = fileData.getFilename();
		String contentType = fileData.getContentType();
				
		Map<String, String[]> metaData = formData.asFormUrlEncoded();
		if (!metaData.containsKey("authToken") || !metaData.containsKey("name")) {
			throw new BadRequestException("error.internal", "At least one request parameter is missing.");
		}

		// decrypt authToken and check whether a user exists who has the app installed
		if (metaData.get("authToken").length != 1) {
			throw new BadRequestException("error.invalid.token", "Invalid authToken.");
		}
	
		AccessContext authToken = ExecutionInfo.checkToken(request, metaData.get("authToken")[0], false, false);
		Stats.setPlugin(authToken.getUsedPlugin());
		
		System.out.println("Passed 1");
		if (authToken.getSingleReadableRecord() != null) throw new BadRequestException("error.internal", "This view is readonly.");
			try {
																			
			// create record
			Record record = new Record();
			record._id = new MidataId();
			record.app = authToken.getUsedPlugin();
			record.owner = authToken.getLegacyOwner();
			record.creator = authToken.getActor();
			record.modifiedBy = record.creator;
			record.created = record._id.getCreationDate();
			record.name = metaData.get("name")[0];
			record.description = metaData.containsKey("description") ? metaData.get("description")[0] : null;
			String[] formats = metaData.get("format");
			record.format = (formats != null && formats.length == 1) ? formats[0] : (contentType != null) ? contentType : "application/octet-stream";
			String[] contents = metaData.get("content");
			String[] codes = metaData.get("code");
			
			ContentInfo.setRecordCodeAndContent(authToken.getUsedPlugin(), record, codes != null ? new HashSet<String>(Arrays.asList(codes)) : null, contents != null ? contents[0] : null);					
						
			if (metaData.containsKey("data")) {
				String data = metaData.get("data")[0];
				
					BasicDBObject att = new BasicDBObject(CMaps							
							.map("title", filename)
							.map("contentType", contentType)
					        .map("size", handle.getLength())
					);
					record.data = BasicDBObject.parse(data);
					
					Object rt = record.data.get("resourceType");
					
					if (rt != null && rt.equals("Media")) {
					    record.data.put("content", att);	
					} else {
						BasicDBObject attachment = new BasicDBObject();
						attachment.put("attachment", att);
						BasicDBList contentArray = new BasicDBList();
						contentArray.add(attachment);
						record.data.put("content", contentArray);
					}
				
			} else {
			
				record.data = new BasicDBObject(CMaps
						.map("resourceType", "Binary")
						.map("type", "file")
						.map("title", filename)
						.map("contentType", contentType)
				        .map("size", 0)
				);
			 		
			}
					
			createRecord(authToken, record, Collections.singletonList(handle));
					
			Stats.finishRequest(request, "200");
			ObjectNode obj = Json.newObject();
			obj.put("_id", record._id.toString());
			return ok(obj).withHeader("Access-Control-Allow-Origin", "*");
		} catch (AppException e) {
			if (handle != null) handle.removeAfterFailure();
			return badRequest(e.getMessage()).withHeader("Access-Control-Allow-Origin", "*");
		} 
			
		} catch (Exception e2) {
			if (handle != null) handle.removeAfterFailure();
			ErrorReporter.report("Plugin API", request, e2);
			return internalServerError(e2.getMessage()).withHeader("Access-Control-Allow-Origin", "*");			
		} finally {
			ServerTools.endRequest();			
		}
		
	}
		
	
	//@BodyParser.Of(BodyParser.Json.class)
	@VisualizationCall
	public Result generateId(Request request) throws JsonValidationException, AppException {
		//JsonNode json = request.body().asJson();		
		//AccessContext inf = ExecutionInfo.checkSpaceToken(request, json.get("authToken").asText());									
		return ok(new MidataId().toString());
	}
	

	private static CompletionStage<Result> badRequestPromise(final String errorMessage) {
		return CompletableFuture.completedFuture(badRequest(errorMessage));

	}
		
	@VisualizationCall
	public Result getImageToken(Request request) throws AppException {
        String param = request.header("Authorization").get();
		
		if (param != null && param.startsWith("Bearer ")) {
			  SpaceToken authToken = SpaceToken.decrypt(request, param.substring("Bearer ".length()));
			  if (authToken == null) OAuth2.invalidToken(); 
	          AccessContext info = ExecutionInfo.checkSpaceToken(authToken);
	          if (info == null) OAuth2.invalidToken(); 
	          Stats.setPlugin(info.getUsedPlugin());
	          String id = request.queryString("_id").orElseThrow();
	          MidataId recId = MidataId.from(id);
	          if (authToken.recordId != null && !authToken.recordId.equals(recId)) { OAuth2.invalidToken(); } 
	          authToken.recordId = recId;
	          try {
				return ok("https://"+InstanceConfig.getInstance().getPlatformServer()+request.uri()+"&access_token="+URLEncoder.encode(authToken.encrypt(), "UTF-8")).as("text/plain");
			  } catch (UnsupportedEncodingException e) {
				 throw new InternalServerException("error.internal", e);
			  } 
	    } 
		throw new BadRequestException("error.invalid.token", "Invalid or expired authToken.", Http.Status.UNAUTHORIZED);	
	}
}
