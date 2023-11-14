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

package utils.context;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import controllers.Circles;
import models.Consent;
import models.MidataId;
import models.Record;
import models.enums.ConsentStatus;
import models.enums.ConsentType;
import models.enums.WritePermissionType;
import utils.ConsentQueryTools;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.access.Feature_FormatGroups;
import utils.access.Feature_Pseudonymization;
import utils.access.QueryEngine;
import utils.auth.KeyManager;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class ConsentAccessContext extends AccessContext{

	private Consent consent;
	private boolean sharingQuery;
	private String ownerName;
	private MidataId ownerpseudoId;
	//private final Set<String> reqfields = Sets.create("sharingQuery", "createdBefore", "validUntil");
	
	public ConsentAccessContext(Consent consent, APSCache cache, AccessContext parent) {
		super(cache, parent);
		this.consent = consent;
		setStudyOwnerName();
		
	}
	
	public ConsentAccessContext(Consent consent, AccessContext parent)  {
		super(parent.getCache(), parent);
		this.consent = consent;
		setStudyOwnerName();
	}
	
	private void setStudyOwnerName()  {
		if (consent.type.equals(ConsentType.STUDYRELATED) && consent.ownerName == null) {
			consent.ownerName = consent.name;
			if (consent.ownerName != null && consent.ownerName.startsWith("Study:")) consent.ownerName = consent.ownerName.substring("Study:".length());
		}
	}
	
	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		if (consent.writes == null) return parent==null || parent.mayCreateRecord(record);
		if (!consent.writes.isCreateAllowed()) return false;
		if (!consent.isWriteable()) return false;
		if (consent.writes.isUnrestricted()) return parent==null || parent.mayCreateRecord(record);
		
		if (!sharingQuery && consent.sharingQuery == null) {
			  consent.sharingQuery = Circles.getQueries(consent.owner, consent._id);
			  sharingQuery = true;
		}
		if (consent.sharingQuery == null) return false;
		return !QueryEngine.listFromMemory(this, consent.sharingQuery, Collections.singletonList(record)).isEmpty() && (parent==null || parent.mayCreateRecord(record));
		
	}

	@Override
	public boolean mayUpdateRecord(DBRecord stored, Record newVersion) throws InternalServerException {
		
		if (consent.writes == null) return false;
		if (!consent.writes.isUpdateAllowed()) return false;
		if (!consent.isWriteable()) return false;
		if (consent.type.equals(ConsentType.STUDYRELATED)) {			
			if (parent != null && parent instanceof UserGroupAccessContext && parent.parent != null) {				
				return parent.parent.mayUpdateRecord(stored, newVersion);
			}
		}
		if (parent != null) return parent.mayUpdateRecord(stored, newVersion);
		return true;
	}
	
	@Override
	public String getAccessInfo(DBRecord rec) throws AppException {
		WritePermissionType wt = consent.writes;
		if (wt==null) wt = WritePermissionType.NONE;
		boolean inFilter = consent.sharingQuery != null && !QueryEngine.listFromMemory(this, consent.sharingQuery, Collections.singletonList(rec)).isEmpty();		
		return "\n- Does the record pass the access filter? ["+inFilter+"]\n- Is record creation allowed in general? ["+wt.isCreateAllowed()+"]\n- Is update of records allowed in general? ["+wt.isUpdateAllowed()+"]"+"]\n- Has consent a status that allows writing? ["+consent.isWriteable()+"]";
	}

	@Override
	public boolean mustPseudonymize() {
		return (consent.type.equals(ConsentType.STUDYPARTICIPATION) && consent.ownerName != null && (parent == null || parent.mustPseudonymize()));
	}
	
	@Override
	public boolean mustRename() {
		return (consent.type.equals(ConsentType.STUDYRELATED) && consent.ownerName != null);		
	}

	@Override
	public MidataId getTargetAps() {
		return consent._id;
	}

	@Override
	public boolean isIncluded(DBRecord record) throws AppException {
		if (consent.writes == null) return false;
		
		loadSharingQuery();
		
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
	
	public String getOwnerType() {
		if (consent.type.equals(ConsentType.STUDYRELATED)) return "Group";
		return null;
	}

	@Override
	public MidataId getOwner() {
		return consent.owner;
	}

	@Override
	public MidataId getOwnerPseudonymized() throws AppException {
		if (consent.type.equals(ConsentType.STUDYRELATED)) return consent.owner;
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
		loadSharingQuery();		
		return Feature_FormatGroups.mayAccess(consent.sharingQuery, content, format);		
	}
	
	private void loadSharingQuery() throws AppException {
		if (!sharingQuery && consent.sharingQuery == null) {
			  consent.sharingQuery = Circles.getQueries(consent.owner, consent._id);
			  sharingQuery = true;
		}
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
		loadSharingQuery();
		return Feature_FormatGroups.getAccessRestriction(consent.sharingQuery, content, format, field);
		
	}

	@Override
	public String getContextName() {
		String result = consent.type.toString();
		result = result.substring(0,1).toUpperCase()+result.substring(1).toLowerCase();
		switch (consent.type) {
		case STUDYRELATED : result = "Project backchannel";break;
		case HCRELATED : result = "Healthcare provider backchannel";break;
		case API: result = "External service use (Patient side)";break;		
		}
		String r = "Consent of type '"+result+"'";
		if (consent.dateOfCreation!=null) r+=", created at "+consent.dateOfCreation.toGMTString();
		return r;
	}
	
	@Override
	public AccessContext forConsent(Consent consent) throws AppException {
		if (this.consent._id == consent._id) return this;
		return super.forConsent(consent);
	}
	
	@Override
	public AccessContext forAccountReshare() {
		if (getAccessor().equals(consent.owner)) return super.forAccountReshare();
		AccessContext p = super.forAccountReshare();
		return new ConsentAccessContext(consent, p.getCache(), p);		
	}

	@Override
	public MidataId getConsentSigner() throws InternalServerException {
		KeyManager.instance.recoverKeyFromAps(this, consent._id);
		return consent._id;
	}	
	
	@Override
	public boolean canCreateActiveConsentsFor(MidataId owner) {		
		return (consent.allowedReshares != null && owner.equals(consent.owner)) || parent.canCreateActiveConsentsFor(owner);
	}
	
	public boolean hasAccessToAllOf(Map<String, Object> targetFilter) throws AppException {
		loadSharingQuery();
		if (consent.allowedReshares != null && ConsentQueryTools.isSubQuery(consent.sharingQuery, targetFilter)) {
			if (parent != null) return parent.hasAccessToAllOf(targetFilter);
			return true;
		} else return false;
	}
		
}
