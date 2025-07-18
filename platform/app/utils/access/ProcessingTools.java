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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import models.MidataId;
import models.Record;
import utils.AccessLog;
import utils.QueryTagTools;
import utils.access.op.AndCondition;
import utils.access.op.Condition;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class ProcessingTools {
 

	private static final DBIterator EMPTY = new IteratorWrapper("empty", Collections.emptyIterator());
	
	public static <A> DBIterator<A> dbiterator(String name, Iterator<A> in) {
		return new IteratorWrapper(name, in);
	}
	
	public static <A> DBIterator<A> empty() {
		return EMPTY;
	}
	
	public static <A> DBIterator<A> singleton(A in) {
		return new IteratorWrapper("singleton", Collections.singletonList(in).iterator());
	}
	
	public static class IteratorWrapper<A> implements DBIterator<A> {

		private Iterator<A> iterator;
		private String title;
		
		public IteratorWrapper(String name, Iterator<A> iterator) {
			this.iterator = iterator;
			this.title = name;
		}
		
		@Override
		public A next() throws AppException {
			return iterator.next();
		}

		@Override
		public boolean hasNext() throws AppException {
			return iterator.hasNext();
		}

		@Override
		public String toString() {
			return title;
		}	
		
		@Override
		public void close() {
						
		}
	
	}
	
	public static DBIterator<DBRecord> sort(Map<String, Object> properties, DBIterator<DBRecord> input) throws AppException {
		if (!properties.containsKey("sort"))
			return input;

		String sortBy = properties.get("sort").toString();								
		
		List<DBRecord> result = new ArrayList<DBRecord>();
		while (input.hasNext())
			result.add(input.next());
		
		if (sortBy.startsWith("lastUpdated")) {
			for (DBRecord r : result) {
				if (r.meta.getDate("lastUpdated") == null)
					r.meta.put("lastUpdated", r.meta.get("created"));
			}
		}
		RecordComparator comp = new RecordComparator(sortBy);
		Collections.sort(result, comp);

		return ProcessingTools.dbiterator("sorted("+result.size()+")",result.iterator());
	}

	public static DBIterator<DBRecord> noDuplicates(DBIterator<DBRecord> input) throws AppException {
		return new DuplicateEliminator<DBRecord>(input);
	}

	public static DBIterator<DBRecord> filterUntilFrom(Query q, DBIterator<DBRecord> input) throws AppException {
		if (q.getFromRecord() != null)
			return new FilterUntilFromRecord(q.getFromRecord(), input);
		return input;
	}

	public static DBIterator<DBRecord> limit(Map<String, Object> properties, DBIterator<DBRecord> input) {		
		Object limitObj = properties.get("limit");
		if (limitObj != null) {			
			int limit = (int) Integer.parseInt(limitObj.toString());
			return new LimitIterator<DBRecord>(limit, input);
		} else
			return input;
	}

	public static List<DBRecord> collect(DBIterator<DBRecord> input) throws AppException {
		List<DBRecord> result = new ArrayList<DBRecord>();
	    try {	
			while (input.hasNext()) {
				result.add(input.next());			
			}
	    } finally { input.close(); }
		AccessLog.log("collected ", Integer.toString(result.size()), " records from ", input.toString());
		return result;
	}

	public static DBIterator<DBRecord> multiQuery(Feature fchain, Query base, DBIterator<Map<String, Object>> queries) throws AppException {
		return new MultiQuery(fchain, base, queries);
	}

	static class MultiQuery extends Feature.MultiSource<Map<String, Object>> {

		private Feature fchain;

		public MultiQuery(Feature fchain, Query base, DBIterator<Map<String, Object>> queries) throws AppException {
			this.query = base;
			this.fchain = fchain;
			init(queries);
		}

		@Override
		public DBIterator<DBRecord> advance(Map<String, Object> next) throws AppException {
			Map<String, Object> comb = Feature_QueryRedirect.combineQuery(next, query.getProperties(), query.getContext());
			if (comb != null) {
				return ProcessingTools.limit(comb, fchain.iterator(new Query("or",next.toString(), comb, query.getFields(), query.getCache(), query.getApsId(), query.getContext(),query).setFromRecord(query.getFromRecord())));
			} else
				return ProcessingTools.empty();
		}

		@Override
		public String toString() {
			return "multi(["+passed+"] "+current.toString()+")";
		}
		
		

	}

	abstract static class FilterIterator<A> implements DBIterator<A> {

		protected A next;
		protected DBIterator<A> chain;
		protected int passed;
		protected int filtered;

		public FilterIterator(DBIterator<A> chain) {
			this.chain = chain;
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		public abstract boolean contained(A obj) throws AppException;

		@Override
		public A next() throws AppException {
			A result = next;			
			boolean condition = false;
			A candidate = null;
			while (!condition && chain.hasNext()) {
				candidate = chain.next();
				condition = contained(candidate);
				if (condition) passed++; else filtered++;
			}
			if (condition) next = candidate; else next = null;
			return result;
		}
		
		@Override
		public void close() {
			chain.close();			
		}
	
		
	}

	static class DuplicateEliminator<A> extends FilterIterator<A> {

		private Set<A> encountered;

		public DuplicateEliminator(DBIterator<A> chain) throws AppException {
			super(chain);
			if (chain.hasNext()) {
				encountered = new HashSet<A>();
				next();
			}				
		}

		@Override
		public boolean contained(A obj) {
			boolean r = encountered.add(obj);
			//if (!r) AccessLog.log("old: "+obj.toString()); else AccessLog.log("new: "+obj.toString()); 
			return r;
		}
		
		@Override
		public String toString() {
			return "distinct(["+passed+"/"+filtered+"] "+chain.toString()+")";
		}

	}

	static class LimitIterator<A> implements DBIterator<A> {
		private int max = 0;
		private int current = 0;
		private DBIterator<A> chain;

		public LimitIterator(int limit, DBIterator<A> chain) {
			this.max = limit;
			this.chain = chain;
			this.current = 0;
		}

		@Override
		public boolean hasNext() throws AppException {
			return current < max && chain.hasNext();
		}

		@Override
		public A next() throws AppException {
			current++;			
			return chain.next();
		}
		
		@Override
		public String toString() {
			return "limit("+max+","+chain.toString()+")";
		}
		
		@Override
		public void close() {
			chain.close();			
		}
		

	}

	static class FilterUntilFromRecord extends FilterIterator<DBRecord> {
		private DBRecord fromRecord;
		private boolean found;

		public FilterUntilFromRecord(DBRecord fromRecord, DBIterator<DBRecord> chain) throws AppException {
			super(chain);
			this.fromRecord = fromRecord;
			this.found = false;
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord obj) {
			if (found)
				return true;
			if (obj._id.equals(fromRecord._id)) {
				found = true;
				AccessLog.log("found record ", fromRecord._id.toString());
				return true;
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "until(["+passed+"/"+filtered+"] "+chain.toString()+")";
		}

	}

	static class FilterByMetaSet extends FilterIterator<DBRecord> {

		private String property;
		private Set values;
		private boolean noPostfilterStreams;

		public FilterByMetaSet(DBIterator<DBRecord> chain, String property, Set values, boolean noPostfilterStreams) throws AppException {
			super(chain);
			this.property = property;
			this.values = values;
			this.noPostfilterStreams = noPostfilterStreams;
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			if (noPostfilterStreams && record.isStream!=null)
				return true;
			return values.contains(record.meta.get(property));
		}
		
		@Override
		public String toString() {
			return "filter(["+passed+"/"+filtered+"]"+property+","+chain.toString()+")";
		}

	}
	
	static class FilterByTag extends FilterIterator<DBRecord> {
				
		private String tag;
		private boolean must_be_present;

		public FilterByTag(DBIterator<DBRecord> chain, String tag, boolean must_be_present) throws AppException {
			super(chain);	
			this.tag = tag;
			this.must_be_present = must_be_present;
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			if (record.isStream!=null) return true;
			Collection tags = (Collection) record.meta.get("tags");
			if (must_be_present) return tags != null && tags.contains(tag);
			return tags == null || !tags.contains(tag);
		}
		
		@Override
		public String toString() {
			return "tag-filter(["+passed+"/"+filtered+"] "+(must_be_present?"":"no ")+tag+" "+chain.toString()+")";
		}

	}
	
	static class AddMetaTag extends FilterIterator<DBRecord> {
		
		private String tag;		

		public AddMetaTag(DBIterator<DBRecord> chain, String tag) throws AppException {
			super(chain);	
			this.tag = tag;			
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			if (record.isStream!=null) return true;
			Collection tags = (Collection) record.meta.get("tags");
			if (tags != null) tags.add(tag);
			return true;
		}
		
		@Override
		public String toString() {
			return "add-tag("+tag+" "+chain.toString()+")";
		}

	}
	
	static class FilterByNonPseudonymizeTag extends FilterIterator<DBRecord> {
				

		public FilterByNonPseudonymizeTag(DBIterator<DBRecord> chain) throws AppException {
			super(chain);	
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			if (record.isStream!=null || record.context==null) return true;
			if (record.meta==null) return false;
			Collection tags = (Collection) record.meta.get("tags");						
			return (!record.context.mustPseudonymize()) || tags == null || !tags.contains(QueryTagTools.SECURITY_NOT_PSEUDONYMISABLE);
		}
		
		@Override
		public String toString() {
			return "filter-non-pseudo(["+passed+"/"+filtered+"] "+chain.toString()+")";
		}

	}
	
	static class ClearByHiddenTag extends FilterIterator<DBRecord> {
		

		public ClearByHiddenTag(DBIterator<DBRecord> chain) throws AppException {
			super(chain);					
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			if (record.isStream!=null) return true;
			BasicBSONList tags = (BasicBSONList) record.meta.get("tags");
			if (tags == null || !tags.contains("security:hidden")) return true;
			record.data = new BasicBSONObject();
			return true;
		}
		
		@Override
		public String toString() {
			return "no-hidden(["+passed+"/"+filtered+"] "+chain.toString()+")";
		}

	}
	
   static class FilterStreams extends FilterIterator<DBRecord> {
		

		public FilterStreams(DBIterator<DBRecord> chain) throws AppException {
			super(chain);					
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			return record.isStream==null;			
		}
		
		@Override
		public String toString() {
			return "no-streams(["+passed+"/"+filtered+"] "+chain.toString()+")";
		}

	}
	
	static class FilterNoMeta extends FilterIterator<DBRecord> {
		
		public FilterNoMeta(DBIterator<DBRecord> chain) throws AppException {
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
			return "meta(["+passed+"/"+filtered+"] "+chain.toString()+")";
		}

	}
	
    static class DecryptRecords extends FilterIterator<DBRecord> {
		
    	private boolean filterDelete;
    	private int minTime;
    	
		public DecryptRecords(DBIterator<DBRecord> chain, int minTime, boolean filterDelete) throws AppException {
			super(chain);	
			this.filterDelete = filterDelete;
			this.minTime = minTime;
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) throws AppException {
			//AccessLog.log("rec meta="+record.meta+" enc="+record.encrypted+" dat="+record.data+" encdat="+record.encryptedData);
			if (record.meta != null && (minTime == 0 || record.time ==0 || record.time >= minTime || record.sharedAt != null)) {				
				 RecordEncryption.decryptRecord(record);	
				 if (record.meta == null) return false;
				 if (!record.meta.containsField("creator") && record.owner != null) record.meta.put("creator", record.owner.toDb());
				 if (filterDelete && record.meta.containsField("deleted")) return false;
				 return true;
			} else return false;		
		}

		@Override
		public String toString() {
			return "decrypt(["+passed+"/"+filtered+"] "+minTime+","+filterDelete+","+chain.toString()+")";
		}

	}

	static class FilterSetByMetaSet extends FilterIterator<DBRecord> {

		private String property;
		private Set values;
		private boolean noPostfilterStreams;

		public FilterSetByMetaSet(DBIterator<DBRecord> chain, String property, Set values, boolean noPostfilterStreams) throws AppException {
			super(chain);
			this.property = property;
			this.values = values;
			this.noPostfilterStreams = noPostfilterStreams;
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			if (noPostfilterStreams && record.isStream!=null)
				return true;
			
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
			return "filter(["+passed+"/"+filtered+"] "+property+","+chain.toString()+")";
		}

	}
	
	static class FilterByDateRange extends FilterIterator<DBRecord> {

		private String property;
		private Date minDate;
		private Date maxDate;

		public FilterByDateRange(DBIterator<DBRecord> chain, String property, Date minDate, Date maxDate) throws AppException {
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
    			AccessLog.log("Record with _id ", record._id.toString(), " has not created date!");
    			return false;
    		}
    		if (minDate != null && cmp.before(minDate)) return false;
			if (maxDate != null && cmp.after(maxDate)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "filter-date(["+passed+"/"+filtered+"] "+property+","+chain.toString()+")";
		}

	}
	
	static class FilterBySharedDate extends FilterIterator<DBRecord> {
		
		private Date minDate;
		private Date maxDate;

		public FilterBySharedDate(DBIterator<DBRecord> chain, Date minDate, Date maxDate) throws AppException {
			super(chain);
			
			this.minDate = minDate;
			this.maxDate = maxDate;
			if (chain.hasNext())
				next();
		}

		@Override
		public boolean contained(DBRecord record) {
			Date cmp = (Date) record.meta.get("lastUpdated");
    		if (cmp == null) cmp = record._id.getCreationDate(); //Fallback for lastUpdated
    		if (cmp == null) {
    			AccessLog.log("Record with _id ", record._id.toString(), " has not created date!");
    			return false;
    		}
    		if (record.sharedAt != null && record.sharedAt.after(cmp)) {
    			cmp = record.sharedAt;    			
    		}    		
    		if (minDate != null && cmp.before(minDate)) return false;
			if (maxDate != null && cmp.after(maxDate)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "filter-date(["+passed+"/"+filtered+"] shared-after,"+chain.toString()+")";
		}

	}
	
	static class FilterByDataQuery extends FilterIterator<DBRecord> {

		private Condition condition;
		private List<DBRecord> nomatch;

		public FilterByDataQuery(DBIterator<DBRecord> chain, Object query, List<DBRecord> nomatch) throws AppException {
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
			//AccessLog.log("chk: "+(accessVal!=null?accessVal.toString():"null"));
			//AccessLog.log("cond:"+condition.toString());
			if (condition.satisfiedBy(accessVal)) {
				//AccessLog.log("TRUE");
				return true;
			} else if (nomatch != null) {
				//AccessLog.log("FALSE");
				nomatch.add(record);
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "filter-data(["+passed+"/"+filtered+"] "+condition.toString()+", "+chain.toString()+")";
		}

	}
	
	static class ConditionalLoad extends BlockwiseLoad {
		
		private Set<String> check;
		private boolean created;
		
		public ConditionalLoad(DBIterator<DBRecord> chain, Query q, int blocksize) {
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

	static class BlockwiseLoad implements DBIterator<DBRecord> {

		private int blocksize;
		protected DBIterator<DBRecord> chain;
		protected Iterator<DBRecord> cache;
		private List<DBRecord> work;
		private Query q;
		private int loaded;
		private int notloaded;

		public BlockwiseLoad(DBIterator<DBRecord> chain, Query q, int blocksize) {
			this.chain = chain;
			this.blocksize = blocksize;
			this.q = q;
			this.cache = Collections.emptyIterator();
			work = new ArrayList<DBRecord>(blocksize);
		}

		@Override
		public boolean hasNext() throws AppException {
			return cache.hasNext() || chain.hasNext();
		}
		
		protected boolean needsLoad(DBRecord record) {
			return record.encrypted == null && record.data == null;
		}

		@Override
		public DBRecord next() throws AppException {
			if (cache.hasNext())
				return cache.next();

			int current = 0;
			work.clear();
			Map<MidataId, DBRecord> fetchIds = new HashMap<MidataId, DBRecord>();
			while (current < blocksize && chain.hasNext()) {
				DBRecord record = chain.next();
				if (needsLoad(record)) {
					DBRecord old = fetchIds.put(record._id, record);
					if (old == null) {
						work.add(record);
						loaded++;
					}
				} else {
					work.add(record);
					notloaded++;
				}
				current++;
			}

		
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

			cache = work.iterator();

			return cache.next();

		}
		
		@Override
		public String toString() {
			return "load([L:"+loaded+",S:"+notloaded+"] "+chain.toString()+")";
		}
		
		@Override
		public void close() {
			chain.close();			
		}
	

	}

	static class ConvertIterator implements DBIterator<Record> {
		private DBIterator<DBRecord> input;
		
		public ConvertIterator(DBIterator input) {
			this.input = input;
		}

		@Override
		public Record next() throws AppException {
			DBRecord rec = input.next();
			Record result = RecordConversion.instance.currentVersionFromDB(rec);
			rec.clearEncryptedFields();
			rec.clearSecrets();
			return result;
		}

		@Override
		public boolean hasNext() throws AppException {
			return input.hasNext();
		}
		
		@Override
		public void close() {
			input.close();			
		}
	
		
	}
	
	static class BlockwiseChainIterator implements DBIterator<DBRecord> {
		
		private Query query;
		private String path;
		private Feature next;
		private int limit = 100;
		private Map<String, Object> properties;
		private DBIterator<DBRecord> current;
		private DBIterator<DBRecord> input;
		private Feature_InMemoryQuery holder;
		
		BlockwiseChainIterator(Query query, String path, Map<String, Object> properties,Feature next, DBIterator<DBRecord> input, Feature_InMemoryQuery holder) throws AppException {
			this.next = next;
			this.input = input;
			this.holder = holder;
			this.query = query;
			this.path = path;
			this.properties = properties;
			advance();
		}
		
		private void advance() throws AppException {
			List<DBRecord> cache = new ArrayList<DBRecord>();
			int idx = 0;
			while (idx < limit && input.hasNext()) {
				cache.add(input.next());
				idx++;
			}
			if (!cache.isEmpty()) {
				holder.setContent(cache);
			    current =  QueryEngine.combineIterator(query, path, properties, next);
			} else current = ProcessingTools.empty();
		}
		
		@Override
		public DBRecord next() throws AppException {
			DBRecord r = current.next();
			while (!current.hasNext() && input.hasNext()) {
				advance();				
			}
		    return r;
		}
		
		@Override
		public boolean hasNext() throws AppException {
			return current.hasNext();			
		}
			
		@Override
		public void close() {
			// TODO Auto-generated method stub
			
		}
		
		public String toString() {
			return "blockwise-filter("+current.toString()+")";
		}
	}
}
