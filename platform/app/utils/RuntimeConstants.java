package utils;

import models.MidataId;
import models.Plugin;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

public class RuntimeConstants {

	public static RuntimeConstants instance;
	
	public final MidataId portalPlugin = Plugin.getByFilename("portal", Sets.create("_id"))._id;
	public final MidataId commonPlugin = Plugin.getByFilename("common", Sets.create("_id"))._id;
	
	public RuntimeConstants() throws InternalServerException {		
	}
}
