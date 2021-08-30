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

package utils.auth;

import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;
import utils.exceptions.AppException;

public class PreLoginSecured extends Security.Authenticator {

	public final static long MAX_SESSION = 1000 * 60 * 60 * 8;
	
	@Override
	public String getUsername(Context ctx) {
	    PortalSessionToken tk = PortalSessionToken.decrypt(ctx.request());
	    if (tk == null) return null;
	    try {
	      if (tk.getHandle()!=null) KeyManager.instance.continueSession(tk.getHandle());
	    } catch (AppException e) { return null; }	    
	   
		// id is the user id in String form
		return tk.getOwnerId().toString();
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return unauthorized();
	}

}
