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

package utils.messaging;

import models.JsonSerializable;

/**
 * SMTP configuration for specific applications
 */
public class SMTPConfig implements JsonSerializable {
	
	/**
	 * host
	 */
	public String host;
	
	/**
	 * port to use, default 587
	 */
	public int port;
	
	/**
	 * use ssl
	 */
	public boolean ssl;
	
	/**
	 * use startTLS
	 */
	public boolean tls;
	
	/**
	 * username for smtp account
	 */
	public String user;
	
	/**
	 * password for smtp account
	 */
	public String password;
	
	/**
	 * sender name to be used in the format "name<email>"
	 */
	public String from;
	   
}
