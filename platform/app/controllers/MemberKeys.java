package controllers;

import java.util.HashSet;

import models.HPUser;
import models.HealthcareProvider;
import models.Member;
import models.MemberKey;
import models.ModelException;
import models.User;
import models.enums.MemberKeyStatus;

import org.bson.types.ObjectId;

public class MemberKeys {

	public static ObjectId getOrCreate(HPUser hpuser, Member member) throws ModelException {
		MemberKey key = MemberKey.getByOwnerAndAuthorizedPerson(member._id, hpuser._id);
		if (key!=null) return key.aps;
		
		HealthcareProvider prov = HealthcareProvider.getById(hpuser.provider);
		
		key = new MemberKey();
		key._id = new ObjectId();
		key.owner = member._id;
		key.organization = hpuser.provider;
		key.authorized = new HashSet<ObjectId>();
		key.authorized.add(hpuser._id);
		key.status = MemberKeyStatus.UNCONFIRMED;
		key.name = prov.name+": "+hpuser.firstname+" "+hpuser.sirname;
		key.aps = RecordSharing.instance.createAnonymizedAPS(member._id, hpuser._id, key._id);
		MemberKey.add(key);
		
		return key.aps;
		
	}

	
}
