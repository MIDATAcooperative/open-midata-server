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

package utils.auth.auth2factor;

import models.MidataId;
import models.User;
import utils.auth.CodeGenerator;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.messaging.Messager;

/**
 * 2-factor Authentication interface
 *
 */
public interface Authenticator {

	
	/**
	 * start new authentication
	 * @param executor user or appInstance
	 * @param prompt Name of application
	 * @param user 
	 * @throws AppException
	 */
	public void startAuthentication(MidataId executor, String prompt, User user) throws AppException;
	
	/**
	 * validate code provided by user
	 * @param executor user or appInstance
	 * @param user user
	 * @param code token provided by user
	 * @return false if code is wrong.
	 * @throws AppException if code has expired
	 */
	public boolean checkAuthentication(MidataId executor, User user, String code) throws AppException;

	/**
	 * end authentication and delete code from database
	 * @param executor user or appInstance
	 * @param user
	 * @throws AppException
	 */
	public void finishAuthentication(MidataId executor, User user) throws AppException;
}
