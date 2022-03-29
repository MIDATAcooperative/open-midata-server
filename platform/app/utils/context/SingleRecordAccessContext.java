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

package utils.context;

import java.util.Map;

import models.MidataId;
import models.Record;
import utils.access.DBRecord;
import utils.collections.CMaps;
import utils.exceptions.AppException;

/**
 * access context that allows only access to a single record
 * @author alexander
 *
 */
public class SingleRecordAccessContext extends AccessContext {

	private MidataId recordId;
	
	public SingleRecordAccessContext(AccessContext parent, MidataId recordId) {
		super(parent.getCache(), parent);
		this.recordId = recordId;
	}

	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {		
		return false;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return true;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {
		return newVersion._id.equals(recordId);
	}

	@Override
	public boolean mustPseudonymize() {
		return parent.mustPseudonymize();
	}

	@Override
	public boolean mustRename() {
		return parent.mustRename();
	}

	@Override
	public boolean mayContainRecordsFromMultipleOwners() {
		return false;
	}

	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		return parent.mayAccess(content, format);
	}

	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {		
		return null;
	}

	@Override
	public MidataId getSelf() {
		return parent.getSelf();
	}

	@Override
	public MidataId getTargetAps() {
		return parent.getTargetAps();
	}

	@Override
	public MidataId getOwner() {
        return parent.getOwner();
	}

	@Override
	public MidataId getOwnerPseudonymized() throws AppException {
        return parent.getOwnerPseudonymized();
	}

	@Override
	public String getOwnerName() throws AppException {
        return parent.getOwnerName();
	}

	@Override
	public String getContextName() {
		return "single-record";
	}
	
	public MidataId getSingleReadableRecord() {
		return recordId;
	}
	
	@Override
	public String toString() {
		return "single-record("+recordId+" "+parentString()+")";
	}

	@Override
	public Map<String, Object> getQueryRestrictions() {
		return CMaps.map("force-local", true);
	}
	
	
	
	
}
