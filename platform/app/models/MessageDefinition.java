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

package models;

import java.util.Map;

import models.enums.MessageReason;

/**
 * Template for a predefined message
 *
 */
public class MessageDefinition implements JsonSerializable {
	
	/**
     * reason for message
     */
	public MessageReason reason;
	
    /**
     * additional code used to distinguish between different types of consents etc.
     */
	public String code;
	
	/**
	 * localized texts for message
	 */
	public Map<String, String> text;
	
	/**
	 * localized titles for message
	 */
	public Map<String, String> title;
	
	
	
}
