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

package controllers.research;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64InputStream;
import org.hl7.fhir.r4.model.DomainResource;

import com.fasterxml.jackson.databind.JsonNode;

import actions.APICall;
import akka.NotUsed;
import akka.stream.ActorAttributes;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import controllers.APIController;
import models.MidataId;
import models.Model;
import models.Plugin;
import models.Record;
import models.ResearchUser;
import models.Study;
import models.User;
import models.UserGroupMember;
import models.enums.AuditEventType;
import models.enums.UserRole;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Request;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.access.DBIterator;
import utils.access.RecordManager;
import utils.audit.AuditManager;
import utils.auth.DeveloperSecured;
import utils.auth.KeyManager;
import utils.auth.PortalSessionToken;
import utils.auth.RecordToken;
import utils.auth.ResearchSecured;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.csv.CSVConverter;
import utils.csv.CSVDefinition;
import utils.db.LostUpdateException;
import utils.db.FileStorage.FileData;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.fhir.FHIRServlet;
import utils.fhir.FHIRTools;
import utils.fhir.MidataPractitionerResourceProvider;
import utils.fhir.ResourceProvider;
import utils.json.JsonOutput;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

public class CSVDownload extends APIController {

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	@Security.Authenticated(ResearchSecured.class)	
	public Result updateCSVDef(Request request, String studyIdStr) throws JsonValidationException, AppException, LostUpdateException {
		
		// validate json
		JsonNode json = request.body().asJson();
		
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
				
		MidataId studyid = MidataId.from(studyIdStr);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create(Study.ALL, "name", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "requiredInformation", "anonymous", "code",
				"startDate", "endDate", "dataCreatedBefore", "type", "autoJoinGroup", "autoJoinTestGroup", "autoJoinExecutor"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		UserGroupMember self = UserGroupMember.getByGroupAndActiveMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");

		CSVDefinition csv = new CSVDefinition();
		csv._id = study._id;
		csv.jsonDefinition = JsonValidation.getJsonString(json, "jsonDefinition");
		
		List<String> files = new ArrayList<String>();
		JsonNode mapping = Json.parse(csv.jsonDefinition);
		for (JsonNode entry : mapping) {
			if (entry.hasNonNull("file")) files.add(entry.path("file").asText());
		}
		csv.names = files;
		
		CSVConverter convert = new CSVConverter(mapping, null);
		convert.prepareMapping();
		
		CSVDefinition.add(csv);
		
		return ok();
	}
		
	@APICall
	@Security.Authenticated(ResearchSecured.class)	
	public Result getCSVDef(Request request, String studyIdStr) throws AppException {
		
		// validate json
		JsonNode json = request.body().asJson();
		
		MidataId userId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		AccessContext context = portalContext(request);
				
		MidataId studyid = MidataId.from(studyIdStr);

		User user = ResearchUser.getById(userId, Sets.create("firstname", "lastname"));
		Study study = Study.getById(studyid, Sets.create(Study.ALL, "name", "owner", "executionStatus", "participantSearchStatus", "validationStatus", "requiredInformation", "anonymous", "code",
				"startDate", "endDate", "dataCreatedBefore", "type", "autoJoinGroup", "autoJoinTestGroup", "autoJoinExecutor"));

		if (study == null)
			throw new BadRequestException("error.notauthorized.study", "Study does not belong to organization.");

		UserGroupMember self = UserGroupMember.getByGroupAndActiveMember(studyid, userId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");

		CSVDefinition csv = CSVDefinition.getById(study._id, Sets.create("names", "jsonDefinition"));
		if (csv==null) return ok();
		return ok(JsonOutput.toJson(csv, "CSVDefinition", Sets.create("_id", "names", "jsonDefinition")));
	}
	
	@APICall
	@Security.Authenticated(ResearchSecured.class)
	public Result downloadCSV(Request request, String id, final String studyGroup, final String mode, final String file) throws AppException, IOException {

		final MidataId studyid = new MidataId(id);		
		final MidataId executorId = new MidataId(request.attrs().get(play.mvc.Security.USERNAME));
		final UserRole role = getRole();

		Map<String, String[]> params = request.queryString();

		final Date startDate = params.containsKey("startDate") ? new Date(Long.parseLong(params.get("startDate")[0])) : null;
		final Date endDate = params.containsKey("endDate") ? new Date(Long.parseLong(params.get("endDate")[0])) : null;

		UserGroupMember self = UserGroupMember.getByGroupAndActiveMember(studyid, executorId);
		if (self == null)
			throw new AuthException("error.notauthorized.action", "User not member of study group");
		if (!self.getConfirmedRole().mayExportData())
			throw new BadRequestException("error.notauthorized.action", "User is not allowed to export data.");

		final String handle = PortalSessionToken.session().handle;
		AccessContext context = ContextManager.instance.createSession(PortalSessionToken.session()).forAccount();

		return downloadCSV(context, handle, studyid, role, startDate, endDate, studyGroup, mode, file);
	}
	
	public static Result downloadCSV(AccessContext initialInf, final String handle, final MidataId studyid, final UserRole role, final Date startDate, final Date endDate, final String studyGroup,
			final String mode, final String file) throws AppException, IOException {
		final MidataId executorId = initialInf.getAccessor();
		final Study study = Study.getById(studyid, Sets.create("name", "type", "executionStatus", "participantSearchStatus", "validationStatus", "owner", "groups", "createdBy", "code"));

		if (study == null)
			throw new BadRequestException("error.unknown.study", "Unknown Study");
		
		final CSVDefinition csvdef = CSVDefinition.getById(study._id, CSVDefinition.ALL);
		if (csvdef == null)
			throw new BadRequestException("error.unknown.study", "Unknown Study");
		
        JsonNode def = Json.parse(csvdef.jsonDefinition);
        final CSVConverter converter = new CSVConverter(def, file);
        converter.prepareMapping();
        
		AuditManager.instance.addAuditEvent(AuditEventType.DATA_EXPORT, executorId, null, study);
		
		//StringBuffer out = new StringBuffer();

		AccessLog.log("exeId=" + executorId);
		KeyManager.instance.continueSession(handle, executorId);
		ResourceProvider.setAccessContext(initialInf);		
		

		if (initialInf.mayAccess("Practitioner", "fhir/Practitioner")) {
			Set<UserGroupMember> ugms = UserGroupMember.getAllUserByGroup(study._id);
			Map<MidataId, UserGroupMember> idmap = new HashMap<MidataId, UserGroupMember>();
			for (UserGroupMember member : ugms)
				idmap.put(member.member, member);
			Set<User> users = User.getAllUser(CMaps.map("_id", idmap.keySet()).map("role", UserRole.RESEARCH), User.ALL_USER);

			ResourceProvider<DomainResource, Model> pprov = FHIRServlet.myProviders.get("Practitioner");
			for (User user : users) {
				String location = FHIRServlet.getBaseUrl() + "/" + pprov.getResourceType().getSimpleName() + "/" + user._id.toString();
				String ser = pprov.serialize(MidataPractitionerResourceProvider.practitionerFromMidataUser(user));
				if (ser!=null && ser.length()>0) converter.process(Json.parse(ser));								
			}
		}

	
		final akka.japi.function.Creator<Iterator<ByteString>> creator = new akka.japi.function.Creator<Iterator<ByteString>>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Iterator<ByteString> create() throws Exception {
				try {
					KeyManager.instance.continueSession(handle, executorId);
					AccessContext context = ContextManager.instance.createSessionForDownloadStream(executorId, role);
					ResourceProvider.setAccessContext(context);
					DBIterator<Record> allRecords = RecordManager.instance.listIterator(executorId, role, initialInf,
							CMaps.map("export", mode).map("study", study._id).map("study-group", studyGroup).mapNotEmpty("shared-after", startDate).mapNotEmpty("updated-before", endDate),
							RecordManager.COMPLETE_DATA);
					
					return new RecIterator(allRecords, converter);
				} finally {
					ServerTools.endRequest();
				}
			}

			class RecIterator implements Iterator<ByteString> {

				private DBIterator<Record> it;				
				private CSVConverter converter;
				private boolean itNext;

				RecIterator(DBIterator it, CSVConverter converter) {
					this.it = it;
					this.converter = converter;
				}

				@Override
				public boolean hasNext() {
					try {				
						itNext = it.hasNext();
						boolean v = itNext || converter.hasData();					
						return v;
					} catch (Exception e) {
						ErrorReporter.report("study export", null, e);
						throw new RuntimeException(e);
					}
				}

				@Override
				public ByteString next() {
					try {
												
						KeyManager.instance.continueSession(handle, executorId);

						AccessContext context = ContextManager.instance.createSessionForDownloadStream(executorId, role);
						ResourceProvider.setAccessContext(context);
                        String out = null;
                        boolean cancel = false;
                        do {
						if (itNext) {
							long start = System.currentTimeMillis();
							Record rec = it.next();
							if (rec._id == null)
								System.out.println("no id");
							if (rec.owner == null)
								System.out.println("no owner");
	
							String format = rec.format.startsWith("fhir/") ? rec.format.substring("fhir/".length()) : "Basic";
	
							ResourceProvider<DomainResource, Model> prov = FHIRServlet.myProviders.get(format);
	
							DomainResource r = prov.parse(rec, prov.getResourceType());
								
							if (r != null) {
								String ser = prov.serialize(r);
								/*
								int attpos;
								int idx = 0;
								do {
								  attpos = ser.indexOf(FHIRTools.BASE64_PLACEHOLDER_FOR_STREAMING);
								  if (attpos > 0) {
									out.append(ser.substring(0, attpos));
									FileData fileData = RecordManager.instance.fetchFile(context, new RecordToken(rec._id.toString(), rec.context.getTargetAps().toString()), idx);
	
									int BUFFER_SIZE = 3 * 1024;
	
									try (InputStreamReader in = new InputStreamReader(new Base64InputStream(fileData.inputStream, true, -1, null));) {
	
										char[] chunk = new char[BUFFER_SIZE];
										int len = 0;
										while ((len = in.read(chunk)) != -1) {
											out.append(String.valueOf(chunk, 0, len));
										}
	
									}
	                                ser = ser.substring(attpos + FHIRTools.BASE64_PLACEHOLDER_FOR_STREAMING.length());
	                                idx++;
								} else
									out.append(ser);
								} while (attpos>=0);
								*/
								//System.out.println("SER="+ser);
								if (ser != null && ser.length()>0) converter.process(Json.parse(ser));
								out = converter.fetch();
								//System.out.println("FETCH="+out);
							} 
						} else {
							converter.endMapping();
							out = converter.fetch();
							//System.out.println("FETCH-FINAL="+out);
							cancel = true;
						}
						if (out==null || out.length()==0) {
							itNext = it.hasNext();
							if (cancel) out=" ";
						}
                        } while (!cancel && (out == null || out.length()==0));
					    
						// System.out.println("done record");
						// AccessLog.log("done record");
						return ByteString.fromString(out.toString());
					} catch (Throwable e) {
						System.out.println("EXCEPTION");
						System.out.println(AccessLog.getReport());
						e.printStackTrace();
						if (e instanceof Exception)
							ErrorReporter.report("study export", null, (Exception) e);
						throw new RuntimeException(e);
					} finally {
						// System.out.println("FINALLY:"+AccessLog.getReport());
						ServerTools.endRequest();
					}
				}

			}

		};

		AuditManager.instance.success();
		
		Source<ByteString, NotUsed> main = Source.fromIterator(creator).withAttributes(ActorAttributes.dispatcher("my-thread-pool-dispatcher"));	

		// Serves this stream with 200 OK
		Result result = ok().chunked(main).as("text/csv; charset=utf-8");
		String fileName = file.endsWith(".csv") ? file : file + ".csv";
		return setAttachmentContentDisposition(result, fileName);				
	}
}
