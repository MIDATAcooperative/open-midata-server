package utils.fhir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Property;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.exceptions.FHIRException;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.typesafe.config.ConfigException.BugOrBroken;

import controllers.PluginsAPI;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.FhirTerser;

import models.Record;

import utils.AccessLog;
import utils.ErrorReporter;
import utils.access.RecordManager;
import utils.access.VersionedDBRecord;
import utils.auth.ExecutionInfo;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.exceptions.AppException;


public  abstract class ResourceProvider<T extends BaseResource> implements IResourceProvider {

	public static FhirContext ctx = FhirContext.forDstu3();
	
	public static FhirContext ctx() {
		return ctx;
	}
	
	
	public static ThreadLocal<ExecutionInfo> tinfo = new ThreadLocal<ExecutionInfo>();
	
	
	
	public static void setExecutionInfo(ExecutionInfo info) {
		tinfo.set(info);
	}
	
	public static ExecutionInfo info() {
		return tinfo.get();
	}
	
	
	public abstract Class<T> getResourceType();
	
	@Read()
	public T getResourceById(@IdParam IIdType theId) throws AppException {
		Record record = RecordManager.instance.fetch(info().executorId, info().targetAPS, new MidataId(theId.getIdPart()));
		IParser parser = ctx().newJsonParser();
		T p = parser.parseResource(getResourceType(), record.data.toString());
		processResource(record, p);		
		return p;
	}
	
	
	public abstract List<Record> searchRaw(SearchParameterMap params) throws AppException;
		
	
	public List<T> search(SearchParameterMap params) {
		try {									
		   List<T> resources = parse(searchRaw(params), getResourceType());	
		   
		   if (!params.getIncludes().isEmpty()) {
			   FhirTerser terser = ResourceProvider.ctx().newTerser();
			   
			   for (Include inc : params.getIncludes()) {
				   for (T res : resources) {
					   String type = inc.getParamType();
					   String name = inc.getParamName();
					   
					   List<IBaseReference> refs = terser.getValues(res, type+"."+name, IBaseReference.class);
					   if (refs != null) {
						   for (IBaseReference r : refs) {
							   IIdType refElem = r.getReferenceElement();
							   IBaseResource result = FHIRServlet.myProviders.get(refElem.getResourceType()).getResourceById(refElem);
							   AccessLog.log("added:"+result.toString());
							   //r.setDisplay(null);
							   //r.setReference(null);
							   r.setResource(result);							  
							   
							   
						   }
					   }
				   }
			   }
		   }
		   
		   return resources;

	    } catch (AppException e) {
	       ErrorReporter.report("FHIR (search)", null, e);	       
		   return null;
	    } catch (NullPointerException e2) {
			ErrorReporter.report("FHIR (search)", null, e2);	 
			throw new InternalErrorException(e2);
		}
     }
	
	
	public static Set<String> referencesToIds(Collection<ReferenceParam> refs) {
		
		Set<String> ids = new HashSet<String>();
		for (ReferenceParam ref : refs)
			ids.add(ref.getIdPart().toString());
		return ids;				
	}
	
	
	
	public static Set<String> tokensToStrings(TokenOrListParam params) {
		Set<String> result = new HashSet<String>();
		for (TokenParam p : params.getValuesAsQueryTokens()) {			
			if (p.getSystem() != null) {
			  result.add(p.getSystem()+" "+p.getValue());
			} else result.add("http://loinc.org "+p.getValue());
		}
		return result;
	}
	
	public static Set<String> tokensToCodeStrings(TokenOrListParam params) {
		Set<String> result = new HashSet<String>();
		for (TokenParam p : params.getValuesAsQueryTokens()) {			
			result.add(p.getValue());			
		}
		return result;
	}
		
	
	public List<T> parse(List<Record> result, Class<T> resultClass) {
		ArrayList<T> parsed = new ArrayList<T>();	
	    IParser parser = ctx().newJsonParser();
	    for (Record rec : result) {
		  try {
			T p = parser.parseResource(resultClass, rec.data.toString());
	        processResource(rec, p);											
			parsed.add(p);
	  	  } catch (DataFormatException e) {
		  }
	    }
	    return parsed;
	}
		
	
	public void processResource(Record record, T resource) {
		resource.setId(record._id.toString());
		resource.getMeta().setVersionId(record.version);
		resource.getMeta().setLastUpdated(record.lastUpdated);
	}
	
	public static Record newRecord(String format) {
		Record record = new Record();
		record._id = new MidataId();
		record.creator = info().executorId;
		record.format = format;
		record.app = info().pluginId;
		record.created = new Date(System.currentTimeMillis());
		record.code = new HashSet<String>();
		record.owner = info().ownerId;
		return record;
	}
	
	public static Record fetchCurrent(IIdType theId)  {
		try {
			if (theId == null) throw new UnprocessableEntityException("id missing");
			if (theId.getIdPart() == null || theId.getIdPart().length() == 0) throw new UnprocessableEntityException("id local part missing");
			if (!isLocalId(theId)) throw new UnprocessableEntityException("id is not local resource");
			
			Record record = RecordManager.instance.fetch(info().executorId, info().targetAPS, new MidataId(theId.getIdPart()));
			
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
	
	private static boolean isLocalId(IIdType theId) {
		if (theId.getBaseUrl() == null) return true;
		return false;
	}

	public static void insertRecord(Record record, IBaseResource resource) {
		AccessLog.logBegin("begin insert FHIR record");
		try {
			String encoded = ctx.newJsonParser().encodeResourceToString(resource);
			record.data = (DBObject) JSON.parse(encoded);
			PluginsAPI.createRecord(info(), record);			
		} catch (AppException e) {
			ErrorReporter.report("FHIR (insert record)", null, e);				
			throw new InternalErrorException(e);
		} catch (NullPointerException e2) {
			ErrorReporter.report("FHIR (insert record)", null, e2);	 
			throw new InternalErrorException(e2);
		}
		AccessLog.logEnd("end insert FHIR record");
	}
	
	public static void insertRecord(Record record, IBaseResource resource, Attachment attachment) throws UnprocessableEntityException {
		if (attachment == null || attachment.isEmpty()) {
			insertRecord(record, resource);
			return;
		} 
		AccessLog.logBegin("begin insert FHIR record with attachment");
		try {						
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
			
			PluginsAPI.createRecord(info(), record, data, fileName, contentType);			
		} catch (AppException e) {
			ErrorReporter.report("FHIR (insert record)", null, e);	 
			throw new InternalErrorException(e);
		} catch (NullPointerException e2) {
			ErrorReporter.report("FHIR (insert record)", null, e2);	 
			throw new InternalErrorException(e2);
		}
		AccessLog.logEnd("end insert FHIR record with attachment");
	}
	
	public static void updateRecord(Record record, IBaseResource resource) {
		try {
			String encoded = ctx.newJsonParser().encodeResourceToString(resource);
			record.data = (DBObject) JSON.parse(encoded);	
			record.version = resource.getMeta().getVersionId();
			RecordManager.instance.updateRecord(info().executorId, info().targetAPS, record);
		} catch (AppException e) {
			ErrorReporter.report("FHIR (update record)", null, e);	 
			throw new InternalErrorException(e);
		} catch (NullPointerException e2) {
			ErrorReporter.report("FHIR (update record)", null, e2);	 
			throw new InternalErrorException(e2);
		}
	}
	
	public void clean(T resource) {
		
	}
	
	public void prepare(Record record, T theResource)  { }
	
	public Record init() { return null; }
		
	
	
	public MethodOutcome outcome(String type, Record record, IBaseResource resource) {
		
		
		String version = record.version;
		if (version == null) version = VersionedDBRecord.INITIAL_VERSION;
		MethodOutcome retVal = new MethodOutcome(new IdType(type, record._id.toString(), version), true);
		
        retVal.setResource(resource);
        
		return retVal;
	}
}
