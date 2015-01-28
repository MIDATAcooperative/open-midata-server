package utils.auth;

import java.math.BigInteger;
import java.security.SecureRandom;

public class CodeGenerator {

	private static SecureRandom random = new SecureRandom();

	public static String nextCode() {
	    return new BigInteger(130, random).toString(32);
	}
}
