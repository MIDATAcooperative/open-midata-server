package utils.access;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.ArrayUtils;
import org.bson.BSON;
import org.bson.BSONObject;

import akka.japi.Pair;
import models.MidataId;
import models.enums.APSSecurityLevel;
import utils.auth.KeyManager;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * utility functions for encrypting and decrypting data 
 *
 */
public class EncryptionUtils {
	
	public final static byte DEFAULT_CIPHER_ALGORITHM = 0;
	public final static byte DEFAULT_KEY_ALGORITHM = 0;
	
	public final static String[] KEY_ALG = new String[] { "AES" };
	public final static String[] CIPHER_ALG = new String[] { "AES" };
	
	private static SecureRandom random = new SecureRandom();
	
	private static SecretKeySpec getKeySpec(byte[] key) {
		if (key.length == 16) {
			return new SecretKeySpec(key, KEY_ALG[0]);
		}
		byte version = key[0];		
		
		return new SecretKeySpec(key, 4, key.length-4, KEY_ALG[version]);
	}
	
	private static byte getCipherAlg(byte[] key) {
		if (key.length == 16) return 0;
		return key[1];
	}
	
	private static byte[] getKey(byte cipherVersion, SecretKey spec) {
		int version = ArrayUtils.indexOf(KEY_ALG, spec.getAlgorithm());
		byte[] enc = spec.getEncoded();
		byte[] result = new byte[enc.length+4];
		result[0] = (byte) version;
		result[1] = cipherVersion;
		result[2] = 0;
		result[3] = 0;
		System.arraycopy(enc, 0, result, 4, enc.length);
		return result;
	}

	public static byte[] generateKey(byte keyAlgorithm, byte cipherAlg) {
		try {
			KeyGenerator keygen = KeyGenerator.getInstance(KEY_ALG[keyAlgorithm]);
		    SecretKey aesKey = keygen.generateKey();
			
			return getKey(cipherAlg, aesKey);
		} catch (NoSuchAlgorithmException e) {
			throw new NullPointerException("CRYPTO BROKEN");
		}
	}
	
	public static byte[] generateKey() {
		return generateKey(DEFAULT_KEY_ALGORITHM, DEFAULT_CIPHER_ALGORITHM);
	}
	
	public static BSONObject decryptBSON(byte[] key, byte[] encrypted) throws InternalServerException {
		try {
			SecretKey keySpec = getKeySpec(key);
			String ciperAlg = CIPHER_ALG[getCipherAlg(key)];
			Cipher c = Cipher.getInstance(ciperAlg);
			c.init(Cipher.DECRYPT_MODE, keySpec);

			byte[] cipherText = encrypted; 
			byte[] bson = EncryptionUtils.derandomize(c.doFinal(cipherText));
		   												
	    	BSONObject obj =BSON.decode(bson);
	    	
	    	return obj;
	    			    	
		} catch (InvalidKeyException e) {
			throw new InternalServerException("error.internal.cryptography", e);
		} catch (NoSuchPaddingException e2) {
			throw new InternalServerException("error.internal.cryptography",e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new InternalServerException("error.internal.cryptography",e3);
		} catch (BadPaddingException e4) {
			throw new InternalServerException("error.internal.cryptography",e4);
		} catch (IllegalBlockSizeException e5) {
			throw new InternalServerException("error.internal.cryptography",e5);
		} 

	}
	
	public static byte[] encryptBSON(byte[] key, BSONObject obj) throws InternalServerException {
		try {
			SecretKey keySpec = getKeySpec(key);
			String ciperAlg = CIPHER_ALG[getCipherAlg(key)];
			
			Cipher c = Cipher.getInstance(ciperAlg);
			c.init(Cipher.ENCRYPT_MODE, keySpec);

		    byte[] bson = BSON.encode(obj);
			byte[] cipherText = c.doFinal(EncryptionUtils.randomize(bson));
							
	    	return cipherText;
		} catch (InvalidKeyException e) {
			throw new InternalServerException("error.internal.cryptography", e);
		} catch (NoSuchPaddingException e2) {
			throw new InternalServerException("error.internal.cryptography", e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new InternalServerException("error.internal.cryptography", e3);
		} catch (BadPaddingException e4) {
			throw new InternalServerException("error.internal.cryptography", e4);
		} catch (IllegalBlockSizeException e5) {
			throw new InternalServerException("error.internal.cryptography", e5);
		} 
	
	}
	
	public static InputStream encryptStream(byte[] key, InputStream in) throws InternalServerException {
		try {
			SecretKey keySpec = getKeySpec(key);
			String ciperAlg = CIPHER_ALG[getCipherAlg(key)];
			
			Cipher c = Cipher.getInstance(ciperAlg);
			c.init(Cipher.ENCRYPT_MODE, keySpec);

		    return new CipherInputStream(in, c);
								    	
		} catch (InvalidKeyException e) {
			throw new InternalServerException("error.internal.cryptography", e);
		} catch (NoSuchPaddingException e2) {
			throw new InternalServerException("error.internal.cryptography", e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new InternalServerException("error.internal.cryptography", e3);
		}
	
	}
	
	public static InputStream decryptStream(byte[] key, InputStream in) throws InternalServerException {
		try {
			SecretKey keySpec = getKeySpec(key);
			String ciperAlg = CIPHER_ALG[getCipherAlg(key)];
			
			Cipher c = Cipher.getInstance(ciperAlg);						
			c.init(Cipher.DECRYPT_MODE, keySpec);

		    return new CipherInputStream(in, c);
								    	
		} catch (InvalidKeyException e) {
			throw new InternalServerException("error.internal.cryptography", e);
		} catch (NoSuchPaddingException e2) {
			throw new InternalServerException("error.internal.cryptography", e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new InternalServerException("error.internal.cryptography", e3);
		}
	
	}
	
	public static void addKey(MidataId target, EncryptedAPS eaps) throws AppException {
		if (eaps.getSecurityLevel().equals(APSSecurityLevel.NONE) || eaps.getAPSKey() == null) {
			if (target.equals(eaps.getOwner())) {
				eaps.setKey("owner", eaps.getOwner().toByteArray());
			} else {
				eaps.setKey(target.toString(), null);
			}
		} else {
		   if (target.equals(eaps.getOwner())) {
			   eaps.setKey("owner", KeyManager.instance.encryptKey(target, eaps.getAPSKey()));
		   } else {
			   eaps.setKey(target.toString(), KeyManager.instance.encryptKey(target, eaps.getAPSKey()));	
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
	
	public static Pair<byte[], byte[]> splitKey(byte[] data) {		
			byte[] b1 = new byte[data.length];
			byte[] b2 = new byte[data.length];
			
			random.nextBytes(b1);
			for (int i=0;i<data.length;i++) {				
				b2[i] = (byte) (data[i] ^ b1[i]);
			}
			return new Pair<byte[], byte[]>(b1, b2);		
	}
	
	public static byte[] joinKey(byte[] b1, byte[] b2) throws InternalServerException {		
		if (b1.length != b2.length) throw new InternalServerException("error.internal", "Key length mismatch");
		
		byte[] result = new byte[b1.length];
		for (int i=0;i<b1.length;i++) {				
			result[i] = (byte) (b1[i] ^ b2[i]);
		}
		return result;		
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
