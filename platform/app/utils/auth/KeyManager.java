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
import javax.crypto.ShortBufferException;

import models.KeyInfo;
import models.MobileAppInstance;
import models.User;

import org.bson.types.ObjectId;

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
	public final static String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
	
	private Map<String, byte[]> pks = new HashMap<String, byte[]>();	
	
	/**
	 * encrypt a key with the target entities public key and return the encrypted key
	 * @param target id of user or app instance from which the public key should be used
	 * @param keyToEncrypt the key to encrypt
	 * @return encrypted key
	 * @throws EncryptionNotSupportedException
	 * @throws InternalServerException
	 */
	public byte[] encryptKey(ObjectId target, byte[] keyToEncrypt) throws EncryptionNotSupportedException, InternalServerException {
				    		
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
			 
			Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
			c.init(Cipher.ENCRYPT_MODE, pubKey);
		    
			byte[] cipherText = c.doFinal(EncryptionUtils.randomize(keyToEncrypt));
			//c.doFinal(keyToEncrypt, 0, keyToEncrypt.length, cipherText);
			
			return cipherText;
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal.cryptography", e);		
		} catch (NoSuchPaddingException e2) {
			throw new InternalServerException("error.internal.cryptography", e2);
		} catch (InvalidKeyException e3) {
			throw new InternalServerException("error.internal.cryptography", e3);
		} catch (InvalidKeySpecException e4) {
			throw new InternalServerException("error.internal.cryptography", e4);
		} catch (BadPaddingException e5) {
			throw new InternalServerException("error.internal.cryptography", e5);
		} catch (IllegalBlockSizeException e6) {
			throw new InternalServerException("error.internal.cryptography", e6);
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
	public byte[] decryptKey(ObjectId target, byte[] keyToDecrypt) throws InternalServerException, AuthException {
		try {
			
			byte key[] = pks.get(target.toString());
						
			if (key == null) throw new AuthException("error.auth.relogin", "Authorization Failure");
			
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
			
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			PrivateKey privKey = keyFactory.generatePrivate(spec);
			 
			Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
			c.init(Cipher.DECRYPT_MODE, privKey);
		    
			byte[] cipherText = c.doFinal(keyToDecrypt);
						
			return EncryptionUtils.derandomize(cipherText);
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal.cryptography", e);		
		} catch (NoSuchPaddingException e2) {
			throw new InternalServerException("error.internal.cryptography", e2);
		} catch (InvalidKeyException e3) {
			throw new InternalServerException("error.internal.cryptography", e3);
		} catch (InvalidKeySpecException e4) {
			throw new InternalServerException("error.internal.cryptography", e4);
		} catch (BadPaddingException e5) {
			throw new InternalServerException("error.internal.cryptography", e5);
		} catch (IllegalBlockSizeException e6) {
			throw new InternalServerException("error.internal.cryptography", e6);
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
	public byte[] generateKeypairAndReturnPublicKey(ObjectId target) throws InternalServerException {
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
	public byte[] generateKeypairAndReturnPublicKey(ObjectId target, String passphrase) throws InternalServerException {
		try {
		   KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
		   
		   KeyPair pair = generator.generateKeyPair();
		   PublicKey pub = pair.getPublic();
		   PrivateKey priv = pair.getPrivate();
		   
		   KeyInfo keyinfo = new KeyInfo();
		   keyinfo._id = target;
		   if (passphrase == null) {
		     keyinfo.privateKey = priv.getEncoded();
		   } else {
			 keyinfo.privateKey = EncryptionUtils.applyKey(priv.getEncoded(), passphrase); 
			 keyinfo.type = 1;
		   }
		   KeyInfo.add(keyinfo);
		   
		   return pub.getEncoded();
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal.cryptography", e);
		}
	}
	
	/**
	 * Unlock a user or app instance account
	 * 
	 * The account stays unlocked until lock is called for the account
	 * @param target id of user or app instance to unlock
	 * @param passphrase the passphrase for the private key or null if no passphrase has been applied 
	 * @throws InternalServerException
	 */
	public void unlock(ObjectId target, String passphrase) throws InternalServerException {
		KeyInfo inf = KeyInfo.getById(target);
		if (inf == null) pks.put(target.toString(), null);
		else {
			if (inf.type == 1) {
				pks.put(target.toString(), EncryptionUtils.applyKey(inf.privateKey, passphrase));
			} else pks.put(target.toString(), inf.privateKey);
		}
	}
	
	/**
	 * Remove a private key from memory
	 * 
	 * @param target id of user or app instance for which the private key should be cleared
	 * @throws InternalServerException
	 */
	public void lock(ObjectId target) throws InternalServerException {
		pks.remove(target.toString());
	}
	
	
}
