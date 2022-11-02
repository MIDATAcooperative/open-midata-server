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
import org.bson.BSONObject;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;

import com.mongodb.BasicDBObject;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
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
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.QueryTagTools;
import utils.access.EncryptedFileHandle;
import utils.access.RecordManager;
import utils.access.ReuseFileHandle;
import utils.access.UpdateFileHandleSupport;
import utils.access.VersionedDBRecord;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ConsentAccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
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
	
	public Record init(T theResource) { return newRecord(getRecordFormat()); }
	
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
		
		BSONObject data = (BSONObject) record.data;
		convertToR4(record, data);
		IParser parser = ctx().newJsonParser();		
		T p = parser.parseResource(getResourceType(), JsonOutput.toJsonString(data));
		processResource(record, p);		
		return p;
	}
	
	@History()
	public List<T> getHistory(@IdParam IIdType theId) throws AppException {
	   List<Record> records = RecordManager.instance.list(info().getAccessorRole(), info(), CMaps.map("_id", new MidataId(theId.getIdPart())).map("history", true).map("sort","lastUpdated desc"), RecordManager.COMPLETE_DATA);
	   if (records.isEmpty()) throw new ResourceNotFoundException(theId); 
	   
	   List<T> result = new ArrayList<T>(records.size());
	   IParser parser = ctx().newJsonParser();
	   for (Record record : records) {	
		    if (record.data == null || !record.data.containsField("resourceType")) continue;
		    Object data = record.data;
			convertToR4(record, data);
			T p = parser.parseResource(getResourceType(), JsonOutput.toJsonString(data));
			processResource(record, p);
			result.add(p);
	   }
	   
	   return result;
	}
	
	public Query buildQuery(SearchParameterMap params) throws AppException {
		return null;
	}
	
	public int countResources(SearchParameterMap params) {
		try {
			Query query = buildQuery(params);
			if (query == null) {
				params.setCount(10000);
				List<Record> recs = searchRaw(params);
				return recs.size();
			}			
			AccessContext info = info();
			return query.executeCount(info);
		} catch (InternalServerException e3) {
		   ErrorReporter.report("FHIR (count)", null, e3);
		   throw new InternalErrorException("Internal error during search (count)");
	    } catch (AppException e) {
	       ErrorReporter.report("FHIR (count)", null, e);	      
		   throw new InvalidRequestException(e.getMessage());
	    } catch (NullPointerException e2) {
			ErrorReporter.report("FHIR (count)", null, e2);	 
			throw new InternalErrorException("internal error during FHIR search (count)");
		}
	}
	
	@Override
	public List<Record> searchRaw(SearchParameterMap params) throws AppException {
		Query query = buildQuery(params);
		if (query == null) {			
			throw new InternalServerException("error.internal", "neither searchRaw nor buildQuery implemented");
		}
		AccessContext info = info();
		return query.execute(info);
	}
	
	@Override
	public void createPrepare(Record record, T theResource) throws AppException {		
		prepare(record, theResource);
		prepareTags(record, theResource);
	}
	
	@Override
	public T createExecute(Record record, T theResource) throws AppException {
		List<Attachment> attachments = getAttachments(theResource);		
		insertRecord(record, theResource, attachments, info());
		return theResource;
	}
	
	@Override
	public void updatePrepare(Record record, T theResource) throws AppException {
		record.creator = info().getActor();
		record.modifiedBy = record.creator;
		prepare(record, theResource);	
		prepareTags(record, theResource);
	}
	
	private final static Set<String> alwaysAllowedTags = Sets.create("security:public", "security:generated");
	
	public void prepareTags(Record record, T theResource) throws AppException {
		//boolean hiddenTagFound = false;
		Set<String> tags = new HashSet<String>();
		if (theResource.getMeta().hasSecurity()) {
			List<Coding> codes = theResource.getMeta().getSecurity();
			for (Coding c : codes) {
			    String internal = QueryTagTools.getTagForCoding(c.getSystem(), c.getCode());
			    if (internal != null) tags.add(internal);				
			}
		}
		record.tags = tags.isEmpty() ? null : tags;
		
		AccessContext info = info();
		
		List<String> addTags = info.getAccessRestrictionList(record.content, record.format, "add-tag");
		List<String> allowTags = info.getAccessRestrictionList(record.content, record.format, "allow-tag");
		if (record.tags != null) {
			for (String usedTag : record.tags) {			   
			   if (usedTag.startsWith("security:") && !alwaysAllowedTags.contains(usedTag) && !allowTags.contains(usedTag) && !addTags.contains(usedTag)) throw new PluginException(info.getUsedPlugin(), "error.plugin", "Not allowed security tag used: '"+usedTag+"'");	
			}
		}
		
		for (String tag : addTags) {
			if (record.tags==null || !record.tags.contains(tag)) {
			  record.addTag(tag);
			  Pair<String, String> coding = QueryTagTools.getSystemCodeForTag(tag);
			  theResource.getMeta().addSecurity(new Coding(coding.getLeft(), coding.getRight(), null));
			}
		}
				
	}
	
	@Override
	public void updateExecute(Record record, T theResource) throws AppException {
		List<Attachment> attachments = getAttachments(theResource);	
		updateRecord(record, theResource, attachments);
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
	    			convertToR4(rec, rec.data);
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
		record.format = format;
		record.app = info().getUsedPlugin();
		record.created = record._id.getCreationDate();
		record.code = new HashSet<String>();
		record.owner = info().getLegacyOwner();
		record.version = VersionedDBRecord.INITIAL_VERSION;
		return record;
	}
	
	public List<Attachment> getAttachments(T resource) {
		return Collections.emptyList();
	}

	private String getAttachmentBaseUrl() {
		return "https://"+InstanceConfig.getInstance().getPlatformServer()+"/v1/records/file?_id=";		
	}
	public void processAttachments(Record record, T resource) {
		List<Attachment> atts = getAttachments(resource);	
		int idx=0;
		for (Attachment attachment : atts) { 
		  if (attachment != null && attachment.getUrl() == null && attachment.getData() == null) {	
			  String url = getAttachmentBaseUrl()+record._id+"_"+idx;
			  attachment.setUrl(url);
			  idx++;
		  }
		}
	}
	
	@Override
	public Record fetchCurrent(IIdType theId, T resource)  {
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
			throw new InternalErrorException(e.getMessage());
		} catch (NullPointerException e2) {
			ErrorReporter.report("FHIR (fetch current record)", null, e2);	 
			throw new InternalErrorException("internal error during fetch current record");
		}
	}
	
	public void insertRecord(Record record, T resource) throws AppException {
		insertRecord(record, resource, null, info());
	}
	
	public void insertRecord(Record record, T resource, AccessContext targetContext) throws AppException {
		insertRecord(record, resource, null, targetContext);
	}
	
	public void insertRecord(Record record, T resource, List<Attachment> att) throws AppException {
		insertRecord(record, resource, att, info());
	}
	
	public static void insertRecord(AccessContext targetConsent, Record record, IBaseResource resource) throws AppException {
		insertRecord(targetConsent, record, resource, null);
	}
	
	public static void insertRecord(AccessContext targetConsent, Record record, IBaseResource resource, List<EncryptedFileHandle> handles) throws AppException {
		AccessLog.logBegin("begin insert FHIR record");		    
												
			String encoded = ctx.newJsonParser().encodeResourceToString(resource);			
			record.data = BasicDBObject.parse(encoded);	
			record.addTag("fhir:r4");
			
			try {
			  PluginsAPI.createRecord(targetConsent, record, handles);			
			} finally {
		      AccessLog.logEnd("end insert FHIR record");
			}
	}
	
	public MidataId insertMessageRecord(Record record, T resource) throws AppException {
		AccessContext inf = info();
		MidataId shareFrom = inf.getAccessor();
		MidataId owner = record.owner;
		
		List<Attachment> attachments = getAttachments(resource);
				
		if (!owner.equals(inf.getAccessor())) {
			Consent consent = Circles.getOrCreateMessagingConsent(inf, inf.getAccessor(), owner, owner, false);
			insertRecord(record, resource, attachments, new ConsentAccessContext(consent, info()));
			shareFrom = consent._id;
		} else {
			insertRecord(record, resource, attachments, info());
		}
		return shareFrom;
	}
	
	public List<UpdateFileHandleSupport> attachmentsToHandles(List<Attachment> attachments) throws AppException  {
		if (attachments==null || attachments.isEmpty()) return Collections.emptyList();
		List<UpdateFileHandleSupport> handles = new ArrayList<UpdateFileHandleSupport>();
		for (Attachment attachment : attachments) {
		    if (attachment==null) continue;		
			InputStream data = null;
			UpdateFileHandleSupport handle1 = null;
			
			String contentType = attachment.getContentType();
			String fileName = attachment.getTitle();
			
			byte[] dataArray = attachment.getData();
			if (dataArray != null)  data = new ByteArrayInputStream(dataArray);
			else if (attachment.getUrl() != null) {
				String url = attachment.getUrl();
				
				if (url.startsWith("midata-file://")) {
					EncryptedFileHandle handle = EncryptedFileHandle.fromString(info().getAccessor(), url);
					if (handle == null) throw new UnprocessableEntityException("Malformed midata-file URL");
					
					UnlinkedBinary file = UnlinkedBinary.getById(handle.getId());
					if (file==null || file.isExpired()) throw new UnprocessableEntityException("Midata-file URL has already expired.");
					
					if (!file.owner.equals(info().getAccessor())) throw new UnprocessableEntityException("Midata-file URL is not owned by you.");				
					if (fileName!=null) handle.rename(fileName);
					file.delete();
					handle1 = handle;
				} else if (url.startsWith(getAttachmentBaseUrl())) {
					int p = url.lastIndexOf("_");
					if (p<0) throw new UnprocessableEntityException("Illegal file url");
					String idxPart = url.substring(p+1);
					try {
						int idx = Integer.parseInt(idxPart);
						handle1 = new ReuseFileHandle(idx);
					} catch (NumberFormatException e) {
						throw new UnprocessableEntityException("Illegal file url");
					}
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
							
			if (data != null) {
			   handle1 = RecordManager.instance.addFile(data, fileName, contentType);
			}
			if (handle1 == null) throw new UnprocessableEntityException("Missing attachment data");
			handles.add(handle1);
		}
		return handles;
	}
	
	public void insertRecord(Record record, T resource, List<Attachment> attachments, AccessContext targetContext) throws AppException {
		
		List<EncryptedFileHandle> handles1 = null;
		if (attachments != null && !attachments.isEmpty()) {			
			List<UpdateFileHandleSupport> handles = attachmentsToHandles(attachments);	
			handles1 = handles.isEmpty() ? Collections.emptyList() : new ArrayList<EncryptedFileHandle>();
			for (UpdateFileHandleSupport uf : handles) {
				AccessLog.log("handle attachment attachment");
				if (uf instanceof EncryptedFileHandle) handles1.add((EncryptedFileHandle) uf); else throw new UnprocessableEntityException("Illegal attachment URL");
			}
		}
		
		insertRecord(targetContext, record, (IBaseResource) resource, handles1);		
	}
	
	public void updateRecord(Record record, IBaseResource resource, List<Attachment> attachments) throws AppException {
		if (resource.getMeta() != null && resource.getMeta().getVersionId() != null && !record.version.equals(resource.getMeta().getVersionId())) throw new ResourceVersionConflictException("Wrong resource version supplied!") ;
        List<UpdateFileHandleSupport> handles = attachmentsToHandles(attachments);
		
        record.addTag("fhir:r4");
		String encoded = ctx.newJsonParser().encodeResourceToString(resource);
		record.data = BasicDBObject.parse(encoded);	
		record.version = resource.getMeta().getVersionId();
		record.version = RecordManager.instance.updateRecord(info().getAccessor(), info().getUsedPlugin(), info(), record, handles);	
	}
	
	/**
	 * Sets id field and meta section
	 */
	public void processResource(Record record, T resource) throws AppException {
		resource.setId(new IdType(resource.fhirType(), record._id.toString(), record.version));
		resource.getMeta().setVersionId(record.version);
		if (record.lastUpdated == null) resource.getMeta().setLastUpdated(record.created);
		else resource.getMeta().setLastUpdated(record.lastUpdated);
		
		Extension meta = new Extension("http://midata.coop/extensions/metadata");
		
		if (record.app != null) {
		  Plugin creatorApp = Plugin.getById(record.app);		
		  if (creatorApp != null) meta.addExtension("app", new Coding("http://midata.coop/codesystems/app", creatorApp.filename, creatorApp.name));
		}
		if (record.creator != null) meta.addExtension("creator", FHIRTools.getReferenceToUser(record.creator, record.creator.equals(record.owner) ? record.ownerName : null ));
		if (record.modifiedBy != null && !record.version.equals("0")) meta.addExtension("modifiedBy", FHIRTools.getReferenceToUser(record.modifiedBy, record.modifiedBy.equals(record.owner) ? record.ownerName : null ));
				
		resource.getMeta().addExtension(meta);
		processAttachments(record, resource);
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
	
	protected void convertToR4(Record fromDB, Object in) {
		if (fromDB.tags == null || !fromDB.tags.contains("fhir:r4")) convertToR4(in);		
	}
	
	public String serialize(T resource) {
		serializeAttachments(resource);
    	return ctx.newJsonParser().encodeResourceToString(resource);
    }
	
	public void serializeAttachments(T resource) {
		List<Attachment> attachments = getAttachments(resource);
		for (Attachment att : attachments) {
          if (att != null) {		
			att.setUrl(null);
			att.setDataElement(new Base64BinaryType(FHIRTools.BASE64_PLACEHOLDER_FOR_STREAMING));
		  }
		}
	}
}
