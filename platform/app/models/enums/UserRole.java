package models.enums;

import utils.AccessLog;

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
   ANY;
   
   public static UserRole fromShortString(String r) {	   
	   if (r == null || r.equals("m")) return UserRole.MEMBER;
	   if (r.equals("r")) return UserRole.RESEARCH;
	   else if (r.equals("d")) return UserRole.DEVELOPER;
	   else if (r.equals("a")) return UserRole.ADMIN;
	   else if (r.equals("p")) return UserRole.PROVIDER;
	   return UserRole.ANY;
   }
   
   public String toShortString() {
	   switch (this) {
	   case MEMBER: return "m";
	   case RESEARCH: return "r";
	   case DEVELOPER: return "d";
	   case ADMIN: return "a";
	   case PROVIDER: return "p";
	   default: return "-";
	   }
   }
}
