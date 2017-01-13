package utils.access;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import models.MidataId;
import models.UserGroupMember;
import utils.AccessLog;
import utils.auth.KeyManager;
import utils.exceptions.AppException;

public class Feature_UserGroups extends Feature {

	private Feature next;

	public Feature_UserGroups(Feature next) {
		this.next = next;
	}
	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		if (q.getApsId().equals(q.getCache().getOwner())) {			
			if (!q.isRestrictedToSelf()) {
				Set<UserGroupMember> isMemberOfGroups = UserGroupMember.getAllByMember(q.getCache().getOwner());
				if (!isMemberOfGroups.isEmpty()) {
					List<DBRecord> results = next.query(q);
					for (UserGroupMember ugm : isMemberOfGroups) {
						results.addAll(doQueryAsGroup(ugm, q));
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
		newprops.put("owner", "other"); // XXXXXXXXX
		Query qnew = new Query(newprops, q.getFields(), q.getCache().getSubCache(group), group);
		List<DBRecord> result = next.query(qnew);
		AccessLog.logEnd("end user group query for group="+group.toString());
		
		return result;
	}

}
