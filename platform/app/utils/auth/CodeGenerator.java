package utils.auth;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * generate public, human readable IDs (like MIDATA ID)
 *
 */
public class CodeGenerator {

	private static SecureRandom random = new SecureRandom();

	/**
	 * Generates a 8 digit code with a "-" after the 4th character
	 * @return the generated code
	 */
	public static String nextCode() {
	    String code = new BigInteger(130, random).toString(32);
	    code = code.toUpperCase();
	    code = code.substring(0,4) + "-" + code.substring(4,8);
	    return code;
	}
	
	public static String nextToken() {
	    String code = new BigInteger(130, random).toString(32);
	    code = code.toUpperCase();
	    code = code.substring(0,6);
	    return code;
	}
	
	/**
	 * Generates a 8 digit code with a "-" after the 4th character
	 * Uniqueness is not garanteed and must be check by the caller
	 * @return the generated code
	 */
	public static String nextUniqueCode() {
		return nextCode();
	}
	
	/**
	 * Generate a random passphrase
	 * @return the generated passphrase
	 */
	public static String generatePassphrase() {
		return new BigInteger(130, random).toString(32);
	}
}
