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

import models.MidataId;
import models.Record;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.exceptions.AppException;

public class IndexAccessContext extends AccessContext {

	private MidataId selfUser;
	private boolean pseudonymize;
	
	
	public IndexAccessContext(APSCache cache, boolean pseudonymize) {
		super(cache, null);
		selfUser = cache.getAccountOwner();
		this.pseudonymize = pseudonymize;
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
		return pseudonymize;
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
		return "index("+selfUser+","+pseudonymize+")";
	}

	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {	
		return null;
	}

	@Override
	public String getContextName() {
		return "Index Access";
	}
	
}
