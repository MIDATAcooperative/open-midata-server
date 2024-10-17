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

import java.util.Collections;
import java.util.Set;

import models.MidataId;
import models.Record;
import models.enums.ProjectDataFilter;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.access.Feature_Pseudonymization;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class IndexAccessContext extends AccessContext {

	private MidataId selfUser;
	private boolean pseudonymize;
	private Set<ProjectDataFilter> dataFilters;
	private String salt;
	
	
	public IndexAccessContext(APSCache cache, boolean pseudonymize, Set<ProjectDataFilter> dataFilters) {
		super(cache, null);
		selfUser = cache.getAccountOwner();
		this.pseudonymize = pseudonymize;
		this.dataFilters = dataFilters;
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
	
	public Set<ProjectDataFilter> getProjectDataFilters() throws InternalServerException {
		return dataFilters != null ? dataFilters : Collections.emptySet();
	}
	
	@Override
	public String getSalt() throws AppException {
		if (salt != null) return salt;
		salt = Feature_Pseudonymization.getSalt(this);
		return salt;
	}
	
}
