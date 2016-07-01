package utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.AccessLog;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;


import models.Circle;
import models.Consent;
import models.Record;
import models.StudyParticipation;
import models.enums.ConsentType;

/**
 * queries made to the access permission set of a user 
 * also query his other access permission sets based on the query parameters.
 *
 */
public class Feature_AccountQuery extends Feature {

	private Feature next;

	public Feature_AccountQuery(Feature next) {
		this.next = next;
	}

	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {

		if (q.getApsId().equals(q.getCache().getOwner())) {
			if (AccessLog.detailedLog) AccessLog.logBegin("Begin process owner aps");
			Set<String> sets = q.restrictedBy("owner") ? q.getRestriction("owner") : Collections.singleton("all");
			Set<ObjectId> studies = q.restrictedBy("study") ? q.getObjectIdRestriction("study") : null;

			List<DBRecord> result = null;

			if (studies != null) {
				result = new ArrayList<DBRecord>();

				Set<StudyParticipation> consents = new HashSet<StudyParticipation>();
				
				if (q.restrictedBy("study-group")) {
					Set<String> groups = q.getRestriction("study-group");
					for (ObjectId studyId : studies) {
						for (String grp : groups) {
							consents.addAll(StudyParticipation.getActiveParticipantsByStudyAndGroup(studyId, grp, Sets.create("pstatus", "ownerName")));
						}
					}
					
				} else {				
					for (ObjectId studyId : studies) {
						consents.addAll(StudyParticipation.getActiveParticipantsByStudy(studyId, Sets.create("pstatus", "ownerName")));
					}
				}

				for (StudyParticipation sp : consents) {
					List<DBRecord> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), sp._id));

					
					setOwnerField(q, sp, consentRecords);
					setIdAndConsentField(q, sp._id, consentRecords);							
					result.addAll(consentRecords);
				}
			} else {

				if ((sets.contains("self") || sets.contains("all") || sets.contains(q.getApsId().toString())) && !q.restrictedBy("consent-after")) {
					result = next.query(q);
					setIdAndConsentField(q, q.getApsId(), result);						
				} else {
					result = new ArrayList<DBRecord>();
				}

				Set<Consent> consents = getConsentsForQuery(q);
										
				for (Consent circle : consents) {
				   List<DBRecord> consentRecords = next.query(new Query(q.getProperties(), q.getFields(), q.getCache(), circle._id));
				   setOwnerField(q, circle, consentRecords);
				   setIdAndConsentField(q, circle._id, consentRecords);							
				   result.addAll(consentRecords);
				}				
				
			}
			if (AccessLog.detailedLog) AccessLog.logEnd("End process owner aps #size="+result.size());
			return result;
		} else {
			List<DBRecord> result = next.query(q);

			setIdAndConsentField(q, q.getApsId(), result);
			
			return result;
		}
	}
	
	private void setIdAndConsentField(Query q, ObjectId sourceAps, List<DBRecord> targetRecords) {
		if (q.returns("id")) {
			for (DBRecord record : targetRecords)
				record.id = record._id.toString() + "." + sourceAps.toString();
		}
		if (q.returns("consentAps")) {
			for (DBRecord record : targetRecords) record.consentAps = sourceAps;
		}
	}
	
	private void setOwnerField(Query q, Consent c, List<DBRecord> targetRecords) throws AppException {
		boolean oname = q.returns("ownerName");
		if (q.returns("owner") || oname) {
			for (DBRecord record : targetRecords) {
				
					record.owner = c.type.equals(ConsentType.STUDYPARTICIPATION) ? c._id : c.owner;
					
					if (oname) {
						QueryEngine.fetchFromDB(q, record);
						RecordEncryption.decryptRecord(record);						
						record.meta.put("ownerName", c.ownerName);
					}					
				
			}
		}		
	}
	
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
	
	protected static boolean mainApsIncluded(Query q) throws BadRequestException {
		if (!q.restrictedBy("owner")) return true;
		Set<String> sets = q.getRestriction("owner");
		return sets.contains("self") || sets.contains("all") || sets.contains(q.getApsId().toString());
	}
	protected static Set<Consent> getConsentsForQuery(Query q) throws AppException {
		Set<Consent> consents = Collections.EMPTY_SET;
		Set<String> sets = q.restrictedBy("owner") ? q.getRestriction("owner") : Collections.singleton("all");
		if (sets.contains("all") || sets.contains("other") || sets.contains("shared")) {			
			if (sets.contains("shared"))
				consents = new HashSet<Consent>(Circle.getAllActiveByMember(q.getCache().getOwner()));
			else
				consents = Consent.getAllActiveByAuthorized(q.getCache().getOwner());																
		} else {
			Set<ObjectId> owners = new HashSet<ObjectId>();
			for (String owner : sets) {
				if (ObjectId.isValid(owner)) {
					ObjectId id = new ObjectId(owner);
					if (!id.equals(q.getCache().getOwner())) owners.add(id);
				}
			}
			if (!owners.isEmpty()) {
				consents = Consent.getAllActiveByAuthorizedAndOwners(q.getCache().getOwner(), owners);
				if (consents.size() < owners.size()) consents.addAll(Consent.getByIdsAndAuthorized(owners, q.getCache().getOwner(), Sets.create("name", "order", "owner", "type", "ownerName")));
			}
		}
		consents = applyConsentTimeFilter(q, consents);
		return consents;
	}
	

}
