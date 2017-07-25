package utils.fhir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.FhirTerser;
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
import utils.access.RecordManager;
import utils.access.VersionedDBRecord;
import utils.auth.ExecutionInfo;
import utils.collections.CMaps;
import utils.collections.ReferenceTool;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

/**
 * Base class for FHIR resource providers. There is one provider subclass for each FHIR resource type.
 *
 */
public  abstract class ResourceProvider<T extends DomainResource> implements IResourceProvider {

		
	public static FhirContext ctx = FhirContext.forDstu3();
	
	/**
	 * Returns FHIR context class
	 * @return FHIR context for DSTU3
	 */
	public static FhirContext ctx() {
		return ctx;
	}
	
	
	private static ThreadLocal<ExecutionInfo> tinfo = new ThreadLocal<ExecutionInfo>();
	
	
	/**
	 * Set ExecutionInfo (Session information) for current Thread to be used by FHIR classes
	 * @param info ExecutionInfo to be used
	 */
	public static void setExecutionInfo(ExecutionInfo info) {
		tinfo.set(info);
	}
	
	/**
	 * Retrives ExecutionInfo for current thread
	 * @return ExecutionInfo
	 */
	public static ExecutionInfo info() {
		ExecutionInfo inf = tinfo.get();
		if (inf == null) throw new AuthenticationException();
		return inf;
	}
	
	/**
	 * Returns the class of FHIR resources provided by this resource provider
	 * @return Subclass of BaseResource 
	 */
	public abstract Class<T> getResourceType();
	
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
			List<Record> result = RecordManager.instance.list(info().executorId, info().targetAPS, CMaps.map("_id", new MidataId(theId.getIdPart())).map("version", theId.getVersionIdPart()), RecordManager.COMPLETE_DATA);
			record = result.isEmpty() ? null : result.get(0);
		} else {
		    record = RecordManager.instance.fetch(info().executorId, info().targetAPS, new MidataId(theId.getIdPart()));
		}
		if (record == null) throw new ResourceNotFoundException(theId);					
		IParser parser = ctx().newJsonParser();
		T p = parser.parseResource(getResourceType(), record.data.toString());
		processResource(record, p);		
		return p;
	}
	
	@History()
	public List<T> getHistory(@IdParam IIdType theId) throws AppException {
	   List<Record> records = RecordManager.instance.list(info().executorId, info().targetAPS, CMaps.map("_id", new MidataId(theId.getIdPart())).map("history", true).map("sort","lastUpdated desc"), RecordManager.COMPLETE_DATA);
	   if (records.isEmpty()) throw new ResourceNotFoundException(theId); 
	   
	   List<T> result = new ArrayList<T>(records.size());
	   IParser parser = ctx().newJsonParser();
	   for (Record record : records) {		   
			T p = parser.parseResource(getResourceType(), record.data.toString());
			processResource(record, p);
			result.add(p);
	   }
	   
	   return result;
	}
	
	/**
	 * Default wrapper for FHIR create
	 * @param theResource
	 * @return
	 */
	protected MethodOutcome createResource(@ResourceParam T theResource) {

		try {
			return create(theResource);
		} catch (BaseServerResponseException e) {
			throw e;
		} catch (BadRequestException e2) {
			throw new InvalidRequestException(e2.getMessage());
		} catch (InternalServerException e3) {
			ErrorReporter.report("FHIR (create resource)", null, e3);
			throw new InternalErrorException(e3);
		} catch (Exception e4) {
			ErrorReporter.report("FHIR (create resource)", null, e4);
			throw new InternalErrorException(e4);
		}		

	}
	
	/**
	 * Default wrapper for FHIR update
	 * @param theId
	 * @param theResource
	 * @return
	 */
	protected MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam T theResource) {

		try {
			if (theId.getVersionIdPart() == null && (theResource.getMeta() == null || theResource.getMeta().getVersionId() == null)) throw new PreconditionFailedException("Resource version missing!");
			return update(theId, theResource);
		} catch (BaseServerResponseException e) {
			throw e;
		} catch (BadRequestException e2) {
			throw new InvalidRequestException(e2.getMessage());
		} catch (InternalServerException e3) {
			ErrorReporter.report("FHIR (update resource)", null, e3);
			throw new InternalErrorException(e3);
		} catch (Exception e4) {
			ErrorReporter.report("FHIR (update resource)", null, e4);
			throw new InternalErrorException(e4);
		}		

	}
	
	/**
	 * Default implementation for create
	 * @param theResource
	 * @return
	 * @throws AppException
	 */
	protected MethodOutcome create(T theResource) throws AppException {
		Record record = init();
		prepare(record, theResource);	
		insertRecord(record, theResource);
		processResource(record, theResource);				
		
		return outcome(theResource.getResourceType().name(), record, theResource);				
	}
	
	/**
	 * Default implementation for update
	 * @param theId
	 * @param theResource
	 * @return
	 * @throws AppException
	 */
	protected MethodOutcome update(IdType theId, T theResource) throws AppException {
		Record record = fetchCurrent(theId);
		prepare(record, theResource);		
		updateRecord(record, theResource);	
		processResource(record, theResource);
		
		return outcome(theResource.getResourceType().name(), record, theResource);				
	}
	
	
	public abstract List<Record> searchRaw(SearchParameterMap params) throws AppException;
		
	
	public List<IBaseResource> search(SearchParameterMap params) {
		try {		
			List<IBaseResource> results = new ArrayList<IBaseResource>();
		   List<T> resources = parse(searchRaw(params), getResourceType());	
		   results.addAll(resources);
		   if (!params.getIncludes().isEmpty()) {
			   FhirTerser terser = ResourceProvider.ctx().newTerser();
			   
			   for (Include inc : params.getIncludes()) {
				   for (T res : resources) {
					   String type = inc.getParamType();
					   String name = inc.getParamName();
					   if (type==null || name==null) throw new InvalidRequestException("Invalid/incomplete parameter for _include.");
					   String field = searchParamNameToPathMap.get(type + ":" + name);
					   if (field == null) throw new NotImplementedOperationException("Value for _include is not supported by the server.");
					   String path = type + "." + field;
					   Set<String> allowedTypes = searchParamNameToTypeMap.get(type + ":" + name);					   
					   List<IBaseReference> refs = terser.getValues(res, path, IBaseReference.class);
					   if (refs != null) {
						   for (IBaseReference r : refs) {
							   IIdType refElem = r.getReferenceElement();
							   String rtype = refElem.getResourceType();
							   if (allowedTypes != null && !allowedTypes.contains(rtype)) continue;
							   ResourceProvider prov = FHIRServlet.myProviders.get(refElem.getResourceType());
							   if (prov != null) {
								   IBaseResource result = prov.getResourceById(refElem);
								   
								   r.setResource(result);
								   results.add(result);
								   
								   
								   /*r.setDisplay(null);
								   r.setReference(null);*/
								   
							   }
							   
							   
						   }
					   }
				   }
			   }
			   
			   
		   }
		   
		   if (!params.getRevIncludes().isEmpty()) {
			   for (Include inc : params.getRevIncludes()) {
				   String type = inc.getParamType();
				   String name = inc.getParamName();	
				   if (type == null || name == null) throw new InvalidRequestException("Incomplete parameter for _revinclude");
				   ReferenceOrListParam vals = new ReferenceOrListParam();
				   for (T res : resources) {
				      vals.add(new ReferenceParam(type+"/"+res.getId()));
				   }
				   ResourceProvider prov = FHIRServlet.myProviders.get(type);
				   if (prov==null) throw new InvalidRequestException("Unknown resource type for _revinclude");
				   SearchParameterMap newsearch = new SearchParameterMap();
				   newsearch.add(name, vals);
				   List<IBaseResource> alsoAdd = prov.search(newsearch);
				   results.addAll(alsoAdd);  					   				   				   				   
			   }
		   }
		   
		   return results;

	    } catch (AppException e) {
	       ErrorReporter.report("FHIR (search)", null, e);	       
		   return null;
	    } catch (NullPointerException e2) {
			ErrorReporter.report("FHIR (search)", null, e2);	 
			throw new InternalErrorException(e2);
		}
     }
	
	
	protected static Map<String,String> searchParamNameToPathMap = new HashMap<String,String>();
	protected static Map<String,Set<String>> searchParamNameToTypeMap = new HashMap<String,Set<String>>();
	protected static Map<String,String> searchParamNameToTokenMap = new HashMap<String,String>();
	
	public static IQueryParameterType asQueryParameter(String resourceType, String searchParam, ReferenceParam param) {
		String type = searchParamNameToTokenMap.get(resourceType+"."+searchParam);
		if (type==null) throw new NotImplementedOperationException("Search '"+searchParam+"' not defined on resource '"+resourceType+"'.");
		if (type.contains("Token")) return param.toTokenParam(ctx());
		if (type.contains("String")) return param.toStringParam(ctx());
		if (type.contains("Date")) return param.toDateParam(ctx());
		if (type.contains("Quantity")) return param.toQuantityParam(ctx());
		if (type.contains("Number")) return param.toNumberParam(ctx());
		return null;
	}
	
	public static void registerSearches(String resourceType, Class baseClass, String methodName) {
		for (Method m : baseClass.getMethods()) {
			if (m.getName().equals(methodName)) {
				for (Parameter p : m.getParameters()) {
					OptionalParam op = p.getAnnotation(OptionalParam.class);
					if (op != null) {
						searchParamNameToTokenMap.put(resourceType+"."+op.name(), p.getType().getSimpleName());
					}
				}
			}
		}
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
		
	
	public List<T> parse(List<Record> result, Class<T> resultClass) throws AppException {
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
	
	public T parse(Record record, Class<T> resultClass) throws AppException {
		return parse(Collections.singletonList(record), resultClass).iterator().next();
	}
	
    public String serialize(T resource) {
    	return ctx.newJsonParser().encodeResourceToString(resource);
    }
	
	public void processResource(Record record, T resource) throws AppException {
		resource.setId(new IdType(resource.fhirType(), record._id.toString(), record.version));
		resource.getMeta().setVersionId(record.version);
		if (record.lastUpdated == null) resource.getMeta().setLastUpdated(record.created);
		else resource.getMeta().setLastUpdated(record.lastUpdated);
		
		Extension meta = new Extension("http://midata.coop/extensions/metadata");
		
		if (record.app != null) {
		  Plugin creatorApp = Plugin.getById(record.app);		
		  meta.addExtension("app", new Coding("http://midata.coop/codesystems/app", creatorApp.filename, creatorApp.name));
		}
		if (record.creator != null) meta.addExtension("creator", FHIRTools.getReferenceToUser(record.creator, record.creator.equals(record.owner) ? record.ownerName : null ));
				
		resource.getMeta().addExtension(meta);
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

	public void insertRecord(Record record, IBaseResource resource) throws AppException {
		insertRecord(record, resource, (MidataId) null);
	}
	
	public static void insertRecord(Record record, IBaseResource resource, MidataId targetConsent) throws AppException {
		AccessLog.logBegin("begin insert FHIR record");		    
			String encoded = ctx.newJsonParser().encodeResourceToString(resource);			
			record.data = (DBObject) JSON.parse(encoded);			
			PluginsAPI.createRecord(info(), record, targetConsent);			
		
		AccessLog.logEnd("end insert FHIR record");
	}
	
	public MidataId insertMessageRecord(Record record, IBaseResource resource) throws AppException {
		ExecutionInfo inf = info();
		MidataId shareFrom = inf.executorId;
		MidataId owner = record.owner;
		if (!owner.equals(inf.executorId)) {
			Consent consent = Circles.getOrCreateMessagingConsent(inf.executorId, inf.executorId, owner, owner, false);
			insertRecord(record, resource, consent._id);
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
			
			PluginsAPI.createRecord(info(), record, data, fileName, contentType, null);			
		
		AccessLog.logEnd("end insert FHIR record with attachment");
	}
	
	public void updateRecord(Record record, IBaseResource resource) throws AppException {
		if (resource.getMeta() != null && resource.getMeta().getVersionId() != null && !record.version.equals(resource.getMeta().getVersionId())) throw new ResourceVersionConflictException("Wrong resource version supplied!") ;
		String encoded = ctx.newJsonParser().encodeResourceToString(resource);
		record.data = (DBObject) JSON.parse(encoded);	
		record.version = resource.getMeta().getVersionId();
		record.version = RecordManager.instance.updateRecord(info().executorId, info().targetAPS, record);
	
	}
	
	public void clean(T resource) {
		resource.getMeta().setExtension(null);
		resource.setId((IIdType) null);		
	}
	
	public void prepare(Record record, T theResource) throws AppException { }
	
	public Record init() { return null; }
		
	
	
	public MethodOutcome outcome(String type, Record record, IBaseResource resource) {
		
		
		String version = record.version;
		if (version == null) version = VersionedDBRecord.INITIAL_VERSION;
		MethodOutcome retVal = new MethodOutcome(new IdType(type, record._id.toString(), version), true);
		
        retVal.setResource(resource);
        
		return retVal;
	}
		
	protected boolean cleanAndSetRecordOwner(Record record, Reference subjectRef) throws AppException  {
		boolean cleanSubject = true;
		if (subjectRef != null) {
			IIdType target = subjectRef.getReferenceElement();
			if (target != null && !target.isEmpty()) {
				String rt = target.getResourceType();
																													
				if (rt != null && rt.equals("Patient")) {
					String tId = target.getIdPart();
					if (tId.equals(info().executorId.toString())) {
						
					} else {
						//cleanSubject = false;
						record.owner = FHIRTools.getUserIdFromReference(target);
					}
					
					
				} else cleanSubject = false;
			}
		}
		return cleanSubject;
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

}
