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

package utils;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/*
 * PBKDF2 salted password hashing.
 * Author: havoc AT defuse.ca
 * www: http://crackstation.net/hashing-security.htm
 */
public class PasswordHash {	
	public static final String[] PBKDF2_ALGORITHM = new String[] { "PBKDF2WithHmacSHA1", "PBKDF2WithHmacSHA512"  };
	public static final int DEFAULT_HASH_ALGORITHM = 1;

	// The following constants may be changed without breaking existing hashes.
	public static final int SALT_BYTE_SIZE = 512;
	public static final int HASH_BYTE_SIZE = 512;
	public static final int PBKDF2_ITERATIONS = 65536;

	public static final int ITERATION_INDEX = 0;
	public static final int SALT_INDEX = 1;
	public static final int PBKDF2_INDEX = 2;
	public static final int ALGORITHM_INDEX = 3;
	
	private static SecureRandom random = new SecureRandom();

	/**
	 * Returns a salted PBKDF2 hash of the password.
	 * 
	 * @param password the password to hash
	 * @return a salted PBKDF2 hash of the password
	 */
	public static String createHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return createHash(password.toCharArray());
	}

	/**
	 * Returns a salted PBKDF2 hash of the password.
	 * 
	 * @param password the password to hash
	 * @return a salted PBKDF2 hash of the password
	 */
	public static String createHash(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return createHash(password, DEFAULT_HASH_ALGORITHM, SALT_BYTE_SIZE, PBKDF2_ITERATIONS, HASH_BYTE_SIZE);
	}
	
	public static String createInsecureQuickHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return createHash(password.toCharArray(), DEFAULT_HASH_ALGORITHM, 128, 10000, 128);
	}
	
	public static String createHash(char[] password, int hashAlgorithm, int saltByteSize, int pbkdf2Iterations, int hashByteSize) throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Generate a random salt		
		byte[] salt = new byte[saltByteSize];
		random.nextBytes(salt);

		// Hash the password
		byte[] hash = pbkdf2(hashAlgorithm, password, salt, pbkdf2Iterations, hashByteSize);
		// format iterations:salt:hash[:alg]
		return pbkdf2Iterations + ":" + toHex(salt) + ":" + toHex(hash)+":"+hashAlgorithm;
	}
	
	public static String createHashGivenSalt(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
		
		// Hash the password
		byte[] hash = pbkdf2(0, password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE);
		// format iterations:salt:hash
		return PBKDF2_ITERATIONS + ":" + toHex(salt) + ":" + toHex(hash);
	}

	/**
	 * Validates a password using a hash.
	 * 
	 * @param password the password to check
	 * @param correctHash the hash of the valid password
	 * @return true if the password is correct, false if not
	 */
	public static boolean validatePassword(String password, String correctHash) throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		return validatePassword(password.toCharArray(), correctHash);
	}

	/**
	 * Validates a password using a hash.
	 * 
	 * @param password the password to check
	 * @param correctHash the hash of the valid password
	 * @return true if the password is correct, false if not
	 */
	public static boolean validatePassword(char[] password, String correctHash) throws NoSuchAlgorithmException,
			InvalidKeySpecException {		
		// Decode the hash into its parameters
		String[] params = correctHash.split(":");
		int iterations = Integer.parseInt(params[ITERATION_INDEX]);
		byte[] salt = fromHex(params[SALT_INDEX]);
		byte[] hash = fromHex(params[PBKDF2_INDEX]);
		int algorithm = (params.length <= ALGORITHM_INDEX) ? 0 : Integer.parseInt(params[ALGORITHM_INDEX]);
		// Compute the hash of the provided password, using the same salt,
		// iteration count, and hash length
		byte[] testHash = pbkdf2(algorithm, password, salt, iterations, hash.length);
		// Compare the hashes in constant time. The password is correct if
		// both hashes match.
		boolean result = slowEquals(hash, testHash);
		AccessLog.log("password validation result="+result);
		return result;
	}
	
	/**
	 * Does this hash use the newest hashing function?
	 * @param hash
	 * @return
	 */
	public static boolean needsUpgrade(String hash) {
		return !hash.endsWith(":"+DEFAULT_HASH_ALGORITHM);
	}

	/**
	 * Compares two byte arrays in length-constant time. This comparison method is used so that password hashes cannot
	 * be extracted from an on-line system using a timing attack and then attacked off-line.
	 * 
	 * @param a the first byte array
	 * @param b the second byte array
	 * @return true if both byte arrays are the same, false if not
	 */
	private static boolean slowEquals(byte[] a, byte[] b) {
		int diff = a.length ^ b.length;
		for (int i = 0; i < a.length && i < b.length; i++)
			diff |= a[i] ^ b[i];
		return diff == 0;
	}

	/**
	 * Computes the PBKDF2 hash of a password.
	 * 
	 * @param password the password to hash.
	 * @param salt the salt
	 * @param iterations the iteration count (slowness factor)
	 * @param bytes the length of the hash to compute in bytes
	 * @return the PBDKF2 hash of the password
	 */
	private static byte[] pbkdf2(int algorithm, char[] password, byte[] salt, int iterations, int bytes)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM[algorithm]);
		return skf.generateSecret(spec).getEncoded();
	}

	/**
	 * Converts a string of hexadecimal characters into a byte array.
	 * 
	 * @param hex the hex string
	 * @return the hex string decoded into a byte array
	 */
	private static byte[] fromHex(String hex) {
		byte[] binary = new byte[hex.length() / 2];
		for (int i = 0; i < binary.length; i++) {
			binary[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return binary;
	}

	/**
	 * Converts a byte array into a hexadecimal string.
	 * 
	 * @param array the byte array to convert
	 * @return a length*2 character string encoding the byte array
	 */
	private static String toHex(byte[] array) {
		BigInteger bi = new BigInteger(1, array);
		String hex = bi.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if (paddingLength > 0)
			return String.format("%0" + paddingLength + "d", 0) + hex;
		else
			return hex;
	}

}