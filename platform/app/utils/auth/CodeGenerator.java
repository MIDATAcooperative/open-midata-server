package utils.auth;

import java.math.BigInteger;
import java.security.SecureRandom;

public class CodeGenerator {

	private static SecureRandom random = new SecureRandom();

	public static String nextCode() {
	    String code = new BigInteger(130, random).toString(32);
	    code = code.toUpperCase();
	    code = code.substring(0,4) + "-" + code.substring(4,8);
	    return code;
	}
}
