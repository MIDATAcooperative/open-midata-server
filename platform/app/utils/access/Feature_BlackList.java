package utils.access;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bson.BasicBSONObject;

import utils.exceptions.AppException;

/**
 * access permission sets may contain a blacklist of record IDs that are never contained in this APS.
 * (makes only sense with query based access permission sets)
 *
 */
public class Feature_BlackList extends Feature {

    private Feature next;
    private Set<String> blacklist; 
	
	public Feature_BlackList(APS target, Feature next) throws AppException {
		this.next = next;
		initBlacklist(target);
	}
	
	private void initBlacklist(APS target) throws AppException {
		blacklist = new HashSet<String>();
		BasicBSONObject list = target.getMeta("_exclude");
		if (list != null) {
			Collection idlist = (Collection) list.get("ids");
			for (Object id : idlist) {
				blacklist.add(id.toString());
			}
		}
	}
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (blacklist.isEmpty()) return next.iterator(q);
		return new BlackListIterator(next.iterator(q));
		
		
	}
		
	class BlackListIterator implements DBIterator<DBRecord> {
		BlackListIterator(DBIterator<DBRecord> chain) {
			this.chain = chain;
		}
		
		private DBRecord next;
		private DBIterator<DBRecord> chain;

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public DBRecord next() throws AppException {
			DBRecord result = next;
			next = null;
			while (next == null && chain.hasNext()) {
				DBRecord record = chain.next();
				if (!blacklist.contains(record._id.toString())) next = record;	
			}			
			return result;
		}	
		
		@Override
		public String toString() {
			return "blacklist("+chain.toString()+")";
		}
		
	}
		
}
