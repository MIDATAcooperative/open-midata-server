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
 * Status of a study participation code.
 *
 */
public enum ParticipationCodeStatus {
	
  /**
   * Code has not been used yet 
   */
   UNUSED,
   
   /**
    * Code has been given away but has not been entered by anyone.
    */
   SHARED,
   
   /**
    * Code has been used.
    */
   USED,
   
   /**
    * Code has been blocked and cannot be used any longer.
    */
   BLOCKED,
   
   /**
    * Code is infinitely reusable
    */
   REUSEABLE
}
