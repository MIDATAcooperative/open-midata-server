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
 * General interest of a MIDATA member to participate in a study
 *
 */
public enum ParticipationInterest {
	
  /**
   * MIDATA member is generally not interested in study participation
   */
  NONE,
  
  /**
   * MIDATA member is generally interested to participate in a study
   */
  ALL,
  
  /**
   * MIDATA member is interested to participate in studies with specific topics
   */
  SOME,
  
  /**
   * MIDATA member has not selected any preference about study participation yet.
   */
  UNSET
}
