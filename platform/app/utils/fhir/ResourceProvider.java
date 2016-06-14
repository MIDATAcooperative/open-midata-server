package utils.fhir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import models.Record;

import utils.auth.ExecutionInfo;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;

public class ResourceProvider {

	public static FhirContext ctx = FhirContext.forDstu2();
	public static ThreadLocal<ExecutionInfo> tinfo = new ThreadLocal<ExecutionInfo>();
	
	public FhirContext ctx() {
		return ctx;
	}
	
	public static void setExecutionInfo(ExecutionInfo info) {
		tinfo.set(info);
	}
	
	public ExecutionInfo info() {
		return tinfo.get();
	}
	
	public static void addRestriction(Map<String, Object> crit, StringParam content, String... path) {
		addRestriction(crit, content.getValue(), path, 0);
	}
	
	public static void addRestriction(Map<String, Object> crit, TokenParam content, String... path) {
		addRestriction(crit, content.getValue(), path, 0);
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
	
	public static Set<String> refsToObjectIds(ReferenceOrListParam params) {
		Set<String> result = new HashSet<String>();
		for (ReferenceParam p : params.getValuesAsQueryTokens()) {
			result.add(p.getIdPart());
		}
		return result;
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
	
	public static void processResource(Record record, BaseResource resource) {
		resource.setId(record._id.toString());	
		ResourceMetadataKeyEnum.VERSION.put(resource, record.version);
		ResourceMetadataKeyEnum.UPDATED.put(resource, new InstantDt(record.lastUpdated));
	}
}
