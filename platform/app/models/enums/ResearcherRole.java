package models.enums;

public enum ResearcherRole {

	HC,
	
	MONITOR,
	
	PI;
	
	public boolean mayWriteData() {
		return false;
	}
	
	public boolean mayReadData() {
		return true;
	}
	
	public boolean pseudonymizedAccess() {
		return this == PI;
	}
	
	public boolean mayChangeTeam() {
		return true;
	}
	
	public boolean mayExportData() {
		return true;
	}
}
