package utils.messaging;

import utils.AccessLog;
import utils.InstanceConfig;
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
		if (aeskey == null) {
			if (InstanceConfig.getInstance().getInstanceType().disableServiceKeyProtection()) {
				aeskey = new byte[16+4];
			} else throw new InternalServerException("error.internal", "Background service key missing");
		}
		return EncryptionUtils.randomizeSameAs(EncryptionUtils.encrypt(EncryptionUtils.derandomize(aeskey), input.getBytes()), aeskey);
	}
	
	public static String decrypt(byte[] input) throws InternalServerException {
		if (input == null) return null;
		if (aeskey == null) {
			if (InstanceConfig.getInstance().getInstanceType().disableServiceKeyProtection()) {
				aeskey = new byte[16+4];
			} else throw new InternalServerException("error.internal", "Background service key missing");			
		}		
		if (!EncryptionUtils.checkMatch(aeskey,  input)) {
			AccessLog.log("Other version of background service key used!");
			return null; 
		}
		return new String(EncryptionUtils.decrypt(EncryptionUtils.derandomize(aeskey), EncryptionUtils.derandomize(input)));
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
