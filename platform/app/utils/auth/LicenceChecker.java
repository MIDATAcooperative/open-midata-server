package utils.auth;

import java.util.Date;
import java.util.Set;

import models.HPUser;
import models.Licence;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.Space;
import models.UserGroupMember;
import models.enums.ConsentStatus;
import models.enums.EntityType;
import utils.AccessLog;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

public class LicenceChecker {

	public static boolean isValid(MidataId userId, Licence lic) throws InternalServerException {
	  if (lic == null) {
		  AccessLog.log("isValid:no licence");
		  return false;
	  }
	  if (lic.status != ConsentStatus.ACTIVE) {
		  AccessLog.log("isValid:wrong status");
		  return false;
	  }
	  if (lic.expireDate != null && lic.expireDate.before(new Date(System.currentTimeMillis()))) {
		  AccessLog.log("isValid:licence expired");
		  return false;
	  }
	  
	  if (lic.licenseeType == EntityType.USERGROUP) {
		  UserGroupMember ugm = UserGroupMember.getByGroupAndActiveMember(lic.licenseeId, userId);		  
		  if (ugm == null) {
			  AccessLog.log("isValid:no longer part of user group");
			  return false;
		  }
	  }
	  AccessLog.log("isValid:ok");
	  return true;
	}
	
	public static boolean licenceRequired(Plugin plugin) {
		return plugin.licenceDef != null;
	}
	
	public static MidataId hasValidLicence(MidataId userId, Plugin plugin, MidataId licenceId) throws AppException {
		if (plugin.licenceDef == null) return null;
				
		if (licenceId != null) {
		  AccessLog.log("check stored licence id="+licenceId);
		  Licence lic = Licence.getById(licenceId);
		  if (isValid(userId, lic)) return lic._id;
		}
		
		if (plugin.licenceDef.allowedEntities.contains(EntityType.USER)) {
			AccessLog.log("check user licence");
			Licence lic = Licence.getActiveLicenceByLicenseeAndApp(userId, EntityType.USER, plugin._id);
			if (isValid(userId, lic)) return lic._id;
		}
		if (plugin.licenceDef.allowedEntities.contains(EntityType.ORGANIZATION)) {
			AccessLog.log("check org licence");
			HPUser user = HPUser.getById(userId, Sets.create("provider"));
			if (user != null && user.provider != null) {
				Licence lic = Licence.getActiveLicenceByLicenseeAndApp(user.provider, EntityType.ORGANIZATION, plugin._id);
				if (isValid(userId, lic)) return lic._id;
			}
		}
		if (plugin.licenceDef.allowedEntities.contains(EntityType.USERGROUP)) {
			AccessLog.log("check usergroup licence");
			Set<UserGroupMember> ugms = UserGroupMember.getAllActiveByMember(userId);
			for (UserGroupMember ugm : ugms) {
				Licence lic = Licence.getActiveLicenceByLicenseeAndApp(ugm.userGroup, EntityType.USERGROUP, plugin._id);
				if (isValid(userId, lic)) return lic._id;
			}			
		}
		AccessLog.log("no valid licence found user="+userId+" plugin="+plugin._id);
		return null;
	}
	
	public static void checkSpace(MidataId userId, Plugin plugin, Space space) throws AppException {
		if (licenceRequired(plugin)) {
			MidataId lic = hasValidLicence(userId, plugin, space.licence);
			if (lic == null) {
				throw new BadRequestException("error.missing.licence", "No licence or licence expired.");
			}
			if (!lic.equals(space.licence)) {
				space.licence = lic;
				Space.set(space._id, "licence", space.licence);
			}
		}
	}
	
	public static boolean checkAppInstance(MidataId userId, Plugin plugin, MobileAppInstance instance) throws AppException {
		if (licenceRequired(plugin)) {
			MidataId lic = hasValidLicence(userId, plugin, instance!=null ? instance.licence : null);
			if (lic == null) {
				return false;
			}
			if (!lic.equals(instance.licence)) {
				instance.licence = lic;
				MobileAppInstance.set(instance._id, "licence", instance.licence);
			}
		}
		return true;
	}
}
