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
