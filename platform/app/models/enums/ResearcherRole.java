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

package models.enums;

import models.JsonSerializable;

public class ResearcherRole implements JsonSerializable {

	public boolean readData;
	public boolean writeData;
	public boolean pseudo;
	public boolean changeTeam;
	public boolean export;
	public boolean auditLog;
	public boolean participants;
	public boolean setup;
	public boolean applications;
	public String roleName;
	public String id;
	
	public boolean mayWriteData() {
		return writeData;//return this == STUDYNURSE || this == HC;
	}
	
	public boolean mayReadData() {
		return readData; //this != SPONSOR;
	}
	
	public boolean pseudonymizedAccess() {
		return pseudo; //this == INVESTIGATOR || this == RESEARCHER || this == SPONSOR;
	}
	
	public boolean mayChangeTeam() {
		return changeTeam; // this == SPONSOR || this == INVESTIGATOR;
	}
	
	public boolean mayExportData() {
		return export; //this == INVESTIGATOR || this == RESEARCHER;
	}
	
	public boolean auditLogAccess() {
		return auditLog; //this == MONITOR || this == INVESTIGATOR || this == HC;
	}
	
	public boolean manageParticipants() {
		return participants;
	}
	
	public boolean maySetup() {
		return setup;
	}
	
	public boolean mayUseApplications() {
		return applications;
	}
	
	public boolean may(Permission permission) {
		switch(permission) {
		case READ_DATA: return readData;	
		case WRITE_DATA: return writeData;	
		case CHANGE_TEAM: return changeTeam;
		case EXPORT: return export; 
		case AUDIT_LOG: return auditLog;
		case PARTICIPANTS: return participants;
		case SETUP: return setup;
		case APPLICATIONS: return applications;
		case ANY: return true;
		}
		return false;
	}
	
	public static ResearcherRole SPONSOR() {
		ResearcherRole result = new ResearcherRole();
		result.changeTeam = true;
		result.auditLog = true;
		result.readData = true;
		result.pseudo = true;
		result.export = true;
		result.participants = true;
		result.setup = true;
		result.applications = true;
		result.roleName = "Sponsor";
		result.id = "SPONSOR";
		return result;
	}
	
	public static ResearcherRole DEVELOPER() {
		ResearcherRole result = new ResearcherRole();
		result.changeTeam = true;
		result.auditLog = true;
		result.readData = false;
		result.pseudo = true;
		result.export = false;
		result.participants = false;
		result.setup = true;
		result.applications = true;
		result.roleName = "Developer";
		result.id = "DEVELOPER";
		return result;
	}
	
	public static ResearcherRole HC() {
		ResearcherRole result = new ResearcherRole();
		result.changeTeam = true;
		result.auditLog = true;
		result.readData = true;
		result.writeData = true;
		result.participants = false;
		result.pseudo = false;
		result.export = false;
		result.setup = true;
		result.applications = true;
		result.roleName = "HC";
		result.id = "HC";
		return result;
	}
	
	public static ResearcherRole MANAGER() {
		ResearcherRole result = new ResearcherRole();
		result.changeTeam = true;
		result.auditLog = true;
		result.readData = false;
		result.writeData = false;
		result.participants = false;
		result.pseudo = false;
		result.export = false;
		result.setup = true;
		result.applications = true;
		result.roleName = "MANAGER";
		result.id = "MANAGER";
		return result;
	}
	
	public static ResearcherRole SUBORGANIZATION() {
		ResearcherRole result = new ResearcherRole();
		result.changeTeam = false;
		result.auditLog = true;
		result.readData = true;
		result.writeData = true;
		result.participants = false;
		result.pseudo = false;
		result.export = false;
		result.setup = false;
		result.applications = false;
		result.roleName = "SUBORGANIZATION";
		result.id = "SUBORGANIZATION";
		return result;
	}
	
	public static ResearcherRole PARENTORGANIZATION() {
		ResearcherRole result = new ResearcherRole();
		result.changeTeam = false;
		result.auditLog = true;
		result.readData = false;
		result.writeData = false;
		result.participants = true;
		result.pseudo = false;
		result.export = false;
		result.setup = false;
		result.applications = false;
		result.roleName = "PARENTORGANIZATION";
		result.id = "PARENTORGANIZATION";
		return result;
	}
	
	public static ResearcherRole SUBPROJECT() {
        ResearcherRole result = new ResearcherRole();
        result.changeTeam = false;
        result.auditLog = true;
        result.readData = true;
        result.writeData = false;
        result.participants = false;
        result.pseudo = true;
        result.export = true;
        result.setup = false;
        result.applications = false;
        result.roleName = "SUBPROJECT";
        result.id = "SUBPROJECT";
        return result;
    }
	
	public static ResearcherRole UNCONFIRMED() {
		ResearcherRole result = new ResearcherRole();
		result.changeTeam = false;
		result.auditLog = false;
		result.readData = false;
		result.writeData = false;
		result.participants = false;
		result.pseudo = false;
		result.export = false;
		result.setup = false;
		result.applications = false;
		result.roleName = "UNCONFIRMED";
		result.id = "UNCONFIRMED";
		return result;
	}
	
	public void merge(ResearcherRole role) {
		this.changeTeam |= role.changeTeam;
		this.auditLog |= role.auditLog;
		this.readData |= role.readData;
		this.writeData |= role.writeData;
		this.participants |= role.participants;
		this.pseudo &= role.pseudo;
		this.export |= role.export;
		this.setup |= role.setup;
		this.applications |= role.applications;
	}
}
