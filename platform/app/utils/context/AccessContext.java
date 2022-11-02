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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import models.Consent;
import models.MidataId;
import models.MobileAppInstance;
import models.Record;
import models.ServiceInstance;
import models.Space;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.UserRole;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.access.Feature_PublicData;
import utils.access.Feature_UserGroups;
import utils.collections.RequestCache;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public abstract class AccessContext {
	
	protected AccessContext parent;
	protected APSCache cache;
	
	AccessContext(APSCache cache, AccessContext parent) {
		this.cache = cache;
		this.parent = parent;
	}

	/**
	 * Is it allowed to create this record in the current context?
	 * @param record
	 * @return
	 * @throws AppException
	 */
	public abstract boolean mayCreateRecord(DBRecord record) throws AppException;
	
	/**
	 * Is the given record already included in this context?
	 * Otherwise sharing might be needed to include it
	 * @param record
	 * @return
	 * @throws AppException
	 */
	public abstract boolean isIncluded(DBRecord record) throws AppException;
	
	/**
	 * Is it allowed to update this record in the current context?
	 * @param stored old version of record already stored
	 * @param newVersion new version of record
	 * @return
	 */
	public abstract boolean mayUpdateRecord(DBRecord stored, Record newVersion);
	
	/**
	 * Must records be pseudonymized in the current context?
	 * @return
	 */
	public abstract boolean mustPseudonymize();
	
	/**
	 * Must the owner/creator of records be renamed? This is similar to pseudonymization but not the same.
	 * It is used for data shared by projects. It is not desired that the researcher that has created
	 * the data is displayed as owner to project participants. Instead the project name should be used.  
	 * @return
	 */
	public abstract boolean mustRename();
	
	/**
	 * May this context contain records for more than one owner?
	 * @return
	 */
	public abstract boolean mayContainRecordsFromMultipleOwners();
	
	/**
	 * Is it allowed to access this combination of record content and format in the current context?
	 * @param content
	 * @param format
	 * @return
	 * @throws AppException
	 */
	public abstract boolean mayAccess(String content, String format) throws AppException;
	
	/**
	 * Search this contexts access filter for a restriction to the given content and format and return
	 * additional restrictions to the provided field
	 * @param content content-type to search access filter for
	 * @param format format to search access filter for
	 * @param field name of field to return additional restrictions 
	 * @return
	 * @throws AppException
	 */
	public abstract Object getAccessRestriction(String content, String format, String field) throws AppException;
	
	public List<String> getAccessRestrictionList(String content, String format, String field) throws AppException {
		Object result = getAccessRestriction(content, format, field);
		if (result == null) return Collections.emptyList();
		if (result instanceof String) return Collections.singletonList((String) result);
		if (result instanceof List) return (List<String>) result;
		if (result instanceof Collection) return new ArrayList<String>((Collection) result);
		throw new InternalServerException("error.internal", "Unknown restriction");
	}
	
	/**
	 * create history records during updates with this context?
	 * @return
	 */
	public boolean produceHistory() {
		return true;
	}
	
	/**
	 * return target user for query restrictions to 'self' 
	 * @return
	 */
	public abstract MidataId getSelf();
	
	/**
	 * returns main access permission set to runs queries using this context against
	 * @return
	 */
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
	
	/**
	 * return restrictions that must be added to query during query processing
	 * @return
	 */
	public Map<String, Object> getQueryRestrictions() {
		if (parent != null) return parent.getQueryRestrictions(); else return null;
	}
	
	/**
	 * Get parent context or null
	 * @return
	 */
	public AccessContext getParent() {
		return parent;
	}
	
	/**
	 * get APSCache for this context
	 * @return
	 */
	public APSCache getCache() {
		return cache;
	}
	
	/**
	 * get RequestCache for current request 
	 * @return
	 */
	public RequestCache getRequestCache() {
		if (parent != null) return parent.getRequestCache();
		return null;
	}
	
	/**
	 * get top-level APSCache
	 * @return
	 */
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
	
	/**
	 * what is the user role of the accessor?
	 * This is used for handling security tags
	 * @return
	 */
	public UserRole getAccessorRole() {
		if (parent != null) return parent.getAccessorRole();
		return UserRole.ANY;
	}
	
	/**
	 * helper function for creating log lines
	 * @return
	 */
	protected String parentString() {
		if (parent==null) return "(cache "+cache.getAccessor()+"/"+cache.getAccountOwner()+")";
		return parent.toString();
	}
	
	/**
	 * helper function for creating bug reports
	 * @return
	 */
	public abstract String getContextName();
	
	/**
	 * helper function for creating bug reports
	 * @param rec
	 * @return
	 * @throws AppException
	 */
	public String getAccessInfo(DBRecord rec) throws AppException {
		return "";
	}
		
	/**
	 * helper function for creating bug reports
	 * @param stored
	 * @param newVersion
	 * @return
	 * @throws AppException
	 */
	public String getMayUpdateReport(DBRecord stored, Record newVersion) throws AppException {
		boolean result = mayUpdateRecord(stored, newVersion);
		String report = getContextName()+getAccessInfo(stored)+"\n- Conclusion: is update allowed for this entry of the chain? ["+result+"]\n";
		if (parent != null) return parent.getMayUpdateReport(stored, newVersion)+"\n"+report;
		return report;
	}
	
	/**
	 * helper function for creating bug reports
	 * @param record
	 * @return
	 * @throws AppException
	 */
	public String getMayCreateRecordReport(DBRecord record) throws AppException {
		boolean result = mayCreateRecord(record);
		String report = getContextName()+getAccessInfo(record)+"\n- Conclusion: is creation allowed for this entry of the chain? ["+result+"]\n";
		if (parent != null) return parent.getMayCreateRecordReport(record)+"\n"+report;
		return report;
	}
	
	/**
	 * create child context that uses the given Context
	 * @param consent
	 * @return
	 * @throws AppException
	 */
	public AccessContext forConsent(Consent consent) throws AppException {		
		return new ConsentAccessContext(consent, getCache(), this);
	}
	
	/**
	 * create child context for resharing records from the given Consent
	 * @param consent
	 * @return
	 * @throws AppException
	 */
	public AccessContext forConsentReshare(Consent consent) throws AppException {
		return new ConsentAccessContext(consent, getCache(), null);
	}
	
	/**
	 * create child context for working with public data
	 * @return
	 * @throws AppException
	 */
	public AccessContext forPublic() throws AppException {
		return new PublicAccessContext(Feature_PublicData.getPublicAPSCache(getRootCache()), this);
	}
	
	/**
	 * create child context for accessors user account
	 * @return
	 * @throws AppException
	 */
	public AccessContext forAccount() {
		return new AccountAccessContext(getCache(), this);
	}
	
	/**
	 * returns top-level context
	 * @return
	 */
	protected AccessContext getRootContext() {
		if (parent != null) return parent.getRootContext();
		return this;
	}
	
	/**
	 * create child context for user account with full access for resharing records to another consent
	 * @return
	 */
	public AccessContext forAccountReshare() {
		return new AccountAccessContext(getRootCache(), getRootContext());		
	}
	
	/**
	 * create child context that uses a specific app for accessing
	 * @param app
	 * @return
	 * @throws AppException
	 */
	public AccessContext forApp(MobileAppInstance app) throws AppException {	
		throw new NullPointerException();
	}
	
	/**
	 * create child context that uses a specific Space for accessing
	 * @param space
	 * @param self
	 * @return
	 */
	public AccessContext forSpace(Space space, MidataId self) {		
		throw new NullPointerException();		
	}
	
	/**
	 * create child context for using the given ServiceInstance
	 * @param instance
	 * @return
	 * @throws AppException
	 */
	public AccessContext forServiceInstance(ServiceInstance instance) throws AppException {
		return new ServiceInstanceAccessContext(getCache(), instance);
	}
	
	/**
	 * create child context for using the given UserGroup
	 * @param ugm
	 * @return
	 * @throws AppException
	 */
	public UserGroupAccessContext forUserGroup(UserGroupMember ugm) throws AppException {
		return new UserGroupAccessContext(ugm, Feature_UserGroups.findApsCacheToUse(getCache(), ugm), this);
	}
	
	/**
	 * create child context for the given apsId. Tries to determine if the given apsId is a consent or space.
	 * @param aps
	 * @return
	 * @throws AppException
	 */
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
	
	/**
	 * create child context for resharing records from the given aps
	 * @param aps
	 * @return
	 * @throws AppException
	 */
	public AccessContext forApsReshare(MidataId aps) throws AppException {
		return forAps(aps);
	}
	
	/**
	 * create child context that can only access one record
	 * @param recordId
	 * @return
	 */
	public AccessContext forSingleRecord(MidataId recordId) {
		return new SingleRecordAccessContext(this, recordId);
	}
	
	/**
	 * is this a view on a single record?
	 * @return
	 */
	public MidataId getSingleReadableRecord() {
		if (parent != null) return parent.getSingleReadableRecord();
		return null;
	}
	
	/**
	 * Is this a request using a user defined FHIR endpoint?
	 * @return
	 */
	public String getOverrideBaseUrl() {
		if (parent != null) return parent.getOverrideBaseUrl();
		return null;
	}
	
	/**
	 * which plugin is used to perform actions with this context?
	 * @return
	 */
	public MidataId getUsedPlugin() {
		if (parent != null) return parent.getUsedPlugin();
		return RuntimeConstants.instance.portalPlugin; 
	}
	
	/**
	 * Data owner as provided by session token. Only used for compatibility with some old code.
	 * @return
	 */
	public MidataId getLegacyOwner() {
		if (parent != null) return parent.getLegacyOwner();
		return getOwner();
	}
	
	/**
	 * clear current cache
	 */
	public void clearCache() {
		try {
			getCache().finishTouch();
		} catch (AppException e) {
			AccessLog.logException("clearCache", e);
		}
		getCache().clear();
	}
	
	/**
	 * return context used for query engine internal queries without the user requests context
	 * @return
	 */
	public AccessContext internal() {
		return new DummyAccessContext(getCache());
	}
	
	/**
	 * cleanup context after use
	 */
	public void cleanup() {
		if (parent != null) parent.cleanup();
	}
	
}
