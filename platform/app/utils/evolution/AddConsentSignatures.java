package utils.evolution;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import models.Admin;
import models.Consent;
import models.MidataId;
import models.enums.AccountSecurityLevel;
import models.enums.ConsentStatus;
import models.enums.ContractStatus;
import models.enums.EMailStatus;
import models.enums.Gender;
import models.enums.SecondaryAuthType;
import models.enums.SubUserRole;
import models.enums.UserRole;
import models.enums.UserStatus;
import models.enums.WritePermissionType;
import utils.ConsentQueryTools;
import utils.RuntimeConstants;
import utils.auth.KeyManager;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class AddConsentSignatures {

	public static void execute() throws AppException {
		
		Admin admin = new Admin();
		admin._id = RuntimeConstants.systemSignatureUser;
		admin.email = "consent-signature-patch";
		admin.emailLC = "consent-signature-patch";
		admin.password = null;
		admin.role = UserRole.ADMIN;
		admin.subroles = EnumSet.noneOf(SubUserRole.class);
		admin.status = UserStatus.BLOCKED;
		admin.contractStatus = ContractStatus.SIGNED;
		admin.agbStatus = ContractStatus.SIGNED;	
		admin.registeredAt = new Date();
		admin.resettokenTs = 0;				
		admin.confirmationCode = "";
		admin.firstname = "Consent Signature Patch";
		admin.lastname = "Service";
		admin.gender = Gender.OTHER;
		admin.security = AccountSecurityLevel.KEY;
		
		admin.emailStatus = EMailStatus.VALIDATED;
		admin.authType = SecondaryAuthType.NONE;
		KeyManager.instance.login(1000l*60l*60l, false);
		admin.publicKey = KeyManager.instance.generateKeypairAndReturnPublicKeyInMemory(RuntimeConstants.systemSignatureUser, null);
										
		List<Consent> consents = null;
		MidataId min = null;
		do {
			consents = Consent.getSome(min);
			System.out.println("min="+min+" size="+consents.size());
			for (Consent consent : consents) {
				min = consent._id;
				if (consent.dateOfCreation == null) {
					consent.dateOfCreation = consent._id.getCreationDate();
					Consent.set(consent._id, "dateOfCreation", consent.dateOfCreation);
				}
				if (consent.lastUpdated == null) consent.lastUpdated = new Date();
				if (consent.writes==null) {
					System.out.println("NOWRITE id="+consent._id+" TYPE="+consent.type);
					consent.writes = WritePermissionType.UPDATE_AND_CREATE;
				}
				if (consent.status == ConsentStatus.ACTIVE) {
					
					Map<String, Object> query = ConsentQueryTools.getSharingQuery(consent, false);
					if (query == null) {
						switch (consent.type) {
						case EXTERNALSERVICE:
							consent.status = ConsentStatus.DELETED;
							query = ConsentQueryTools.getEmptyQuery();
							break;
						case CIRCLE:
						case HEALTHCARE:
						case HCRELATED:
							query = ConsentQueryTools.getEmptyQuery();
							break;
						default:
							System.out.println(consent.type + " " + consent._id + " QUERY NULL");
							query = ConsentQueryTools.getEmptyQuery();
						}						

					}
					if (consent.sharingQuery == null) consent.sharingQuery = query;
				} else {
					Map<String, Object> query = ConsentQueryTools.getSharingQuery(consent, false);
					if (query == null) consent.sharingQuery = ConsentQueryTools.getEmptyQuery();
				}
				ConsentQueryTools.temporarySignature(consent);
				consent.updateMetadata();
			}

		} while (!consents.isEmpty());
		
		Admin.add(admin);
	}
}
