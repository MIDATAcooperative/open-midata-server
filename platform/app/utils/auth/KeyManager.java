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

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bson.BSONObject;

import akka.japi.Pair;
import models.ConsentReshare;
import models.KeyInfo;
import models.KeyInfoExtern;
import models.MidataId;
import models.MobileAppInstance;
import models.PersistedSession;
import models.ServiceInstance;
import models.User;
import models.UserGroup;
import utils.AccessLog;
import utils.access.EncryptionUtils;
import utils.access.RecordManager;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;

/**
 * Manager for the private and public keys of users and external apps
 *
 */
public class KeyManager implements KeySession {

	/**
	 * The one and only KeyManager instance
	 */
	public static KeyManager instance = new KeyManager();
	
	/**
	 * key algorithm used
	 */
	public final static String KEY_ALGORITHM = "RSA";
	
	public final static int KEY_SIZE = 4096;
	
	/**
	 * cipher algorithm used
	 */
	public final static String CIPHERS[] = new String[] { "RSA/ECB/PKCS1Padding", "RSA/ECB/OAEPWithSHA-256AndMGF1Padding" };
	
	public final static String SIGNATURE_ALG[] = new String[] { "SHA256withRSA" };
	
	public final static byte DEFAULT_CIPHER_ALG = 1;
	
	public final static byte DEFAULT_SIGNATURE_ALG = 0;
	
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
	
	/**
	 * private key is protected by AES key
	 */
	public final static int KEYPROTECTION_AESKEY = 3;
	
	/**
	 * private key decryption failure
	 */
	public final static int KEYPROTECTION_FAIL = -1;
		
	
	private Map<String, KeyRing> keySessions = new ConcurrentHashMap<String, KeyRing>();	
	
	private static ThreadLocal<KeyManager.KeyManagerSession> session = new ThreadLocal<KeyManager.KeyManagerSession>();
	
	public KeyManager() {
		new CleanerThread().start();
	}
	
	/**
	 * encrypt a key with the target entities public key and return the encrypted key
	 * @param target id of user or app instance from which the public key should be used
	 * @param keyToEncrypt the key to encrypt
	 * @return encrypted key
	 * @throws EncryptionNotSupportedException
	 * @throws InternalServerException
	 */
	public byte[] encryptKey(MidataId target, byte[] keyToEncrypt, boolean mustExist) throws EncryptionNotSupportedException, InternalServerException {
		byte[] pk = getPublicKey(target, mustExist);
		if (pk == null && !mustExist) return null;
	    return encryptKey(pk , keyToEncrypt);	
	}
	
	private byte[] getPublicKey(MidataId target, boolean mustExist) throws EncryptionNotSupportedException, InternalServerException {
		return session.get().getPublicKey(target, mustExist);
	}
	
	private byte[] putPublicKey(MidataId target, byte[] key) {
		KeyManagerSession currentSession = session.get(); 
		if (currentSession != null) return currentSession.putPublicKey(target, key); else return key;
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
			throw new InternalServerException("error.internal", "Cryptography error");		
		} catch (NoSuchPaddingException e2) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (InvalidKeyException e3) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (InvalidKeySpecException e4) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (BadPaddingException e5) {
			throw new InternalServerException("error.internal", "Cryptography error");
		} catch (IllegalBlockSizeException e6) {
			throw new InternalServerException("error.internal", "Cryptography error");
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
	
	public void backupKeyInAps(AccessContext context, MidataId target) throws AppException {
		KeyManagerSession current = session.get();
		current.backupKeyInAps(context, target);
	}
	
	public boolean recoverKeyFromAps(AccessContext context, MidataId target) throws InternalServerException {
		KeyManagerSession current = session.get();
		return current.recoverKeyFromAps(context, target);
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
		   generator.initialize(KEY_SIZE);
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
		   
		   
		   return putPublicKey(target, pub.getEncoded());
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", "Cryptography error");
		}
	}
	
	public void saveExternalPrivateKey(MidataId user, String pk) throws InternalServerException {
		 KeyInfoExtern keyInfoExtern = new KeyInfoExtern();
		 keyInfoExtern._id = user;
		 keyInfoExtern.privateKey = pk;
		  
		 KeyInfoExtern.update(keyInfoExtern);
		 
		 KeyInfo.delete(user);
	}
	
	public void removeExternalPrivateKey(MidataId user) throws InternalServerException, AuthException {
		KeyManagerSession current = session.get();
		current.removeExternalPrivateKey(user);
	}
	
	public void newFutureLogin(User user) throws AuthException, InternalServerException {
		KeyManagerSession current = session.get();
		current.newFutureLogin(user._id, user.publicExtKey);
	}
	
	public String newAESKey(MidataId executor) throws AuthException, InternalServerException {
		KeyManagerSession current = session.get();
		return current.newAESKey(executor);
	}
	
	public byte[] readExternalPublicKey(String pub) throws InternalServerException {
		
		pub = pub.replaceAll("\\n", "");
		pub = pub.replaceAll("\\r", "");		
		pub = pub.replace("-----BEGIN PUBLIC KEY-----", "");		
		pub = pub.replace("-----END PUBLIC KEY-----", "");
		
		try {
		    KeyFactory kf = KeyFactory.getInstance("RSA");				    
		    X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(pub));
		    RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(keySpecX509);
		    return pubKey.getEncoded();
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", "Internal error");
		}
        catch (InvalidKeySpecException e2) {
        	throw new InternalServerException("error.internal", "Invalid key specification for external public key.");
        }		
	}
	
	public String login(long expire, boolean persistable) {
		AccessLog.log("Key-Ring: new session with duration=", Long.toString(expire));
		String handle;
		String passkey;
		do {
			handle = new BigInteger(130, random).toString(32);
			passkey = persistable ? new BigInteger(130, random).toString(32) : null;
		} while (keySessions.containsKey(handle));
		KeyRing keyring = new KeyRing(System.currentTimeMillis() + expire, passkey);
		keySessions.put(handle, keyring);
		session.set(new KeyManagerSession(handle, keyring));
		return passkey != null ? (handle+";"+passkey) : handle;
	}
	
	public void continueSession(String fhandle) throws AppException {		
		continueSession(fhandle, null);
	}
	
	public void continueSession(String fhandle, MidataId user) throws AppException {		
		//AccessLog.log("Key-Ring: continue session");
		if (fhandle == null) return;
		int p = fhandle.indexOf(";");
		String handle = p > 0 ? fhandle.substring(0, p) : fhandle;
		boolean inlineKey = (p>0 && fhandle.charAt(p+1) == '+');
		KeyRing ring = keySessions.get(handle);
						
		if (ring != null && !inlineKey) {
			session.set(new KeyManagerSession(handle, ring));
			return;
		} else if (p>0) {
			if (inlineKey) {
				if (user != null) {
					handle = new BigInteger(130, random).toString(32);
					byte[] key = Base64.getDecoder().decode(fhandle.substring(p+2));
					KeyRing keyring = new KeyRing(System.currentTimeMillis() + 1000l * 60l *10l, null);
					keySessions.put(handle, keyring);
					session.set(new KeyManagerSession(handle, keyring));
					keyring.addKey(user.toString(), key);
					AccessLog.log("Key-Ring: Adding key for executor:", user.toString());
					return;
				} else {
					AccessLog.log("Key-Ring: Internal Sessiontoken used but no user given.");
				}
			} else {
				PersistedSession psession = PersistedSession.getById(handle);
				if (psession != null && psession.timeout > System.currentTimeMillis()) {
					String passkey = fhandle.substring(p+1);
					KeyRing keyring = new KeyRing(psession.timeout, passkey);
					keySessions.put(handle, keyring);
					session.set(new KeyManagerSession(handle, keyring));
					if (psession.splitkey != null) {
					  keyring.addKey(psession.user.toString(), EncryptionUtils.applyKey(psession.splitkey, passkey));
					  AccessLog.log("Key-Ring: Persisted session for executor:", psession.user.toString());
					} else AccessLog.log("Key-Ring: Keyless session for executor:", psession.user.toString());
					return;
				}
			}
		}			
		throw new AuthException("error.relogin", "Session expired. Please relogin.");
	}
	
	public String currentHandle(MidataId executor) throws AuthException {
		KeyManagerSession current = session.get();
		if (current != null) {
			return current.getPersisted(executor);		
		}
		return null;
	}
	
	public String currentHandleOptional(MidataId executor) throws AuthException {
		KeyManagerSession current = session.get();
		if (current != null) {
			// if (!current.pks.keys.containsKey(executor.toString())) return null;
			return current.handle;		
		}
		return null;
	}
	
	public void logout() {		
		KeyManagerSession current = session.get();
		if (current != null) {
			AccessLog.log("Key-Ring: end session");
			keySessions.remove(current.handle);
			try {
			  PersistedSession.delete(current.handle);
			} catch (AppException e) {}
		} else {
			AccessLog.log("Key-Ring: no session");
		}		
	}	
	
	public void clear() {		
		session.set(null);
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
	 * Deletes a key from the storage
	 * @param target ID of key to delete
	 * @throws InternalServerException
	 */
	public void deleteKey(MidataId target) throws InternalServerException {
		KeyInfo.delete(target);			
	}
	
	private SecureRandom random = new SecureRandom();
	    	  	
	
	
	private byte[] getKey(String handle, MidataId keyId) throws AuthException {
		KeyRing keyRing = keySessions.get(handle);
		if (keyRing == null) return null;
		return keyRing.getKey(keyId.toString());
	}
	
	private void setKey(String handle, MidataId keyId, byte[] key) {
		KeyRing keyRing = keySessions.get(handle);
		//if (keyRing == null) return null;
		keyRing.addKey(keyId.toString(), key);
	}
	
	class KeyManagerSession implements KeySession {
		
		private KeyRing pks;				
		public String handle;	
		private Map<MidataId, byte[]> publicKeyCache = new HashMap<MidataId, byte[]>();
		
		KeyManagerSession(String handle, KeyRing pks) {
			this.handle = handle;		
			this.pks = pks;
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
				
				byte key[] = pks.getKey(target.toString());
							
				if (key == null) {
					AccessLog.log("no key in memory for user=", target.toString());
					throw new AuthException("error.relogin", "Authorization Failure");
				}
				
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
				AccessLog.log("decrypt key error: key user="+target);
				throw new InternalServerException("error.internal", "Cryptography error");		
			} catch (NoSuchPaddingException e2) {
				AccessLog.log("decrypt key error: key user="+target);
				throw new InternalServerException("error.internal", "Cryptography error");
			} catch (InvalidKeyException e3) {
				AccessLog.log("decrypt key error: key user="+target);
				throw new InternalServerException("error.internal", "Cryptography error");
			} catch (InvalidKeySpecException e4) {
				AccessLog.log("decrypt key error: key user="+target);
				throw new InternalServerException("error.internal", "Cryptography error");
			} catch (BadPaddingException e5) {
				AccessLog.log("decrypt key error: key user="+target);
				throw new InternalServerException("error.internal", "Cryptography error");
			} catch (IllegalBlockSizeException e6) {
				AccessLog.log("decrypt key error: key user="+target);
				throw new InternalServerException("error.internal", "Cryptography error");
			} 
		}
		
		private byte[] putPublicKey(MidataId target, byte[] key) {
			publicKeyCache.put(target, key);
			return key;
		}
		
		private byte[] getPublicKey(MidataId target, boolean mustExist) throws EncryptionNotSupportedException, InternalServerException {
			byte[] fromCache = publicKeyCache.get(target);
			if (fromCache != null) return fromCache;
			
			User user = User.getById(target, Sets.create("publicKey"));
			if (user != null) {			
				if (user.publicKey == null) throw new EncryptionNotSupportedException("User has no public key");
				publicKeyCache.put(target, user.publicKey);
				return user.publicKey;
			}
			
			MobileAppInstance mai = MobileAppInstance.getById(target, Sets.create("publicKey"));
			if (mai != null) {
				if (mai.publicKey == null) throw new EncryptionNotSupportedException("No public key");	
				publicKeyCache.put(target, mai.publicKey);
				return mai.publicKey;
			}
			
			UserGroup ug = UserGroup.getById(target, Sets.create("publicKey"));
			if (ug != null) {
				if (ug.publicKey == null) throw new EncryptionNotSupportedException("No public key");
				publicKeyCache.put(target, ug.publicKey);
				return ug.publicKey;
			}

			ServiceInstance si = ServiceInstance.getById(target, Sets.create("publicKey"));
			if (si != null) {
				if (si.publicKey == null) throw new EncryptionNotSupportedException("No public key");	
				publicKeyCache.put(target, si.publicKey);
				return si.publicKey;
			}
			
			ConsentReshare cr = ConsentReshare.getById(target, Sets.create("publicKey"));
			if (cr != null) {
				if (cr.publicKey == null) throw new EncryptionNotSupportedException("No public key");	
				publicKeyCache.put(target, cr.publicKey);
				return cr.publicKey;
			}
			
			if (!mustExist) return null;
			
			throw new EncryptionNotSupportedException("No public key");	

		}
		
		public boolean verifyHash(MidataId target, byte[] encryptedHash, byte[] message) throws InternalServerException {
			byte[] publicKey = getPublicKey(target, true);
			try {						
				X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey);
				
				KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
				PublicKey pubKey = keyFactory.generatePublic(spec);
				 
				byte algorithm = encryptedHash[0];				
				byte[] withoutAlg = new byte[encryptedHash.length-1];						
				System.arraycopy(encryptedHash, 1, withoutAlg, 0, withoutAlg.length);
				
				
				Signature signature = Signature.getInstance(SIGNATURE_ALG[algorithm]);
				signature.initVerify(pubKey);
				signature.update(message);
				
				return signature.verify(withoutAlg);
			} catch (NoSuchAlgorithmException e) {
				throw new InternalServerException("error.internal", "Cryptography error");		
			} catch (SignatureException e2) {
				throw new InternalServerException("error.internal", "Cryptography error");
			} catch (InvalidKeyException e3) {
				throw new InternalServerException("error.internal", "Cryptography error");
			} catch (InvalidKeySpecException e4) {
				throw new InternalServerException("error.internal", "Cryptography error");			
			} 
		}
		
		public byte[] encryptHash(MidataId target, byte[] hash) throws InternalServerException, AuthException {
			try {
				
				byte key[] = pks.getKey(target.toString());
							
				if (key == null) {
					AccessLog.log("no key in memory for user="+target);
					throw new AuthException("error.relogin", "Authorization Failure");
				}
				
				PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
				
				KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
				PrivateKey privKey = keyFactory.generatePrivate(spec);
				
				Signature signature = Signature.getInstance(SIGNATURE_ALG[DEFAULT_SIGNATURE_ALG]);
				signature.initSign(privKey);
								
				signature.update(hash);
				byte[] cipherText = signature.sign();
														
				byte[] result = new byte[cipherText.length+1];
				result[0] = DEFAULT_SIGNATURE_ALG;				
				System.arraycopy(cipherText, 0, result, 1, cipherText.length);
				
				return result;
			} catch (NoSuchAlgorithmException e) {
				AccessLog.log("encryptHash error: key user="+target);
				throw new InternalServerException("error.internal", "Cryptography error");		
			} catch (SignatureException e2) {
				AccessLog.log("encryptHash error: key user="+target);
				throw new InternalServerException("error.internal", "Cryptography error");
			} catch (InvalidKeyException e3) {
				AccessLog.log("encryptHash error: key user="+target);
				throw new InternalServerException("error.internal", "Cryptography error");
			} catch (InvalidKeySpecException e4) {
				AccessLog.log("encryptHash error: key user="+target);
				throw new InternalServerException("error.internal", "Cryptography error");
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
			byte[] oldKey = pks.getKey(target.toString());
			if (oldKey == null) throw new AuthException("error.relogin", "Authorization Failure");		
			KeyInfo keyinfo = new KeyInfo();
			keyinfo._id = target;						
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
				// pks.addKey(target.toString(), null);
				return KEYPROTECTION_FAIL;
			}
			else {
				if (inf.type == KEYPROTECTION_PASSPHRASE) {
					if (passphrase != null) {
	 				   pks.addKey(target.toString(), EncryptionUtils.applyKey(inf.privateKey, passphrase));
					}
					return KEYPROTECTION_PASSPHRASE;
				} if (inf.type == KEYPROTECTION_AESKEY) {
					if (passphrase != null) {
					   byte[] fullkey = Base64.getDecoder().decode(passphrase);
					   if (EncryptionUtils.checkMatch(fullkey, inf.privateKey)) {
					     byte[] aeskey = EncryptionUtils.derandomize(fullkey);			
					     byte[] pk = EncryptionUtils.decrypt(aeskey, EncryptionUtils.derandomize(inf.privateKey));
					     pks.addKey(target.toString(), pk);
					   } else return KEYPROTECTION_FAIL;
					}
					return KEYPROTECTION_AESKEY;
				} else {
					pks.addKey(target.toString(), inf.privateKey);
					return KEYPROTECTION_NONE;
				}
			}
		}
		
		/**
		 * Unlock a user or app instance account
		 * 
		 * The account stays unlocked until lock is called for the account
		 * @param target id of user or app instance to unlock
		 * @param aeskey the passphrase for the private key or null if no passphrase has been applied 
		 * @throws InternalServerException
		 */
		public int unlock(MidataId target, String sessionCode, byte[] pubkey) throws InternalServerException, AuthException {
			
			if (pubkey == null) return unlock(target, sessionCode);
			if (sessionCode == null) return KEYPROTECTION_AESKEY;
			
			FutureLogin fl = FutureLogin.getById(target);
			byte[] aeskey = EncryptionUtils.derandomize(Base64.getDecoder().decode(sessionCode));			
			byte[] pk = EncryptionUtils.decrypt(aeskey, fl.intPart);
			pks.addKey(target.toString(), pk);
			
			newFutureLogin(target, pubkey);
   
			return KEYPROTECTION_NONE;
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
			if (pks.keys.containsKey(target.toString())) return;
			KeyInfo inf = KeyInfo.getById(source);
			if (inf == null) {			
				throw new InternalServerException("error.internal", "Private key info not found.");
			}
			
			if (inf.type != KEYPROTECTION_SPLITKEY) {
				throw new InternalServerException("error.internal", "Private key has wrong type.");
			}
			pks.addKey(target.toString(), EncryptionUtils.joinKey(inf.privateKey, splitkey) );		
		}
		
		/**
		 * Deletes a key from the storage
		 * @param target ID of key to delete
		 * @throws InternalServerException
		 */
		public void deleteKey(MidataId target) throws InternalServerException {
			KeyInfo.delete(target);
			pks.remove(target.toString());
		}
		
		protected void removeExternalPrivateKey(MidataId user) throws InternalServerException, AuthException {
            byte key[] = pks.getKey(user.toString());			
			if (key == null) throw new AuthException("error.relogin", "Authorization Failure");
					
			KeyInfo inf = new KeyInfo();
		    inf._id = user;
		    inf.privateKey = key;
		    inf.type = KEYPROTECTION_NONE;
		    KeyInfo.update(inf);
		    
		    KeyInfoExtern.delete(user);		    
		}
		
		/**
		 * Generates a key duplicate encoded with another key
		 */
		public byte[] generateAlias(MidataId source, MidataId target) throws AuthException, InternalServerException {
			byte key[] = pks.getKey(source.toString());
			
			if (key == null) throw new AuthException("error.relogin", "Authorization Failure");
		
			Pair<byte[], byte[]> split = EncryptionUtils.splitKey(key);
			KeyInfo keyinfo = new KeyInfo();
			keyinfo._id = target;		 
			keyinfo.privateKey = split.second();
			keyinfo.type = KEYPROTECTION_SPLITKEY;		   
			KeyInfo.add(keyinfo);
			
			return split.first();
			
		}
		
		public void backupKeyInAps(AccessContext context, MidataId target) throws AppException {
			RecordManager.instance.setMeta(context, target, "_pk", CMaps.map("key", pks.getKey(target.toString())));
		}
		
		public boolean recoverKeyFromAps(AccessContext context, MidataId target) throws InternalServerException {
			BSONObject o = RecordManager.instance.getMeta(context, target, "_pk");
			if (o != null) {
				byte[] key = (byte[]) o.get("key");
				pks.addKey(target.toString(), key);
				return true;
			}
			return false;
		}
		
		public void newFutureLogin(MidataId user, byte[] pubkey) throws AuthException, InternalServerException {
            byte key[] = pks.getKey(user.toString());			
			if (key == null) throw new AuthException("error.relogin", "Authorization Failure");		
			
			byte[] aesKey = EncryptionUtils.generateKey();			
			FutureLogin futureLogin = new FutureLogin();
			futureLogin._id = user;
			futureLogin.intPart = EncryptionUtils.encrypt(aesKey, key);
			futureLogin.extPartEnc = KeyManager.instance.encryptKey(pubkey, aesKey);            
			futureLogin.set();
		}
		
		public String newAESKey(MidataId user) throws AuthException, InternalServerException {
            byte key[] = pks.getKey(user.toString());			
			if (key == null) throw new AuthException("error.relogin", "Authorization Failure");		
			
			byte[] aesKey = EncryptionUtils.generateKey();
			
			KeyInfo inf = KeyInfo.getById(user);
			if (inf == null) {
				inf = new KeyInfo();
				inf._id = user;				
			}
			inf.privateKey = EncryptionUtils.randomize(EncryptionUtils.encrypt(aesKey, key));
			inf.type = KEYPROTECTION_AESKEY;
			KeyInfo.update(inf);
			
			return Base64.getEncoder().encodeToString(EncryptionUtils.randomizeSameAs(aesKey, inf.privateKey));
		}
		
		/**
		 * Generate a new public/private key pair, protect the private key with a passphrase, store it in db or memory and return the public key.
		 * 
		 * This method will protect the generated private key with a passphrase.
		 * 
		 * @param target id of user or application instance for which this keypair should be generated
		 * @param passphrase passphrase to apply the the private key
		 * @param inMemory if true the private key will only be kept in memory and not be stored to the database
		 * @return public key
		 * @throws InternalServerException
		 */
		public byte[] generateKeypairAndReturnPublicKeyInMemory(MidataId target, String passphrase) throws InternalServerException {
			try {
			   KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
			   generator.initialize(KEY_SIZE);
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
			   
			   pks.addKey(target.toString(), keyinfo.privateKey); 			   
			   
			   return putPublicKey(target, pub.getEncoded());
			} catch (NoSuchAlgorithmException e) {
				throw new InternalServerException("error.internal", "Cryptography error");
			}
		}

		@Override
		public void persist(MidataId target) throws AppException {
			byte key[] = pks.getKey(target.toString());		
			//if (key == null) throw new AuthException("error.relogin", "Authorization Failure");
		
			if (pks.passkey != null) {
											
				PersistedSession session = new PersistedSession();
				session.set_id(this.handle);				
				session.timeout = pks.expire;
				session.user = target;
				
				if (key != null) {
				  byte[] split = EncryptionUtils.applyKey(key, pks.passkey);
				  session.splitkey = split;
				}
				
				session.add();				
			} 
		}
		
		public String getPersisted(MidataId executor) throws AuthException {
			byte key[] = pks.getKey(executor.toString());		
			if (key == null) throw new AuthException("error.relogin", "Authorization Failure");
		
			return handle+";+"+Base64.getEncoder().encodeToString(key);			
		}
	}
	
	class KeyRing {
		private Map<String, byte[]> keys; 
		private long expire;
		private String passkey;
		
		KeyRing(long expire, String passkey) {
			this.keys = new ConcurrentHashMap<String, byte[]>(8, 0.9f, 1);
			this.expire = expire;
			this.passkey = passkey;
		}
		
		byte[] getKey(String name) {
			return keys.get(name);
		}
		
		void addKey(String name, byte[] key) {
			keys.put(name,  key);
		}
		
		void remove(String name) {
			keys.remove(name);
		}
		
		long getExpires() {
			return expire;
		}
	}
	
	class CleanerThread extends Thread {
        @Override
        public void run() {        	
        	try {
	            while (true) {
	                cleanMap();               
	                Thread.sleep(10000);                
	            }
        	} catch (InterruptedException e) {                
            }        	
        }

        private void cleanMap() {        	
            long currentTime = System.currentTimeMillis();
            Iterator<Entry<String, KeyRing>> it = keySessions.entrySet().iterator();
            while (it.hasNext()) {
            	Entry<String, KeyRing> entry = it.next();
            	if (entry.getValue().expire < currentTime) it.remove();
            }
        }
    }

	@Override
	public byte[] decryptKey(MidataId target, byte[] keyToDecrypt) throws InternalServerException, AuthException {
		return session.get().decryptKey(target, keyToDecrypt);
	}
	
    public byte[] encryptHash(MidataId target, byte[] hash) throws InternalServerException, AuthException {
    	return session.get().encryptHash(target, hash);
    }
	
	public boolean verifyHash(MidataId target, byte[] encryptedHash, byte[] msg) throws InternalServerException {
		return session.get().verifyHash(target, encryptedHash, msg);
	}
	

	@Override
	public void changePassphrase(MidataId target, String newPassphrase) throws InternalServerException, AuthException {
		session.get().changePassphrase(target, newPassphrase);		
	}

	@Override
	public int unlock(MidataId target, String passphrase) throws InternalServerException {
		return session.get().unlock(target, passphrase);		
	}
	
	@Override
	public int unlock(MidataId target, String sessionCode, byte[] pubkey) throws InternalServerException, AuthException {
		return session.get().unlock(target, sessionCode, pubkey);		
	}

	@Override
	public void unlock(MidataId target, MidataId source, byte[] splitkey) throws InternalServerException {
		session.get().unlock(target, source, splitkey);		
	}

	@Override
	public byte[] generateAlias(MidataId source, MidataId target) throws AuthException, InternalServerException {
		return session.get().generateAlias(source, target);		
	}

	@Override
	public byte[] generateKeypairAndReturnPublicKeyInMemory(MidataId target, String passphrase) throws InternalServerException {
		return session.get().generateKeypairAndReturnPublicKeyInMemory(target, passphrase);
	}

	@Override
	public void persist(MidataId target) throws AppException {
		session.get().persist(target);		
	}
}
