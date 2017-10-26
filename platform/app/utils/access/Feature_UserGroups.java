package utils.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import models.MidataId;
import models.StudyParticipation;
import models.UserGroupMember;
import models.enums.ResearcherRole;
import utils.AccessLog;
import utils.auth.KeyManager;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;

public class Feature_UserGroups extends Feature {

	private Feature next;

	public Feature_UserGroups(Feature next) {
		this.next = next;
	}
	
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
	
	protected List<DBRecord> doQueryAsGroup(UserGroupMember ugm, Query q) throws AppException {		
		MidataId group = ugm.userGroup;
		AccessLog.logBegin("start user group query for group="+group.toString());
		BasicBSONObject obj = q.getCache().getAPS(ugm._id, ugm.member).getMeta("_usergroup");
		KeyManager.instance.unlock(group, ugm._id, (byte[]) obj.get("aliaskey"));
		Map<String, Object> newprops = new HashMap<String, Object>();
		newprops.putAll(q.getProperties());
		newprops.put("usergroup", ugm.userGroup);
		APSCache subcache = q.getCache().getSubCache(group); 
		if (ugm.role == null) ugm.role = ResearcherRole.HC();
		
		if (q.restrictedBy("export") && !ugm.role.mayExportData()) throw new AuthException("error.notauthorized.export", "You are not allowed to export this data.");
		
		if (!ugm.role.mayReadData()) return Collections.emptyList();
		
		if (ugm.role.pseudonymizedAccess()) {
			
			 if (q.restrictedBy("owner")) {
				   Set<StudyParticipation> parts = StudyParticipation.getActiveParticipantsByStudyAndGroupsAndIds(q.restrictedBy("study") ? q.getMidataIdRestriction("study") : null, q.getRestrictionOrNull("study-group"), group, q.getMidataIdRestriction("owner"), Sets.create("name", "order", "owner", "ownerName", "type"));
				   Set<String> owners = new HashSet<String>();
				   for (StudyParticipation part : parts) {
					  owners.add(part.owner.toString());
				   }
				   newprops.put("owner", owners);
		    }		
			
		}
		MidataId aps = (q.getApsId().equals(ugm.member) || q.getContext() instanceof DummyAccessContext) ? group : q.getApsId();
		Query qnew = new Query(newprops, q.getFields(), subcache, aps, new UserGroupAccessContext(ugm, subcache, q.getContext()));
		List<DBRecord> result = next.query(qnew);
		AccessLog.logEnd("end user group query for group="+group.toString());
		
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
		Set<UserGroupMember> isMemberOfGroups = UserGroupMember.getAllActiveByMember(cache.getAccountOwner());
		if (!isMemberOfGroups.isEmpty()) {
			
			for (UserGroupMember ugm : isMemberOfGroups) {
				if (target.hasAccess(ugm.userGroup)) return ugm;
			}				
		}
		return null;
	}
	
	protected static APSCache findApsCacheToUse(APSCache cache, MidataId targetAps) throws AppException {
		UserGroupMember ugm = identifyUserGroupMember(cache, targetAps);
		return findApsCacheToUse(cache, ugm);			
	}
	
	protected static APSCache findApsCacheToUse(APSCache cache, UserGroupMember ugm) throws AppException {		
		if (ugm == null) return cache;		
		
		BasicBSONObject obj = cache.getAPS(ugm._id, ugm.member).getMeta("_usergroup");
		KeyManager.instance.unlock(ugm.userGroup, ugm._id, (byte[]) obj.get("aliaskey"));		
		return cache.getSubCache(ugm.userGroup);				
	}
		
}
