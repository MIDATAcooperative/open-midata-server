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

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.google.common.io.BaseEncoding;

import utils.AccessLog;
import utils.InstanceConfig;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

public class TokenCrypto {

	private static final String instanceSecret = InstanceConfig.getInstance().getConfig().getString("play.http.secret.key");
	
	private static final SecretKey tokenKey = new SecretKeySpec(instanceSecret.getBytes(), 0, 32, "AES");
	private static final SecretKey signKey = new SecretKeySpec(instanceSecret.getBytes(), 32, 32, "HMACSHA256");
	
	private final static int IV_LENGTH = 16;
	private final static int HMAC_LENGTH = 16;
	
	private static byte[] signToken(byte[] iv, int off, int len, byte[] encrypted, int off2, int len2) throws InternalServerException {
		try {
		    Mac mac = Mac.getInstance("HMACSHA256");
		    mac.init(signKey);
		    mac.update(iv, off ,len);
		    mac.update(encrypted, off2 ,len2);
		    return mac.doFinal();
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (InvalidKeyException e2) {
			throw new InternalServerException("error.internal", "Cryptography error");
		}
	}
	
	public static String sha256ThenBase64(String input) throws InternalServerException  {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(input.getBytes("ASCII")); 
			byte[] digest = md.digest();
			return Base64.encodeBase64URLSafeString(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (UnsupportedEncodingException e7) {
			throw new InternalServerException("error.internal", "Cryptography error");
		}
	}
	
	public static String sha512(String input) throws InternalServerException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update(input.getBytes("UTF-8")); 
			byte[] digest = md.digest();
			return BaseEncoding.base16().lowerCase().encode(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (UnsupportedEncodingException e7) {
			throw new InternalServerException("error.internal", "Cryptography error");
		}
	}
	
	public static String encryptToken(String input) throws InternalServerException  {	
		try{ 			
			byte[] unencrypted = input.getBytes("utf-8");
					
			String ciperAlg = "AES/CBC/PKCS5Padding";
			Cipher c = Cipher.getInstance(ciperAlg);
			c.init(Cipher.ENCRYPT_MODE, tokenKey);
	
			byte[] iv = c.getIV();						
			byte[] encrypted = c.doFinal(unencrypted);
			byte[] signature = signToken(iv, 0, iv.length, encrypted, 0, encrypted.length);
			
			if (iv.length != IV_LENGTH) throw new InternalServerException("error.internal", "IV length is not as expected.");
			if (signature.length < HMAC_LENGTH) throw new InternalServerException("error.internal", "HMAC length is not as expected.");
			
			byte[] result = new byte[IV_LENGTH + encrypted.length + HMAC_LENGTH];
			System.arraycopy(iv, 0, result, 0, iv.length);
			System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
			System.arraycopy(signature, 0, result, iv.length + encrypted.length, HMAC_LENGTH);
									
			return Base64.encodeBase64URLSafeString(result);			
	    } catch (BadPaddingException e) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (IllegalBlockSizeException e2) {
			throw new InternalServerException("error.internal", "Cryptography error");	 
		} catch (NoSuchPaddingException e4) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (NoSuchAlgorithmException e5) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (InvalidKeyException e6) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (UnsupportedEncodingException e7) {
			throw new InternalServerException("error.internal", "Cryptography error");
		}
		
		
	}
	
	public static String decryptToken(String input) throws AppException {		
		try {			
			byte[] encrypted = Base64.decodeBase64(input);
			
			byte[] sign = signToken(encrypted, 0, IV_LENGTH, encrypted, IV_LENGTH, encrypted.length - IV_LENGTH - HMAC_LENGTH);
			int j = encrypted.length - HMAC_LENGTH ;
			for (int i=0;i<HMAC_LENGTH;i++) {
				if (sign[i] != encrypted[i+j]) {					
					throw new BadRequestException("error.invalid.token", "Invalid token");
				}			
			}
			String ciperAlg = "AES/CBC/PKCS5Padding";
			IvParameterSpec ips = new IvParameterSpec(encrypted,0,IV_LENGTH);
			
			Cipher c = Cipher.getInstance(ciperAlg);
			c.init(Cipher.DECRYPT_MODE, tokenKey, ips);
			 		
			byte[] unencrypted = c.doFinal(encrypted,IV_LENGTH,encrypted.length - IV_LENGTH - HMAC_LENGTH);			
			return new String(unencrypted, "utf-8");
		} catch (BadPaddingException e) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (IllegalBlockSizeException e2) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (InvalidAlgorithmParameterException e3) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (NoSuchPaddingException e4) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (NoSuchAlgorithmException e5) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (InvalidKeyException e6) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (UnsupportedEncodingException e7) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (IllegalArgumentException e8) {
			throw new BadRequestException("error.invalid.token", "Invalid token");
		}
	}
}
