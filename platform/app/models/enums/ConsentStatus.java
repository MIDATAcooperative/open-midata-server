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
 * status of a consent
 *
 */
public enum ConsentStatus {
	
   /**
    * the consent is still under construction and not ready for use
    */
   DRAFT,
      
	/**
	 * the consent is currently active
	 */
   ACTIVE,
   
   /**
    * the consent is active, consent has been given on paper but not on the platorm by the user itself
    * such a consent can only access and write new data created using this consent
    */
   PRECONFIRMED,
   
   /**
    * the consent was not created by the member and therefore needs confirmation by him
    */
   UNCONFIRMED,
   
   /**
    * the consent is no longer active. it has expired.
    */
   EXPIRED,
   
   /**
    * the consent has been rejected by the member
    */
   REJECTED,
   
   /**
    * the consents data is frozen
    */
   FROZEN,
   
   /**
    * consent needs revalidation
    */
   INVALID,
   
   /**
    * the consent has been deleted
    */
   DELETED;
   
   public boolean isSharingData() {
	   return this == ACTIVE || this == FROZEN || this == PRECONFIRMED;
   }
     
}
