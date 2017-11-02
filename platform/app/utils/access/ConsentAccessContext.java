package utils.access;

import java.util.Collections;
import java.util.Set;

import controllers.Circles;
import models.Consent;
import models.MidataId;
import models.enums.ConsentType;
import models.enums.WritePermissionType;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class ConsentAccessContext extends AccessContext{

	private Consent consent;
	private boolean sharingQuery;
	//private final Set<String> reqfields = Sets.create("sharingQuery", "createdBefore", "validUntil");
	
	public ConsentAccessContext(Consent consent, APSCache cache, AccessContext parent) throws AppException {
		super(cache, parent);
		this.consent = consent;
		
	}
	
	public ConsentAccessContext(Consent consent, AccessContext parent) throws AppException {
		super(parent.getCache(), parent);
		this.consent = consent;		
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
		
		return !QueryEngine.listFromMemory(cache, consent.sharingQuery, Collections.singletonList(record)).isEmpty() && (parent==null || parent.mayCreateRecord(record));
		
	}

	@Override
	public boolean mayUpdateRecord() {
		if (consent.writes == null) return false;
		if (!consent.writes.isUpdateAllowed()) return false;
		if (parent != null) return parent.mayUpdateRecord();
		return true;
	}

	@Override
	public boolean mustPseudonymize() {
		return consent.type.equals(ConsentType.STUDYPARTICIPATION) && consent.ownerName != null && (parent == null || parent.mustPseudonymize());		
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
		return !QueryEngine.listFromMemory(cache, consent.sharingQuery, Collections.singletonList(record)).isEmpty();
	}
	
	public Consent getConsent() {
		return consent;
	}

	public String getOwnerName() {
		return consent.ownerName;
	}

	@Override
	public MidataId getOwner() {
		return consent.owner;
	}

	@Override
	public MidataId getOwnerPseudonymized() {
		return consent._id;
	}

}
