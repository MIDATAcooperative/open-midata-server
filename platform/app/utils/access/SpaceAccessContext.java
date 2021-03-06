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

import java.util.Map;

import org.bson.BSONObject;

import controllers.Circles;
import models.MidataId;
import models.Record;
import models.Space;
import utils.exceptions.AppException;

public class SpaceAccessContext extends AccessContext {

	private Space space;
	private MidataId self;
	private boolean sharingQuery;
	private Map<String, Object> restrictions;
		
	
	public SpaceAccessContext(Space space, APSCache cache, AccessContext parent, MidataId self) {
		super(cache, parent);
		this.space = space;
		this.self = self;
	}
	
	public SpaceAccessContext withRestrictions(Map<String, Object> restrictions) {
		this.restrictions = restrictions;
		return this;
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		return parent==null || parent.mayCreateRecord(record);
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {
		return true;
	}

	@Override
	public boolean mustPseudonymize() {	
		return false;
	}
	@Override
	public MidataId getTargetAps() {
		return space._id;
	}
	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		return record.owner.equals(space.owner);
	}
	@Override
	public String getOwnerName() {		
		return null;
	}
	@Override
	public MidataId getOwner() {
		return space.owner; //cache.getAccountOwner();
	}
	@Override
	public MidataId getOwnerPseudonymized() {
		return space.owner;
	}
	@Override
	public MidataId getSelf() {
		return self;
	}
	
	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		if (!sharingQuery && space.query == null) {
			  BSONObject q = RecordManager.instance.getMeta(cache.getExecutor(), space._id, "_query");
			  if (q != null) space.query = q.toMap();			  
			  sharingQuery = true;
		}
		
		return Feature_FormatGroups.mayAccess(space.query, content, format);	
	}
	
	public Space getInstance() {
		return space;
	}
	
	public Map<String, Object> getQueryRestrictions() {
		return restrictions;
	}
	
	@Override
	public boolean mayContainRecordsFromMultipleOwners() {		
		return true;
	}
	
	@Override
	public String toString() {
		return "space("+space._id+" "+parentString()+")";
	}

	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {
		return Feature_FormatGroups.getAccessRestriction(space.query, content, format, field);
	}

	@Override
	public String getContextName() {
		return "Plugin-Space '"+space.name+"'";
	}

}
