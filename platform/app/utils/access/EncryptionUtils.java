package utils.access;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import models.ModelException;

import org.bson.BSON;
import org.bson.BSONObject;

import utils.auth.CodeGenerator;

public class EncryptionUtils {
	
	public final static String CIPHER_ALGORITHM = "AES";

	public static SecretKey generateKey(String keyAlgorithm) {
		try {
			KeyGenerator keygen = KeyGenerator.getInstance(keyAlgorithm);
		    SecretKey aesKey = keygen.generateKey();
			
			return aesKey;
		} catch (NoSuchAlgorithmException e) {
			throw new NullPointerException("CRYPTO BROKEN");
		}
	}
	
	public static BSONObject decryptBSON(SecretKey key, byte[] encrypted) throws ModelException {
		try {
			Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
			c.init(Cipher.DECRYPT_MODE, key);

			byte[] cipherText = encrypted; 
			byte[] bson = CodeGenerator.derandomize(c.doFinal(cipherText));
		   												
	    	return BSON.decode(bson);
	    			    	
		} catch (InvalidKeyException e) {
			throw new ModelException(e);
		} catch (NoSuchPaddingException e2) {
			throw new ModelException(e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new ModelException(e3);
		} catch (BadPaddingException e4) {
			throw new ModelException(e4);
		} catch (IllegalBlockSizeException e5) {
			throw new ModelException(e5);
		} 

	}
	
	public static byte[] encryptBSON(SecretKey key, BSONObject obj) throws ModelException {
		try {
			Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
			c.init(Cipher.ENCRYPT_MODE, key);

		    byte[] bson = BSON.encode(obj);
			byte[] cipherText = c.doFinal(CodeGenerator.randomize(bson));
							
	    	return cipherText;
		} catch (InvalidKeyException e) {
			throw new ModelException(e);
		} catch (NoSuchPaddingException e2) {
			throw new ModelException(e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new ModelException(e3);
		} catch (BadPaddingException e4) {
			throw new ModelException(e4);
		} catch (IllegalBlockSizeException e5) {
			throw new ModelException(e5);
		} 
	
	}
	
}
