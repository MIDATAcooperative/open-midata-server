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
   MONITOR,
   
   /**
    * the user with UserRole ADMIN is a user administrator
    */
   USERADMIN,
   
   /**
    * the user with UserRole ADMIN manages and validates studies 
    */
   STUDYADMIN,
   
   /**
    * the user with UserRole ADMIN manages content types
    */
   CONTENTADMIN,
   
   /**
    * the user with UserRole ADMIN manages plugins
    */
   PLUGINADMIN,
   
   /**
    * the user with UserRole ADMIN administrates news messages
    */
   NEWSWRITER,
   
   /**
    * the user with UserRole ADMIN may create and manage administrator accounts
    */
   SUPERADMIN,
   
   /**
    * the user with UserRole MEMBER is a trial user
    */
   TRIALUSER,
   
   /**
    * the user with UserRole MEMBER is a confirmed user who is no member of the cooperative
    */
   NONMEMBERUSER,
   
   /**
    * the user with UserRole MEMBER has been registered for participation in a specific study
    */
   STUDYPARTICIPANT,
   
   /**
    * the user with UserRole MEMBER is a member of the cooperative
    */
   MEMBEROFCOOPERATIVE;
   
   
   
   public boolean mayManageAccounts() {
	   return true;
   }
}
