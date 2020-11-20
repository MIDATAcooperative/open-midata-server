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
    * the user is a MASTER user that may register other users with the same role and organization
    */
   MASTER,
   
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
    * the user with UserRole MEMBER has been registered for participation in a specific study
    */
   STUDYPARTICIPANT,
   
   /**
    * the user with UserRole MEMBER has been registered for using a specfic app
    */
   APPUSER,
   
   /**
    * the user with UserRole MEMBER is a member of the cooperative
    */
   MEMBEROFCOOPERATIVE;
   
   
   
   public boolean mayManageAccounts() {
	   return true;
   }
}
