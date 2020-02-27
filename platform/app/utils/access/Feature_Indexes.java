package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Consent;
import models.MidataId;
import utils.AccessLog;
import utils.RuntimeConstants;
import utils.access.index.IndexDefinition;
import utils.access.index.IndexMatch;
import utils.access.index.IndexRoot;
import utils.access.index.Lookup;
import utils.access.op.AlternativeFieldAccess;
import utils.access.op.AndCondition;
import utils.access.op.Condition;
import utils.access.op.FieldAccess;
import utils.access.op.OrCondition;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

public class Feature_Indexes extends Feature {

	private Feature next;

	public Feature_Indexes(Feature next) {
		this.next = next;
	}

	public final static int INDEX_REVERSE_USE = 300;
	public final static int AUTOCREATE_INDEX_COUNT = 30;
	public final static int NO_SECOND_INDEX_COUNT = 30;

	protected static AccessContext getContextForAps(Query q, MidataId aps) throws AppException {
		if (!q.getCache().getAPS(aps).isUsable())
			return null;
		if (q.getApsId().equals(aps))
			return q.getContext();
		if (q.getCache().getExecutor().equals(aps))
			return new AccountAccessContext(q.getCache(), q.getContext());
		Consent c = q.getCache().getConsent(aps);
		if (c == null)
			return null;
		return new ConsentAccessContext(c, q.getContext());

	}

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		if (q.restrictedBy("index") && !q.restrictedBy("_id")) {

			if (!q.restrictedBy("format"))
				throw new BadRequestException("error.invalid.query", "Queries using an index must be restricted by format!");
		
			boolean forceCreate = q.getCache().getExecutor().equals(RuntimeConstants.publicGroup) && q.getApsId().equals(RuntimeConstants.instance.publicUser);
			IndexPseudonym pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getExecutor(), q.getApsId(), forceCreate);

			if (pseudo == null) {
				List<DBRecord> recs = next.query(q);
				if (recs.size() > AUTOCREATE_INDEX_COUNT) {
					pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getExecutor(), q.getApsId(), true);
				} else {
					return ProcessingTools.dbiterator("non-indexed()", recs.iterator());
				}
			}

			AccessLog.logBegin("start index query");
			long startTime = System.currentTimeMillis();

			Object indexQueryUnparsed = q.getProperties().get("index");
			Condition indexQueryParsed = null;

			if (indexQueryUnparsed instanceof Condition) {
				indexQueryParsed = (Condition) indexQueryUnparsed;
			} else {
				indexQueryParsed = AndCondition.parseRemaining(indexQueryUnparsed).optimize();
				AccessLog.log("Optimized query: " + indexQueryParsed.toString());
				indexQueryParsed = indexQueryParsed.indexExpression();
			}

			AccessLog.log("Index query: " + indexQueryParsed.toString());

			Set<MidataId> targetAps = determineTargetAps(q);

			List<DBRecord> result = Collections.emptyList();

			if (targetAps != null && targetAps.isEmpty()) {
				AccessLog.logEnd("end index query no target APS");
				return ProcessingTools.empty();
			}

			IndexUse myAccess = parse(pseudo, q.getRestriction("format"), indexQueryParsed);

			Collection<IndexMatch> matches = myAccess.query(q, targetAps);		
            AccessLog.log("index matches: "+matches.size());
			Set<MidataId> allAps = new HashSet<MidataId>();

			Map<MidataId, Set<MidataId>> filterMatches = new HashMap<MidataId, Set<MidataId>>();
			for (IndexMatch match : matches) {
				if (targetAps == null || targetAps.contains(match.apsId)) {
					Set<MidataId> ids = filterMatches.get(match.apsId);
					if (ids == null) {
						ids = new HashSet<MidataId>();
						filterMatches.put(match.apsId, ids);
						allAps.add(match.apsId);
					}
					ids.add(match.recordId);
				}
			}

			Map<MidataId, List<DBRecord>> newRecords = new HashMap<MidataId, List<DBRecord>>();

			AccessLog.logBegin("start to look for new entries");
			Feature nextWithProcessing = new Feature_ProcessFilters(next);
			
			if (targetAps != null) {
				for (MidataId id : targetAps) {
					long v = myAccess.version(id);
					result = null;
					AccessContext context = getContextForAps(q, id);
					if (context != null) {
						if (context instanceof ConsentAccessContext && ((ConsentAccessContext) context).getConsent().dataupdate <= v) {
							continue;
						} 
						//if (context instanceof ConsentAccessContext) AccessLog.log("TIMESTAMP "+((ConsentAccessContext) context).getConsent().dataupdate+" vs "+v);
						List<DBRecord> add;
						Query updQuery = new Query(q, CMaps.mapPositive("shared-after", v).map("owner", "self"), id, context);
						add = QueryEngine.filterByDataQuery(nextWithProcessing.query(updQuery), indexQueryParsed, null);
						AccessLog.log("found new updated entries aps=" + id + ": " + add.size());
						result = QueryEngine.combine(result, add);						
						if (result != null) {
							newRecords.put(id, result);
							allAps.add(id);
						}
					}
				}
			} else {
				long v = myAccess.version(null);
				// AccessLog.log("vx="+v);
				List<DBRecord> add;
				add = QueryEngine.filterByDataQuery(nextWithProcessing.query(new Query(q, CMaps.mapPositive("shared-after", v).map("consent-limit",1000))), indexQueryParsed, null);
				AccessLog.log("found new updated entries: " + add.size());
				result = QueryEngine.combine(result, add);				
				if (result != null && !result.isEmpty()) {
					for (DBRecord record : result) {
						MidataId id = record.context.getTargetAps();
						List<DBRecord> recs = newRecords.get(id);
						if (recs == null) {
							recs = new ArrayList<DBRecord>();
							newRecords.put(id, recs);
							allAps.add(id);
						}
						recs.add(record);
					}
				}
			}
			AccessLog.logEnd("end to look for new entries");
			long endTime2 = System.currentTimeMillis();

			if (allAps.size() > Feature_AccountQuery.MIN_FOR_ACCELERATION) {
			  List<Consent> prefetched = new ArrayList<Consent>(Consent.getAllByAuthorized(q.getCache().getAccountOwner(), CMaps.map("_id", allAps), Consent.SMALL));
			  q.getCache().cache(prefetched);
			  FasterDecryptTool.accelerate(q, prefetched);
			}
			List<AccessContext> contexts = new ArrayList<AccessContext>(allAps.size());
			for (MidataId aps : allAps) {
				AccessContext context = getContextForAps(q, aps);
				if (context != null)
					contexts.add(context);
			}
			Collections.sort(contexts, new ContextComparator());
			AccessLog.log("index matches "+contexts.size()+" contexts");

			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct", "encryptedData");
			queryFields.addAll(q.getFieldsFromDB());

			AccessLog.logEnd("end index query");
			if (contexts.isEmpty()) return ProcessingTools.empty();
			return ProcessingTools.noDuplicates(new IndexIterator(q, myAccess, contexts, newRecords, filterMatches));				

		} else
			return next.iterator(q);
	}

	public static Set<MidataId> determineTargetAps(Query q) throws BadRequestException, AppException {
		Set<MidataId> targetAps;

		if (!q.getApsId().equals(q.getCache().getAccountOwner())) {
			targetAps = Collections.singleton(q.getApsId());
		} else {
			boolean allTarget = Feature_AccountQuery.allApsIncluded(q);

			if (allTarget) {
				targetAps = null;
			} else {
				List<Consent> consents = Feature_AccountQuery.getConsentsForQuery(q, true);
				targetAps = new HashSet<MidataId>();
				if (Feature_AccountQuery.mainApsIncluded(q))
					targetAps.add(q.getApsId());
				for (Consent consent : consents)
					targetAps.add(consent._id);
			}
		}
		return targetAps;
	}
	
	public class IndexIterator extends Feature.MultiSource<AccessContext> {

		private Query q;
		private Set<String> queryFields;
		private Map<MidataId, List<DBRecord>> newRecords;
		private Map<MidataId, Set<MidataId>> matches;
		private IndexUse myAccess;
		private AccessContext currentContext;
		private int size;

		IndexIterator(Query q, IndexUse myAccess2, List<AccessContext> contexts, Map<MidataId, List<DBRecord>> newRecords, Map<MidataId, Set<MidataId>> matches) throws AppException {
			this.q = q;
			this.query = q;
			this.myAccess = myAccess2;
			queryFields = Sets.create("stream", "time", "document", "part", "direct", "encryptedData");
			queryFields.addAll(q.getFieldsFromDB());
			this.newRecords = newRecords;
			this.matches = matches;
			
			if (q.getFromRecord() != null) {
				DBRecord from = q.getFromRecord();
				if (from.owner != null) {
					Iterator<AccessContext> it = contexts.iterator();
					while (it.hasNext()) {					  
						AccessContext c = it.next();
						  if (c.getOwner().equals(from.owner)) {
							  init(c, it);
							  return;
						  }
					  }
					  init(it);
					  return;
				}
			}
			
			this.init(contexts.iterator());
		}

		@Override
		public DBIterator<DBRecord> advance(AccessContext context) throws AppException {
			MidataId aps = context.getTargetAps();
			List<DBRecord> result = newRecords.get(aps);
			if (result == null)
				result = new ArrayList<DBRecord>();
			AccessLog.log("Now processing aps:" + aps.toString());

			Set<MidataId> ids = matches.get(aps);
            if (ids != null && !ids.isEmpty()) {
			if (ids.size() > INDEX_REVERSE_USE) {
				Query q4 = new Query(q, CMaps.map(), aps, context);
				List<DBRecord> unindexed = next.query(q4);
				int size = unindexed.size();
				if (size > 0) {
					result = QueryEngine.modifyable(result);
					for (DBRecord candidate : unindexed) {
						candidate.consentAps = aps;
						if (ids.contains(candidate._id))
							result.add(candidate);
					}
				}
				//AccessLog.log("add unindexed =" + unindexed.size());

			} else {
				Map<String, Object> readRecs = new HashMap<String, Object>();
				boolean add = false;
				boolean directQuery = true;
				if (ids.size() > 5) {
					Map<String, Object> props = new HashMap<String, Object>();
					props.putAll(q.getProperties());
					props.put("streams", "only");
					props.put("owner", "self");
					List<DBRecord> matchStreams = next.query(new Query(props, Sets.create("_id"), q.getCache(), aps, context));
					//AccessLog.log("index query streams " + matchStreams.size() + " matches.");
					if (matchStreams.isEmpty())
						directQuery = false;
					else {
						Set<MidataId> streams = new HashSet<MidataId>();
						for (DBRecord r : matchStreams)
							streams.add(r._id);
						readRecs.put("stream", streams);
					}
					if (!aps.equals(q.getCache().getAccountOwner()))
						add = true;
				}
				readRecs.put("_id", ids);
				// Deleted records may be needed if history version is queried!
				// readRecs.put("encryptedData", QueryEngine.NOTNULL);

				int directSize = 0;
				if (directQuery) {
					long time = System.currentTimeMillis();
					List<DBRecord> partresult = DBRecord.getAllList(readRecs, queryFields);
					//AccessLog.log("db time:" + (System.currentTimeMillis() - time));

					Query q3 = new Query(q, CMaps.map("strict", "true"), aps, context);
					partresult = Feature_Prefetch.lookup(q3, partresult, next, false);
					for (DBRecord record : partresult)
						record.consentAps = aps;
					result = QueryEngine.combine(result, partresult);
					directSize = partresult.size();
				}

				if (add) {
					Query q2 = new Query(q, CMaps.map(q.getProperties()).map("_id", ids), aps, context);
					List<DBRecord> additional = next.query(q2);
					for (DBRecord record : additional)
						record.consentAps = aps;
					result = QueryEngine.combine(result, additional);
					AccessLog.log("looked up directly=" + directSize + " additionally=" + additional.size());
				} else {
					AccessLog.log("looked up directly=" + directSize);
				}

			}
			
			myAccess.revalidate(q.getCache().getExecutor(), result);	
            }
			Collections.sort(result);
			size = result.size();
			currentContext = context;
			return ProcessingTools.dbiterator("index-use()", result.iterator());
		}

		@Override
		public String toString() {
			return "index-access(["+passed+"] { ow:"+currentContext.getOwner().toString()+", id:"+currentContext.getTargetAps().toString()+", size:"+size+" })";
		}
		
		

	}
	

	IndexUse parse(IndexPseudonym pseudo, Set<String> format, Condition indexQuery) throws InternalServerException {
		if (indexQuery instanceof AndCondition) {
			List<IndexUse> uses = new ArrayList<IndexUse>();
			for (Condition check : ((AndCondition) indexQuery).getParts()) {
				uses.add(parse(pseudo, format, check));
			}
			return new IndexAnd(uses);
		} else if (indexQuery instanceof OrCondition) {
			List<IndexUse> uses = new ArrayList<IndexUse>();
			for (Condition check : ((OrCondition) indexQuery).getParts()) {
				uses.add(parse(pseudo, format, check));
			}
			return new IndexOr(uses);
		} else if (indexQuery instanceof FieldAccess) {
			return new IndexAccess(pseudo, format, indexQuery);
		} else if (indexQuery instanceof AlternativeFieldAccess) {
			return new IndexAccess(pseudo, format, indexQuery);
		} else
			throw new InternalServerException("error.internal", "Bad index query expression");
	}

	private final Map<MidataId, IndexRoot> cachedIndexRoots = new HashMap<MidataId, IndexRoot>();

	interface IndexUse {
		Collection<IndexMatch> query(Query q, Set<MidataId> targetAps) throws AppException;

		void revalidate(MidataId executor, List<DBRecord> result) throws AppException;

		long version(MidataId aps);
	}

	class IndexAccess implements IndexUse {
		IndexDefinition index;

		IndexPseudonym pseudo;
		Set<String> format;

		Condition revalidationQuery;

		List<String> pathes;
		Condition[] condition;

		Collection<IndexMatch> matches;
		IndexRoot root;
		boolean doupdate = false;

		IndexAccess(IndexPseudonym pseudo, Set<String> format, Condition indexQuery) throws InternalServerException {
			this.pseudo = pseudo;
			this.format = format;
			this.revalidationQuery = indexQuery;

			int idx = 0;
			condition = new Condition[1 /* indexQuery.size() */];
			pathes = new ArrayList();

			if (indexQuery instanceof FieldAccess) {

				FieldAccess fa = (FieldAccess) indexQuery;
				pathes.add(fa.getField());
				condition[0] = AndCondition.parseRemaining(fa.getCondition()).optimize();
			} else if (indexQuery instanceof AlternativeFieldAccess) {
				AlternativeFieldAccess fa = (AlternativeFieldAccess) indexQuery;
				pathes.add(fa.getField());
				condition[0] = AndCondition.parseRemaining(fa.getCondition()).optimize();
			} else
				throw new InternalServerException("error.internal", "Index expression not useable.");

		}

		public void prepare() throws AppException {
			index = IndexManager.instance.findIndex(pseudo, format, pathes);
			if (index == null) {
				AccessLog.logBegin("start index creation");
				index = IndexManager.instance.createIndex(pseudo, format, pathes);
				AccessLog.logEnd("end index creation");
			}
			root = cachedIndexRoots.get(index._id);
			doupdate = false;
			if (root == null) {
				root = IndexManager.instance.getIndexRoot(pseudo, index);
				doupdate = true;
				cachedIndexRoots.put(index._id, root);
			}
		}
		
		public int getCoverage() throws AppException {
			if (index == null) prepare();
			return root.getEstimatedIndexCoverage(new Lookup(condition));
		}
		
		public void dontuse() {
			index = null;
		}
		
		public int getEstimatedTotal() {
			switch (root.getMaxDepth()) {
			case 0:
			case 1: return 100;
			case 2: return 10000;
			default: return 1000000;
			}
		}

		public Collection<IndexMatch> query(Query q, Set<MidataId> targetAps) throws AppException {
			long t1 = System.currentTimeMillis();
			if (index == null) prepare();
			
			long t2 = System.currentTimeMillis();
			if (targetAps != null && targetAps.size() == 1) {
				matches = IndexManager.instance.queryIndex(root, condition, targetAps.iterator().next());
			} else {
				matches = IndexManager.instance.queryIndex(root, condition);
			}
			if (doupdate)
				IndexManager.instance.triggerUpdate(pseudo, q.getCache(), q.getCache().getExecutor(), index, targetAps);
			AccessLog.log("Index use: prep=" + (t2 - t1) + " query=" + (System.currentTimeMillis() - t2));
			return matches;

		}

		public void revalidate(MidataId executor, List<DBRecord> result) throws AppException {
			if (index == null)
				return;
			IndexManager.instance.revalidate(result, executor, pseudo, root, revalidationQuery, condition);
		}

		public long version(MidataId aps) {
			if (root != null && index != null) {
				if (aps == null)
					return root.getAllVersion();
				return root.getVersion(aps);
			}
			return -1;
		}
	}

	class IndexAnd implements IndexUse {
		List<IndexUse> parts;

		IndexAnd(List<IndexUse> parts) {
			this.parts = parts;
		}

		@Override
		public Collection<IndexMatch> query(Query q, Set<MidataId> targetAps) throws AppException {
			Collection<IndexMatch> results = null;

			int divider = 100;
			
			for (IndexUse part : parts) {
				if (results != null && part instanceof IndexAccess && results.size() < divider) {
					int coverage = ((IndexAccess) part).getCoverage();
					 if (coverage > 10 && ((IndexAccess) part).getCoverage() > 100 * results.size() / divider) {
						((IndexAccess) part).dontuse();
						continue;
					 }
				}
				
				Collection<IndexMatch> partResult = part.query(q, targetAps);
				if (results == null) {
					results = partResult;
					if (part instanceof IndexAccess) {
						divider = ((IndexAccess) part).getEstimatedTotal();
					}
					
				} else if (partResult != null) {
					Set<IndexMatch> check = new HashSet<IndexMatch>(partResult);
					Collection<IndexMatch> newresult = new ArrayList<IndexMatch>();
					for (IndexMatch match : results) {
						if (check.contains(match))
							newresult.add(match);
					}
					results = newresult;
				}
				if (results.size() < NO_SECOND_INDEX_COUNT)
					return results;
			}
			return results;
		}

		@Override
		public void revalidate(MidataId executor, List<DBRecord> result) throws AppException {
			for (IndexUse part : parts)
				part.revalidate(executor, result); 
		}

		public long version(MidataId aps) {
			long r = -1;
			for (IndexUse part : parts) {
				long v = part.version(aps);
				if (r == -1)
					r = v;
				else if (v > -1 && v < r)
					r = v;
			}
			return r;
		}

	}

	class IndexOr implements IndexUse {
		List<IndexUse> parts;

		IndexOr(List<IndexUse> parts) {
			this.parts = parts;
		}

		@Override
		public Collection<IndexMatch> query(Query q, Set<MidataId> targetAps) throws AppException {
			Collection<IndexMatch> results = null;

			for (IndexUse part : parts) {
				Collection<IndexMatch> partResult = part.query(q, targetAps);
				if (results == null) {
					results = partResult;
				} else {
					results.addAll(partResult);
				}
			}
			return results;
		}

		@Override
		public void revalidate(MidataId executor, List<DBRecord> result) throws AppException {
			// for (IndexUse part : parts) part.revalidate(result);
		}

		public long version(MidataId aps) {
			long r = -1;
			for (IndexUse part : parts) {
				long v = part.version(aps);
				if (r == -1)
					r = v;
				else if (v > -1 && v < r)
					r = v;
			}
			return r;
		}
	}

	static class ContextComparator implements Comparator<AccessContext> {

		@Override
		public int compare(AccessContext o1, AccessContext o2) {
			int c = o1.getOwner().compareTo(o2.getOwner());
			return c == 0 ? o1.getTargetAps().compareTo(o2.getTargetAps()) : c;
		}

	}

}
