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

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

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
import utils.ErrorReporter;
import utils.QueryTagTools;
import utils.access.RecordManager;
import utils.access.VersionedDBRecord;
import utils.collections.CMaps;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.exceptions.RequestTooLargeException;

/**
 * Base class for FHIR resource providers. There is one provider subclass for each FHIR resource type.
 *
 */
public  abstract class ResourceProvider<T extends DomainResource, M extends Model> implements IResourceProvider {

		
	public static FhirContext ctx = FhirContext.forR4();
	
	/**
	 * Returns FHIR context class
	 * @return FHIR context for R4
	 */
	public static FhirContext ctx() {
		return ctx;
	}
	
	
	private static ThreadLocal<AccessContext> tinfo = new ThreadLocal<AccessContext>();
	
	
	/**
	 * Set AccessContext (Session information) for current Thread to be used by FHIR classes
	 * @param info AccessContext to be used
	 */
	public static void setAccessContext(AccessContext info) {
		tinfo.set(info);
	}
	
	/**
	 * Retrives AccessContext for current thread
	 * @return AccessContext
	 */
	public static AccessContext info() {
		AccessContext inf = tinfo.get();
		if (inf == null) throw new AuthenticationException();
		return inf;
	}
	
	public static boolean hasInfo() {
		AccessContext inf = tinfo.get();
		return inf != null;
	}
		
	/**
	 * Returns the class of FHIR resources provided by this resource provider
	 * @return Subclass of BaseResource 
	 */
	public abstract Class<T> getResourceType();
	
	
	public List<T> getHistory(@IdParam IIdType theId, @ca.uhn.fhir.rest.annotation.Count Integer theCount) throws AppException {		
		return null;
	}

	
	public abstract M fetchCurrent(IIdType theId, T resource, boolean versioned) throws AppException;
	
	public abstract void processResource(M record, T resource) throws AppException;
			
	protected int simpleCountResources(SearchParameterMap params) {
		try {
			int count = 0;
			params.setCount(2000);
			params.setFrom(null);
			do {
			  List<M> recs = searchRaw(params);
			  count += recs.size();			  						
			  if (recs.size() == 2001) {			   
				 params.setFrom(getFromId(recs.get(recs.size() - 1)));				   
			  } else params.setFrom(null);
			} while (params.getFrom() != null);
			return count;
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
	
	public int countResources(SearchParameterMap params) {
		return simpleCountResources(params);
	}
	
	public abstract List<M> searchRaw(SearchParameterMap params) throws AppException;
	
	public abstract T getResourceById(@IdParam IIdType theId) throws AppException;
	
	public String getResourceUrl(String baseUrl, IBaseResource r) {
		String res = baseUrl+"/"+r.getIdElement().toVersionless().toString();		
		return res;
	}
	
	public Bundle searchBundle(SearchParameterMap params, RequestDetails theDetails) {
		Bundle result = new Bundle();
		
				
		try {
			
			if (params.getCount() != null && params.getCount() == 0) {
				int total = countResources(params);
				result.setTotal(total);
				return result;
			}
			
			List<IBaseResource> res = search(params);
			for (IBaseResource r : res) {					
				result.addEntry().setResource((Resource) r).setFullUrl(getResourceUrl(theDetails.getFhirServerBase(), r));				
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
	
	public List<T> basicSearch(SearchParameterMap params) throws AppException {
		List<M> raw = searchRaw(params);
	    List<T> resources = parse(raw, getResourceType());
	   		  
	   if (params.getCount() != null && raw.size() == params.getCount() + 1) {			   
		   params.setFrom(getFromId(raw.get(raw.size() - 1)));
		   if (resources.size() > params.getCount()) {
		     resources = resources.subList(0, params.getCount());
		   }
	   } else params.setFrom(null);
	   
	   return resources;
	}
	
	public List<IBaseResource> search(SearchParameterMap params) {
		try {		
			List<IBaseResource> results = new ArrayList<IBaseResource>();			
		    List<T> resources = basicSearch(params);		   		  
		   
		   results.addAll(resources);
		   
		   if (!params.getIncludes().isEmpty()) {
			   FhirTerser terser = ResourceProvider.ctx().newTerser();
			   Set<IIdType> existingIds = new HashSet<>();
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
							   if (refElem==null) continue;
							   String rtype = refElem.getResourceType();
							   if (allowedTypes != null && !allowedTypes.contains(rtype)) continue;
							   if (existingIds.contains(refElem)) continue;
							   ResourceProvider prov = FHIRServlet.myProviders.get(refElem.getResourceType());
							   if (prov != null) {							       
								   IBaseResource result = prov.getResourceById(refElem);
								   if (result != null && existingIds.add(result.getIdElement())) {								   
								     r.setResource(result);
								     results.add(result);
								   }
								   
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
		} catch (InternalServerException e3) {
		   ErrorReporter.report("FHIR (search)", null, e3);
		   throw new InternalErrorException("Internal error during search");
	    } catch (AppException e) {
	       ErrorReporter.report("FHIR (search)", null, e);	      
		   throw new InvalidRequestException(e.getMessage());
	    } catch (NullPointerException e2) {
			ErrorReporter.report("FHIR (search)", null, e2);	 
			throw new InternalErrorException("internal error during FHIR search");
		}
     }
	
	
	protected static Map<String,String> searchParamNameToPathMap = new HashMap<String,String>();
	protected static Map<String,Set<String>> searchParamNameToTypeMap = new HashMap<String,Set<String>>();
	protected static Map<String,String> searchParamNameToTokenMap = new HashMap<String,String>();
	protected static Set<String> pathesWithVersion = new HashSet<String>();
	
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
	
	public static void addPathWithVersion(String path) {
		pathesWithVersion.add(path);
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
		if (theId.getBaseUrl() == null && MidataId.isValid(theId.getIdPart())) return true;
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
					if (tId.equals(info().getLegacyOwner().toString())) {
						
					} else {
						//cleanSubject = false;
						record.owner = FHIRTools.getUserIdFromReference(target);
					}
					
					
				} else cleanSubject = false;
			} else if (subjectRef.getDisplay() != null) cleanSubject = false;
		}
		return cleanSubject;
	}
	
	protected void convertToR4(M fromDB, Object in) {
		if (FHIRVersionConvert.doConvert(fromDB._id)) convertToR4(in);
	}
	
	protected abstract void convertToR4(Object in);
	
	public boolean checkAccessible() throws AppException {
		AccessContext info = info();						
		if (!info.mayAccess(getResourceType().getSimpleName(), "fhir/"+getResourceType().getSimpleName())) return false;
		return true;
	}
	
	protected int getProcessingOrder() {
		return 2;
	}
	
	public boolean addSecurityTag(T theResource, String tag) {
		Pair<String, String> coding = QueryTagTools.getSystemCodeForTag(tag);
		if (theResource.getMeta().hasSecurity()) {
			List<Coding> codes = theResource.getMeta().getSecurity();
			for (Coding c : codes) {
			    if (coding.getLeft().equals(c.getSystem()) && coding.getRight().equals(c.getCode())) return false;			    
			}
		}
		theResource.getMeta().addSecurity(new Coding(coding.getLeft(), coding.getRight(), null));
		return true;
	}
	
	public String getIdForReference(M record) {
		return record._id.toString();
	}
	
	
}
