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

import controllers.research.Studies;
import models.MidataId;
import models.Record;
import models.StudyRelated;
import utils.AccessLog;
import utils.access.RecordManager;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * delaying publish records to study results for transactions 
 *
 */
public class StudyPublishBuffer {

	private Set<MidataId> records_to_publish;
	private Set<MidataId> records_to_publish_private;
	private AccessContext context;
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
	public void add(AccessContext context, Record record) throws AppException {
		if (this.context == null) this.context = context;
		if (context != this.context) throw new InternalServerException("error.internal", "Execution context changed.");
		if (records_to_publish == null) records_to_publish = new HashSet<MidataId>();
		records_to_publish.add(record._id);
		if (!lazy) save();
	}
	
	/**
	 * Add record that needs to be part of study export but not public
	 * @param context
	 * @param record
	 * @throws AppException
	 */
	public void addPrivate(AccessContext context, Record record) throws AppException {
		if (this.context == null) this.context = context;
		if (context != this.context) throw new InternalServerException("error.internal", "Execution context changed.");
		if (records_to_publish_private == null) records_to_publish_private = new HashSet<MidataId>();
		records_to_publish_private.add(record._id);
		if (!lazy) save();
	}
	
	/**
	 * publish previously added records
	 * @throws AppException
	 */
	public void save() throws AppException {
		savePrivate();
		if (records_to_publish == null || records_to_publish.isEmpty()) return;
						
		BSONObject query = RecordManager.instance.getMeta(context, context.getTargetAps(), "_query");
		if (query != null && query.containsField("target-study")) {
			Map<String, Object> q = query.toMap(); 
			MidataId studyId = MidataId.from(q.get("target-study"));
			AccessLog.log("publishing #recs=", Integer.toString(records_to_publish.size()), " to study ", studyId.toString());
			Object groupObj = q.get("target-study-group");
			String group = groupObj != null ? groupObj.toString() : null;
			Set<StudyRelated> srs = StudyRelated.getActiveByOwnerGroupAndStudy(context.getAccessor(), group, studyId, Sets.create("_id"));
			if (!srs.isEmpty()) {
				for (StudyRelated sr : srs ) {
				  RecordManager.instance.share(context, context.getLegacyOwner(), sr._id, records_to_publish, false);
				}
			}
		}
		
		records_to_publish.clear();
		this.context = null;
	}
	
	private void savePrivate() throws AppException {
		if (records_to_publish_private == null || records_to_publish_private.isEmpty()) return;
		
		BSONObject query = RecordManager.instance.getMeta(context, context.getTargetAps(), "_query");
		if (query != null && query.containsField("target-study-private")) {
			Map<String, Object> q = query.toMap(); 
			MidataId studyId = MidataId.from(q.get("target-study-private"));
			AccessLog.log("local publishing #recs=", Integer.toString(records_to_publish_private.size()), " to study ", studyId.toString());
			Object groupObj = q.get("target-study-group");
			String group = groupObj != null ? groupObj.toString() : null;
			
			Set<StudyRelated> srs = StudyRelated.getActiveByOwnerGroupAndStudyPrivate(context.getAccessor(), group, studyId, Sets.create("_id"));
			if (!srs.isEmpty()) {
				for (StudyRelated sr : srs ) {
				  RecordManager.instance.share(context, context.getLegacyOwner(), sr._id, records_to_publish_private, false);
				}
			}
		}
		
		records_to_publish_private.clear();		
	}
}
