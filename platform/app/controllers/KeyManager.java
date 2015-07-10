package controllers;

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
import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import utils.auth.CodeGenerator;
import utils.auth.EncryptionNotSupportedException;
import utils.collections.Sets;


public class KeyManager {

	public static KeyManager instance = new KeyManager();
	
	public final static String KEY_ALGORITHM = "RSA";
	public final static String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
	
	private Map<String, byte[]> pks = new HashMap<String, byte[]>();
	
	public byte[] encryptKey(ObjectId target, byte[] keyToEncrypt) throws EncryptionNotSupportedException, ModelException {
		try {
			User user = User.getById(target, Sets.create("publicKey"));
			
			if (user.publicKey == null) throw new EncryptionNotSupportedException();
			
			X509EncodedKeySpec spec = new X509EncodedKeySpec(user.publicKey);
			
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			PublicKey pubKey = keyFactory.generatePublic(spec);
			 
			Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
			c.init(Cipher.ENCRYPT_MODE, pubKey);
		    
			byte[] cipherText = c.doFinal(CodeGenerator.randomize(keyToEncrypt));
			//c.doFinal(keyToEncrypt, 0, keyToEncrypt.length, cipherText);
			
			return cipherText;
		} catch (NoSuchAlgorithmException e) {
			throw new ModelException(e);		
		} catch (NoSuchPaddingException e2) {
			throw new ModelException(e2);
		} catch (InvalidKeyException e3) {
			throw new ModelException(e3);
		} catch (InvalidKeySpecException e4) {
			throw new ModelException(e4);
		} catch (BadPaddingException e5) {
			throw new ModelException(e5);
		} catch (IllegalBlockSizeException e6) {
			throw new ModelException(e6);
		} 
	}
	
	public byte[] decryptKey(ObjectId target, byte[] keyToDecrypt) throws ModelException {
		try {
			
			//byte key[] = pks.get(target.toString());
			
			KeyInfo inf = KeyInfo.getById(target);
			byte key[] = inf.privateKey;
			
			if (key == null) throw new ModelException("Authorization Failure");
			
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
			
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			PrivateKey privKey = keyFactory.generatePrivate(spec);
			 
			Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
			c.init(Cipher.DECRYPT_MODE, privKey);
		    
			byte[] cipherText = c.doFinal(keyToDecrypt);
						
			return CodeGenerator.derandomize(cipherText);
		} catch (NoSuchAlgorithmException e) {
			throw new ModelException(e);		
		} catch (NoSuchPaddingException e2) {
			throw new ModelException(e2);
		} catch (InvalidKeyException e3) {
			throw new ModelException(e3);
		} catch (InvalidKeySpecException e4) {
			throw new ModelException(e4);
		} catch (BadPaddingException e5) {
			throw new ModelException(e5);
		} catch (IllegalBlockSizeException e6) {
			throw new ModelException(e6);
		} 
	}
	
	public byte[] generateKeypairAndReturnPublicKey(ObjectId target) throws ModelException {
		try {
		   KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
		   
		   KeyPair pair = generator.generateKeyPair();
		   PublicKey pub = pair.getPublic();
		   PrivateKey priv = pair.getPrivate();
		   
		   KeyInfo keyinfo = new KeyInfo();
		   keyinfo._id = target;
		   keyinfo.privateKey = priv.getEncoded();
		   KeyInfo.add(keyinfo);
		   
		   return pub.getEncoded();
		} catch (NoSuchAlgorithmException e) {
			throw new ModelException(e);
		}
	}
	
	public void unlock(ObjectId target, String password) throws ModelException {
		KeyInfo inf = KeyInfo.getById(target);
		if (inf == null) pks.put(target.toString(), null);
		else pks.put(target.toString(), inf.privateKey);
	}
	
	public void lock(ObjectId target) throws ModelException {
		pks.remove(target.toString());
	}
	
	
}
