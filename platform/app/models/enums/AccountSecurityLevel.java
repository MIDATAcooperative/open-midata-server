package models.enums;

public enum AccountSecurityLevel {
	NONE,            // Account is not protected by keypair
	KEY,             // Account is protected by key in keystore
	KEY_PASSPHRASE,  // Account is protected by key in keystore and passphrase
	KEY_FILE         // Account is protected by externally stored keyfile	
}
