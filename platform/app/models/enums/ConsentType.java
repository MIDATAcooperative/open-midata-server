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
 * the type of a consent
 *
 */
public enum ConsentType {

  /**
   * a consent where one MIDATA member shares records with other MIDATA members
   */
  CIRCLE,
  
  /**
   * a consent where one MIDATA member shares records with a study
   */
  STUDYPARTICIPATION,
  
  /**
   * a consent where one MIDATA member shares records with a healthcare provider
   */
  HEALTHCARE,
  
  /**
   * a consent where a research organization shares records with participants of a study
   */
  STUDYRELATED,
  
  /**
   * a consent where a healthcare provider shares records with one of his patients
   */
  HCRELATED,
  
  /**
   * a consent where one MIDATA member shares records with a mobile app instance
   */
  EXTERNALSERVICE,

  /**
   * a consent where one MIDATA member shares records with an external service
   */
  API,
  
  /**
   * a consent that has been created by the system (for message delivery)
   */
  IMPLICIT
}
