package utils.access;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import models.AccessPermissionSet;
import models.enums.APSSecurityLevel;

import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.types.ObjectId;

import controllers.KeyManager;

import utils.auth.CodeGenerator;
import utils.auth.EncryptionNotSupportedException;
import utils.exceptions.AppException;
import utils.exceptions.ModelException;

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
			throw new ModelException("error.internal.cryptography", e);
		} catch (NoSuchPaddingException e2) {
			throw new ModelException("error.internal.cryptography",e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new ModelException("error.internal.cryptography",e3);
		} catch (BadPaddingException e4) {
			throw new ModelException("error.internal.cryptography",e4);
		} catch (IllegalBlockSizeException e5) {
			throw new ModelException("error.internal.cryptography",e5);
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
			throw new ModelException("error.internal.cryptography", e);
		} catch (NoSuchPaddingException e2) {
			throw new ModelException("error.internal.cryptography", e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new ModelException("error.internal.cryptography", e3);
		} catch (BadPaddingException e4) {
			throw new ModelException("error.internal.cryptography", e4);
		} catch (IllegalBlockSizeException e5) {
			throw new ModelException("error.internal.cryptography", e5);
		} 
	
	}
	
	public static InputStream encryptStream(SecretKey key, InputStream in) throws ModelException {
		try {
			Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
			c.init(Cipher.ENCRYPT_MODE, key);

		    return new CipherInputStream(in, c);
								    	
		} catch (InvalidKeyException e) {
			throw new ModelException("error.internal.cryptography", e);
		} catch (NoSuchPaddingException e2) {
			throw new ModelException("error.internal.cryptography", e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new ModelException("error.internal.cryptography", e3);
		}
	
	}
	
	public static InputStream decryptStream(SecretKey key, InputStream in) throws ModelException {
		try {
			Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
			c.init(Cipher.DECRYPT_MODE, key);

		    return new CipherInputStream(in, c);
								    	
		} catch (InvalidKeyException e) {
			throw new ModelException("error.internal.cryptography", e);
		} catch (NoSuchPaddingException e2) {
			throw new ModelException("error.internal.cryptography", e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new ModelException("error.internal.cryptography", e3);
		}
	
	}
	
	public static void addKey(ObjectId target, EncryptedAPS eaps) throws AppException, EncryptionNotSupportedException {
		if (eaps.getSecurityLevel().equals(APSSecurityLevel.NONE) || eaps.getAPSKey() == null) {
			if (target.equals(eaps.getOwner())) {
				eaps.setKey("owner", eaps.getOwner().toByteArray());
			} else {
				eaps.setKey(target.toString(), null);
			}
		} else {
		   if (target.equals(eaps.getOwner())) {
			   eaps.setKey("owner", KeyManager.instance.encryptKey(target, eaps.getAPSKey().getEncoded()));
		   } else {
			   eaps.setKey(target.toString(), KeyManager.instance.encryptKey(target, eaps.getAPSKey().getEncoded()));	
		   }
		}
		
	}
	
	public static byte[] applyKey(byte[] data, String key) {
		try {
			byte[] result = new byte[data.length];
			byte[] keyc = key.getBytes("UTF-8");
			
			for (int i=0;i<data.length;i++) {
				result[i] = (byte) (data[i] ^ keyc[i % keyc.length]);
			}
			return result;
		} catch (UnsupportedEncodingException e) {
			throw new NullPointerException();
		}
	}
	
}
