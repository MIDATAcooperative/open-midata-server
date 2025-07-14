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

package utils.auth;

import models.User;
import models.enums.TokenType;
import utils.AccessLog;
import utils.exceptions.AppException;

public class OTPTools {

	public final static long EMAIL_TOKEN_LIFETIME = 1000l * 60l * 60l * 24l *3l;
	
	public static PasswordResetToken issueToken(User targetUser, TokenType type) throws AppException {
	    PasswordResetToken token;
		if (targetUser.resettoken != null 
			&& targetUser.resettokenTs > 0 && System.currentTimeMillis() - targetUser.resettokenTs < EMAIL_TOKEN_LIFETIME - 1000l * 60l * 60l 
			&& targetUser.resettokenType != null && targetUser.resettokenType.isReusableFor(type)) {
			  token = new PasswordResetToken(targetUser._id, targetUser.role.toString().toLowerCase(), targetUser.resettoken);
		} else {		
		      token = new PasswordResetToken(targetUser._id, targetUser.role.toString().toLowerCase(), true);
		}
		targetUser.updateResetToken(token.token, System.currentTimeMillis(), type);		
		return token;
	}
	
	public static boolean checkToken(User targetUser, String token, boolean allowExpired) {
		return allowExpired ? checkTokenAllowExpired(targetUser, token) : checkToken(targetUser, token);
	}
	
	public static boolean checkToken(User targetUser, String token) {		 
		 boolean tokenOk = (token != null && targetUser.resettoken != null 		    		    
	    		   && targetUser.resettoken.equals(token)
	    		   && System.currentTimeMillis() - targetUser.resettokenTs < EMAIL_TOKEN_LIFETIME);		 
		 return tokenOk;
	}
	
	public static boolean checkTokenAllowExpired(User targetUser, String token) {
		 boolean tokenOk = (token != null && targetUser.resettoken != null 		    		    
	    		   && targetUser.resettoken.equals(token));
		 return tokenOk;
	}
	
	public static void clearToken(User targetUser) throws AppException {
		targetUser.updateResetToken(null, targetUser.resettokenTs, null);
	}
	
	public static boolean tokenConfirmsEMail(User targetUser) {
		return (targetUser.resettokenType == null 
			|| targetUser.resettokenType == TokenType.WELCOME_MAIL
			|| targetUser.resettokenType == TokenType.PWRESET_MAIL
			|| targetUser.resettokenType == TokenType.OTP_MAIL);
	}
	
	public static boolean tokenConfirmsMobileNumber(User targetUser) {
		return targetUser.resettokenType == TokenType.OTP_SMS;			
	}
}
