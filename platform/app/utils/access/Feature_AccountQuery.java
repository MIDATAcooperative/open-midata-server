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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import models.Circle;
import models.Consent;
import models.MidataId;
import models.StudyParticipation;
import models.StudyRelated;
import utils.AccessLog;
import utils.context.AccessContext;
import utils.context.ConsentAccessContext;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.RequestTooLargeException;

/**
 * queries made to the access permission set of a user 
 * also query his other access permission sets based on the query parameters.
 *
 */
public class Feature_AccountQuery extends Feature {

	public final static int MAX_CONSENTS_IN_QUERY = 1000;
	public final static int MIN_FOR_ACCELERATION = 10;
	
	private Feature next;

	public Feature_AccountQuery(Feature next) {
		this.next = next;
	}
	
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {

		if (q.getApsId().equals(q.getCache().getAccountOwner()) && !q.restrictedBy("force-local")) {			
			return new AccountThenConsents(next, q);
		} else {		
			DBIterator<DBRecord> result = next.iterator(q);
			if (result.hasNext()) {
				result = new IdAndConsentFieldIterator(result, q.getContext(), q.getApsId(), q.returns("id"));
			}			
			return result;
		}
	}
	
	static class AccountThenConsents extends Feature.MultiSource<Integer> {
			
		private Feature next;	
		private boolean account;
		
		AccountThenConsents(Feature next, Query q) throws AppException {						
			this.next = next;
			this.query = q;
			
			Integer[] steps = {1,2};
			init(ProcessingTools.dbiterator("",  Arrays.asList(steps).iterator()));
		}
		
		@Override
		public DBIterator<DBRecord> advance(Integer step) throws AppException {
            if (step == 1) {
            	account = true;
            	Set<String> sets = query.restrictedBy("owner") ? query.getRestriction("owner") : Collections.singleton("all");    			    		

    			if (((sets.contains("self") || sets.contains("all") || sets.contains(query.getApsId().toString())) && !query.restrictedBy("consent-after") && !query.getContext().isUserGroupContext() /*("usergroup"*/ && !query.restrictedBy("study") /*&& !query.restrictedBy("study-related")*/)) {
    				return new IdAndConsentFieldIterator(next.iterator(query), query.getContext(), query.getApsId(), query.returns("id"));						
    			} else {
    				return ProcessingTools.empty();
    			}
            } else if (step == 2) {            	            	
            
            	account = false;
            	
            	List<Consent> consents = getConsentsForQuery(query, false, query.restrictedBy("consent-limit"));
    			
            	if (query.restrictedBy("consent-limit")) {
            		
            		if (consents.size() > MAX_CONSENTS_IN_QUERY) {
            			AccessLog.logQuery(query.getApsId(), query.getProperties(), query.getFields());
            			throw new RequestTooLargeException("error.toomany.consents", "Too many consents in query #="+consents.size());
            		}
            		
            	}
            	
            	/*
    			if (!q.restrictedBy("consent-limit")) {
    				if (consents.size() > MAX_CONSENTS_IN_QUERY) throw new RequestTooLargeException("error.toomany.consents", "Too many consents in query #="+consents.size());
    			}*/
            	if (consents.isEmpty()) return ProcessingTools.empty();    									            
            	
            	return ProcessingTools.noDuplicates(new ConsentIterator(next, query, consents));
    						
            }
			return null;
		}

		@Override
		public String toString() {			
			return (account ? "account(" : "acc-consents(")+"["+passed+"] "+current.toString()+")";
		}
						
	}
	
	static class BlockwiseConsentPrefetch implements DBIterator<Consent> {

		private int blocksize;
		private int pos;
		//private int maxsize;
		protected Iterator<Consent> all;
		protected Iterator<Consent> cache;	
		private APSCache apscache;
		private boolean freemem = false;

		public BlockwiseConsentPrefetch(Query q, List<Consent> consents, int blocksize) {
			this.all = consents.iterator();
			this.blocksize = blocksize;			
			this.apscache = q.getCache();
			this.cache = Collections.emptyIterator();
			//this.maxsize = consents.size();
			this.pos = 0;		
		}
		
		public BlockwiseConsentPrefetch(APSCache cache, Iterator<Consent> consents, int blocksize) {
			this.all = consents;
			this.blocksize = blocksize;			
			this.apscache = cache;
			this.cache = Collections.emptyIterator();
			//this.maxsize = consents.size();
			this.pos = 0;		
			this.freemem = true;
		}
		
		public BlockwiseConsentPrefetch(Query q, List<Consent> consents, int blocksize, int startpos) {
			this.all = consents.subList(startpos, consents.size()).iterator();
			this.blocksize = blocksize;			
			this.apscache = q.getCache();
			this.cache = Collections.emptyIterator();
			//this.maxsize = consents.size();
			this.pos = startpos;		
		}

		@Override
		public boolean hasNext() {
			return cache.hasNext() || all.hasNext();
		}
				
		@Override
		public Consent next() throws AppException {
			if (cache.hasNext())
				return cache.next();

			int end = 0;
			
			if (freemem) apscache.resetConsentCache();
			
			List<Consent> sublist = new ArrayList<Consent>(blocksize);
			while (all.hasNext() && end<blocksize) { sublist.add(all.next());end++; }
			
			FasterDecryptTool.accelerate(apscache, sublist);
			
			//AccessLog.log("get consent "+pos+" - "+(pos+end));
			pos = pos+end;
			cache = sublist.iterator();			         
			return cache.next();

		}			
		
		@Override
		public void close() {
		   // Close Consent iteration?			
		}

	}
	
	static class ConsentIterator extends Feature.MultiSource<Consent> {

		private Feature next;
		private Consent thisconsent;
		
		ConsentIterator(Feature next, Query q, List<Consent> consents) throws AppException {	
			this.next = next;
			this.query = q;					
			
			if (q.getFromRecord() != null) {
				AccessLog.log("CONSENT ITERATOR from=",q.getFromRecord().toString());
				DBRecord r = q.getFromRecord();
				if (r.context.getOwner() != null) {
				  MidataId targetOwner = r.context.getOwner();
				  //AccessLog.log("ConsentIterator: fromRecord="+r._id+" owner="+targetOwner);
				  int pos = 0;
				  Iterator<Consent> it = consents.iterator();
				  while (it.hasNext()) {					  
					  Consent c = it.next();
					  if (c.owner.equals(targetOwner)) {						  
						  AccessLog.log("ConsentIterator: skipping ",Integer.toString(pos)," consents");
						  init(new BlockwiseConsentPrefetch(q, consents, 105, pos));
						  return;
					  }
					  pos++;
				  }
				  init(ProcessingTools.dbiterator("", it));
				  return;
				} else AccessLog.log("ConsentIterator: fromRecord!=null owner==null");
			}
			init(new BlockwiseConsentPrefetch(q, consents, 105));
		}
		
		@Override
		public DBIterator<DBRecord> advance(Consent circle) throws AppException {
			ConsentAccessContext context = new ConsentAccessContext(circle, query.getContext());
			DBIterator<DBRecord> consentRecords = next.iterator(new Query("consent","consent="+circle._id,query.getProperties(), query.getFields(), query.getCache(), circle._id, context, query));
			thisconsent = circle;
			return new IdAndConsentFieldIterator(consentRecords, context, circle._id, query.returns("id"));
		}

		@Override
		public String toString() {
			if (thisconsent==null) return "consent(done)";
			return "consent(["+passed+"] "+thisconsent._id.toString()+","+current.toString()+")";
		}
		
		
		
		
		
	}

	static class IdAndConsentFieldIterator implements DBIterator<DBRecord> {

		private DBIterator<DBRecord> chain;
		private AccessContext context;
		private MidataId sourceAps;
		private boolean setid;
		
		IdAndConsentFieldIterator(DBIterator<DBRecord> chain, AccessContext context, MidataId sourceAps, boolean setid) {
			this.chain = chain;
			this.context = context;
			this.sourceAps = sourceAps;
			this.setid = setid;
		}
		
		@Override
		public boolean hasNext() throws AppException {
			return chain.hasNext();
		}

		@Override
		public DBRecord next() throws AppException {
			DBRecord record = chain.next();
			record.context = context;
			record.consentAps = sourceAps;
			if (setid) record.id = record._id.toString() + "." + sourceAps.toString();
			
			return record;
		}

		@Override
		public String toString() {
			return "set-context("+chain.toString()+")";
		}
		
		@Override
		public void close() {
			chain.close();			
		}
		
		
		
	}

	private void setIdAndConsentField(Query q, AccessContext context, MidataId sourceAps, List<DBRecord> targetRecords) {
		for (DBRecord record : targetRecords) record.context = context;
		if (q.returns("id")) {
			for (DBRecord record : targetRecords)
				record.id = record._id.toString() + "." + sourceAps.toString();
		}
		if (q.returns("consentAps")) {
			for (DBRecord record : targetRecords) record.consentAps = sourceAps;
		}
	}
	
	/*
	protected static void setOwnerField(Query q, Consent c, List<DBRecord> targetRecords) throws AppException {
		boolean oname = q.returns("ownerName");
		if (q.returns("owner") || oname) {
			for (DBRecord record : targetRecords) {
				
					record.owner = (c.type.equals(ConsentType.STUDYPARTICIPATION) && c.ownerName != null && q.getContext().mustPseudonymize()) ? c._id : c.owner;
					
					if (oname && c.ownerName != null) {
						QueryEngine.fetchFromDB(q, record);
						RecordEncryption.decryptRecord(record);						
						record.meta.put("ownerName", c.ownerName);
						
						//Bugfix for older records
						String creator = record.meta.getString("creator");
						if (creator != null && creator.equals(c.owner.toString())) record.meta.remove("creator");
					}					
				
			}
		}		
	}
	
	protected static void setOwnerField(Query q, List<DBRecord> targetRecords) throws AppException {
		if (q.returns("owner") || q.returns("ownerName")) {			
			Consent c = q.getCache().getConsent(q.getApsId());	
			if (c != null) {
			  Feature_AccountQuery.setOwnerField(q, c, targetRecords);
			}
		}		
	}
	*/
	private static List<Consent> applyConsentTimeFilter(Query q, List<Consent> consents) throws AppException {
		if (q.restrictedBy("consent-after") && !consents.isEmpty()) {
			long consentDate = ((Date) q.getProperties().get("consent-after")).getTime();
			List<Consent> filtered = new ArrayList<Consent>(consents.size());
			for (Consent consent : consents) {
			   APS consentaps = q.getCache().getAPS(consent._id, consent.owner);
			   if (consentDate < consentaps.getLastChanged()) {
				   filtered.add(consent);
			   }	
			}
			return filtered;
		}
		return consents;
	}
	
	private static List<Consent> applyLimit(List<Consent> consents, long limit) throws AppException {
		if (limit > 0 && !consents.isEmpty()) {
		   List<Consent> filtered = new ArrayList<Consent>(consents.size());
		   for (Consent c : consents) {
			   if (c.dataupdate > limit) filtered.add(c);
		   }
		   return filtered;
		}
		return consents;
	}
	
	private static List<Consent> applyWriteFilters(Query q, List<Consent> consents) throws AppException {
		if (q.restrictedBy("updatable") && !consents.isEmpty()) {			
			List<Consent> filtered = new ArrayList<Consent>(consents.size());
			for (Consent consent : consents) {
			   if (consent.writes == null || consent.writes.isUpdateAllowed()) filtered.add(consent);
			}
			return filtered;
		}
		return consents;
	}
	
	private static List<Consent> applyTypeFilters(Query q, List<Consent> consents) throws AppException {
		Set<String> types = q.getRestrictionOrNull("consent-type-exclude");
		if (types != null && !consents.isEmpty()) {			
			List<Consent> filtered = new ArrayList<Consent>(consents.size());
						
			for (Consent consent : consents) {
			   if (!types.contains(consent.type.toString())) filtered.add(consent);
			   else if (consent.owner.equals(q.getContext().getSelf())) filtered.add(consent);
			}
			return filtered;
		}
		return consents;
	}
	
	protected static boolean mainApsIncluded(Query q) throws AppException {
		if (!q.restrictedBy("owner")) return true;
		Set<String> sets = q.getRestrictionOrNull("owner");
		return sets == null || sets.contains("self") || sets.contains("all") || sets.contains(q.getApsId().toString());
	}
	
	protected static boolean allApsIncluded(Query q) throws BadRequestException {
		if (!q.restrictedBy("owner") && !q.restrictedBy("consent-type-exclude")) return true;
		return false;
	}
	
	protected static List<Consent> getConsentsForQuery(Query q, boolean prefetch, boolean withLimit) throws AppException {
		List<Consent> consents = Collections.emptyList();
		Set<String> sets = q.restrictedBy("owner") ? q.getRestriction("owner") : Collections.singleton("all");
		Set<MidataId> studies = q.restrictedBy("study") ? q.getMidataIdRestriction("study") : null;
		Set<String> studyGroups = null;
		
	    if (q.restrictedBy("study")) {
	    	Set<MidataId> owners = null;
	    	
	    	if (!sets.contains("all")) {
		    	owners = new HashSet<MidataId>();
				for (String owner : sets) {
					if (MidataId.isValid(owner)) {
						MidataId id = new MidataId(owner);
						// removed condition: if (!id.equals(q.getCache().getAccountOwner()))*/
						// prevents get by id for resources of project analyzer
						owners.add(id);
					}
				}				
	    	}
	    	
	    	if (q.restrictedBy("study-group")) {
	    		studyGroups = q.getRestriction("study-group");
	    	}
	    	
	    	//consents = new HashSet<Consent>(StudyParticipation.getActiveParticipantsByStudyAndGroupsAndIds(studies, studyGroups, q.getCache().getAccountOwner(), sets.contains("all") ? null : owners, Sets.create("name", "order", "owner", "ownerName", "type")));
	    	long limit = 0;
			if (q.restrictedBy("created-after")) limit = q.getMinCreatedTimestamp();
			if (q.restrictedBy("updated-after")) limit = Math.max(limit, q.getMinUpdatedTimestamp());				
			if (q.restrictedBy("shared-after")) limit = Math.max(limit,  q.getMinSharedTimestamp());
	    		   
			if (withLimit) {
				if (q.restrictedBy("consent-after") || q.restrictedBy("consent-type-exclude") || q.restrictedBy("updatable")) withLimit = false;
			}
			
			if (q.restrictedBy("study-related")) {	
				if (q.getStringRestriction("study-related").equals("public")) {
					consents = new ArrayList<Consent>(StudyRelated.getActiveByAuthorizedGroupAndStudyPublic(q.getCache().getAccountOwner(), studyGroups, studies, sets.contains("all") ? null : owners, Consent.SMALL, limit));	
				} else {
				    consents = new ArrayList<Consent>(StudyRelated.getActiveByAuthorizedGroupAndStudy(q.getCache().getAccountOwner(), studyGroups, studies, sets.contains("all") ? null : owners, Consent.SMALL, limit));
				}
			} else if (q.restrictedBy("participant-related")) {
				consents =  new ArrayList<Consent>(StudyParticipation.getActiveOrRetreatedParticipantsByStudyAndGroupsAndParticipant(studies, studyGroups, q.getCache().getAccountOwner(), sets.contains("all") ? null : owners, Consent.SMALL, true, limit, withLimit ? (10+MAX_CONSENTS_IN_QUERY) : Integer.MAX_VALUE));		    	
			} else {
		    	consents =  new ArrayList<Consent>(StudyParticipation.getActiveOrRetreatedParticipantsByStudyAndGroupsAndParticipant(studies, studyGroups, q.getCache().getAccountOwner(), sets.contains("all") ? null : owners, Consent.SMALL, true, limit, withLimit ? (10+MAX_CONSENTS_IN_QUERY) : Integer.MAX_VALUE));
		    	AccessLog.log("found consents (participants): ", Integer.toString(consents.size()));
		    	consents.addAll(StudyRelated.getActiveByAuthorizedGroupAndStudy(q.getCache().getAccountOwner(), studyGroups, studies, sets.contains("all") ? null : owners, Consent.SMALL, limit));
			}
	    	// consents = applyLimit(consents, limit); Alread done by read query
            q.getCache().cache(consents);	    		    
	    	AccessLog.log("found consents (total): ", Integer.toString(consents.size()));
	    } else if (sets.contains("all") || sets.contains("other") || sets.contains("shared")) {			
			if (sets.contains("shared"))
				consents = new ArrayList<Consent>(Circle.getAllActiveByMember(q.getCache().getAccountOwner()));
			else {
				long limit = 0;
				if (q.restrictedBy("created-after")) limit = q.getMinCreatedTimestamp();
				if (q.restrictedBy("updated-after")) limit = Math.max(limit,  q.getMinUpdatedTimestamp());				
				if (q.restrictedBy("shared-after")) limit = Math.max(limit,  q.getMinSharedTimestamp());

				consents = new ArrayList<Consent>(q.getCache().getAllActiveConsentsByAuthorized(limit));
				consents = applyLimit(consents, limit);
			}
		} else {
			Set<MidataId> owners = new HashSet<MidataId>();
			for (String owner : sets) {
				if (MidataId.isValid(owner)) {
					MidataId id = new MidataId(owner);
					if (!id.equals(q.getCache().getAccountOwner()) || q.getContext().isUserGroupContext()) owners.add(id);
				}
			}
			if (!owners.isEmpty()) {
				long limit = 0;
				if (q.restrictedBy("created-after")) limit = q.getMinCreatedTimestamp();
				if (q.restrictedBy("updated-after")) limit = Math.max(limit,  q.getMinUpdatedTimestamp());				
				if (q.restrictedBy("shared-after")) limit = Math.max(limit,  q.getMinSharedTimestamp());
				
				consents = q.getCache().getAllActiveByAuthorizedAndOwners(owners, limit);
				consents = applyLimit(consents, limit);
				//if (consents.size() < owners.size()) consents.addAll(Consent.getByIdsAndAuthorized(owners, q.getCache().getAccountOwner(), Sets.create("name", "order", "owner", "type", "ownerName")));
			}
		}
		consents = applyConsentTimeFilter(q, consents);
		consents = applyTypeFilters(q, consents);
		if (prefetch) {
			if (consents.size() > MIN_FOR_ACCELERATION) {
				FasterDecryptTool.accelerate(q.getCache(), consents);
			} else if (consents.size() < MAX_CONSENTS_IN_QUERY) q.getCache().prefetch(consents, null);
		}
		consents = applyWriteFilters(q, consents);
		Collections.sort(consents);
		return consents;
	}			

}
