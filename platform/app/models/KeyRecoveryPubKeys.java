package models;

import java.util.Map;

public class KeyRecoveryPubKeys {

	public Map<String, String> publicKeys;
	
	public static KeyRecoveryPubKeys getKeys() {
		KeyRecoveryPubKeys result = new KeyRecoveryPubKeys();
		
		return result;
	}
}
