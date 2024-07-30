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
 * Types of links between an application and a study
 *
 */
public enum StudyAppLinkType {

	/**
	 * offer to participate
	 */
	OFFER_P,

	OFFER_EXTRA_PAGE,

	OFFER_INLINE_AGB,
	
	/**
	 * participation is required = AUTOADD_P, CHECK_P, LINK_P
	 */
	REQUIRE_P,
	
	/**
	 * automatically add participation
	 */
	AUTOADD_P,
	
	/**
	 * participation is required, otherwise block/logout
	 */
	CHECK_P,
	
	/**
	 * participation id / pseudonym will be returned upon login  
	 */
	LINKED_P,
	
	/**
	 * recommend plugin/app upon participation
	 */
	RECOMMEND_A,
	
	/**
	 * automatically add plugin upon participation
	 */
	AUTOADD_A,
	
	DATALINK
}
