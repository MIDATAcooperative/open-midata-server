package utils.auth;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import akka.japi.Pair;
import models.KeyInfo;
import models.MidataId;
import models.MobileAppInstance;
import models.User;
import utils.access.EncryptionUtils;
import utils.collections.Sets;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;

/**
 * Manager for the private and public keys of users and external apps
 *
 */
public class KeyManager {

	/**
	 * The one and only KeyManager instance
	 */
	public static KeyManager instance = new KeyManager();
	
	/**
	 * key algorithm used
	 */
	public final static String KEY_ALGORITHM = "RSA";
	
	/**
	 * cipher algorithm used
	 */
	public final static String CIPHERS[] = new String[] { "RSA/ECB/PKCS1Padding", "RSA/ECB/OAEPWithSHA-256AndMGF1Padding" };
	
	public final static byte DEFAULT_CIPHER_ALG = 1;
	
	/**
	 * private key is not protected
	 */
	public final static int KEYPROTECTION_NONE = 0;
	
	/**
	 * private key is protected by passphrase
	 */
	public final static int KEYPROTECTION_PASSPHRASE = 1;
	
	/**
	 * private key is split into two parts
	 */
	public final static int KEYPROTECTION_SPLITKEY = 2;
		
	
	private Map<String, byte[]> pks = new HashMap<String, byte[]>();	
	
	/**
	 * encrypt a key with the target entities public key and return the encrypted key
	 * @param target id of user or app instance from which the public key should be used
	 * @param keyToEncrypt the key to encrypt
	 * @return encrypted key
	 * @throws EncryptionNotSupportedException
	 * @throws InternalServerException
	 */
	public byte[] encryptKey(MidataId target, byte[] keyToEncrypt) throws EncryptionNotSupportedException, InternalServerException {
				    		
			User user = User.getById(target, Sets.create("publicKey"));
			if (user != null) {			
				if (user.publicKey == null) throw new EncryptionNotSupportedException("User has no public key");			
				return encryptKey(user.publicKey , keyToEncrypt);
			}
			
			MobileAppInstance mai = MobileAppInstance.getById(target, Sets.create("publicKey"));
			if (mai != null) {
				if (mai.publicKey == null) throw new EncryptionNotSupportedException("No public key");			
				return encryptKey(mai.publicKey , keyToEncrypt);
			}
			
			throw new EncryptionNotSupportedException("No public key");	
	}
	
	/**
	 * encrypt a key with a given public key
	 * @param publicKey the public key that should be used for encryption
	 * @param keyToEncrypt the key that shall be encrypted
	 * @return the encrypted keyToEncrypt
	 * @throws EncryptionNotSupportedException
	 * @throws InternalServerException
	 */
	public byte[] encryptKey(byte[] publicKey, byte[] keyToEncrypt) throws EncryptionNotSupportedException, InternalServerException {
		try {						
			X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey);
			
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			PublicKey pubKey = keyFactory.generatePublic(spec);
			 
			Cipher c = Cipher.getInstance(CIPHERS[DEFAULT_CIPHER_ALG]);
			c.init(Cipher.ENCRYPT_MODE, pubKey);
		    
			byte[] cipherText = c.doFinal(EncryptionUtils.randomize(keyToEncrypt));
			//c.doFinal(keyToEncrypt, 0, keyToEncrypt.length, cipherText);
			byte[] result = new byte[cipherText.length+4];
			result[0] = DEFAULT_CIPHER_ALG;
			result[1] = result[2] = result[3] = 0;
			System.arraycopy(cipherText, 0, result, 4, cipherText.length);
			return result;
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", e);		
		} catch (NoSuchPaddingException e2) {
			throw new InternalServerException("error.internal", e2);
		} catch (InvalidKeyException e3) {
			throw new InternalServerException("error.internal", e3);
		} catch (InvalidKeySpecException e4) {
			throw new InternalServerException("error.internal", e4);
		} catch (BadPaddingException e5) {
			throw new InternalServerException("error.internal", e5);
		} catch (IllegalBlockSizeException e6) {
			throw new InternalServerException("error.internal", e6);
		} 
	}
	
	/**
	 * Decrypt an encrypted key using the entities private key
	 * 
	 * This operation will only work if the corresponding account has been unlocked before.
	 * 
	 * @param target the id of the user or application instance from which the private key should be used
	 * @param keyToDecrypt the encrypted key that shall be decrypted
	 * @return the decrypted keyToDecrypt
	 * @throws InternalServerException
	 * @throws AuthException
	 */
	public byte[] decryptKey(MidataId target, byte[] keyToDecrypt) throws InternalServerException, AuthException {
		try {
			
			byte key[] = pks.get(target.toString());
						
			if (key == null) throw new AuthException("error.relogin", "Authorization Failure");
			
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
			
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			PrivateKey privKey = keyFactory.generatePrivate(spec);
			 
			byte algorithm = 0;
			int offset = 0;
			if (keyToDecrypt[1] == 0 && keyToDecrypt[2] == 0 && keyToDecrypt[3] == 0) {
				algorithm = keyToDecrypt[0];
				offset = 4;
			}
			Cipher c = Cipher.getInstance(CIPHERS[algorithm]);
			c.init(Cipher.DECRYPT_MODE, privKey);
		    
			byte[] cipherText = c.doFinal(keyToDecrypt, offset, keyToDecrypt.length - offset);
						
			return EncryptionUtils.derandomize(cipherText);
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", e);		
		} catch (NoSuchPaddingException e2) {
			throw new InternalServerException("error.internal", e2);
		} catch (InvalidKeyException e3) {
			throw new InternalServerException("error.internal", e3);
		} catch (InvalidKeySpecException e4) {
			throw new InternalServerException("error.internal", e4);
		} catch (BadPaddingException e5) {
			throw new InternalServerException("error.internal", e5);
		} catch (IllegalBlockSizeException e6) {
			throw new InternalServerException("error.internal", e6);
		} 
	}
	
	/**
	 * Generate a new public/private key pair, store the private key in the database and return the public key
	 * 
	 * This method will not add additional protection to the private key
	 * 
	 * @param target id of user for whom the keypair should be generated
	 * @return public key
	 * @throws InternalServerException
	 */
	public byte[] generateKeypairAndReturnPublicKey(MidataId target) throws InternalServerException {
		return generateKeypairAndReturnPublicKey(target, null);
	}
	
	/**
	 * Generate a new public/private key pair, protect the private key with a passphrase, store it in db and return the public key.
	 * 
	 * This method will protect the generated private key with a passphrase.
	 * 
	 * @param target id of user or application instance for which this keypair should be generated
	 * @param passphrase passphrase to apply the the private key
	 * @return public key
	 * @throws InternalServerException
	 */
	public byte[] generateKeypairAndReturnPublicKey(MidataId target, String passphrase) throws InternalServerException {
		try {
		   KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
		   
		   KeyPair pair = generator.generateKeyPair();
		   PublicKey pub = pair.getPublic();
		   PrivateKey priv = pair.getPrivate();
		   
		   KeyInfo keyinfo = new KeyInfo();
		   keyinfo._id = target;
		   if (passphrase == null) {
		     keyinfo.privateKey = priv.getEncoded();
		     keyinfo.type = KEYPROTECTION_NONE;
		   } else {
			 keyinfo.privateKey = EncryptionUtils.applyKey(priv.getEncoded(), passphrase); 
			 keyinfo.type = KEYPROTECTION_PASSPHRASE;
		   }
		   KeyInfo.add(keyinfo);
		   
		   return pub.getEncoded();
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", e);
		}
	}
	
	/**
	 * Sets a new passphrase for a private key
	 * @param target id of key to be changed
	 * @param newPassphrase new passphrase to be applied
	 * @throws InternalServerException
	 * @throws AuthException
	 */
	public void changePassphrase(MidataId target, String newPassphrase) throws InternalServerException, AuthException {
		byte[] oldKey = pks.get(target.toString());
		if (oldKey == null) throw new AuthException("error.relogin", "Authorization Failure");		
		KeyInfo keyinfo = KeyInfo.getById(target);
		if (keyinfo == null) throw new InternalServerException("error.internal", "Private key info not found.");
		
		keyinfo.privateKey = EncryptionUtils.applyKey(oldKey, newPassphrase);
		keyinfo.type = KEYPROTECTION_PASSPHRASE;
		KeyInfo.update(keyinfo);
	}
	
	/**
	 * Unlock a user or app instance account
	 * 
	 * The account stays unlocked until lock is called for the account
	 * @param target id of user or app instance to unlock
	 * @param passphrase the passphrase for the private key or null if no passphrase has been applied 
	 * @throws InternalServerException
	 */
	public int unlock(MidataId target, String passphrase) throws InternalServerException {
		KeyInfo inf = KeyInfo.getById(target);
		if (inf == null) {
			pks.put(target.toString(), null);
			return -1;
		}
		else {
			if (inf.type == KEYPROTECTION_PASSPHRASE) {
				if (passphrase != null) {
 				   pks.put(target.toString(), EncryptionUtils.applyKey(inf.privateKey, passphrase));
				}
				return KEYPROTECTION_PASSPHRASE;
			} else {
				pks.put(target.toString(), inf.privateKey);
				return KEYPROTECTION_NONE;
			}
		}
	}
	
	/**
	 * Unlock a user account using an alias and split key
	 * 
	 * The account stays unlocked until lock is called for the account
	 * @param target id of user or app instance to unlock
	 * @param source id of alias
	 * @param splitkey keyfragment used to unlock the account 
	 * @throws InternalServerException
	 */
	public void unlock(MidataId target, MidataId source, byte[] splitkey) throws InternalServerException {
		KeyInfo inf = KeyInfo.getById(source);
		if (inf == null) {			
			throw new InternalServerException("error.internal", "Private key info not found.");
		}
		
		if (inf.type != KEYPROTECTION_SPLITKEY) {
			throw new InternalServerException("error.internal", "Private key has wrong type.");
		}
		pks.put(target.toString(), EncryptionUtils.joinKey(inf.privateKey, splitkey) );		
	}
	
	/**
	 * Remove a private key from memory
	 * 
	 * @param target id of user or app instance for which the private key should be cleared
	 * @throws InternalServerException
	 */
	public void lock(MidataId target) throws InternalServerException {
		pks.remove(target.toString());
	}

	/**
	 * Retrieves type of key protection applied
	 * @param target id of key
	 * @return type of key protection (0=none, 1=passphrase)
	 * @throws InternalServerException
	 */
	public int getKeyType(MidataId target) throws InternalServerException {
		KeyInfo inf = KeyInfo.getById(target);
		return inf.type;
	}
	
	/**
	 * Generates a key duplicate encoded with another key
	 */
	public byte[] generateAlias(MidataId source, MidataId target) throws AuthException, InternalServerException {
		byte key[] = pks.get(source.toString());
		
		if (key == null) throw new AuthException("error.relogin", "Authorization Failure");
	
		Pair<byte[], byte[]> split = EncryptionUtils.splitKey(key);
		KeyInfo keyinfo = new KeyInfo();
		keyinfo._id = target;		 
		keyinfo.privateKey = split.second();
		keyinfo.type = KEYPROTECTION_SPLITKEY;		   
		KeyInfo.add(keyinfo);
		
		return split.first();
		
	}
	
	/**
	 * Deletes a key from the storage
	 * @param target ID of key to delete
	 * @throws InternalServerException
	 */
	public void deleteKey(MidataId target) throws InternalServerException {
		KeyInfo.delete(target);
		lock(target);		
	}
}
