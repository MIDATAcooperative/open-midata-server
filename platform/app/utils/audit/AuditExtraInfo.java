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

package utils.audit;

public class AuditExtraInfo {

	String practitionerReference;
	String practitionerName;
	
	// agent.who
	String organizationName; 
	String organizationReference;
	
	// agent.location.display/reference
	String locationName;
	String locationReference;
	
	// agent.purposeOfUse CC
	String purposeCoding;
	String purposeName;
	
	public boolean isUsed() {
		return practitionerReference != null
				|| practitionerName != null
				|| organizationName != null
				|| organizationReference != null
				|| locationName != null
				|| locationReference != null
				|| purposeCoding != null
				|| purposeName != null;
	}

	public String getPractitionerReference() {
		return practitionerReference;
	}

	public String getPractitionerName() {
		return practitionerName;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public String getOrganizationReference() {
		return organizationReference;
	}

	public String getLocationName() {
		return locationName;
	}

	public String getLocationReference() {
		return locationReference;
	}

	public String getPurposeCoding() {
		return purposeCoding;
	}

	public String getPurposeName() {
		return purposeName;
	}
	
	
	
}
