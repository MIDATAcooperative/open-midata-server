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
	
	public static ResearcherRole SPONSOR() {
		ResearcherRole result = new ResearcherRole();
		result.changeTeam = true;
		result.auditLog = true;
		result.readData = true;
		result.pseudo = true;
		result.export = true;
		result.participants = true;
		result.setup = true;
		result.roleName = "Sponsor";
		result.id = "SPONSOR";
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
		result.setup = false;
		result.roleName = "HC";
		result.id = "HC";
		return result;
	}
}