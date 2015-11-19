package models.enums;

/**
 * The role of a user of the platform.
 *
 */
public enum UserRole {
	
   /**
    * The user is a MIDATA member
    */
   MEMBER,
   
   /**
    * The user is an ADMINISTRATOR
    */
   ADMIN,
   
   /**
    * The user is a RESEARCHER (who is involved in studies)
    */
   RESEARCH,
   
   /**
    * The user is a health professional
    */
   PROVIDER,
   
   /**
    * The user is a plugin or mobile app developer for the MIDATA platform
    */
   DEVELOPER,
   
   /**
    * No specific role. This value may only be used for restrictions and not be assigned to a user account.
    */
   ANY
}
