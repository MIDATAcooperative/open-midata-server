package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class KeyRecoveryProcess extends Model {

    protected @NotMaterialized static final String collection = "keyprocess";
    
    public @NotMaterialized static final Set<String> ALL = Sets.create("name", "started", "shares", "nextPassword", "nextPk", "encShares", "nextShares", "intPart", "challenge", "nextPublicExtKey", "ready");
	
    /**
     * Name of user
     */
    public String name;
    
    /**
     * Date password recovery started
     */
    public Date started;
    
    /**
     * Next external public key to be used
     */
    public byte[] nextPublicExtKey;
    
    /**
     * Next private key (encrypted with new password)
     */
    public String nextPk;
    
    /**
     * Next password (hash of hash)
     */
    public String nextPassword;
    
    /**
     * Ready for recovery
     */
    public boolean ready;
    
    /**
     * Decrypted recovery shares
     */
	public Map<String, String> shares;
	
	public Map<String, String> encShares;
	
	public Map<String, String> nextShares;
	
    public byte[] intPart;
	
	public String challenge;
	
	public static Set<KeyRecoveryProcess> getUnfinished() throws InternalServerException {
		return Model.getAll(KeyRecoveryProcess.class, collection, CMaps.map("ready", false), ALL, 1000);
	}
	
	public static long count() throws InternalServerException {
		return Model.count(KeyRecoveryProcess.class, collection, CMaps.map("ready", false));
	}
	
	public static KeyRecoveryProcess getById(MidataId id) throws InternalServerException {
		return Model.get(KeyRecoveryProcess.class, collection, CMaps.map("_id", id), ALL);
	}
	
	public static void add(KeyRecoveryProcess keyinfo) throws InternalServerException {
		Model.insert(collection, keyinfo);
	}
	
	public static void update(KeyRecoveryProcess keyinfo) throws InternalServerException {
		Model.upsert(collection, keyinfo);
	}
	
	public static void delete(MidataId id) throws InternalServerException {
		Model.delete(KeyRecoveryProcess.class, collection, CMaps.map("_id", id));
	}
}
