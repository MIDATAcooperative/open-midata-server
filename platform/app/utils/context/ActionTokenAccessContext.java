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
import models.enums.UserRole;
import utils.RuntimeConstants;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.auth.ActionToken;
import utils.collections.RequestCache;
import utils.exceptions.AppException;

/**
 * Context for an requests that are using an action token and no logged in user
 *
 */
public class ActionTokenAccessContext extends AccessContext {
	
	private ActionToken token;
	private RequestCache requestCache = new RequestCache();
	
	
	public ActionTokenAccessContext(ActionToken token) {
		super(new APSCache(null, null), null);
		this.token = token;
	}
	
	public ActionToken getActionToken() {
		return token;
	}

	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		throw new NullPointerException();
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		throw new NullPointerException();
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {
		throw new NullPointerException();
	}

	@Override
	public boolean mustPseudonymize() {
		throw new NullPointerException();
	}

	@Override
	public boolean mustRename() {
		throw new NullPointerException();
	}

	@Override
	public boolean mayContainRecordsFromMultipleOwners() {
		return true;
	}

	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		throw new NullPointerException();
	}

	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {
		throw new NullPointerException();
	}

	@Override
	public MidataId getSelf() {
		throw new NullPointerException();
	}

	@Override
	public MidataId getTargetAps() {
		throw new NullPointerException();
	}

	@Override
	public MidataId getOwner() {
		throw new NullPointerException();
	}

	@Override
	public MidataId getOwnerPseudonymized() throws AppException {
		throw new NullPointerException();
	}

	@Override
	public String getOwnerName() throws AppException {
		throw new NullPointerException();
	}

	@Override
	public String getContextName() {
		return "Action("+token.action.toString()+")";
	}

	@Override
	public String getOwnerType() {
		throw new NullPointerException();
	}

	@Override
	public RequestCache getRequestCache() {
		return requestCache;
	}

	
	@Override
	public MidataId getActor() {
		return RuntimeConstants.instance.anonymousUser;
	}

	@Override
	public MidataId getAccessor() {
		return RuntimeConstants.instance.anonymousUser;
	}

	@Override
	public UserRole getAccessorRole() {
		throw new NullPointerException();
	}

	@Override
	protected AccessContext getRootContext() {
		return this;
	}

	@Override
	public MidataId getUsedPlugin() {
		throw new NullPointerException();
	}

	@Override
	public MidataId getLegacyOwner() {
		throw new NullPointerException();	}

	@Override
	public boolean canCreateActiveConsentsFor(MidataId owner) {
		return false;
	}
	
	public boolean hasAccessToAllOf(Map<String, Object> targetFilter) {
		return false;
	}
	
	

}
