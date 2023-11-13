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

import models.Consent;
import models.MidataId;
import models.Record;
import utils.access.APSCache;
import utils.access.DBRecord;
import utils.exceptions.AppException;

/**
 * This context is the same as a consent access context except that it allows 
 * to create the patient and pseudonymized patient without checking the access filter
 * @author alexander
 *
 */
public class CreateParticipantContext extends ConsentAccessContext {

	public CreateParticipantContext(Consent consent, APSCache cache, AccessContext parent) throws AppException {
		super(consent, cache, parent);
	}
		

	@Override
	public boolean mayCreateRecord(DBRecord record) throws AppException {
		if ((record.meta.getString("content").equals("PseudonymizedPatient") ||
			 record.meta.getString("content").equals("Patient"))
			&& record.meta.getString("format").equals("fhir/Patient")) return true;
		return super.mayCreateRecord(record);
	}
	
	

	@Override
	public boolean mustPseudonymize() {
		return false;
	}


	@Override
	public String getContextName() {
		return super.getContextName()+" (Reshare)";
	}
	
	@Override
	public boolean isUserGroupContext() {	
		return false;
	}
	
	
}
