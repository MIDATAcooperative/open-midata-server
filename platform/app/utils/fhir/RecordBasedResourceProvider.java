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

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

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
import utils.AccessLog;
import utils.ErrorReporter;
import utils.access.AccessContext;
import utils.access.ConsentAccessContext;
import utils.access.EncryptedFileHandle;
import utils.access.RecordManager;
import utils.access.VersionedDBRecord;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.exceptions.AppException;

public abstract class RecordBasedResourceProvider<T extends DomainResource> extends ReadWriteResourceProvider<T, Record> {

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
			List<Record> result = RecordManager.instance.list(info().executorId, info().role, info().context, CMaps.map("_id", new MidataId(theId.getIdPart())).map("version", theId.getVersionIdPart()), RecordManager.COMPLETE_DATA);
			record = result.isEmpty() ? null : result.get(0);
		} else {
		    record = RecordManager.instance.fetch(info().executorId, info().role, info().targetAPS, new MidataId(theId.getIdPart()));
		}
		if (record == null || record.data == null || !record.data.containsField("resourceType")) throw new ResourceNotFoundException(theId);					
		IParser parser = ctx().newJsonParser();
		T p = parser.parseResource(getResourceType(), record.data.toString());
		processResource(record, p);		
		return p;
	}
	
	@History()
	public List<T> getHistory(@IdParam IIdType theId) throws AppException {
	   List<Record> records = RecordManager.instance.list(info().executorId, info().role, info().context, CMaps.map("_id", new MidataId(theId.getIdPart())).map("history", true).map("sort","lastUpdated desc"), RecordManager.COMPLETE_DATA);
	   if (records.isEmpty()) throw new ResourceNotFoundException(theId); 
	   
	   List<T> result = new ArrayList<T>(records.size());
	   IParser parser = ctx().newJsonParser();
	   for (Record record : records) {	
		    if (record.data == null || !record.data.containsField("resourceType")) continue;
			T p = parser.parseResource(getResourceType(), record.data.toString());
			processResource(record, p);
			result.add(p);
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
		insertRecord(record, theResource);
	}
	
	@Override
	public void updatePrepare(Record record, T theResource) throws AppException {		
		prepare(record, theResource);	
		prepareTags(record, theResource);
	}
	
	public void prepareTags(Record record, T theResource) throws AppException {
		boolean tagFound = false;
		if (theResource.getMeta().hasSecurity()) {
			List<Coding> codes = theResource.getMeta().getSecurity();
			for (Coding c : codes) {
				if (c.getSystem().equals("http://terminology.hl7.org/CodeSystem/v3-ActCode") && c.getCode().equals("PHY")) tagFound = true;
			}
		}
		if (tagFound) {
			if (record.tags == null) record.tags = new HashSet<String>();
			record.tags.add("security:hidden");
		} else {
			if (record.tags != null) record.tags.remove("security:hidden");
		}
	}
	
	@Override
	public void updateExecute(Record record, T theResource) throws AppException {
		updateRecord(record, theResource);
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
	    			T p = parser.parseResource(resultClass, rec.data.toString());
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
		record.creator = info().executorId;
		record.format = format;
		record.app = info().pluginId;
		record.created = record._id.getCreationDate();
		record.code = new HashSet<String>();
		record.owner = info().ownerId;
		record.version = VersionedDBRecord.INITIAL_VERSION;
		return record;
	}
	
	@Override
	public Record fetchCurrent(IIdType theId)  {
		try {
			if (theId == null) throw new UnprocessableEntityException("id missing");
			if (theId.getIdPart() == null || theId.getIdPart().length() == 0) throw new UnprocessableEntityException("id local part missing");
			if (!isLocalId(theId)) throw new UnprocessableEntityException("id is not local resource");
			
			Record record = RecordManager.instance.fetch(info().executorId, info().role, info().targetAPS, new MidataId(theId.getIdPart()));
			
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
		insertRecord(record, resource, info().context);
	}
	
	public static void insertRecord(Record record, IBaseResource resource, AccessContext targetConsent) throws AppException {
 		insertRecord(info(), record, resource, targetConsent);
	}
	
	public static void insertRecord(ExecutionInfo info, Record record, IBaseResource resource, AccessContext targetConsent) throws AppException {
		AccessLog.logBegin("begin insert FHIR record");		    
			String encoded = ctx.newJsonParser().encodeResourceToString(resource);			
			record.data = (DBObject) JSON.parse(encoded);	
			try {
			  PluginsAPI.createRecord(info, record, targetConsent);			
			} finally {
		      AccessLog.logEnd("end insert FHIR record");
			}
	}
	
	public MidataId insertMessageRecord(Record record, IBaseResource resource) throws AppException {
		ExecutionInfo inf = info();
		MidataId shareFrom = inf.executorId;
		MidataId owner = record.owner;
		if (!owner.equals(inf.executorId)) {
			Consent consent = Circles.getOrCreateMessagingConsent(inf.executorId, inf.executorId, owner, owner, false);
			insertRecord(record, resource, new ConsentAccessContext(consent, info().context));
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
			byte[] dataArray = attachment.getData();
			if (dataArray != null)  data = new ByteArrayInputStream(dataArray);
			else if (attachment.getUrl() != null) {
				try {
				  data = new URL(attachment.getUrl()).openStream();
				} catch (MalformedURLException e) {
					throw new UnprocessableEntityException("Malformed URL");
				} catch (IOException e2) {
					throw new UnprocessableEntityException("IO Exception");
				}
			} 
			String contentType = attachment.getContentType();
			String fileName = attachment.getTitle();
			
			attachment.setData(null);
			attachment.setUrl(null);
			
			String encoded = ctx.newJsonParser().encodeResourceToString(resource);
			record.data = (DBObject) JSON.parse(encoded);
			
			EncryptedFileHandle handle = RecordManager.instance.addFile(data, fileName, contentType);
			PluginsAPI.createRecord(info(), record, handle, fileName, contentType, info().context);			
		
		AccessLog.logEnd("end insert FHIR record with attachment");
	}
	
	public void updateRecord(Record record, IBaseResource resource) throws AppException {
		if (resource.getMeta() != null && resource.getMeta().getVersionId() != null && !record.version.equals(resource.getMeta().getVersionId())) throw new ResourceVersionConflictException("Wrong resource version supplied!") ;
		String encoded = ctx.newJsonParser().encodeResourceToString(resource);
		record.data = (DBObject) JSON.parse(encoded);	
		record.version = resource.getMeta().getVersionId();
		record.version = RecordManager.instance.updateRecord(info().executorId, info().context, record);
	
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
				
		resource.getMeta().addExtension(meta);
	}
	
	/**
	 * Set record.code field based on a CodeableConcept field in the FHIR resource
	 * @param record MIDATA record
	 * @param cc CodeableConcept to set
	 * @return display of codeable concept
	 */
	protected String setRecordCodeByCodeableConcept(Record record, CodeableConcept cc, String defaultContent) {
	  return setRecordCodeByCodings(record, cc != null ? cc.getCoding() : null, defaultContent);
	}
	
	protected String setRecordCodeByCodings(Record record, List<Coding> codings, String defaultContent) {
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
			  
				ContentInfo.setRecordCodeAndContent(record, record.code, null);
			  
			  } else {
				  ContentInfo.setRecordCodeAndContent(record, null, defaultContent);
			  }
		  } catch (AppException e) {
			    ErrorReporter.report("FHIR (set record code)", null, e);	
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
	       ExecutionInfo inf = info();
			
			MidataId owner = record.owner;
			
			for (IIdType ref : personRefs) {
				if (FHIRTools.isUserFromMidata(ref)) { 
					   TypedMidataId target = FHIRTools.getMidataIdFromReference(ref);
					   if (!target.getMidataId().equals(owner)) {
					     Consent consent = Circles.getOrCreateMessagingConsent(inf.executorId, owner, target.getMidataId(), owner, target.getType().equals("Group"));
					     RecordManager.instance.share(inf.executorId, shareFrom, consent._id, consent.owner, Collections.singleton(record._id), true);
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
}
