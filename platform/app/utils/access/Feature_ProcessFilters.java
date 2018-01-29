package utils.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import utils.AccessLog;
import utils.exceptions.AppException;

public class Feature_ProcessFilters extends Feature {

	private Feature next;

	public Feature_ProcessFilters(Feature next) {
		this.next = next;
	}

	@Override
	protected Iterator<DBRecord> iterator(Query q) throws AppException {

		Iterator<DBRecord> result = next.iterator(q);

		if (!result.hasNext())
			return result;

		if (q.getFromRecord() != null) result = new ProcessingTools.FilterUntilFromRecord(q.getFromRecord(), ProcessingTools.noDuplicates(result));
		int minTime = q.getMinTime();	

		boolean indexQuery = q.restrictedBy("index") && !q.getApsId().equals(q.getCache().getAccountOwner());

		Map<MidataId, DBRecord> fetchIds = new HashMap<MidataId, DBRecord>();

		if (q.getFetchFromDB() || indexQuery) {
			result = new ProcessingTools.BlockwiseLoad(result, q, 101);
		} else {
			Set<String> check = q.mayNeedFromDB();
			if (!check.isEmpty())
				result = new ProcessingTools.ConditionalLoad(result, q, 101);
		}

		boolean checkDelete = !q.restrictedBy("deleted");

		result = new ProcessingTools.DecryptRecords(result, minTime, checkDelete);

		result = new ProcessingTools.FilterNoMeta(result);

		if (q.restrictedBy("history-date")) {
			result = new Feature_Versioning.HistoryDate(result, q);
		}

		if (q.restrictedBy("creator")) {
			result = new ProcessingTools.FilterByMetaSet(result, "creator", q.getIdRestrictionDB("creator"), false);
		}
		if (q.restrictedBy("app")) {
			result = new ProcessingTools.FilterByMetaSet(result, "app", q.getIdRestrictionDB("app"), q.restrictedBy("no-postfilter-streams"));
		}
		if (q.restrictedBy("name")) {
			result = new ProcessingTools.FilterByMetaSet(result, "name", q.getRestriction("name"), false);
		}
		if (q.restrictedBy("code")) {
			result = new ProcessingTools.FilterSetByMetaSet(result, "code", q.getRestriction("code"), false);
		}

		if (indexQuery) {
			AccessLog.log("Manually applying index query aps=" + q.getApsId().toString());
			result = new ProcessingTools.FilterByDataQuery(result, q.getProperties().get("index"), null);
		}

		if (q.restrictedBy("data")) {
			result = new ProcessingTools.FilterByDataQuery(result, q.getProperties().get("data"), null);
		}

		if (q.getMinDateCreated() != null || q.getMaxDateCreated() != null) {
			result = new ProcessingTools.FilterByDateRange(result, "created", q.getMinDateCreated(), q.getMaxDateCreated());
		}
		if (q.getMinDateUpdated() != null || q.getMaxDateUpdated() != null) {
			result = new ProcessingTools.FilterByDateRange(result, "lastUpdated", q.getMinDateUpdated(), q.getMaxDateUpdated());
		}

		return result;

	}

	/*
	 * @Override protected List<DBRecord> query(Query q) throws AppException {
	 * List<DBRecord> result = next.query(q); return
	 * QueryEngine.postProcessRecordsFilter(q, result); }
	 */

}
