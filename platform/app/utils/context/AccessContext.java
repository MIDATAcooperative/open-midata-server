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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Consent;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Record;
import models.ServiceInstance;
import models.Space;
import models.UserGroup;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import models.enums.Permission;
import models.enums.ProjectDataFilter;
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
	public abstract boolean mayUpdateRecord(DBRecord stored, Record newVersion) throws InternalServerException;
	
	/**
	 * Must records be pseudonymized in the current context?
	 * @return
	 */
	public abstract boolean mustPseudonymize();
	
	/**
	 * Return set of data filters that must be applied
	 * @return
	 */
	public Set<ProjectDataFilter> getProjectDataFilters() throws InternalServerException {
		if (parent != null) return parent.getProjectDataFilters();
		return Collections.EMPTY_SET;
	}
	
	public String getSalt() throws AppException {
		if (parent != null) return parent.getSalt();
		return null;
	}
	
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
	
	public Map<String, Object> getAccessRestrictions() throws AppException {
		if (parent != null) return parent.getAccessRestrictions(); else return null;
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
	
	public String getOwnerType() {
		return null;
	}
	
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
	 * what team or organization is processing this request? (may be null)
	 * @return
	 * @throws InternalServerException
	 */
	 public MidataId getUserGroupAccessor() {
		 if (isUserGroupContext()) return getAccessor();
		 if (parent != null) return parent.getUserGroupAccessor();
		 return null;
	 }
	
	public EntityType getAccessorEntityType() throws InternalServerException {
		if (parent != null) return parent.getAccessorEntityType();
		
		Plugin pl = Plugin.getById(getUsedPlugin());
		if (pl.type.equals("external") || pl.type.equals("broker") || pl.type.equals("endpoint")) return EntityType.SERVICES;
		return EntityType.USER;
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
		if (consent.owner != null && consent.owner.equals(getAccessor())) return new ConsentAccessContext(consent, getCache(), this);
		if (consent.authorized != null && consent.authorized.contains(getAccessor())) return new ConsentAccessContext(consent, getCache(), this);
		
		if (consent.authorized != null && consent.entityType != EntityType.USER) {
		   for (MidataId id : consent.authorized) {
			   List<UserGroupMember> ugms = getCache().getByGroupAndActiveMember(id, getAccessor(), Permission.ANY);			   
			   if (ugms != null) {
				   AccessContext result = forUserGroup(ugms);				   
				   return new ConsentAccessContext(consent, result.getCache(), result); 
			   }
		   }
		}
		
		AccessLog.log("context="+toString()+" consent="+consent._id);
		throw new InternalServerException("error.internal", "Consent context not createable");
	}
	
	/**
	 * create child context for resharing records from the given Consent
	 * @param consent
	 * @return
	 * @throws AppException
	 */
	public AccessContext forConsentReshare(Consent consent) throws AppException {
		return new CreateParticipantContext(consent, getCache(), getRootContext());		
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
		AccessContext rootContext = getRootContext();
		return new AccountAccessContext(rootContext.getCache(), getRootContext());		
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
		return new ServiceInstanceAccessContext(getCache(), getRequestCache(), instance);
	}
	
	/**
	 * create child context for using the given UserGroup
	 * @param ugm
	 * @return
	 * @throws AppException
	 */
	public UserGroupAccessContext forUserGroup(UserGroupMember ugm) throws AppException {
		boolean pseudo = (this instanceof UserGroupAccessContext) ? mustPseudonymize() : false;
		return new UserGroupAccessContext(ugm, Feature_UserGroups.findApsCacheToUse(getCache(), ugm), this, pseudo);
	}
	
	public UserGroupAccessContext forUserGroup(MidataId userGroup, Permission permission) throws AppException {
		List<UserGroupMember> ugms = cache.getByGroupAndActiveMember(userGroup, cache.getAccessor(), permission);
		return forUserGroup(ugms);		
	}
	
	public UserGroupAccessContext forUserGroup(List<UserGroupMember> ugms) throws AppException {				
		APSCache cache = getCache();
		APSCache subcache = cache;	
		boolean pseudo = (this instanceof UserGroupAccessContext) ? mustPseudonymize() : false;
		for (UserGroupMember ugmx : ugms) {
		    //AccessLog.log("XXXXX > "+ugmx.getConfirmedRole().roleName+" p="+ugmx.getConfirmedRole().pseudonymizedAccess());
		    pseudo = pseudo || ugmx.getConfirmedRole().pseudonymizedAccess();
		    subcache = Feature_UserGroups.readySubCache(cache, subcache, ugmx);
		}
		UserGroupMember ugm = ugms.get(ugms.size()-1);		
		return new UserGroupAccessContext(ugm, subcache, this, pseudo);
	}
	
	public boolean usesUserGroupsForQueries() throws InternalServerException {
		if (getAccessorEntityType() == EntityType.SERVICES) return false;
		if (getAccessorEntityType() == EntityType.USER && getAccessorRole() == UserRole.MEMBER) return false;
		return true;
	}
	
	public Set<UserGroupMember> getAllActiveByMember(Permission permission) throws AppException {
		if (!usesUserGroupsForQueries()) return Collections.emptySet();
		return getCache().getAllActiveByMember(permission);
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
        	  if (!consent.isSharingData() && !consent.owner.equals(getAccessor())) throw new InternalServerException("error.internal",  "Consent-Context creation not possible");
        	  return forConsent(consent);
          }
          
          Space space = Space.getByIdAndOwner(aps, getOwner(), Space.ALL);
          if (space != null) return new SpaceAccessContext(space, getCache(), null, space.owner);
          
          
          List<UserGroupMember> ugms = getCache().getByGroupAndActiveMember(aps, getAccessor(), Permission.READ_DATA);
          if (ugms != null)  return forUserGroup(ugms);
         
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
	
	/**
	 * finished use of context
	 * @throws AppException
	 */
	public void close() throws AppException {
		getCache().finishTouch();
	}
	
	/**
	 * Is it possible to create active consents for accessor with this context?
	 * @return
	 */
	public boolean canCreateActiveConsentsFor(MidataId owner) {
		if (parent != null) return parent.canCreateActiveConsentsFor(owner);
		return cache.getAccountOwner().equals(owner) || owner.equals(getAccessor());
	}
	
	/**
	 * Is it possible to access all data listed in the given filter?
	 * @param targetFilter
	 * @return
	 */
	public boolean hasAccessToAllOf(Map<String, Object> targetFilter) throws AppException {
		if (parent != null) return parent.hasAccessToAllOf(targetFilter);
		return true;
	}
	
	public MidataId getPatientRecordId() {
		if (parent != null) return parent.getPatientRecordId();
		return cache.getAccountOwner();
	}
	
	public boolean isUserGroupContext() {
		if (parent != null) return parent.isUserGroupContext();
		return false;
	}
	
	/**
	 * which entity will sign consent changes done with this context
	 * @return
	 * @throws InternalServerException
	 */
	public MidataId getConsentSigner() throws InternalServerException {
		if (parent != null) return parent.getConsentSigner();
		return null;
	}
	
	/**
	 * get parties that are allowed to manage consents created with this context
	 */
	public Set<MidataId> getManagers() throws InternalServerException {
		Set<MidataId> managers = new HashSet<MidataId>();
		addManagers(managers);
		return managers;
	}
	
	/**
	 * add parties that are allowed to manage consents created with this context
	 * @param managers
	 * @throws InternalServerException
	 */
	public void addManagers(Set<MidataId> managers) throws InternalServerException {
		if (parent != null) parent.addManagers(managers);
	}
	
}
