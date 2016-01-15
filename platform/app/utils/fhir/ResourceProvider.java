package utils.fhir;

import java.util.HashMap;
import java.util.Map;

import utils.auth.ExecutionInfo;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
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
}
