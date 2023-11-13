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
import models.enums.Permission;
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
	
	
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.restrictedBy("usergroup")) {
			MidataId usergroup = q.getMidataIdRestriction("usergroup").iterator().next();
			
			if (usergroup.equals(q.getCache().getAccountOwner())) return next.iterator(q);
			List<UserGroupMember> isMemberOfGroup = q.getCache().getByGroupAndActiveMember(usergroup, q.getCache().getAccessor(), Permission.READ_DATA);
			if (isMemberOfGroup == null) throw new InternalServerException("error.internal", "Not member of provided user group");
			return doQueryAsGroup(isMemberOfGroup, q);					
		}
		
		if (q.getApsId().equals(q.getCache().getAccountOwner())) {				
			
			if (!q.isRestrictedToSelf()) {
				Set<UserGroupMember> isMemberOfGroups = q.getContext().getAllActiveByMember();
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
			List<UserGroupMember> path = query.getCache().getByGroupAndActiveMember(usergroup, query.getContext().getAccessor(), Permission.READ_DATA);
			if (path == null || path.isEmpty()) return ProcessingTools.empty();
			return doQueryAsGroup(path, query);
			
		}

		@Override
		public String toString() {
			return "usergroups(["+passed+"] "+current.toString()+")";
		}
		
		
		
		
		
	}

	protected DBIterator<DBRecord> doQueryAsGroup(List<UserGroupMember> ugms, Query q) throws AppException {
		if (ugms.isEmpty()) throw new InternalServerException("error.internal", "doQueryAsGroup requires UserGroup");
		MidataId group = null;
		APSCache subcache = q.getCache();
	    UserGroupMember lastUgm = null;
	    UserGroupMember firstUgm = null;
	    boolean mayExportData = true;
	    boolean pseudonymizeAccess = false;
	    boolean mayReadData = true;
	    AccessContext context = q.getContext();
		for (UserGroupMember ugm : ugms) {
			if (firstUgm==null) firstUgm = ugm;
			group = ugm.userGroup;	
			lastUgm = ugm;			
			subcache = readySubCache(q.getCache(), subcache,  ugm);
			if (ugm.role == null) ugm.role = ResearcherRole.HC();
			mayExportData = mayExportData && ugm.role.mayExportData();
			mayReadData = mayReadData && ugm.role.mayReadData();
			pseudonymizeAccess = pseudonymizeAccess || ugm.role.pseudonymizedAccess();
			context = new UserGroupAccessContext(ugm, subcache, context);
			//AccessLog.log("QUERY AS GROUP pA="+pseudonymizeAccess+" context="+context.toString());
		}
		
		Map<String, Object> newprops = new HashMap<String, Object>();
		newprops.putAll(q.getProperties());
		newprops.put("usergroup", lastUgm.userGroup);		
		 				
		if (q.restrictedBy("export")) {
			if (!mayExportData) throw new AuthException("error.notauthorized.export", "You are not allowed to export this data.");
			if (!pseudonymizeAccess) {
				if (q.getStringRestriction("export").equals("pseudonymized")) { pseudonymizeAccess = true; }
			}
		}
						
		if (!mayReadData) return ProcessingTools.empty();		
				
		// AK : Removed instanceof DummyAccessContext : Does not work correctly when listing study participants records on portal		 
		MidataId aps = (q.getApsId().equals(firstUgm.member) /*|| q.getContext() instanceof DummyAccessContext */) ? group : q.getApsId();
		
		AccessLog.logBeginPath("ug("+lastUgm.userGroup+")", null);
		Query qnew = new Query("ug","ug="+lastUgm.userGroup,newprops, q.getFields(), subcache, aps, context ,q).setFromRecord(q.getFromRecord());
		if (pseudonymizeAccess) {
			 AccessLog.log("do pseudonymized");
			 lastUgm.role.pseudo = true;
			 if (!Feature_Pseudonymization.pseudonymizedIdRestrictions(qnew, next, group, newprops)) {
				 AccessLog.logEndPath("cannot unpseudonymize");
				 return ProcessingTools.empty();
			 }
			 qnew = new Query(qnew, "unpseudonymized", newprops);
		} else {
			AccessLog.log("is not pseudonymized");
		}
		
		DBIterator<DBRecord> result = next.iterator(qnew);	
		AccessLog.logEndPath("inited hasNext="+result.hasNext());
		return result;
	}		
	
	protected static MidataId identifyUserGroup(APSCache cache, MidataId targetAps) throws AppException {
		UserGroupMember ugm = identifyUserGroupMember(cache, targetAps);
		return ugm == null ? null : ugm.userGroup;		
	}
	
	protected static UserGroupMember identifyUserGroupMember(APSCache cache, MidataId targetAps) throws InternalServerException {
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
	
	protected static APSCache findApsCacheToUse(APSCache cache, MidataId targetAps) throws InternalServerException {
		APS target = cache.getAPS(targetAps);
		if (target.hasAccess(RuntimeConstants.instance.publicGroup)) return Feature_PublicData.getPublicAPSCache(cache);
		
		UserGroupMember ugm = identifyUserGroupMember(cache, targetAps);
		return findApsCacheToUse(cache, ugm);			
	}
	
	public static APSCache findApsCacheToUse(APSCache cache, UserGroupMember ugm) throws InternalServerException {		
		if (ugm == null) return cache;		
				
		if (!cache.hasSubCache(ugm.userGroup)) {
			APSCache subcache = cache;
			List<UserGroupMember> ugms = cache.getByGroupAndActiveMember(ugm, cache.getAccessor(), Permission.ANY);
			for (UserGroupMember ugmx : ugms) subcache = readySubCache(cache, subcache, ugmx);					
		}
		return cache.getSubCache(ugm.userGroup);				
	}
	
	
			
	public static APSCache readySubCache(APSCache cache, APSCache subcache, UserGroupMember ugm) throws InternalServerException {
		BasicBSONObject obj = subcache.getAPS(ugm._id, ugm.member).getMeta("_usergroup");
		KeyManager.instance.unlock(ugm.userGroup, ugm._id, (byte[]) obj.get("aliaskey"));
		return cache.getSubCache(ugm.userGroup);
	}

	public static void loadKey(AccessContext context, UserGroupMember ugm) throws AppException {				
		
		BSONObject obj = RecordManager.instance.getMeta(context /*ugm.member*/, ugm._id, "_usergroup");
		KeyManager.instance.unlock(ugm.userGroup, ugm._id, (byte[]) obj.get("aliaskey"));				
	}
		
}
