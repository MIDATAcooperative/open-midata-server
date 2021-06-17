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

import models.Consent;
import models.MidataId;
import models.Record;
import models.ServiceInstance;
import models.Space;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import utils.RuntimeConstants;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public abstract class AccessContext {
	
	protected AccessContext parent;
	protected APSCache cache;
	
	AccessContext(APSCache cache, AccessContext parent) {
		this.cache = cache;
		this.parent = parent;
	}

	public abstract boolean mayCreateRecord(DBRecord record) throws AppException;
	
	public abstract boolean isIncluded(DBRecord record) throws AppException;
	
	public abstract boolean mayUpdateRecord(DBRecord stored, Record newVersion);
	
	public abstract boolean mustPseudonymize();
	
	public abstract boolean mustRename();
	
	public abstract boolean mayContainRecordsFromMultipleOwners();
	
	public abstract boolean mayAccess(String content, String format) throws AppException;
	
	public abstract Object getAccessRestriction(String content, String format, String field) throws AppException; 
	
	public boolean produceHistory() {
		return true;
	}
	
	public abstract MidataId getSelf();
	
	public abstract MidataId getTargetAps();
	
	/**
	 * who is the owner of data with this context?
	 * @return
	 */
	public abstract MidataId getOwner();
	
	/**
	 * who is the pseudonymized owner of data with this context?
	 * @return
	 * @throws AppException
	 */
	public abstract MidataId getOwnerPseudonymized() throws AppException;
	
	/**
	 * what is the name of the owner (possibly pseudonymized)
	 * @return
	 * @throws AppException
	 */
	public abstract String getOwnerName() throws AppException;
	
	public Map<String, Object> getQueryRestrictions() {
		if (parent != null) return parent.getQueryRestrictions(); else return null;
	}
	
	public AccessContext getParent() {
		return parent;
	}
	
	public APSCache getCache() {
		return cache;
	}
	
	public APSCache getRootCache() {
		if (parent != null) return parent.getRootCache();
		return getCache();
	}

	/**
	 * who is the actor, e.g. the person creating records or creating audit events
	 * @return
	 */
	public MidataId getActor() {
		if (parent != null) return parent.getActor();
		return cache.getAccessor();
	}
	
	/**
	 * who is the accessor, e.g. entity with the primary key unlocked
	 * @return
	 */
	public MidataId getAccessor() {
		return cache.getAccessor();
	}
	
	protected String parentString() {
		if (parent==null) return "(cache "+cache.getAccessor()+"/"+cache.getAccountOwner()+")";
		return parent.toString();
	}
	
	public abstract String getContextName();
	
	public String getAccessInfo(DBRecord rec) throws AppException {
		return "";
	}
	
	
	
	public String getMayUpdateReport(DBRecord stored, Record newVersion) throws AppException {
		boolean result = mayUpdateRecord(stored, newVersion);
		String report = getContextName()+" "+getAccessInfo(stored)+": result mayUpdate="+result;
		if (parent != null) return parent.getMayUpdateReport(stored, newVersion)+"\n"+report;
		return report;
	}
	
	public String getMayCreateRecordReport(DBRecord record) throws AppException {
		boolean result = mayCreateRecord(record);
		String report = getContextName()+" "+getAccessInfo(record)+": result mayCreate="+result;
		if (parent != null) return parent.getMayCreateRecordReport(record)+"\n"+report;
		return report;
	}
	
	public AccessContext forConsent(Consent consent) throws AppException {		
		return new ConsentAccessContext(consent, getCache(), this);
	}
	
	public AccessContext forConsentReshare(Consent consent) throws AppException {
		return new ConsentAccessContext(consent, getCache(), null);
	}
	
	public AccessContext forPublic() throws AppException {
		return new PublicAccessContext(Feature_PublicData.getPublicAPSCache(getRootCache()), this);
	}
	
	public AccessContext forServiceInstance(ServiceInstance instance) throws AppException {
		return new ServiceInstanceAccessContext(getCache(), instance);
	}
	
	public UserGroupAccessContext forUserGroup(UserGroupMember ugm) throws AppException {
		return new UserGroupAccessContext(ugm, Feature_UserGroups.findApsCacheToUse(getCache(), ugm), this);
	}
	
	public AccessContext forAps(MidataId aps) throws AppException {
		if (getTargetAps().equals(aps)) return this;
		else if (aps.equals(RuntimeConstants.instance.publicUser)) return forPublic();
		else {
          Consent consent = Consent.getByIdUnchecked(aps, Consent.ALL);
          if (consent != null) {
        	  if (consent.status != ConsentStatus.ACTIVE && consent.status != ConsentStatus.FROZEN && !consent.owner.equals(getAccessor())) throw new InternalServerException("error.internal",  "Consent-Context creation not possible");
        	  return forConsent(consent);
          }
          
          Space space = Space.getByIdAndOwner(aps, getOwner(), Space.ALL);
          if (space != null) return new SpaceAccessContext(space, getCache(), null, space.owner);
		}
		
		throw new InternalServerException("error.internal",  "Consent creation not possible");
	}
	
	public AccessContext forApsReshare(MidataId aps) throws AppException {
		return forAps(aps);
	}
	
}
