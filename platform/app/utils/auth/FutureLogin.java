package utils.auth;

import java.util.Set;

import models.JsonSerializable;
import models.MidataId;
import models.Model;
import models.PersistedSession;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

public class FutureLogin extends Model {
	
	private static final String collection = "futurelogins";
	private static final Set<String> ALL = Sets.create("user", "intPart", "extPartEnc");
	
	public MidataId user;
	
	public byte[] intPart;
	
	public byte[] extPartEnc;
	
	public void set() throws InternalServerException {
		Model.upsert(collection, this);
	}
	
	public static FutureLogin getById(MidataId user) throws InternalServerException {
		return Model.get(FutureLogin.class, collection, CMaps.map("_id", user), ALL);
	}
	
	public static void delete(MidataId user) throws InternalServerException {
		Model.delete(PersistedSession.class, collection, CMaps.map("_id", user));
	}		
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);
	}
	
}
