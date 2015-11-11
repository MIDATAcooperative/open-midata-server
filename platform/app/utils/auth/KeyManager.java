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


public class KeyManager {

	public static KeyManager instance = new KeyManager();
	
	public final static String KEY_ALGORITHM = "RSA";
	public final static String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
	
	private Map<String, byte[]> pks = new HashMap<String, byte[]>();	
	
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
	
	public byte[] generateKeypairAndReturnPublicKey(ObjectId target) throws InternalServerException {
		return generateKeypairAndReturnPublicKey(target, null);
	}
	
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
	
	public void unlock(ObjectId target, String password) throws InternalServerException {
		KeyInfo inf = KeyInfo.getById(target);
		if (inf == null) pks.put(target.toString(), null);
		else {
			if (inf.type == 1) {
				pks.put(target.toString(), EncryptionUtils.applyKey(inf.privateKey, password));
			} else pks.put(target.toString(), inf.privateKey);
		}
	}
	
	public void lock(ObjectId target) throws InternalServerException {
		pks.remove(target.toString());
	}
	
	
}
