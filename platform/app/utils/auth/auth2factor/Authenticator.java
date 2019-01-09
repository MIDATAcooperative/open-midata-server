package utils.auth.auth2factor;

import models.MidataId;
import utils.auth.CodeGenerator;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.messaging.Messager;

/**
 * 2-factor Authentication implementation
 *
 */
public class Authenticator {

	/**
	 * Lifetime of SMS tokens
	 */
	public static final long TOKEN_EXPIRE_TIME = 1000l * 60l * 15l;
	
	/**
	 * start new SMS authentication
	 * @param executor user or appInstance
	 * @param prompt Name of application
	 * @param phone phone number of user
	 * @throws AppException
	 */
	public static void startAuthentication(MidataId executor, String prompt, String phone) throws AppException {
		SecurityToken token = new SecurityToken();
		token._id = executor;
		token.created = System.currentTimeMillis();
		token.token = CodeGenerator.nextToken();
		token.add();
		
		Messager.sendSMS(phone, prompt+": "+token.token);		
	}
	
	/**
	 * validate code provided by user
	 * @param executor user or appInstance
	 * @param code token provided by user
	 * @return false if code is wrong.
	 * @throws AppException if code has expired
	 */
	public static boolean checkAuthentication(MidataId executor, String code) throws AppException {
		SecurityToken token = SecurityToken.getById(executor);
		if (token == null) throw new BadRequestException("error.expired.securitytoken", "Token does not exist.");
		if (token.created < System.currentTimeMillis() - TOKEN_EXPIRE_TIME) throw new BadRequestException("error.expired.token", "Token expired.");
		if (!token.token.equals(code.toUpperCase())) throw new BadRequestException("error.invalid.securitytoken", "Token not correct.");
		return true;
	}
	
	/**
	 * end authentication and delete code from database
	 * @param executor user or appInstance
	 * @throws AppException
	 */
	public static void finishAuthentication(MidataId executor) throws AppException {
		SecurityToken.delete(executor);
	}
}
