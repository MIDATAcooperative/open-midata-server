package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Consent;
import models.MidataId;
import utils.AccessLog;
import utils.access.index.IndexDefinition;
import utils.access.index.IndexMatch;
import utils.access.index.IndexRoot;
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
	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		if (q.restrictedBy("index") && !q.restrictedBy("_id")) {
			
			if (!q.restrictedBy("format")) throw new BadRequestException("error.invalid.query", "Queries using an index must be restricted by format!");
			
			IndexPseudonym pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getExecutor(), q.getApsId(), false);
			
		    if (pseudo == null) {
				List<DBRecord> recs = next.query(q);
				if (recs.size() > AUTOCREATE_INDEX_COUNT) {
					pseudo = IndexManager.instance.getIndexPseudonym(q.getCache(), q.getCache().getExecutor(), q.getApsId(), true);
				} else { return recs; }
			}			
			
			AccessLog.logBegin("start index query");
			long startTime = System.currentTimeMillis();
			
			Object indexQueryUnparsed = q.getProperties().get("index");
			Condition indexQueryParsed = null;
			
			if (indexQueryUnparsed instanceof Condition) {
				indexQueryParsed = (Condition) indexQueryUnparsed;
			} else {
				indexQueryParsed = AndCondition.parseRemaining(indexQueryUnparsed).optimize();
				AccessLog.log("Optimized query: "+indexQueryParsed.toString());
				indexQueryParsed = indexQueryParsed.indexExpression();
			}
			
			AccessLog.log("Index query: "+indexQueryParsed.toString());
				
																						
			Set<MidataId> targetAps;
			
			if (!q.getApsId().equals(q.getCache().getAccountOwner())) {
				targetAps = Collections.singleton(q.getApsId());
			} else {
				boolean allTarget = Feature_AccountQuery.allApsIncluded(q);
													
				if (allTarget) {
				   targetAps = null;			   			  
				} else {
				   Set<Consent> consents = Feature_AccountQuery.getConsentsForQuery(q);			
				   targetAps =  new HashSet<MidataId>();
				   if (Feature_AccountQuery.mainApsIncluded(q)) targetAps.add(q.getApsId());
				   for (Consent consent : consents) targetAps.add(consent._id);
				}
			}
						
			List<DBRecord> result = new ArrayList<DBRecord>();
			
			
			IndexUse myAccess = parse(pseudo, q.getRestriction("format"), indexQueryParsed);			
				
			long afterPrepareTime = System.currentTimeMillis();
			
			Collection<IndexMatch> matches = myAccess.query(q, targetAps);
			
			long afterQuery = System.currentTimeMillis();
			
			Map<MidataId, Set<MidataId>> filterMatches = new HashMap<MidataId, Set<MidataId>>();
			for (IndexMatch match : matches) {
				if (targetAps == null || targetAps.contains(match.apsId)) {
					Set<MidataId> ids = filterMatches.get(match.apsId);
					if (ids == null) {
						ids = new HashSet<MidataId>();
						filterMatches.put(match.apsId, ids);
					}
					ids.add(match.recordId);				
				}
			}
			
			Set<String> queryFields = Sets.create("stream", "time", "document", "part", "direct", "encryptedData");
			queryFields.addAll(q.getFieldsFromDB());
			
			for (Map.Entry<MidataId, Set<MidataId>> entry : filterMatches.entrySet()) {				
			   MidataId aps = entry.getKey();
			   AccessLog.log("Now processing aps:"+aps.toString());
			   			  
			   if (q.getCache().getAPS(aps).isUsable()) {
			   
				   Set<MidataId> ids = entry.getValue();
				   
				   if (ids.size() > INDEX_REVERSE_USE) {
					   Query q4 = new Query(q, CMaps.map(), aps);
					   List<DBRecord> unindexed = next.query(q4);
					   for (DBRecord candidate : unindexed) {
						   candidate.consentAps = aps;
						   if (ids.contains(candidate._id)) result.add(candidate);
					   }
					   AccessLog.log("add unindexed ="+unindexed.size());
					   //result.addAll(unindexed);
				   } else {
					   Map<String, Object> readRecs = new HashMap<String, Object>();
					   boolean add = false;
					   boolean directQuery = true;
					   if (ids.size() > 5) {
						    Map<String, Object> props = new HashMap<String, Object>();
							props.putAll(q.getProperties());
							props.put("streams", "only");
							List<DBRecord> matchStreams = next.query(new Query(props, Sets.create("_id"), q.getCache(), aps));
							AccessLog.log("index query streams "+matchStreams.size()+" matches.");
							if (matchStreams.isEmpty()) directQuery = false;
							else {
								Set<MidataId> streams = new HashSet<MidataId>();
								for (DBRecord r : matchStreams) streams.add(r._id);
								readRecs.put("stream", streams);
							}
							add = true;
					   }
					   readRecs.put("_id", ids);
					
					   int directSize = 0;
					   if (directQuery) {
						   long time = System.currentTimeMillis();
						   List<DBRecord> partresult = new ArrayList(DBRecord.getAll(readRecs, queryFields));
						   AccessLog.log("db time:"+(System.currentTimeMillis() - time));
						   
						   Query q3 = new Query(q, CMaps.map("strict", "true"), aps);
						   partresult = Feature_Prefetch.lookup(q3, partresult, next);
						   for (DBRecord record : partresult) record.consentAps = aps;
						   result.addAll(partresult);
						   directSize = partresult.size();
					   }
						
						if (add) {
			              Query q2 = new Query(q, CMaps.map(q.getProperties()).map("_id", ids), aps);
			              List<DBRecord> additional = next.query(q2);
			              for (DBRecord record : additional) record.consentAps = aps;
			              result.addAll(additional);
			              AccessLog.log("looked up directly="+directSize+" additionally="+additional.size());
						} else {
			              AccessLog.log("looked up directly="+directSize);
						}		            
						
				   }
			   }
			}
			long endTime = System.currentTimeMillis();

			AccessLog.log("index query found "+matches.size()+" matches, "+result.size()+" in correct aps.");
				
			myAccess.revalidate(result);				
			
			long afterRevalidateTime = System.currentTimeMillis();
			
			AccessLog.logEnd("end index query "+result.size()+" matches. timePrepare="+(afterPrepareTime-startTime)+" exec="+(afterQuery-afterPrepareTime)+" postLookup="+(endTime-afterQuery)+" revalid="+(afterRevalidateTime-endTime));
			return result;
						
			
		} else return next.query(q);
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
		} else throw new InternalServerException("error.internal", "Bad index query expression");
	}
	
	private final Map<MidataId, IndexRoot> cachedIndexRoots = new HashMap<MidataId, IndexRoot>();
	
	interface IndexUse {		
		Collection<IndexMatch> query(Query q, Set<MidataId> targetAps) throws AppException;
		void revalidate(List<DBRecord> result) throws AppException;
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
		
		
		IndexAccess(IndexPseudonym pseudo, Set<String> format, Condition indexQuery) throws InternalServerException {
			this.pseudo = pseudo;
			this.format = format;
			this.revalidationQuery = indexQuery;			
			
			int idx = 0;
			condition = new Condition[1 /*indexQuery.size() */];
			pathes = new ArrayList();

			if (indexQuery instanceof FieldAccess) {
				
			  FieldAccess fa = (FieldAccess) indexQuery;
			  pathes.add(fa.getField());
			  condition[0] = AndCondition.parseRemaining(fa.getCondition()).optimize();
			} else if (indexQuery instanceof AlternativeFieldAccess) {
			  AlternativeFieldAccess fa = (AlternativeFieldAccess) indexQuery;
			  pathes.add(fa.getField());
			  condition[0] = AndCondition.parseRemaining(fa.getCondition()).optimize();
			} else throw new InternalServerException("error.internal", "Index expression not useable.");

		}
		
		public void prepare() throws AppException {
			index = IndexManager.instance.findIndex(pseudo, format, pathes);
			if (index == null) { 
				AccessLog.logBegin("start index creation");
				index = IndexManager.instance.createIndex(pseudo, format, pathes);
				AccessLog.logEnd("end index creation");
			}
		}
		
		public Collection<IndexMatch> query(Query q, Set<MidataId> targetAps) throws AppException {
			long t1 = System.currentTimeMillis();
			prepare();
			root = cachedIndexRoots.get(index._id);
			long t2 = System.currentTimeMillis();
			if (root == null) {
			  root = IndexManager.instance.getIndexRootAndUpdate(pseudo, q.getCache(), q.getCache().getExecutor(), index, targetAps);
			  cachedIndexRoots.put(index._id, root);
			}
			long t3 = System.currentTimeMillis();
			matches = IndexManager.instance.queryIndex(root, condition);
			AccessLog.log("Index use: prep="+(t2-t1)+" update="+(t3-t2)+" query="+(System.currentTimeMillis() - t3));
			return matches;
			
		}
		
		public void revalidate(List<DBRecord> result) throws AppException {
			if (index==null) return;
			IndexManager.instance.revalidate(result, root, revalidationQuery, condition);
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
			
			for (IndexUse part : parts) {
				Collection<IndexMatch> partResult = part.query(q, targetAps);								
				if (results == null) {
					results = partResult;
				} else {
					Set<IndexMatch> check = new HashSet<IndexMatch>(partResult);
					Collection<IndexMatch> newresult = new ArrayList<IndexMatch>();
					for (IndexMatch match : results) {
						if (check.contains(match)) newresult.add(match);
					}
					results = newresult;
				}
				if (results.size() < NO_SECOND_INDEX_COUNT) return results;
			}
			return results;
		}

		@Override
		public void revalidate(List<DBRecord> result) throws AppException {
			for (IndexUse part : parts) part.revalidate(result);			
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
		public void revalidate(List<DBRecord> result) throws AppException {
			//for (IndexUse part : parts) part.revalidate(result);			
		}
	}

	
}
