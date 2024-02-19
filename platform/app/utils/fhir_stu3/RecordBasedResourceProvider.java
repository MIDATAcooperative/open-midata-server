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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.dstu3.model.DateTimeType;

import com.mongodb.BasicDBObject;

import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import controllers.Circles;
import controllers.PluginsAPI;
import models.Consent;
import models.ContentInfo;
import models.MidataId;
import models.Plugin;
import models.Record;
import models.TypedMidataId;
import models.enums.AuditEventType;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.QueryTagTools;
import utils.access.EncryptedFileHandle;
import utils.access.RecordManager;
import utils.access.VersionedDBRecord;
import utils.audit.AuditHeaderTool;
import utils.audit.AuditManager;
import utils.collections.CMaps;
import utils.context.AccessContext;
import utils.context.ConsentAccessContext;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.exceptions.PluginException;
import utils.json.JsonOutput;
import utils.largerequests.UnlinkedBinary;

public abstract class RecordBasedResourceProvider<T extends DomainResource> extends ReadWriteResourceProvider<T, Record> {

	/**
	 * Returns the format field of the records or null
	 * @return
	 */
	public abstract String getRecordFormat();
	
	public Record init() { return newRecord(getRecordFormat()); }
	
	/**
	 * Default implementation to retrieve a FHIR resource by id.
	 * @param theId ID of resource to be retrieved
	 * @return Resource read from database
	 * @throws AppException
	 */
	@Read(version=true)
	public T getResourceById(@IdParam IIdType theId) throws AppException {
		Record record;
		if (theId.hasVersionIdPart()) {
			List<Record> result = RecordManager.instance.list(info().getAccessorRole(), info(), CMaps.map("_id", new MidataId(theId.getIdPart())).map("version", theId.getVersionIdPart()), RecordManager.COMPLETE_DATA);
			record = result.isEmpty() ? null : result.get(0);
		} else {
		    record = RecordManager.instance.fetch(info().getAccessorRole(), info(), new MidataId(theId.getIdPart()), getRecordFormat());
		}
		if (record == null || record.data == null || !record.data.containsField("resourceType")) throw new ResourceNotFoundException(theId);					
		IParser parser = ctx().newJsonParser();
		T p = parser.parseResource(getResourceType(), JsonOutput.toJsonString(record.data));
		processResource(record, p);	
		//AuditHeaderTool.createAuditEntryFromHeaders(info(), AuditEventType.REST_READ, record.context.getOwner());
		return p;
	}
	
	@History()
	public List<T> getHistory(@IdParam IIdType theId) throws AppException {
	   List<Record> records = RecordManager.instance.list(info().getAccessorRole(), info(), CMaps.map("_id", new MidataId(theId.getIdPart())).map("history", true).map("sort","lastUpdated desc").map("limit",2000), RecordManager.COMPLETE_DATA);
	   if (records.isEmpty()) throw new ResourceNotFoundException(theId); 
	   
	   List<T> result = new ArrayList<T>(records.size());
	   IParser parser = ctx().newJsonParser();
	   //boolean audited = false;
	   for (Record record : records) {	
		    if (record.data == null || !record.data.containsField("resourceType")) continue;
			T p = parser.parseResource(getResourceType(), JsonOutput.toJsonString(record.data));
			processResource(record, p);
			result.add(p);
			
			/*if (!audited) {
				AuditHeaderTool.createAuditEntryFromHeaders(info(), AuditEventType.REST_HISTORY, record.context.getOwner());
				audited = true;
			}*/
	   }
	   
	   return result;
	}
	
	@Override
	public void createPrepare(Record record, T theResource) throws AppException {		
		prepare(record, theResource);
		prepareTags(record, theResource);
	}
	
	@Override
	public void createExecute(Record record, T theResource) throws AppException {
		boolean audit = AuditHeaderTool.createAuditEntryFromHeaders(info(), AuditEventType.REST_CREATE, record.owner);
		insertRecord(record, theResource);		
		if (audit) AuditManager.instance.success();
	}
	
	@Override
	public void updatePrepare(Record record, T theResource) throws AppException {
		record.creator = info().getActor();
		record.modifiedBy = record.creator;
		prepare(record, theResource);	
		prepareTags(record, theResource);
	}
	
	public void prepareTags(Record record, T theResource) throws AppException {
		//boolean hiddenTagFound = false;
		Set<String> tags = new HashSet<String>();
		if (theResource.getMeta().hasSecurity()) {
			List<Coding> codes = theResource.getMeta().getSecurity();
			for (Coding c : codes) {
				if (c.getSystem().equals("http://terminology.hl7.org/CodeSystem/v3-ActCode") && c.getCode().equals("PHY")) {
					//hiddenTagFound = true;
					tags.add("security:hidden");
				} else if (c.getSystem().equals("http://terminology.hl7.org/CodeSystem/v3-Confidentiality") && c.getCode().equals("U")) {
					tags.add("security:public");				
				} else if (c.getSystem().equals("http://midata.coop/codesystems/security") && c.getCode().equals("public")) {
					tags.add("security:public");
				} else if (c.getSystem().equals("http://midata.coop/codesystems/security") && c.getCode().equals("generated")) {
					tags.add("security:generated");
				}
			}
		}
		record.tags = tags.isEmpty() ? null : tags;
		/*if (tagFound) {
			if (record.tags == null) record.tags = new HashSet<String>();
			record.tags.add("security:hidden");
		} else {
			if (record.tags != null) record.tags.remove("security:hidden");
		}*/
	}
	
	@Override
	public void updateExecute(Record record, T theResource) throws AppException {
		boolean audit = AuditHeaderTool.createAuditEntryFromHeaders(info(), AuditEventType.REST_UPDATE, record.owner);
		updateRecord(record, theResource);		
		if (audit) AuditManager.instance.success();
	}
	
	@Override
	protected String getFromId(Record resource) {
		return resource._id.toString()+"."+resource.owner.toString();
	}
	
	public List<T> parse(List<Record> result, Class<T> resultClass) throws AppException {
		ArrayList<T> parsed = new ArrayList<T>();	
	    IParser parser = ctx().newJsonParser();
	    for (Record rec : result) {
	    	if (rec.data != null) {
	    		try {
	    			T p = parser.parseResource(resultClass, JsonOutput.toJsonString(rec.data));
	    			processResource(rec, p);											
	    			parsed.add(p);
	    		} catch (DataFormatException e) {
	    		}
	    	}
	    }
	    return parsed;
	}
	
	public static Record newRecord(String format) {
		Record record = new Record();
		record._id = new MidataId();
		record.creator = info().getActor();
		record.modifiedBy = record.creator;
		if (info().isUserGroupContext()) {
            record.creatorOrg = info().getAccessor();
            record.modifiedByOrg = info().getAccessor();
        }
		record.format = format;
		record.app = info().getUsedPlugin();
		record.created = record._id.getCreationDate();
		record.code = new HashSet<String>();
		record.owner = info().getLegacyOwner();
		record.version = VersionedDBRecord.INITIAL_VERSION;
		return record;
	}
	
	@Override
	public Record fetchCurrent(IIdType theId)  {
		try {
			if (theId == null) throw new UnprocessableEntityException("id missing");
			if (theId.getIdPart() == null || theId.getIdPart().length() == 0) throw new UnprocessableEntityException("id local part missing");
			if (!isLocalId(theId)) throw new UnprocessableEntityException("id is not local resource");
			
			Record record = RecordManager.instance.fetch(info().getAccessorRole(), info(), new MidataId(theId.getIdPart()), getRecordFormat());
			
			if (record == null) throw new ResourceNotFoundException("Resource "+theId.getIdPart()+" not found."); 
			if (!record.format.equals("fhir/"+theId.getResourceType())) throw new ResourceNotFoundException("Resource "+theId.getIdPart()+" has wrong resource type."); 
			
			String versionId = theId.getVersionIdPart();
			if (versionId != null) {	  
			   if (!versionId.equals(record.version)) {
			     throw new ResourceVersionConflictException("Unexpected version");
			   }
			}		
			return record;
		} catch (AppException e) {
			ErrorReporter.report("FHIR (fetch current record)", null, e);	 
			throw new InternalErrorException(e);
		} catch (NullPointerException e2) {
			ErrorReporter.report("FHIR (fetch current record)", null, e2);	 
			throw new InternalErrorException(e2);
		}
	}
	
	public void insertRecord(Record record, IBaseResource resource) throws AppException {
		insertRecord(record, resource, info());
	}
	
	public static void insertRecord(Record record, IBaseResource resource, AccessContext targetConsent) throws AppException {
 		insertRecord(targetConsent, record, resource);
	}
	
	public static void insertRecord(AccessContext targetConsent, Record record, IBaseResource resource) throws AppException {
		AccessLog.logBegin("begin insert FHIR record");		    
			String encoded = ctx.newJsonParser().encodeResourceToString(resource);			
			record.data = BasicDBObject.parse(encoded);	
			try {
			  PluginsAPI.createRecord(targetConsent, record);			
			} finally {
		      AccessLog.logEnd("end insert FHIR record");
			}
	}
	
	public MidataId insertMessageRecord(Record record, IBaseResource resource) throws AppException {
		AccessContext inf = info();
		MidataId shareFrom = inf.getAccessor();
		MidataId owner = record.owner;
		if (!owner.equals(inf.getAccessor())) {
			Consent consent = Circles.getOrCreateMessagingConsent(inf, inf.getAccessor(), owner, owner, false);
			insertRecord(record, resource, info().forConsent(consent));
			shareFrom = consent._id;
		} else {
			insertRecord(record, resource);
		}
		return shareFrom;
	}
	
	public void insertRecord(Record record, IBaseResource resource, Attachment attachment) throws AppException {
		if (attachment == null || attachment.isEmpty()) {
			insertRecord(record, resource);
			return;
		} 
		AccessLog.logBegin("begin insert FHIR record with attachment");
							
			InputStream data = null;
			EncryptedFileHandle handle = null;
			
			String contentType = attachment.getContentType();
			String fileName = attachment.getTitle();
			
			byte[] dataArray = attachment.getData();
			if (dataArray != null)  data = new ByteArrayInputStream(dataArray);
			else if (attachment.getUrl() != null) {
				String url = attachment.getUrl();
				
				if (url.startsWith("midata-file://")) {
					handle = EncryptedFileHandle.fromString(info().getAccessor(), url);
					if (handle == null) throw new UnprocessableEntityException("Malformed midata-file URL");
					
					UnlinkedBinary file = UnlinkedBinary.getById(handle.getId());
					if (file==null || file.isExpired()) throw new UnprocessableEntityException("Midata-file URL has already expired.");
					
					if (!file.owner.equals(info().getAccessor())) throw new UnprocessableEntityException("Midata-file URL is not owned by you.");				
					if (fileName!=null) handle.rename(fileName);
					file.delete();
				} else {				
					try {					  
					  if (url.startsWith("http://") || url.startsWith("https://")) {
					    data = new URL(url).openStream();
					  } else throw new UnprocessableEntityException("Malformed URL");
					} catch (MalformedURLException e) {
						throw new UnprocessableEntityException("Malformed URL");
					} catch (IOException e2) {
						throw new UnprocessableEntityException("IO Exception");
					}
				}
			} 
						
			attachment.setData(null);
			attachment.setUrl(null);
			
			String encoded = ctx.newJsonParser().encodeResourceToString(resource);
			record.data = BasicDBObject.parse(encoded);
			
			if (data != null) {
			   handle = RecordManager.instance.addFile(data, fileName, contentType);
			}
			if (handle == null) throw new UnprocessableEntityException("Missing attachment data");
			PluginsAPI.createRecord(info(), record, Collections.singletonList(handle));			
		
		AccessLog.logEnd("end insert FHIR record with attachment");
	}
	
	public void updateRecord(Record record, IBaseResource resource) throws AppException {
		if (resource.getMeta() != null && resource.getMeta().getVersionId() != null && !record.version.equals(resource.getMeta().getVersionId())) throw new ResourceVersionConflictException("Wrong resource version supplied!") ;
		String encoded = ctx.newJsonParser().encodeResourceToString(resource);
		record.data = BasicDBObject.parse(encoded);	
		record.version = resource.getMeta().getVersionId();
		record.version = RecordManager.instance.updateRecord(info().getAccessor(), info().getUsedPlugin(), info(), record, Collections.emptyList());
	
	}
	
	/**
	 * Sets id field and meta section
	 */
	public void processResource(Record record, T resource) throws AppException {
		resource.setId(new IdType(resource.fhirType(), record._id.toString(), record.version));
		resource.getMeta().setVersionId(record.version);
		
        Extension meta = new Extension("http://midata.coop/extensions/metadata");
		
		if (record.lastUpdated == null || record.lastUpdated.equals(record.created)) {
			resource.getMeta().setLastUpdated(record.created);
		} else {
			resource.getMeta().setLastUpdated(record.lastUpdated);
			meta.addExtension("createdAt", new DateTimeType(record.created));
		}
		
		if (record.app != null) {
		  Plugin creatorApp = Plugin.getById(record.app);		
		  if (creatorApp != null) meta.addExtension("app", new Coding("http://midata.coop/codesystems/app", creatorApp.filename, creatorApp.name));
		}
		if (record.creator != null && !record.creator.equals(record.app)) meta.addExtension("creator", FHIRTools.getReferenceToUser(record.creator, record.creator.equals(record.owner) ? record.ownerName : null ));
		if (record.modifiedBy != null && !record.version.equals("0")) meta.addExtension("modifiedBy", FHIRTools.getReferenceToUser(record.modifiedBy, record.modifiedBy.equals(record.owner) ? record.ownerName : null ));	
		
		resource.getMeta().addExtension(meta);
	}
	
	/**
	 * Set record.code field based on a CodeableConcept field in the FHIR resource
	 * @param record MIDATA record
	 * @param cc CodeableConcept to set
	 * @return display of codeable concept
	 */
	protected String setRecordCodeByCodeableConcept(Record record, CodeableConcept cc, String defaultContent) throws InternalServerException {
	  return setRecordCodeByCodings(record, cc != null ? cc.getCoding() : null, defaultContent);
	}
	
	protected String setRecordCodeByCoding(Record record, Coding coding, String defaultContent) throws InternalServerException {
	  return setRecordCodeByCodings(record, coding != null ? Collections.singletonList(coding) : null, defaultContent);
	}
	
	protected String setRecordCodeByCodings(Record record, List<Coding> codings, String defaultContent) throws InternalServerException {
		  record.code = new HashSet<String>(); 
		  String display = null;
		  try {
			  if (codings != null && !codings.isEmpty()) {
				  for (Coding coding : codings) {
					if (coding.getDisplay() != null && display == null) display = coding.getDisplay();
					if (coding.getCode() != null && coding.getSystem() != null) {
						record.code.add(coding.getSystem() + " " + coding.getCode());
					}
				  }	  
              }
			  
			  if (!record.code.isEmpty()) {
			  
				ContentInfo.setRecordCodeAndContent(info().getUsedPlugin(), record, record.code, null);
			  
			  } else {
				  ContentInfo.setRecordCodeAndContent(info().getUsedPlugin(), record, null, defaultContent);
			  }
		  } catch (PluginException e) {
			    ErrorReporter.reportPluginProblem("FHIR (set record code)", null, e);	
				throw new UnprocessableEntityException("Error determining MIDATA record code.");
		  }
		  return display;
		}
	
	/**
	 * Auto-share a record with all person/groups provided
	 * @param record the record to be shared
	 * @param personRefs collection of FHIR references
	 * @throws AppException
	 */
	protected void shareWithPersons(Record record, Collection<IIdType> personRefs, MidataId shareFrom) throws AppException {
	       AccessContext inf = info();
			
			MidataId owner = record.owner;
			
			for (IIdType ref : personRefs) {
				if (FHIRTools.isUserFromMidata(ref)) { 
					   TypedMidataId target = FHIRTools.getMidataIdFromReference(ref);
					   if (!target.getMidataId().equals(owner)) {
					     Consent consent = Circles.getOrCreateMessagingConsent(inf, owner, target.getMidataId(), owner, target.getType().equals("Group"));
					     RecordManager.instance.share(inf, shareFrom, consent._id, consent.owner, Collections.singleton(record._id), false);
					   }
				}
			}
	}
	
	@Override
    public String getVersion(Record record) {
		return record.version;
	}
	
	@Override
	public Date getLastUpdated(Record record) {
		return record.lastUpdated != null ? record.lastUpdated : record.created;
	}
	
	public void addSecurityTag(Record record, DomainResource theResource, String tag) {
		  record.addTag(tag);
		  Pair<String, String> coding = QueryTagTools.getSystemCodeForTag(tag);
		  theResource.getMeta().addSecurity(new Coding(coding.getLeft(), coding.getRight(), null));
	}
}
