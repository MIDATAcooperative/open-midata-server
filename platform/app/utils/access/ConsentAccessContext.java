package utils.access;

import java.util.Collections;

import org.apache.commons.lang3.tuple.Pair;

import controllers.Circles;
import models.Consent;
import models.MidataId;
import models.Record;
import models.enums.ConsentType;
import utils.AccessLog;
import utils.exceptions.AppException;

public class ConsentAccessContext extends AccessContext{

	private Consent consent;
	private boolean sharingQuery;
	private String ownerName;
	private MidataId ownerpseudoId;
	//private final Set<String> reqfields = Sets.create("sharingQuery", "createdBefore", "validUntil");
	
	public ConsentAccessContext(Consent consent, APSCache cache, AccessContext parent) throws AppException {
		super(cache, parent);
		this.consent = consent;
		setStudyOwnerName();
		
	}
	
	public ConsentAccessContext(Consent consent, AccessContext parent) throws AppException {
		super(parent.getCache(), parent);
		this.consent = consent;
		setStudyOwnerName();
	}
	
	private void setStudyOwnerName() throws AppException {
		if (consent.type.equals(ConsentType.STUDYRELATED) && consent.ownerName == null) {
			consent.ownerName = consent.name;
			if (consent.ownerName != null && consent.ownerName.startsWith("Study:")) consent.ownerName = consent.ownerName.substring("Study:".length());
		}
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		if (consent.writes == null) return parent==null || parent.mayCreateRecord(record);
		if (!consent.writes.isCreateAllowed()) return false;
		if (consent.writes.isUnrestricted()) return parent==null || parent.mayCreateRecord(record);
		
		if (!sharingQuery && consent.sharingQuery == null) {
			  consent.sharingQuery = Circles.getQueries(consent.owner, consent._id);
			  sharingQuery = true;
		}
		if (consent.sharingQuery == null) return false;
		return !QueryEngine.listFromMemory(this, consent.sharingQuery, Collections.singletonList(record)).isEmpty() && (parent==null || parent.mayCreateRecord(record));
		
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) {
		AccessLog.log("called");
		if (consent.writes == null) return false;
		if (!consent.writes.isUpdateAllowed()) return false;
		AccessLog.log("called 2");
		if (consent.type.equals(ConsentType.STUDYRELATED)) {
			AccessLog.log("called 3");
			if (parent != null && parent instanceof UserGroupAccessContext && parent.parent != null) {
				AccessLog.log("called 4");
				return parent.parent.mayUpdateRecord(stored, newVersion);
			}
		}
		if (parent != null) return parent.mayUpdateRecord(stored, newVersion);
		return true;
	}

	@Override
	public boolean mustPseudonymize() {
		return (consent.type.equals(ConsentType.STUDYPARTICIPATION) && consent.ownerName != null && (parent == null || parent.mustPseudonymize()))
				|| (consent.type.equals(ConsentType.STUDYRELATED) && consent.ownerName != null);		
	}

	@Override
	public MidataId getTargetAps() {
		return consent._id;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		if (consent.writes == null) return false;
		
		if (!sharingQuery && consent.sharingQuery == null) {
			  consent.sharingQuery = Circles.getQueries(consent.owner, consent._id);
			  sharingQuery = true;
		}
		
		if (consent.sharingQuery == null) return false;
		return !QueryEngine.listFromMemory(this, consent.sharingQuery, Collections.singletonList(record)).isEmpty();
	}
	
	public Consent getConsent() {
		return consent;
	}

	public String getOwnerName() throws AppException {
		if (ownerName!=null) return ownerName;		
		Pair<MidataId,String> p = Feature_Pseudonymization.pseudonymizeUser(getCache(), consent);
		if (p!=null) {
		  ownerName = p.getRight();
		  ownerpseudoId = p.getLeft();
		}
		return ownerName;
		
	}

	@Override
	public MidataId getOwner() {
		return consent.owner;
	}

	@Override
	public MidataId getOwnerPseudonymized() throws AppException {
		if (ownerpseudoId!=null) return ownerpseudoId;
		Pair<MidataId,String> p = Feature_Pseudonymization.pseudonymizeUser(getCache(), consent);
		if (p!=null) {
		   ownerName = p.getRight();
		   ownerpseudoId = p.getLeft();
		}
		return ownerpseudoId;				
	}

	@Override
	public MidataId getSelf() {
		return parent != null ? parent.getSelf() : cache.getAccountOwner();
	}
	
	@Override
	public boolean mayAccess(String content, String format) throws AppException {
		if (!sharingQuery && consent.sharingQuery == null) {
			  consent.sharingQuery = Circles.getQueries(consent.owner, consent._id);
			  sharingQuery = true;
		}
		
		return Feature_FormatGroups.mayAccess(consent.sharingQuery, content, format);		
	}
	
	@Override
	public boolean mayContainRecordsFromMultipleOwners() {		
		return consent.type == ConsentType.EXTERNALSERVICE;
	}
	
	@Override
	public String toString() {
		return "consent("+consent._id+" "+parentString()+")";
	}

	@Override
	public Object getAccessRestriction(String content, String format, String field) throws AppException {
		return Feature_FormatGroups.getAccessRestriction(consent.sharingQuery, content, format, field);
		
	}

}
