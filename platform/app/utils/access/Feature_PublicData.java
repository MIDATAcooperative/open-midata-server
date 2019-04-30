package utils.access;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import utils.RuntimeConstants;
import utils.auth.KeyManager;
import utils.exceptions.AppException;

public class Feature_PublicData extends Feature {

	private Feature next;

	public Feature_PublicData(Feature next) {
		this.next = next;
	}
				
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.restrictedBy("public") && q.getApsId().equals(q.getCache().getAccountOwner())) {
			String mode = q.getStringRestriction("public");
			
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
		
		KeyManager.instance.unlock(RuntimeConstants.instance.publicUser, null);
		APSCache subcache = q.getCache().getSubCache(RuntimeConstants.instance.publicUser);
		
		Map<String, Object> newprops = new HashMap<String, Object>();
		newprops.putAll(q.getProperties());
		newprops.put("public", "only");
		newprops.remove("usergroup");
		
		Query qnew = new Query(newprops, q.getFields(), subcache, RuntimeConstants.instance.publicUser, new PublicAccessContext(subcache, q.getContext())).setFromRecord(q.getFromRecord());
		DBIterator<DBRecord> result = next.iterator(qnew);
		
		return result;	
	}
				
		
}
