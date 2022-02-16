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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.uhn.fhir.rest.api.SortOrderEnum;
import models.Record;
import utils.access.DBIterator;
import utils.access.RecordManager;
import utils.access.op.AndCondition;
import utils.access.op.CompareCaseInsensitive;
import utils.access.op.Condition;
import utils.access.op.EqualsSingleValueCondition;
import utils.access.op.FieldAccess;
import utils.access.op.OrCondition;
import utils.access.AccessContext;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * A query built by the FHIR API that should be processed by MIDATA.
 *
 */
public class Query {

	private Map<String, Object> accountCriteria;
	private Condition indexCriteria;
	private Condition dataCriteria;
	private String[] sorts;
	//private List<> postFilters;
	
	public Query() {
		accountCriteria = new HashMap<String, Object>();
		indexCriteria = null;			
	}
	
	public void putAccount(String name, Object obj) throws InternalServerException {
		if (accountCriteria.containsKey(name)) throw new InternalServerException("error.internal", "criteria already used in query: "+name);
		accountCriteria.put(name, obj);
	}
	
	public void removeAccount(String name) {
		accountCriteria.remove(name);
	}
	
	public void putDataCondition(Condition obj) {
		if (dataCriteria == null) dataCriteria = obj;
		else {
			dataCriteria = AndCondition.and(dataCriteria, obj);
		}
	}
	
	public void putIndexCondition(Condition indexCondition) {
		if (indexCriteria == null) {
			indexCriteria = indexCondition;		
		} else indexCriteria = AndCondition.and(indexCriteria, indexCondition);
	}
	
	public void initSort(String[] sorts) {
		this.sorts = sorts;
	}
	
	public void putSort(String name, SortOrderEnum direction, String path) {
		for (int i=0;i<sorts.length;i++) {
			if (sorts[i].equals(name)) sorts[i] = path+((direction == SortOrderEnum.ASC) ?" asc":" desc");
		}
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
	
	public List<Record> execute(AccessContext info) throws AppException {
		if (indexCriteria!=null) {
			accountCriteria.put("index", indexCriteria);
		}
		if (dataCriteria!=null) {
			accountCriteria.put("data", dataCriteria);
		}
		if (sorts!=null) {
			accountCriteria.put("sort", String.join(",", sorts));
		}
				
		List<Record> result = RecordManager.instance.list(info.getAccessorRole(), info, accountCriteria, Sets.create("owner", "ownerName", "version", "created", "lastUpdated", "data"));
		
		return result;
	}
	
	public DBIterator<Record> executeIterator(AccessContext info) throws AppException {
		if (indexCriteria!=null) {
			accountCriteria.put("index", indexCriteria);
		}
		if (dataCriteria!=null) {
			accountCriteria.put("data", dataCriteria);
		}
		if (sorts!=null) {
			accountCriteria.put("sort", String.join(",", sorts));
		}
				
		return RecordManager.instance.listIterator(info.getAccessor(), info.getAccessorRole(), info, accountCriteria, Sets.create("owner", "ownerName", "version", "created", "lastUpdated", "data"));
				
	}
	
	public Map<String, Object> retrieveAsNormalMongoQuery() throws AppException {		
		Map<String,Object> result = new HashMap<String, Object>();
		result.putAll(accountCriteria);
		result.remove("limit");
		result.remove("from");
		result.remove("updated-after");
		result.remove("updated-before");
		if (dataCriteria != null) result.putAll((Map<String,Object>) dataCriteria.asMongoQuery());
		ObjectIdConversion.convertMidataIds(result, "_id");
		return result;
	}
	
	public Map<String, Object> getAccountCriteria() {
		return accountCriteria;
	}
	
	
	public Object retrieveIndexValues() throws AppException {		
		if (indexCriteria != null) {
			Set<String> results = new HashSet<String>();
            retrieveIndexValues(results, indexCriteria);
            if (results.isEmpty()) return null;
			return results;
		}
		return null;
	}
	
	private void retrieveIndexValues(Set<String> values, Condition cond) {
		if (cond instanceof CompareCaseInsensitive) {
			CompareCaseInsensitive cond2 = (CompareCaseInsensitive) cond;
			values.add(cond2.getValue().toString());
		}
		if (cond instanceof EqualsSingleValueCondition) {
			values.add(((EqualsSingleValueCondition) cond).getValue().toString());
		}
		if (cond instanceof AndCondition) {
			for (Condition c : ((AndCondition) cond).getParts()) {
				retrieveIndexValues(values, c);
			}
		}
		if (cond instanceof OrCondition) {
			for (Condition c : ((OrCondition) cond).getParts()) {
				retrieveIndexValues(values, c);
			}
		}
		if (cond instanceof FieldAccess) {
			retrieveIndexValues(values, ((FieldAccess) cond).getCondition());
		}
	}
}
