package models.enums;

public enum SubUserRole {
   DOCTOR,
   NURSE,
   MANAGER,
   MONITOR;
   
   public boolean mayManageAccounts() {
	   return true;
   }
}
