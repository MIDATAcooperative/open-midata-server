package utils.messaging;

import utils.access.EncryptionUtils;
import utils.exceptions.InternalServerException;
import utils.sync.Instances;

public class ServiceHandler {

	private static byte[] aeskey = null;
	
	public static void setKey(byte[] key) {
		if (aeskey==null) {
			System.out.println("Received background service key");
			aeskey = key;
		}
	}
	
	public static boolean keyAvailable() {
		return aeskey != null;
	}
	
	public static byte[] encrypt(String input) throws InternalServerException {
		if (aeskey == null) throw new InternalServerException("error.internal", "Background service key missing");
		return EncryptionUtils.encrypt(aeskey, input.getBytes());
	}
	
	public static String decrypt(byte[] input) throws InternalServerException {
		if (aeskey == null) throw new InternalServerException("error.internal", "Background service key missing");
		return new String(EncryptionUtils.decrypt(aeskey, input));
	}
	
	public static void startup() {
		if (aeskey == null) {
			utils.sync.Instances.retrieveKey();
		}
	}
	
	public static void shareKey() {
		if (aeskey != null) {
			System.out.println("Sending background service key");
			Instances.sendKey(aeskey);
		}
	}
}
