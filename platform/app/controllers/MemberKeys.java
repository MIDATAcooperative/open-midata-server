package controllers;

import models.HPUser;
import models.Member;
import models.MemberKey;
import models.ModelException;
import models.User;
import models.enums.MemberKeyStatus;

import org.bson.types.ObjectId;

public class MemberKeys {

	public static ObjectId getOrCreate(HPUser hpuser, Member member) throws ModelException {
		MemberKey key = MemberKey.getByMemberAndProvider(member._id, hpuser._id);
		if (key!=null) return key.aps;
		
		key = new MemberKey();
		key._id = new ObjectId();
		key.member = member._id;
		key.provider = hpuser._id;
		key.status = MemberKeyStatus.UNCONFIRMED;
		key.aps = RecordSharing.instance.createAnonymizedAPS(member._id, hpuser._id, key._id);
		MemberKey.add(key);
		
		return key.aps;
		
	}

	
}
