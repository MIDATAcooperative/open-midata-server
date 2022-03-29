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

import org.apache.commons.lang3.tuple.Pair;

import controllers.Circles;
import models.Consent;
import models.MidataId;
import models.Record;
import models.enums.ConsentType;
import models.enums.WritePermissionType;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.access.Feature_FormatGroups;
import utils.access.Feature_Pseudonymization;
import utils.access.QueryEngine;
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
		
		if (consent.writes == null) return false;
		if (!consent.writes.isUpdateAllowed()) return false;
		
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
		return "[ recordPassesFilter="+inFilter+" allowCreate="+wt.isCreateAllowed()+" allowUpdate="+wt.isUpdateAllowed()+" ]";
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

	@Override
	public String getContextName() {
		String result = consent.type.toString();
		result = result.substring(0,1).toUpperCase()+result.substring(1).toLowerCase();
		switch (consent.type) {
		case STUDYRELATED : result = "Project backchannel";break;
		case HCRELATED : result = "Healthcare provider backchannel";break;
		case API: result = "External service use";break;		
		}
		return "Consent of type '"+result+"'";
	}
	
	public AccessContext forConsent(Consent consent) throws AppException {
		if (this.consent._id == consent._id) return this;
		return super.forConsent(consent);
	}	
}
