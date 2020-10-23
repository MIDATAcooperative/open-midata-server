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
 * aggregation levels for summary queries
 *
 */
public enum AggregationType {
	
   /**
    * all records are summarized into one entry
    */
   ALL,
   
   /**
    * all records of the same group are summarized into one entry
    */
   GROUP,
   
   /**
    * all records with the same format are summarized into one entry
    */
   FORMAT,
   
   /**
    * all records with the same content type are summarized into one entry
    */
   CONTENT,
   
   /**
    * all records with the same content type and owner are summarized into one entry
    */
   CONTENT_PER_OWNER,
   
   /**
    * all records with the same content type and app are summarized into one entry
    */
   CONTENT_PER_APP,
   
   /**
    * all records with the same content type, format, group and owner are summarized into one entry
    */
   SINGLE   
}
