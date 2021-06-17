package utils.access;

import java.util.Collections;

import models.Consent;
import models.MidataId;
import utils.exceptions.AppException;

public class PasscodeAccessContext extends ConsentAccessContext {
	
	//MidataId passcodeId;
	
	public PasscodeAccessContext(AccessContext parent, Consent consent, MidataId passcodeId) throws AppException {
		super(consent, parent);
		APSCache temp = new APSCache(passcodeId, parent.getCache().getAccountOwner());
		temp.getAPS(consent._id, consent.owner).addAccess(Collections.singleton(parent.getAccessor()));
		temp.finishTouch();
		parent.getCache().getAPS(consent._id, consent.owner);
		//this.passcodeId = passcodeId;		
	}
	
	
}
