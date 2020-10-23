/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package models;

import utils.exceptions.InternalServerException;

/**
 * Exception that is thrown if the access package tries to use an APS that does not exist.
 *
 */
public class APSNotExistingException extends InternalServerException {
	
	private static final long serialVersionUID = 1L;
		
	private MidataId aps;
	
	public APSNotExistingException(MidataId aps, String msg) {	
		super("error.internal.aps", msg);
		this.aps = aps;		
	}
	
	public MidataId getAps() {
		return aps;
	}
	
	

}
