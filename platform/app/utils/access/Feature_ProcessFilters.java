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

package utils.access;

import java.util.Set;

import utils.AccessLog;
import utils.exceptions.AppException;

public class Feature_ProcessFilters extends Feature {

	private Feature next;

	public Feature_ProcessFilters(Feature next) {
		this.next = next;
	}

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {

		DBIterator<DBRecord> result = next.iterator(q);

		if (!result.hasNext())
			return result;

		if (q.getFromRecord() != null) result = new ProcessingTools.FilterUntilFromRecord(q.getFromRecord(), ProcessingTools.noDuplicates(result));
		int minTime = q.getMinTime();	

		boolean indexQuery = q.restrictedBy("index") && !q.getApsId().equals(q.getCache().getAccountOwner());
		
		if (q.getFetchFromDB() || indexQuery) {
			result = new ProcessingTools.BlockwiseLoad(result, q, 101);
		} else {
			Set<String> check = q.mayNeedFromDB();
			if (!check.isEmpty())
				result = new ProcessingTools.ConditionalLoad(result, q, 101);
		}

		boolean checkDelete = !q.restrictedBy("deleted");

		result = new ProcessingTools.DecryptRecords(result, minTime, checkDelete);

		boolean oname = q.returns("ownerName");
		if (q.returns("owner") || oname || q.returns("data") || q.returns("name")) {
			result = new Feature_Pseudonymization.PseudonymIterator(result, q, oname);		
		}
		
		result = new ProcessingTools.FilterNoMeta(result);

		if (q.restrictedBy("history-date")) {
			result = new Feature_Versioning.HistoryDate(result, q);
		}
		
		result = new ProcessingTools.FilterByNonPseudonymizeTag(result);

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
			result = new ProcessingTools.FilterSetByMetaSet(result, "code", q.getRestriction("code"), true);
		}

		if (indexQuery) {
			AccessLog.log("Manually applying index query aps=", q.getApsId().toString());
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
		if (q.getMinDateShared() != null) {
			result = new ProcessingTools.FilterBySharedDate(result, q.getMinDateShared(), null);
		}
		if (q.restrictedBy("remove-hidden")) {
			result = new ProcessingTools.FilterByTag(result, "security:hidden", false);
		}
		if (q.restrictedBy("clear-hidden")) {
			result = new ProcessingTools.ClearByHiddenTag(result);
		}
		
		Set<String> tags = q.getRestrictionOrNull("tag");
		if (tags != null) {
			for (String tag : tags) {
				result = new ProcessingTools.FilterByTag(result, tag, true);
			}
		}

		return result;

	}

	/*
	 * @Override protected List<DBRecord> query(Query q) throws AppException {
	 * List<DBRecord> result = next.query(q); return
	 * QueryEngine.postProcessRecordsFilter(q, result); }
	 */

}
