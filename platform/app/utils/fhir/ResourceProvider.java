package utils.fhir;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.util.FhirTerser;
import ca.uhn.fhir.util.UrlUtil;
import models.MidataId;
import models.Model;
import models.Record;
import models.enums.UserRole;
import utils.ErrorReporter;
import utils.access.VersionedDBRecord;
import utils.auth.ExecutionInfo;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.exceptions.RequestTooLargeException;

/**
 * Base class for FHIR resource providers. There is one provider subclass for each FHIR resource type.
 *
 */
public  abstract class ResourceProvider<T extends DomainResource, M extends Model> implements IResourceProvider {

		
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
	 * Retrives ExecutionInfo for current thread or default instance
	 * @return ExecutionInfo
	 */
	/*
	public static ExecutionInfo info(MidataId executor, UserRole role) throws InternalServerException {
		ExecutionInfo inf = tinfo.get();
		if (inf == null) return new ExecutionInfo(executor, role);
		return inf;
	}*/
	
	/**
	 * Returns the class of FHIR resources provided by this resource provider
	 * @return Subclass of BaseResource 
	 */
	public abstract Class<T> getResourceType();
	
	
			

	
	public abstract M fetchCurrent(IIdType theId) throws AppException;
	
	public abstract void processResource(M record, T resource) throws AppException;
	
	
	
	public abstract List<M> searchRaw(SearchParameterMap params) throws AppException;
	
	public abstract T getResourceById(@IdParam IIdType theId) throws AppException;
	
	
	public Bundle searchBundle(SearchParameterMap params, RequestDetails theDetails) {
		Bundle result = new Bundle();
		try {
			List<IBaseResource> res = search(params);
			for (IBaseResource r : res) {
				result.addEntry().setResource((Resource) r).setFullUrl(theDetails.getFhirServerBase()+"/"+r.getIdElement().toString());
			}
			
			if (params.getFrom() != null) {
			String p = theDetails.getCompleteUrl();
			int pos = p.indexOf("?");
			p = pos > 0 ? p.substring(0, pos) : p;
			Map<String, String[]> rp = new HashMap<String, String[]>(theDetails.getParameters());
			String[] _page = new String[1];
			_page[0] = params.getFrom();
			rp.put("_page", _page );
			
			StringBuilder b = new StringBuilder();
			for (Entry<String, String[]> next : rp.entrySet()) {
				for (String nextValue : next.getValue()) {
					if (b.length() == 0) {
						b.append('?');
					} else {
						b.append('&');
					}
					b.append(UrlUtil.escapeUrlParam(next.getKey()));
					b.append('=');
					b.append(UrlUtil.escapeUrlParam(nextValue));
				}
			}
			
			result.addLink().setRelation("next").setUrl(p + b.toString());
			result.setTotal(0);
			} else result.setTotal(res.size());
		} catch (RequestTooLargeException e) {
			try { Thread.sleep(1000*10); } catch (InterruptedException e2) {}
			result.addLink().setRelation("next").setUrl(theDetails.getCompleteUrl());
		}
		return result;		
	}
	
	protected String getFromId(M resource) {
		return resource._id.toString();
	}
	
	public List<IBaseResource> search(SearchParameterMap params) {
		try {		
			List<IBaseResource> results = new ArrayList<IBaseResource>();
			List<M> raw = searchRaw(params);
		    List<T> resources = parse(raw, getResourceType());
		   		  
		   if (params.getCount() != null && raw.size() == params.getCount() + 1) {			   
			   params.setFrom(getFromId(raw.get(raw.size() - 1)));
			   if (resources.size() > params.getCount()) {
			     resources = resources.subList(0, params.getCount());
			   }
		   } else params.setFrom(null);
		   
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

		} catch (RequestTooLargeException e2) {
			throw e2;
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
		
	
	public T parse(M record, Class<T> resultClass) throws AppException {
		List<T> result = parse(Collections.singletonList(record), resultClass);
		return result.isEmpty() ? null : result.iterator().next();
	}
	
	public abstract List<T> parse(List<M> result, Class<T> resultClass) throws AppException;
	
    public String serialize(T resource) {
    	return ctx.newJsonParser().encodeResourceToString(resource);
    }
	
			
	protected static boolean isLocalId(IIdType theId) {
		if (theId.getBaseUrl() == null) return true;
		return false;
	}
		
	public void clean(T resource) {
		resource.getMeta().setExtension(null);
		resource.setId((IIdType) null);		
	}
	
	public void prepare(Record record, T theResource) throws AppException { }
	
	
		
	public MethodOutcome outcome(String type, IBaseResource resource) {				
		String version = resource.getMeta().getVersionId();
		if (version == null) version = VersionedDBRecord.INITIAL_VERSION;
		MethodOutcome retVal = new MethodOutcome(new IdType(type, resource.getIdElement().getIdPart(), version), true);
		
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
					if (tId.equals(info().ownerId.toString())) {
						
					} else {
						//cleanSubject = false;
						record.owner = FHIRTools.getUserIdFromReference(target);
					}
					
					
				} else cleanSubject = false;
			} else if (subjectRef.getDisplay() != null) cleanSubject = false;
		}
		return cleanSubject;
	}
	
	
		
		
	

}
