package utils.fhir;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Record;
import utils.access.RecordManager;
import utils.access.op.AndCondition;
import utils.access.op.Condition;
import utils.auth.ExecutionInfo;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class Query {

	private Map<String, Object> accountCriteria;
	private Map<String, Condition> indexCriteria;
	private Condition dataCriteria;
	//private List<> postFilters;
	
	public Query() {
		accountCriteria = new HashMap<String, Object>();
		indexCriteria = null;			
	}
	
	public void putAccount(String name, Object obj) {
		accountCriteria.put(name, obj);
	}
	
	public void putDataCondition(Condition obj) {
		if (dataCriteria == null) dataCriteria = obj;
		else {
			dataCriteria = AndCondition.and(dataCriteria, obj);
		}
	}
	
	public void putIndexCondition(Map<String, Condition> indexCondition) {
		if (indexCriteria == null) indexCriteria = indexCondition;		
	}
	
	private static void addRestriction(Map<String, Object> crit, Object content, String[] path, int idx) {
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
	
	public List<Record> execute(ExecutionInfo info) throws AppException {
		if (indexCriteria!=null && !indexCriteria.isEmpty()) {
			accountCriteria.put("index", indexCriteria);
		}
		if (dataCriteria!=null) {
			accountCriteria.put("data", dataCriteria);
		}
		
		List<Record> result = RecordManager.instance.list(info.executorId, info.targetAPS, accountCriteria, Sets.create("owner", "ownerName", "version", "created", "lastUpdated", "data"));
		ReferenceTool.resolveOwners(result, true, false);
		return result;
	}
}
