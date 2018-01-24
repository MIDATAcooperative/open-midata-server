package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import utils.AccessLog;
import utils.access.op.AndCondition;
import utils.access.op.Condition;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class ProcessingTools {

	public static Iterator<DBRecord> sort(Map<String, Object> properties, Iterator<DBRecord> input) {
		if (!properties.containsKey("sort"))
			return input;

		List<DBRecord> result = new ArrayList<DBRecord>();
		while (input.hasNext())
			result.add(input.next());

		String sortBy = properties.get("sort").toString();
		if (sortBy.startsWith("lastUpdated")) {
			for (DBRecord r : result) {
				if (r.meta.getDate("lastUpdated") == null)
					r.meta.put("lastUpdated", r.meta.get("created"));
			}
		}
		RecordComparator comp = new RecordComparator(sortBy);
		Collections.sort(result, comp);

		return result.iterator();
	}

	public static Iterator<DBRecord> noDuplicates(Iterator<DBRecord> input) {
		return new DuplicateEliminator<DBRecord>(input);
	}

	public static Iterator<DBRecord> filterUntilFrom(Query q, Iterator<DBRecord> input) {
		if (q.getFromRecord() != null)
			return new FilterUntilFromRecord(q.getFromRecord(), input);
		return input;
	}

	public static Iterator<DBRecord> limit(Map<String, Object> properties, Iterator<DBRecord> input) {
		if (properties.containsKey("limit")) {
			Object limitObj = properties.get("limit");
			int limit = (int) Integer.parseInt(limitObj.toString());
			return new LimitIterator<DBRecord>(limit, input);
		} else
			return input;
	}

	public static List<DBRecord> collect(Iterator<DBRecord> input) {
		List<DBRecord> result = new ArrayList<DBRecord>();
		// int fail = 0;
		try {
		while (input.hasNext()) {
			result.add(input.next());
			// fail++; if (fail > 1000) return result; // XXXXXXX
		}
		} catch (RuntimeException e) {
			AccessLog.log("Exception Query:"+input.toString());
			throw e;
		}
		return result;
	}

	public static Iterator<DBRecord> multiQuery(Feature fchain, Query base, Iterator<Map<String, Object>> queries) throws AppException {
		return new MultiQuery(fchain, base, queries);
	}

	static class MultiQuery extends Feature.MultiSource<Map<String, Object>> {

		private Feature fchain;

		public MultiQuery(Feature fchain, Query base, Iterator<Map<String, Object>> queries) throws AppException {
			this.query = base;
			this.fchain = fchain;
			init(queries);
		}

		@Override
		public Iterator<DBRecord> advance(Map<String, Object> next) throws AppException {
			Map<String, Object> comb = Feature_QueryRedirect.combineQuery(next, query.getProperties());
			if (comb != null) {
				return fchain.iterator(new Query(comb, query.getFields(), query.getCache(), query.getApsId(), query.getContext()).setFromRecord(query.getFromRecord()));
			} else
				return Collections.emptyIterator();
		}

		@Override
		public String toString() {
			return "multi("+current.toString()+")";
		}
		
		

	}

	abstract static class FilterIterator<A> implements Iterator<A> {

		protected A next;
		protected Iterator<A> chain;

		public FilterIterator(Iterator<A> chain) {
			this.chain = chain;
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		public abstract boolean contained(A obj);

		@Override
		public A next() {
			A result = next;
			next = null;
			boolean condition = false;
			while (!condition && chain.hasNext()) {
				next = chain.next();
				condition = contained(next);
			}
			return result;
		}

	}

	static class DuplicateEliminator<A> extends FilterIterator<A> {

		private Set<A> encountered;

		public DuplicateEliminator(Iterator<A> chain) {
			super(chain);
			if (chain.hasNext()) {
				encountered = new HashSet<A>();
				next();
			}				
		}

		@Override
		public boolean contained(A obj) {
			return encountered.add(next);
		}
		
		@Override
		public String toString() {
			return "distinct("+chain.toString()+")";
		}

	}

	static class LimitIterator<A> implements Iterator<A> {
		private int max = 0;
		private int current = 0;
		private Iterator<A> chain;

		public LimitIterator(int limit, Iterator<A> chain) {
			this.max = limit;
			this.chain = chain;
			this.current = 0;
		}

		@Override
		public boolean hasNext() {
			return current < max && chain.hasNext();
		}

		@Override
		public A next() {
			current++;
			AccessLog.log("LIMIT " + current);
			return chain.next();
		}
		
		@Override
		public String toString() {
			return "limit("+max+","+chain.toString()+")";
		}

	}

	static class FilterUntilFromRecord extends FilterIterator<DBRecord> {
		private DBRecord fromRecord;
		private boolean found;

		public FilterUntilFromRecord(DBRecord fromRecord, Iterator<DBRecord> chain) {
			super(chain);
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord obj) {
			if (found)
				return true;
			if (obj._id.equals(fromRecord._id)) {
				found = true;
				return true;
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "until("+chain.toString()+")";
		}

	}

	static class FilterByMetaSet extends FilterIterator<DBRecord> {

		private String property;
		private Set values;
		private boolean noPostfilterStreams;

		public FilterByMetaSet(Iterator<DBRecord> chain, String property, Set values, boolean noPostfilterStreams) {
			super(chain);
			this.property = property;
			this.values = values;
			this.noPostfilterStreams = noPostfilterStreams;
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			if (!noPostfilterStreams || !record.isStream)
				return true;
			return values.contains(record.meta.get(property));
		}
		
		@Override
		public String toString() {
			return "filter("+property+","+chain.toString()+")";
		}

	}
	
	static class FilterNoMeta extends FilterIterator<DBRecord> {
		
		public FilterNoMeta(Iterator<DBRecord> chain) {
			super(chain);			
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			return record.meta != null;
		}
		
		@Override
		public String toString() {
			return "meta("+chain.toString()+")";
		}

	}
	
    static class DecryptRecords extends FilterIterator<DBRecord> {
		
    	private boolean filterDelete;
    	private int minTime;
    	
		public DecryptRecords(Iterator<DBRecord> chain, int minTime, boolean filterDelete) {
			super(chain);	
			this.filterDelete = filterDelete;
			this.minTime = minTime;
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record)  {
			if (record.meta != null && (minTime == 0 || record.time ==0 || record.time >= minTime)) {
				try {
				   RecordEncryption.decryptRecord(record);
				} catch (AppException e) {
					throw new RuntimeException(e);
				}
				 if (!record.meta.containsField("creator") && record.owner != null) record.meta.put("creator", record.owner.toDb());
				 if (filterDelete && record.meta.containsField("deleted")) return false;
				 return true;
			} else return false;		
		}

		@Override
		public String toString() {
			return "decrypt("+minTime+","+filterDelete+","+chain.toString()+")";
		}

	}

	static class FilterSetByMetaSet extends FilterIterator<DBRecord> {

		private String property;
		private Set values;
		private boolean noPostfilterStreams;

		public FilterSetByMetaSet(Iterator<DBRecord> chain, String property, Set values, boolean noPostfilterStreams) {
			super(chain);
			this.property = property;
			this.values = values;
			this.noPostfilterStreams = noPostfilterStreams;
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			Object v = record.meta.get(property);
			if (v == null)
				return false;
			;
			if (v instanceof String) {
				if (!values.contains((String) v))
					return false;
			} else {
				boolean any = false;
				for (String vi : (Collection<String>) v) {
					if (values.contains(vi))
						any = true;
				}
				if (!any)
					return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "filter("+property+","+chain.toString()+")";
		}

	}
	
	static class FilterByDateRange extends FilterIterator<DBRecord> {

		private String property;
		private Date minDate;
		private Date maxDate;

		public FilterByDateRange(Iterator<DBRecord> chain, String property, Date minDate, Date maxDate) {
			super(chain);
			this.property = property;
			this.minDate = minDate;
			this.maxDate = maxDate;
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			Date cmp = (Date) record.meta.get(property);
    		if (cmp == null) cmp = record._id.getCreationDate(); //Fallback for lastUpdated
    		if (cmp == null) {
    			AccessLog.log("Record with _id "+record._id.toString()+" has not created date!");
    			return false;
    		}
    		if (minDate != null && cmp.before(minDate)) return false;
			if (maxDate != null && cmp.after(maxDate)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "filter-date("+property+","+chain.toString()+")";
		}

	}
	
	static class FilterByDataQuery extends FilterIterator<DBRecord> {

		private Condition condition;
		private List<DBRecord> nomatch;

		public FilterByDataQuery(Iterator<DBRecord> chain, Object query, List<DBRecord> nomatch) throws InternalServerException {
			super(chain);
			this.nomatch = nomatch;

			if (query instanceof Map<?, ?>)
				condition = new AndCondition((Map<String, Object>) query).optimize();
			else if (query instanceof Condition)
				condition = ((Condition) query).optimize();
			else
				throw new InternalServerException("error.internal", "Query type not implemented");

			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			Object accessVal = record.data;
			if (condition.satisfiedBy(accessVal)) {
				return true;
			} else if (nomatch != null)
				nomatch.add(record);
			return false;
		}
		
		@Override
		public String toString() {
			return "filter-data("+chain.toString()+")";
		}

	}
	
	static class ConditionalLoad extends BlockwiseLoad {
		
		private Set<String> check;
		private boolean created;
		
		public ConditionalLoad(Iterator<DBRecord> chain, Query q, int blocksize) {
			super(chain, q, blocksize);
			check = q.mayNeedFromDB();
			created = check.contains("created");
		}
		
		protected boolean needsLoad(DBRecord record) {
			if (created) {    			
    			record.meta.put("created", record._id.getCreationDate());    			
    		}
    		    		
    				boolean fetch = false;
    		if (record.meta == null) return false;;
    		for (String k : check) if (!record.meta.containsField(k)) return true;
    		return false;    				
		}
		
		@Override
		public String toString() {
			return "conditional-load("+chain.toString()+")";
		}
	}

	static class BlockwiseLoad implements Iterator<DBRecord> {

		private int blocksize;
		protected Iterator<DBRecord> chain;
		protected Iterator<DBRecord> cache;
		private List<DBRecord> work;
		private Query q;

		public BlockwiseLoad(Iterator<DBRecord> chain, Query q, int blocksize) {
			this.chain = chain;
			this.blocksize = blocksize;
			this.q = q;
			this.cache = Collections.emptyIterator();
			work = new ArrayList<DBRecord>(blocksize);
		}

		@Override
		public boolean hasNext() {
			return cache.hasNext() || chain.hasNext();
		}
		
		protected boolean needsLoad(DBRecord record) {
			return record.encrypted == null && record.data == null;
		}

		@Override
		public DBRecord next() {
			if (cache.hasNext())
				return cache.next();

			int current = 0;
			work.clear();
			Map<MidataId, DBRecord> fetchIds = new HashMap<MidataId, DBRecord>();
			while (current < blocksize && chain.hasNext()) {
				DBRecord record = chain.next();
				if (needsLoad(record)) {
					DBRecord old = fetchIds.put(record._id, record);
					if (old == null) work.add(record);						
				} else work.add(record);
				current++;
			}

			try {
				if (!fetchIds.isEmpty()) {
					List<DBRecord> read = QueryEngine.lookupRecordsById(q, fetchIds.keySet(), q.restrictedBy("deleted"));
					for (DBRecord record : read) {
						DBRecord old = fetchIds.get(record._id);
						QueryEngine.fetchFromDB(old, record);
					}
					if (read.size() < fetchIds.size()) {
						for (DBRecord record : fetchIds.values()) {
							if (record.encrypted == null)
								record.meta = null;
						}
					}
				}
			} catch (AppException e) {
				throw new RuntimeException(e);
			}

			cache = work.iterator();

			return cache.next();

		}
		
		@Override
		public String toString() {
			return "load("+chain.toString()+")";
		}

	}

}
