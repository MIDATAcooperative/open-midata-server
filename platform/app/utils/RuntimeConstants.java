/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils;

import models.Admin;
import models.Member;
import models.MidataId;
import models.Plugin;
import models.UserGroup;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

public class RuntimeConstants {

	public static RuntimeConstants instance;
	
	public static final String AUTORUN_USERNAME = "autorun-service";
	public static final String BACKEND_SERVICE = "backend-service";
	public static final String PUBLIC_USER = "public-user";
	public static final String PUBLIC_GROUP = "public-group";
	
	public final MidataId portalPlugin = Plugin.getByFilename("portal", Sets.create("_id"))._id;
	public final MidataId commonPlugin = Plugin.getByFilename("common", Sets.create("_id"))._id;
	public final MidataId autojoinerPlugin = Plugin.getByFilename("midata_autoapprover", Sets.create("_id"))._id;
	public final MidataId autorunService = Admin.getByEmail(AUTORUN_USERNAME, Sets.create("_id"))._id;
	public final MidataId backendService = Admin.getByEmail(BACKEND_SERVICE, Sets.create("_id"))._id;
	public final MidataId publicUser = Member.getByEmail(PUBLIC_USER, Sets.create("_id"))._id;
	public static final MidataId publicGroup = new MidataId("5ccab0dcaed6452048f2b011");	
	
	public RuntimeConstants() throws InternalServerException {		
	}
}
