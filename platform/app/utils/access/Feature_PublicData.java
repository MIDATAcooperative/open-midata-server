package utils.access;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import utils.AccessLog;
import utils.RuntimeConstants;
import utils.auth.KeyManager;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * Allow queries that access a common public data pool
 *
 */
public class Feature_PublicData extends Feature {

	private Feature next;

	public Feature_PublicData(Feature next) {
		this.next = next;
	}
				
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.restrictedBy("public") && q.getApsId().equals(q.getCache().getAccountOwner())) {
			String mode = q.getStringRestriction("public");
			
			// TODO Please remove once ally science is setup correctly
			if (mode.equals("only")) mode = "also";
			// END Remove
			
			if (mode.equals("only")) return doQueryAsPublic(q);
			else if (mode.equals("also")) return new PrivateThenPublicIterator(next, q);										
		}
			
		return next.iterator(q);
	}
	
	class PrivateThenPublicIterator extends Feature.MultiSource<Integer> {
				
		private boolean ispublic;
		
		PrivateThenPublicIterator(Feature next, Query query) throws AppException {
			this.query = query;
			Integer[] steps = {1,2};
			init(ProcessingTools.dbiterator("",  Arrays.asList(steps).iterator()));
		}
		
		@Override
		public DBIterator<DBRecord> advance(Integer step) throws AppException {
			if (step == 1) {				
				ispublic = false;
				return next.iterator(query);
			} else if (step == 2) {
				
				ispublic = true;
				return doQueryAsPublic(query);	
			}
			return null;
		}

		@Override
		public String toString() {
			return "private-public(["+passed+"] "+ispublic+" "+current.toString()+")";
		}
										
	}

	protected DBIterator<DBRecord> doQueryAsPublic(Query q) throws AppException {		
		
		if (q.restrictedBy("owner")) {
			Set<String> owner = q.getRestriction("owner");			
		    if (!owner.contains("all") && !owner.contains(RuntimeConstants.instance.publicUser.toString())) {
			  return ProcessingTools.empty();
		    }
		}
		AccessLog.logBeginPath("do-public", null);
		APSCache subcache = getPublicAPSCache(q.getCache());
		
		Map<String, Object> newprops = new HashMap<String, Object>();
		newprops.putAll(q.getProperties());
		newprops.put("public", "only");
		newprops.remove("usergroup");
		newprops.remove("study");
		newprops.remove("study-group");
		newprops.put("owner", RuntimeConstants.instance.publicUser);
		
		Query qnew = new Query("public",newprops, q.getFields(), subcache, RuntimeConstants.instance.publicUser, new PublicAccessContext(subcache, q.getContext()),q).setFromRecord(q.getFromRecord());
		DBIterator<DBRecord> result = next.iterator(qnew);
		AccessLog.logEndPath(null);
		return result;	
	}
	
	protected static APSCache getPublicAPSCache(APSCache cache) throws InternalServerException {
		if (cache.getAccountOwner().equals(RuntimeConstants.instance.publicUser)) return cache;
		if (!cache.hasSubCache(RuntimeConstants.instance.publicGroup)) KeyManager.instance.unlock(RuntimeConstants.instance.publicGroup, null);
		APSCache subcache = cache.getSubCache(RuntimeConstants.instance.publicGroup);
		subcache.setAccountOwner(RuntimeConstants.instance.publicUser);
		return subcache;
	}
				
		
}
