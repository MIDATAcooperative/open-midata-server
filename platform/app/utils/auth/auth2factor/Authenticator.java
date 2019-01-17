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
