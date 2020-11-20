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

package utils.buffer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import models.MidataId;
import models.Record;
import models.StudyRelated;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.auth.ExecutionInfo;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * delaying publish records to study results for transactions 
 *
 */
public class StudyPublishBuffer {

	private Set<MidataId> records_to_publish;
	private ExecutionInfo inf;
	private boolean lazy;
		
	/**
	 * enabled or disable delay
	 * @param lazy
	 */
	public void setLazy(boolean lazy) {		
		this.lazy = lazy;
	}
	
	/**
	 * Add record that needs to be published as study result
	 * @param inf
	 * @param record
	 * @throws AppException
	 */
	public void add(ExecutionInfo inf, Record record) throws AppException {
		if (this.inf == null) this.inf = inf;
		if (inf != this.inf) throw new InternalServerException("error.internal", "Execution context changed.");
		if (records_to_publish == null) records_to_publish = new HashSet<MidataId>();
		records_to_publish.add(record._id);
		if (!lazy) save();
	}
	
	/**
	 * publish previously added records
	 * @throws AppException
	 */
	public void save() throws AppException {
		if (records_to_publish == null || records_to_publish.isEmpty()) return;
						
		BSONObject query = RecordManager.instance.getMeta(inf.executorId, inf.targetAPS, "_query");
		if (query != null && query.containsField("target-study")) {
			Map<String, Object> q = query.toMap(); 
			MidataId studyId = MidataId.from(q.get("target-study"));
			AccessLog.log("publishing #recs="+records_to_publish.size()+" to study "+studyId);
			Object groupObj = q.get("target-study-group");
			String group = groupObj != null ? groupObj.toString() : null;
			Set<StudyRelated> srs = StudyRelated.getActiveByOwnerGroupAndStudy(inf.executorId, group, studyId, Sets.create("_id"));
			if (!srs.isEmpty()) {
				for (StudyRelated sr : srs ) {
				  RecordManager.instance.share(inf.executorId, inf.ownerId, sr._id, records_to_publish, false);
				}
			}
		}
		
		records_to_publish.clear();
		this.inf = null;
	}
}
