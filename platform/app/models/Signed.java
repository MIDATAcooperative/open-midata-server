package models;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import utils.RuntimeConstants;
import utils.auth.KeyManager;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;

/**
 * Signature for some data
 *
 */
public class Signed implements JsonSerializable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3336989202585971674L;

	/**
	 * signature
	 */
	public byte[] signature;
	
	/**
	 * timestamp of signature
	 */
	public long signedAt;
	
	/**
	 * who signed this (actor)
	 */
	public MidataId signedBy;
	
	/**
	 * signature is not done using the owners key but is based on another consent
	 */
	public MidataId basedOn;
	
	/**
	 * owner of data for this signature (same as signedBy unless done by representative)
	 */
	public MidataId signedFor;
	
	public Signed() {}
	
	public Signed(MidataId objectId, String message, MidataId signedFor, MidataId signedBy, long date) throws InternalServerException, AuthException {
		createSignature(objectId, message, signedFor, signedBy, null, date);
	}
	
	public Signed(MidataId objectId, String message, MidataId signedFor, MidataId signedBy, MidataId basedOn, long date) throws InternalServerException, AuthException {
		createSignature(objectId, message, signedFor, signedBy, basedOn, date);
	}
	
	/**
	 * create signature
	 * @param objectId to which object does this signature belong
	 * @param message the message to sign
	 * @param user which user signs the message
	 * @param date timestamp for signature
	 * @throws InternalServerException
	 * @throws AuthException
	 */
	public void createSignature(MidataId objectId, String message, MidataId signedFor, MidataId signedBy, MidataId basedOn, long date) throws InternalServerException, AuthException {
		this.signedAt = date;
		this.signedBy = signedBy;
		this.signedFor = signedFor;
		this.basedOn = basedOn;
		byte[] msg = createMessageToSign(objectId, message);
		//byte[] hash = createHash(msg);
		this.signature = KeyManager.instance.encryptHash(basedOn!=null ? basedOn : signedFor, msg);
	}
	
	/**
	 * test the signature
	 * @param objectId to which object does this signature belong
	 * @param message the message that has been signed
	 * @param user the user that signed the message
	 * @param date the timestamp the signature must have
	 * @return true if signature is correct
	 * @throws InternalServerException
	 */
	public boolean check(MidataId objectId, String message, MidataId signedFor, long min, long max) throws InternalServerException {
		if (!this.signedFor.equals(signedFor) && !this.signedFor.equals(RuntimeConstants.systemSignatureUser)) return false;
		if (this.signedAt < min || this.signedAt > max) return false;
		return checkSignature(objectId, message);
	}
	
	private boolean checkSignature(MidataId objectId, String message) throws InternalServerException {
		byte[] msg = createMessageToSign(objectId, message);
		//byte[] hash = createHash(msg);
		return KeyManager.instance.verifyHash(this.basedOn != null ? this.basedOn : this.signedFor, this.signature, msg);
		//return Arrays.equals(hash, compare);
	}
	
	private byte[] createMessageToSign(MidataId objectId, String message) {
		StringBuilder build = new StringBuilder();
		build.append(objectId.toString());
		build.append(this.signedBy.toString());
		build.append(this.signedFor.toString());
		build.append(Long.toHexString(this.signedAt));
		if (this.basedOn != null) build.append(this.basedOn.toString());
		build.append(message.trim());
		try {
			return build.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
		
	}
	
	/*private byte[] createHash(byte[] message) throws InternalServerException {
		try {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		return md.digest(message);
		} catch (NoSuchAlgorithmException e) {
			throw new InternalServerException("error.internal", e);
		}
	}*/
	
}
