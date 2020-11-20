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
 * exception that is thrown if an error occurs that is caused by a bug or internal problem.
 *
 */
public class InternalServerException extends AppException {

	private static final long serialVersionUID = 1L;

	public InternalServerException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public InternalServerException(String localeKey, String msg) {
		super(localeKey, msg);
	}

}
