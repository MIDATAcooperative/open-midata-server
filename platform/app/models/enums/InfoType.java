/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package models.enums;

/**
 * Type of information used to describe a Study.
 *
 */
public enum InfoType {
	
   /**
    * Short Summary
    */
   SUMMARY,
   
   /**
    * What is this study doing?
    */
   DESCRIPTION,

   /**
    * Text for App onboarding
    */
   ONBOARDING,
   
   /**
	 * Link to project homepage
	 */
   HOMEPAGE,
   
   /**
    * Contact information
    */
   CONTACT,
   
   /**
    * Instructions for participants
    */
   INSTRUCTIONS,
   
    /**
     * What is the purpose of this research
     */
  PURPOSE,
  
    /**
     * Limitations on who should participate
     */
  AUDIENCE,
  
  /**
   * What is the timeframe for this study
   */
  DURATION,
  
   /**
    * State or region where this study is taking place
    */
  LOCATION,
  
  /**
   * What is the state of progression of the research taking place
   */
  PHASE,

  /**
   * Who is initiator of the research and who is legally responsible
   */
  SPONSOR, 

  /**
   * At which site are activities beeing conducted?
   */
  SITE, 

  /**
   * Information about equipment required by a participant
   */
  DEVICES,
  
  /**
   * Additional notes
   */
  COMMENT
}
