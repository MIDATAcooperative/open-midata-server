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

import models.MidataId;
import models.Record;
import utils.exceptions.AppException;

public class DummyAccessContext extends AccessContext {

	private MidataId selfUser;
	
	
	DummyAccessContext(APSCache cache) {
		super(cache, null);
		selfUser = cache.getAccountOwner();
	}
	
	DummyAccessContext(APSCache cache, MidataId selfUser) {
		super(cache, null);
		this.selfUser = selfUser;
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {		
		return false;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {		
		return false;
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {		
		return false;
	}

	@Override
	public boolean mustPseudonymize() {		
		return false;
	}
	
	@Override
	public boolean mustRename() {		
		return false;
	}

	@Override
	public MidataId getTargetAps() {
		return cache.getAccessor();
	}

	@Override
	public String getOwnerName() {		
		return null;
	}

	@Override
	public MidataId getOwner() {
		return cache.getAccountOwner();
	}

	@Override
	public MidataId getOwnerPseudonymized() {
		return cache.getAccountOwner();
	}

	@Override
	public MidataId getSelf() {
		return selfUser;
	}

	@Override
	public boolean mayAccess(String content, String format) throws AppException {		
		return false;
	}
	
	@Override
	public boolean mayContainRecordsFromMultipleOwners() {		
		return false;
	}

	@Override
	public String toString() {
		return "dummy("+selfUser+")";
	}

	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {	
		return null;
	}

	@Override
	public String getContextName() {
		return "Undefined Context";
	}
	
	

}
