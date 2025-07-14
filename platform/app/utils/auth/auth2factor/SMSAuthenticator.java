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
import models.RateLimitedAction;
import models.User;
import models.enums.AuditEventType;
import utils.audit.AuditManager;
import utils.auth.CodeGenerator;
import utils.auth.KeyManager;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.messaging.Messager;
import utils.messaging.SMSUtils;

/**
 * SMS two factor authentication
 * @author alexander
 *
 */
public class SMSAuthenticator implements Authenticator {

	/**
	 * Lifetime of SMS tokens
	 */
	public static final long TOKEN_EXPIRE_TIME = 1000l * 60l * 10l;
	
	/**
	 * Minimum time between SMS 
	 */
	public static final long MIN_TIME_BETWEEN_SMS = 1000l * 10l * 1l;
	
	/**
	 * Allow 5 SMS per hour
	 */
	public static final long SMS_TIMEFRAME = 1000l * 60l * 60l;
	public static final int MAX_PER_TIMEFRAME = 6;
	
	/**
	 * Maximum number of failed attempts before token is invalid
	 */
	public static final int MAX_FAILED_ATTEMPTS = 3;
	
	/**
	 * start new SMS authentication
	 * @param executor user or appInstance
	 * @param prompt Name of application
	 * @param phone phone number of user
	 * @throws AppException
	 */
	public void startAuthentication(MidataId executor, String prompt, User user) throws AppException {
		if (!RateLimitedAction.doRateLimited(user._id, AuditEventType.USER_AUTHENTICATION, MIN_TIME_BETWEEN_SMS, MAX_PER_TIMEFRAME, SMS_TIMEFRAME)) {
			throw new BadRequestException("error.ratelimit", "Rate limit reached.");
		}
		
		SecurityToken token = new SecurityToken();
		token._id = executor;
		token.created = System.currentTimeMillis();
		token.token = CodeGenerator.nextToken();
		token.add();
		String phone = user.mobile;
		if (phone == null) phone = user.phone;
		
		if (SMSUtils.isAvailable() && !phone.equals("@email")) {
		  Messager.sendSMS(phone, prompt+": "+token.token, null);
		} else {
		  Messager.sendTextMail(user.email, user.firstname+" "+user.lastname, "SMS for "+phone, prompt+": "+token.token, null);
		}
	}
	
	/**
	 * validate code provided by user
	 * @param executor user or appInstance
	 * @param code token provided by user
	 * @return false if code is wrong.
	 * @throws AppException if code has expired
	 */
	public boolean checkAuthentication(MidataId executor, User user, String code) throws AppException {
		SecurityToken token = SecurityToken.getById(executor);
		if (token == null) throw new BadRequestException("error.expired.securitytoken", "Token does not exist.");
		if (token.created < System.currentTimeMillis() - TOKEN_EXPIRE_TIME) throw new BadRequestException("error.expired.securitytoken", "Token expired.");
		if (token.failedAttempts >= MAX_FAILED_ATTEMPTS) {
			KeyManager.instance.logout();
			throw new BadRequestException("error.expired.securitytoken", "Token expired.");
		}
		String tk1 = token.token.toUpperCase().replaceAll("0", "O");
		String tk2 = code.toUpperCase().replaceAll("0", "O");
		if (!tk1.equals(tk2)) {
			token.failedAttempt();
			if (token.failedAttempts >= MAX_FAILED_ATTEMPTS) {
			  KeyManager.instance.logout();
			  throw new BadRequestException("error.expired.securitytoken", "Token expired.");
			} else {			  
			  throw new BadRequestException("error.invalid.securitytoken", "Token not correct.");
			}
		}
		return true;
	}
	
	/**
	 * end authentication and delete code from database
	 * @param executor user or appInstance
	 * @throws AppException
	 */
	public void finishAuthentication(MidataId executor, User user) throws AppException {
		SecurityToken.delete(executor);
	}
}
