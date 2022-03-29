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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import models.MidataId;
import models.UserGroupMember;
import models.enums.ResearcherRole;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.auth.KeyManager;
import utils.context.AccessContext;
import utils.context.UserGroupAccessContext;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;

public class Feature_UserGroups extends Feature {

	private Feature next;

	public Feature_UserGroups(Feature next) {
		this.next = next;
	}
	
	/*
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		if (q.restrictedBy("usergroup")) {
			MidataId usergroup = q.getMidataIdRestriction("usergroup").iterator().next();
			UserGroupMember isMemberOfGroup = UserGroupMember.getByGroupAndMember(usergroup, q.getCache().getAccountOwner());
			if (isMemberOfGroup == null) throw new InternalServerException("error.internal", "Not member of provided user group");
			return doQueryAsGroup(isMemberOfGroup, q);					
		}
		
		if (q.getApsId().equals(q.getCache().getAccountOwner())) {				
			
			if (!q.isRestrictedToSelf()) {
				Set<UserGroupMember> isMemberOfGroups = q.getCache().getAllActiveByMember();
				if (!isMemberOfGroups.isEmpty()) {
					List<DBRecord> results = next.query(q);
					for (UserGroupMember ugm : isMemberOfGroups) {
						results = QueryEngine.combine(results, doQueryAsGroup(ugm, q));
					}
					return results;
				}
			}
		}
		return next.query(q);
	}
	*/
	
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.restrictedBy("usergroup")) {
			MidataId usergroup = q.getMidataIdRestriction("usergroup").iterator().next();
			
			if (usergroup.equals(q.getCache().getAccountOwner())) return next.iterator(q);
			UserGroupMember isMemberOfGroup = UserGroupMember.getByGroupAndActiveMember(usergroup, q.getCache().getAccountOwner());
			if (isMemberOfGroup == null) throw new InternalServerException("error.internal", "Not member of provided user group");
			return doQueryAsGroup(isMemberOfGroup, q);					
		}
		
		if (q.getApsId().equals(q.getCache().getAccountOwner())) {				
			
			if (!q.isRestrictedToSelf()) {
				Set<UserGroupMember> isMemberOfGroups = q.getCache().getAllActiveByMember();
				if (!isMemberOfGroups.isEmpty()) {
					q.setFromRecord(null);
					List<UserGroupMember> members = new ArrayList<UserGroupMember>(isMemberOfGroups);
					Collections.sort(members);
					members.add(0, null);
					return ProcessingTools.noDuplicates(new UserGroupIterator(q, members));
					
				}
			}
		}
		return next.iterator(q);
	}
	
	class UserGroupIterator extends Feature.MultiSource<UserGroupMember> {
				
		UserGroupIterator(Query query, List<UserGroupMember> groups) throws AppException {
			this.query = query;
			init(groups.iterator());
		}
		
		@Override
		public DBIterator<DBRecord> advance(UserGroupMember usergroup) throws AppException {
			
			if (usergroup == null) return next.iterator(query);
			return doQueryAsGroup(usergroup, query);
			
		}

		@Override
		public String toString() {
			return "usergroups(["+passed+"] "+current.toString()+")";
		}
		
		
		
		
		
	}

	protected DBIterator<DBRecord> doQueryAsGroup(UserGroupMember ugm, Query q) throws AppException {		
		MidataId group = ugm.userGroup;
		//AccessLog.logBegin("start user group query for group="+group.toString());
		BasicBSONObject obj = q.getCache().getAPS(ugm._id, ugm.member).getMeta("_usergroup");
		KeyManager.instance.unlock(group, ugm._id, (byte[]) obj.get("aliaskey"));
		Map<String, Object> newprops = new HashMap<String, Object>();
		newprops.putAll(q.getProperties());
		newprops.put("usergroup", ugm.userGroup);		
		APSCache subcache = q.getCache().getSubCache(group); 
		if (ugm.role == null) ugm.role = ResearcherRole.HC();
		
		if (q.restrictedBy("export")) {
			if (!ugm.role.mayExportData()) throw new AuthException("error.notauthorized.export", "You are not allowed to export this data.");
			if (!ugm.role.pseudonymizedAccess()) {
				if (q.getStringRestriction("export").equals("pseudonymized")) { ugm.role.pseudo = true; }
			}
		}
		
		if (!ugm.role.mayReadData()) return ProcessingTools.empty();		
				
		// AK : Removed instanceof DummyAccessContext : Does not work correctly when listing study participants records on portal		 
		MidataId aps = (q.getApsId().equals(ugm.member) /*|| q.getContext() instanceof DummyAccessContext */) ? group : q.getApsId();
		
		AccessLog.logBeginPath("ug("+ugm.userGroup+")", null);
		Query qnew = new Query("ug","ug="+ugm.userGroup,newprops, q.getFields(), subcache, aps, new UserGroupAccessContext(ugm, subcache, q.getContext()),q).setFromRecord(q.getFromRecord());
		if (ugm.role.pseudonymizedAccess()) {
			 AccessLog.log("do pseudonymized");
			 if (!Feature_Pseudonymization.pseudonymizedIdRestrictions(qnew, next, group, newprops)) {
				 AccessLog.logEndPath("cannot unpseudonymize");
				 return ProcessingTools.empty();
			 }
			 qnew = new Query(qnew, "unpseudonymized", newprops);
		} else {
			AccessLog.log("is not pseudonymized");
		}
		
		DBIterator<DBRecord> result = next.iterator(qnew);
		//AccessLog.logEnd("end user group query for group="+group.toString());
		AccessLog.logEndPath("inited hasNext="+result.hasNext());
		return result;
	}
	
	protected static MidataId identifyUserGroup(APSCache cache, MidataId targetAps) throws AppException {
		UserGroupMember ugm = identifyUserGroupMember(cache, targetAps);
		return ugm == null ? null : ugm.userGroup;		
	}
	
	protected static UserGroupMember identifyUserGroupMember(APSCache cache, MidataId targetAps) throws AppException {
		APS target = cache.getAPS(targetAps);
		if (target.isAccessible()) {
			return null;
		}
		Set<UserGroupMember> isMemberOfGroups = cache.getAllActiveByMember();
		if (!isMemberOfGroups.isEmpty()) {
			
			for (UserGroupMember ugm : isMemberOfGroups) {
				if (target.hasAccess(ugm.userGroup)) return ugm;
			}				
		}
		
		AccessLog.log("no suitable cache: targetAps=", targetAps.toString(), " executor=", cache.getAccessor().toString());
		return null;
	}
	
	protected static APSCache findApsCacheToUse(APSCache cache, MidataId targetAps) throws AppException {
		APS target = cache.getAPS(targetAps);
		if (target.hasAccess(RuntimeConstants.instance.publicGroup)) return Feature_PublicData.getPublicAPSCache(cache);
		
		UserGroupMember ugm = identifyUserGroupMember(cache, targetAps);
		return findApsCacheToUse(cache, ugm);			
	}
	
	public static APSCache findApsCacheToUse(APSCache cache, UserGroupMember ugm) throws AppException {		
		if (ugm == null) return cache;		
		
		BasicBSONObject obj = cache.getAPS(ugm._id, ugm.member).getMeta("_usergroup");
		if (!cache.hasSubCache(ugm.userGroup)) KeyManager.instance.unlock(ugm.userGroup, ugm._id, (byte[]) obj.get("aliaskey"));		
		return cache.getSubCache(ugm.userGroup);				
	}

	public static void loadKey(AccessContext context, UserGroupMember ugm) throws AppException {				
		
		BSONObject obj = RecordManager.instance.getMeta(context /*ugm.member*/, ugm._id, "_usergroup");
		KeyManager.instance.unlock(ugm.userGroup, ugm._id, (byte[]) obj.get("aliaskey"));				
	}
		
}
