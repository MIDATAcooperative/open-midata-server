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
	
	public static String nextUniqueCode() {
		return nextCode();
	}
	
	public static byte[] randomize(byte[] source) {
		byte[] key = new byte[4];
		random.nextBytes(key);
		byte[] result = new byte[source.length + 4];
				
		for (int i=0;i<4;i++) result[i] = key[i];
		for (int i=0;i<source.length;i++) result[i+4] = (byte) (source[i] ^ result[i]);
				
		return result;
	}
	
	public static byte[] derandomize(byte[] source) {
		byte[] result = new byte[source.length-4];
		for (int i=4;i<source.length;i++) result[i-4] = (byte) (source[i] ^ source[i-4]);
		return result;
	}
}
