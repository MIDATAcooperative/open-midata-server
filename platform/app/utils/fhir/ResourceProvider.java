package utils.fhir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.hl7.fhir.instance.model.api.IBaseResource;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.Observation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

import models.Record;

import utils.AccessLog;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.exceptions.AppException;


public  abstract class ResourceProvider<T extends BaseResource> implements IResourceProvider {

	public static FhirContext ctx = FhirContext.forDstu3();
	
	public FhirContext ctx() {
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
	public T getResourceById(@IdParam IdType theId) throws AppException {
		Record record = RecordManager.instance.fetch(info().executorId, info().targetAPS, new ObjectId(theId.getIdPart()));
		IParser parser = ctx().newJsonParser();
		T p = parser.parseResource(getResourceType(), record.data.toString());
		processResource(record, p);		
		return p;
	}
	
	public static void addRestriction(Map<String, Object> crit, StringParam content, String... path) {
		addRestriction(crit, content.getValue(), path, 0);
	}
	
	public static void addRestriction(Map<String, Object> crit, TokenOrListParam content, String... path) {
		Set<String> values = tokensToCodeStrings(content);
		addRestriction(crit, values, path, 0);
	}
	
	public static void addRestriction(Map<String, Object> crit, DateParam content, String... path) {
		addRestriction(crit, content.getValueAsString(), path, 0);
	}
	
	public static void addRestriction(Map<String, Object> crit, String rt, ReferenceParam content, String... path) {
		addRestriction(crit, rt+"/"+content.getIdPart(), path, 0);
	}
	
	public static void addRestriction(Map<String, Object> crit, Object content, String[] path, int idx) {
		if (idx == path.length - 1) {
			crit.put(path[idx], content);
		} else {
			Object subCrit = crit.get(path[idx]);
			if (subCrit == null) {
				subCrit = new HashMap<String, Object>();
				crit.put(path[idx], subCrit);
			}
			addRestriction((Map<String, Object>) subCrit, content, path, idx + 1);
		}
	}
	
	public abstract List<Record> searchRaw(SearchParameterMap params) throws AppException;
	
	public List<T> search(SearchParameterMap params) {
		try {									
		   List<T> observations = parse(searchRaw(params), getResourceType());						
		   return observations;

	    } catch (AppException e) {
		   AccessLog.log("ERROR");
		   return null;
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
		
	
	public <T extends BaseResource> List<T> parse(List<Record> result, Class<T> resultClass) {
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
		
	
	public static void processResource(Record record, BaseResource resource) {
		resource.setId(record._id.toString());
		resource.getMeta().setVersionId(record.version);
		resource.getMeta().setLastUpdated(record.lastUpdated);
	}
}
