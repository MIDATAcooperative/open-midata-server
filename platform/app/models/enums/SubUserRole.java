package models.enums;

/**
 * More detailed user role that declared in @see UserRole
 *
 */
public enum SubUserRole {

   /**
    * the user with UserRole PROVIDER is a doctor  
    */
   DOCTOR,
   
   /**
    * the user with UserRole PROVIDER is a nurse  
    */
   NURSE,
   
   /**
    * the user with UserRole PROVIDER is a manager  
    */
   MANAGER,
   
   /**
    * the user with UserRole PROVIDER is a monitor  
    */
   MONITOR;
   
   public boolean mayManageAccounts() {
	   return true;
   }
}
