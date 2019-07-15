package utils.auth.auth2factor;

import models.MidataId;
import models.User;
import utils.auth.CodeGenerator;
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
	public static final long TOKEN_EXPIRE_TIME = 1000l * 60l * 15l;
	
	/**
	 * start new SMS authentication
	 * @param executor user or appInstance
	 * @param prompt Name of application
	 * @param phone phone number of user
	 * @throws AppException
	 */
	public void startAuthentication(MidataId executor, String prompt, User user) throws AppException {
		SecurityToken token = new SecurityToken();
		token._id = executor;
		token.created = System.currentTimeMillis();
		token.token = CodeGenerator.nextToken();
		token.add();
		String phone = user.mobile;
		if (phone == null) phone = user.phone;
		
		if (SMSUtils.isAvailable() && !phone.equals("@email")) {
		  Messager.sendSMS(phone, prompt+": "+token.token);
		} else {
		  Messager.sendTextMail(user.email, user.firstname+" "+user.lastname, "SMS for "+phone, prompt+": "+token.token);
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
		if (token.created < System.currentTimeMillis() - TOKEN_EXPIRE_TIME) throw new BadRequestException("error.expired.token", "Token expired.");
		String tk1 = token.token.toUpperCase().replaceAll("0", "O");
		String tk2 = code.toUpperCase().replaceAll("0", "O");
		if (!tk1.equals(tk2)) throw new BadRequestException("error.invalid.securitytoken", "Token not correct.");
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
