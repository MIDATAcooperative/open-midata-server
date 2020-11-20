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

package utils.exceptions;

/**
 * exception that is thrown if an error occurs because a request with illegal parameters has been issued.
 *
 */
public class BadRequestException extends AppException {	
	
	private static final long serialVersionUID = 1L;
	
	private int statusCode = 400;

	public BadRequestException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public BadRequestException(String localeKey, String msg) {
		super(localeKey, msg);
	}
	
	public BadRequestException(String localeKey, String msg, int statusCode) {
		super(localeKey, msg);
		this.statusCode = statusCode;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
}
