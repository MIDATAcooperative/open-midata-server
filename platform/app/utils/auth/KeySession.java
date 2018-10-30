package utils.auth;

import models.MidataId;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;

public interface KeySession {
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
	public byte[] decryptKey(MidataId target, byte[] keyToDecrypt) throws InternalServerException, AuthException;
	
	/**
	 * Sets a new passphrase for a private key
	 * @param target id of key to be changed
	 * @param newPassphrase new passphrase to be applied
	 * @throws InternalServerException
	 * @throws AuthException
	 */
	public void changePassphrase(MidataId target, String newPassphrase) throws InternalServerException, AuthException;
	
	/**
	 * Unlock a user or app instance account
	 * 
	 * The account stays unlocked until lock is called for the account
	 * @param target id of user or app instance to unlock
	 * @param passphrase the passphrase for the private key or null if no passphrase has been applied 
	 * @throws InternalServerException
	 */
	public int unlock(MidataId target, String passphrase) throws InternalServerException;
	
	/**
	 * Unlock a user account using an alias and split key
	 * 
	 * The account stays unlocked until lock is called for the account
	 * @param target id of user or app instance to unlock
	 * @param source id of alias
	 * @param splitkey keyfragment used to unlock the account 
	 * @throws InternalServerException
	 */
	public void unlock(MidataId target, MidataId source, byte[] splitkey) throws InternalServerException;

	/**
	 * Unlock a user account with a session challenge response
	 * @param target
	 * @param sessionCode
	 * @param pubkey
	 * @throws InternalServerException
	 * @throws AuthException
	 */
	void unlock(MidataId target, String sessionCode, byte[] pubkey) throws InternalServerException, AuthException;

	
	/**
	 * Persist a session with an unlocked key across instances
	 * @param target
	 * @throws InternalServerException
	 */
	public void persist(MidataId target) throws AppException;
	
	/**
	 * Deletes a key from the storage
	 * @param target ID of key to delete
	 * @throws InternalServerException
	 */
	public void deleteKey(MidataId target) throws InternalServerException;
	
	/**
	 * Generates a key duplicate encoded with another key
	 */
	public byte[] generateAlias(MidataId source, MidataId target) throws AuthException, InternalServerException;
	
	/**
	 * Generate a new public/private key pair, protect the private key with a passphrase, store it in memory and return the public key.
	 * 
	 * This method will protect the generated private key with a passphrase.
	 * 
	 * @param target id of user or application instance for which this keypair should be generated
	 * @param passphrase passphrase to apply the the private key
	 * @return public key
	 * @throws InternalServerException
	 */
	public byte[] generateKeypairAndReturnPublicKeyInMemory(MidataId target, String passphrase) throws InternalServerException;

	
}
