package utils.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Circle;
import models.Consent;
import models.MidataId;
import models.StudyParticipation;
import models.enums.ConsentType;
import utils.AccessLog;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.RequestTooLargeException;

/**
 * queries made to the access permission set of a user 
 * also query his other access permission sets based on the query parameters.
 *
 */
public class Feature_AccountQuery extends Feature {

	public final static int MAX_CONSENTS_IN_QUERY = 100;
	
	private Feature next;

	public Feature_AccountQuery(Feature next) {
		this.next = next;
	}

	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {

		if (q.getApsId().equals(q.getCache().getAccountOwner())) {
			if (AccessLog.detailedLog) AccessLog.logBegin("Begin process owner aps");
			long time = System.currentTimeMillis();
			Set<String> sets = q.restrictedBy("owner") ? q.getRestriction("owner") : Collections.singleton("all");
			
			List<DBRecord> result = null;

		

			if ((sets.contains("self") || sets.contains("all") || sets.contains(q.getApsId().toString())) && !q.restrictedBy("consent-after") && !q.restrictedBy("usergroup") && !q.restrictedBy("study")) {
				result = next.query(q);
				setIdAndConsentField(q, q.getContext(), q.getApsId(), result);						
			} else {
				result = new ArrayList<DBRecord>();
			}

			Set<Consent> consents = getConsentsForQuery(q);
			
			if (!q.restrictedBy("consent-limit")) {
				if (consents.size() > MAX_CONSENTS_IN_QUERY) throw new RequestTooLargeException("error.toomany.consents", "Too many consents in query #="+consents.size());
			}
									
			for (Consent circle : consents) {
			   AccessLog.logBegin("start query for consent id="+circle._id);
			   List<DBRecord> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), circle._id, new ConsentAccessContext(circle, q.getContext())));
			   //setOwnerField(q, circle, consentRecords);
			   setIdAndConsentField(q, new ConsentAccessContext(circle, q.getContext()), circle._id, consentRecords);							
			   result.addAll(consentRecords);
			   AccessLog.logEnd("end query for consent");
			}				
				
			
			if (AccessLog.detailedLog) AccessLog.logEnd("End process owner aps #size="+result.size()+" time="+(System.currentTimeMillis()-time));
			return result;
		} else {
			
			List<DBRecord> result = next.query(q);

			setIdAndConsentField(q, q.getContext(), q.getApsId(), result);
			
			return result;
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
	private static Set<Consent> applyConsentTimeFilter(Query q, Set<Consent> consents) throws AppException {
		if (q.restrictedBy("consent-after") && !consents.isEmpty()) {
			long consentDate = ((Date) q.getProperties().get("consent-after")).getTime();
			Set<Consent> filtered = new HashSet<Consent>(consents.size());
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
	
	private static Set<Consent> applyWriteFilters(Query q, Set<Consent> consents) throws AppException {
		if (q.restrictedBy("updatable") && !consents.isEmpty()) {			
			Set<Consent> filtered = new HashSet<Consent>(consents.size());
			for (Consent consent : consents) {
			   if (consent.writes == null || consent.writes.isUpdateAllowed()) filtered.add(consent);
			}
			return filtered;
		}
		return consents;
	}
	
	protected static boolean mainApsIncluded(Query q) throws AppException {
		if (!q.restrictedBy("owner")) return true;
		Set<String> sets = q.getRestriction("owner");
		return sets.contains("self") || sets.contains("all") || sets.contains(q.getApsId().toString());
	}
	
	protected static boolean allApsIncluded(Query q) throws BadRequestException {
		if (!q.restrictedBy("owner") && !q.restrictedBy("study") && !q.restrictedBy("study-group")) return true;
		return false;
	}
	
	protected static Set<Consent> getConsentsForQuery(Query q) throws AppException {
		Set<Consent> consents = Collections.EMPTY_SET;
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
						if (!id.equals(q.getCache().getAccountOwner())) owners.add(id);
					}
				}
	    	}
	    	
	    	if (q.restrictedBy("study-group")) {
	    		studyGroups = q.getRestriction("study-group");
	    	}
	    	
	    	//consents = new HashSet<Consent>(StudyParticipation.getActiveParticipantsByStudyAndGroupsAndIds(studies, studyGroups, q.getCache().getAccountOwner(), sets.contains("all") ? null : owners, Sets.create("name", "order", "owner", "ownerName", "type")));
	    	
	    		    		
	    	consents =  new HashSet<Consent>(StudyParticipation.getActiveParticipantsByStudyAndGroupsAndParticipant(studies, studyGroups, q.getCache().getAccountOwner(), sets.contains("all") ? null : owners, Sets.create("name", "order", "owner", "ownerName", "type"), true));
	    		    
	    	AccessLog.log("found: "+consents.size());
	    } else if (sets.contains("all") || sets.contains("other") || sets.contains("shared")) {			
			if (sets.contains("shared"))
				consents = new HashSet<Consent>(Circle.getAllActiveByMember(q.getCache().getAccountOwner()));
			else {
				long limit = 0;
				if (q.restrictedBy("created-after")) limit = q.getMinCreatedTimestamp();
				if (q.restrictedBy("updated-after")) limit = q.getMinUpdatedTimestamp();				
				if (q.restrictedBy("shared-after")) limit = q.getMinSharedTimestamp();

				consents = new HashSet<Consent>(q.getCache().getAllActiveConsentsByAuthorized(limit));
			}
		} else {
			Set<MidataId> owners = new HashSet<MidataId>();
			for (String owner : sets) {
				if (MidataId.isValid(owner)) {
					MidataId id = new MidataId(owner);
					if (!id.equals(q.getCache().getAccountOwner())) owners.add(id);
				}
			}
			if (!owners.isEmpty()) {
				consents = Consent.getAllActiveByAuthorizedAndOwners(q.getCache().getAccountOwner(), owners);
				//if (consents.size() < owners.size()) consents.addAll(Consent.getByIdsAndAuthorized(owners, q.getCache().getAccountOwner(), Sets.create("name", "order", "owner", "type", "ownerName")));
			}
		}
		consents = applyConsentTimeFilter(q, consents);
		
		q.getCache().cache(consents);
		
		consents = applyWriteFilters(q, consents);
		return consents;
	}			

}
