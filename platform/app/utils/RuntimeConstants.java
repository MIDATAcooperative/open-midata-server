package utils;

import models.Admin;
import models.Member;
import models.MidataId;
import models.Plugin;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

public class RuntimeConstants {

	public static RuntimeConstants instance;
	
	public static final String AUTORUN_USERNAME = "autorun-service";
	public static final String BACKEND_SERVICE = "backend-service";
	public static final String PUBLIC_USER = "public-user";
	
	public final MidataId portalPlugin = Plugin.getByFilename("portal", Sets.create("_id"))._id;
	public final MidataId commonPlugin = Plugin.getByFilename("common", Sets.create("_id"))._id;
	public final MidataId autorunService = Admin.getByEmail(AUTORUN_USERNAME, Sets.create("_id"))._id;
	public final MidataId backendService = Admin.getByEmail(BACKEND_SERVICE, Sets.create("_id"))._id;
	public final MidataId publicUser = Member.getByEmail(PUBLIC_USER, Sets.create("_id"))._id;
	
	public RuntimeConstants() throws InternalServerException {		
	}
}
